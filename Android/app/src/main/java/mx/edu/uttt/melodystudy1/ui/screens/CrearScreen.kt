package mx.edu.uttt.melodystudy1.ui.screens

import android.media.MediaPlayer
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.*
import mx.edu.uttt.melodystudy1.model.PreguntaUI
import mx.edu.uttt.melodystudy1.model.TemaGuardado
import mx.edu.uttt.melodystudy1.network.Constants
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.ui.platform.LocalContext

// ── Colores ───────────────────────────────────────────────────────────────────
private val BgColor      = Color(0xFFF7F5FF)
private val CardColor    = Color(0xFFFFFFFF)
private val PrimaryClr   = Color(0xFF7C5CBF)
private val PrimaryLight = Color(0xFFEDE8FF)
private val InkColor     = Color(0xFF1A1825)
private val InkLight     = Color(0xFF7B7490)
private val SuccessClr   = Color(0xFF4CAF50)
private val ErrorClr     = Color(0xFFC62828)

private val GENEROS = listOf(
    "Pop"     to "🎤",
    "Rock"    to "🎸",
    "Balada"  to "🎹",
    "Clásica" to "🎻"
)

// ── Etapas de progreso con sus pesos (deben sumar 100) ───────────────────────
private data class Etapa(val label: String, val emoji: String, val peso: Int)
private val ETAPAS = listOf(
    Etapa("Generando letra",        "✍️",  15),
    Etapa("Sintetizando voz",       "🎙️", 30),
    Etapa("Convirtiendo con RVC",   "🎤",  20),
    Etapa("Generando música",       "🎵",  25),
    Etapa("Mezclando pistas",       "🎚️", 10)
)

@Composable
fun CrearScreen(
    idUsuario: Int = 0,
    onGuardar: (TemaGuardado) -> Unit = {}
) {
    // ── Estado principal ──────────────────────────────────────────────
    var inputText          by remember { mutableStateOf("") }
    var temaActual         by remember { mutableStateOf("") }
    var letraCancion       by remember { mutableStateOf("") }
    var audioUrl           by remember { mutableStateOf("") }
    // CAMBIO 1: memId (Long) → audioKey (String UUID)
    var audioKey           by remember { mutableStateOf("") }
    var generoSeleccionado by remember { mutableStateOf("Pop") }

    // ── Estado de carga ───────────────────────────────────────────────
    var isLoading          by remember { mutableStateOf(false) }
    var progresoReal       by remember { mutableStateOf(0f) }
    var etapaActual        by remember { mutableStateOf(0) }
    var etapaLabel         by remember { mutableStateOf("") }

    // ── Estado de resultado / guardado ────────────────────────────────
    var hasResult          by remember { mutableStateOf(false) }
    var isGuardando        by remember { mutableStateOf(false) }
    var guardado           by remember { mutableStateOf(false) }
    var errorMsg           by remember { mutableStateOf("") }
    var isPlaying          by remember { mutableStateOf(false) }

    // ── TXT ───────────────────────────────────────────────────────────
    var txtCargado         by remember { mutableStateOf(false) }
    var analizandoTxt      by remember { mutableStateOf(false) }
    var nombreArchivo      by remember { mutableStateOf("") }

    val mediaPlayer = remember { mutableStateOf<MediaPlayer?>(null) }
    val scope       = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val context     = LocalContext.current

    val animProgreso by animateFloatAsState(
        targetValue    = progresoReal,
        animationSpec  = tween(durationMillis = 600, easing = EaseInOutCubic),
        label          = "progreso"
    )

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer.value?.release()
            mediaPlayer.value = null
        }
    }

    // ── Selector de TXT ──────────────────────────────────────────────
    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.data
            if (uri != null) {
                val contenido = try {
                    context.contentResolver.openInputStream(uri)
                        ?.bufferedReader()?.use { it.readText() } ?: ""
                } catch (e: Exception) { "" }
                nombreArchivo = uri.lastPathSegment ?: "archivo.txt"
                if (contenido.isNotBlank()) {
                    scope.launch {
                        analizandoTxt = true
                        errorMsg      = ""
                        try {
                            val (temaExtraido, resumen) = analizarTxt(contenido)
                            inputText  = "Genera una canción educativa sobre $temaExtraido. " +
                                    "Usa esta información: $resumen"
                            txtCargado = true
                        } catch (e: Exception) {
                            errorMsg = "No se pudo analizar el archivo: ${e.message}"
                        } finally {
                            analizandoTxt = false
                        }
                    }
                }
            }
        }
    }

    // ── Helpers de red ────────────────────────────────────────────────

    // CAMBIO 2: retorna Triple<String, String, String> (audioKey UUID, letra, audioUrl)
    suspend fun generarSoloLetra(tema: String, genero: String): Triple<String, String, String> =
        withContext(Dispatchers.IO) {
            val url  = URL(Constants.GENERATE_SONG)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                doOutput       = true
                connectTimeout = 10_000
                readTimeout    = 90_000
            }
            val bodyJson = JSONObject().apply {
                put("tema",   tema)
                put("genero", genero.lowercase())
            }.toString()
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(bodyJson) }
            val code = conn.responseCode
            if (code != 200) throw Exception("Error del servidor: $code")
            val body = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
            val json = JSONObject(body)
            // CAMBIO 2: leer "audioKey" (UUID String) en lugar de "id" (Long)
            Triple(
                json.getString("audioKey"),
                json.getString("letra"),
                json.getString("audioUrl")
            )
        }

    // CAMBIO 3: recibe String en lugar de Long; sin parámetro timestamp
    suspend fun esperarAudio(audioKey: String, timeoutMs: Long = 300_000L): Boolean =
        withContext(Dispatchers.IO) {
            val inicio = System.currentTimeMillis()
            while (System.currentTimeMillis() - inicio < timeoutMs) {
                try {
                    val url  = URL("${Constants.BASE_URL}/audio/temp/$audioKey")
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod  = "GET"
                        connectTimeout = 5_000
                        readTimeout    = 5_000
                    }
                    val code = conn.responseCode
                    conn.disconnect()
                    if (code == 200) return@withContext true
                } catch (_: Exception) {}
                delay(3_000)
            }
            false
        }

    // CAMBIO 4: parámetro audioKey: String y envía "audio_key" en el body
    suspend fun guardarCancion(
        tema: String, genero: String, letra: String,
        audioKey: String
    ): Pair<Long, List<PreguntaUI>> = withContext(Dispatchers.IO) {
        val url  = URL(Constants.SAVE_SONG)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            doOutput       = true
            connectTimeout = 10_000
            readTimeout    = 120_000
        }
        val bodyJson = JSONObject().apply {
            put("id_usuario", idUsuario)
            put("audio_key",  audioKey)   // CAMBIO 4: "audio_key" UUID en lugar de "temp_id"
            put("tema",       tema)
            put("genero",     genero.lowercase())
            put("letra",      letra)
        }.toString()
        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(bodyJson) }
        val code = conn.responseCode
        if (code != 200) throw Exception("Error al guardar: $code")
        val body = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
        val json = JSONObject(body)
        val dbId      = json.getLong("dbId")
        val pregArray = json.getJSONArray("preguntas")
        val preguntas = List(pregArray.length()) { i ->
            val obj      = pregArray.getJSONObject(i)
            val opciones = obj.getJSONArray("opciones")
            PreguntaUI(
                texto    = obj.getString("pregunta"),
                opciones = List(opciones.length()) { j -> opciones.getString(j) },
                correcta = obj.getInt("correcta")
            )
        }
        Pair(dbId, preguntas)
    }

    // ── Lógica principal de envío ─────────────────────────────────────
    // CAMBIO 5: resetear audioKey (String) en lugar de memId (Long)
    fun enviar() {
        if (inputText.isBlank() || isLoading) return

        val temaCapturado = inputText.trim()
        inputText    = ""
        txtCargado   = false
        temaActual   = temaCapturado
        guardado     = false
        errorMsg     = ""
        audioKey     = ""   // CAMBIO 5: reset UUID
        hasResult    = false
        letraCancion = ""
        audioUrl     = ""

        mediaPlayer.value?.stop()
        mediaPlayer.value?.release()
        mediaPlayer.value = null
        isPlaying = false

        scope.launch {
            isLoading    = true
            progresoReal = 0f
            etapaActual  = 0
            etapaLabel   = ETAPAS[0].label

            try {
                etapaActual  = 0
                etapaLabel   = ETAPAS[0].label
                progresoReal = 0.05f

                val (key, letra, tempAudioUrl) = generarSoloLetra(temaCapturado, generoSeleccionado)
                audioKey     = key   // CAMBIO 5: guardar UUID
                letraCancion = letra
                audioUrl     = tempAudioUrl
                progresoReal = 0.15f

                var etapaIdx   = 1
                var acumulado  = 0.15f
                var audioListo = false

                // CAMBIO 5: polling con UUID String, sin timestamp
                val pollingJob = launch(Dispatchers.IO) {
                    audioListo = esperarAudio(audioKey)
                }

                val etapasRestantes  = ETAPAS.drop(1)
                val tiemposEstimados = listOf(12_000L, 8_000L, 10_000L, 4_000L)

                for ((etapa, tiempoMs) in etapasRestantes.zip(tiemposEstimados)) {
                    if (audioListo) break
                    etapaLabel   = etapa.label
                    etapaActual  = etapaIdx
                    val pesoFrac = etapa.peso / 100f
                    val pasos    = 20
                    val delayMs  = tiempoMs / pasos
                    repeat(pasos) {
                        if (!audioListo) {
                            acumulado    += pesoFrac / pasos
                            progresoReal  = acumulado.coerceAtMost(0.92f)
                            delay(delayMs)
                        }
                    }
                    etapaIdx++
                }

                pollingJob.join()

                if (!audioListo) throw Exception("El servidor tardó demasiado generando el audio")

                progresoReal = 1f
                etapaLabel   = "¡Canción lista!"
                delay(400)

                hasResult = true
                scrollState.animateScrollTo(scrollState.maxValue)

            } catch (e: Exception) {
                errorMsg = "Error: ${e.message}"
            } finally {
                isLoading    = false
                progresoReal = 0f
            }
        }
    }

    // ── Reproducción ──────────────────────────────────────────────────
    fun escuchar() {
        if (audioUrl.isBlank()) return
        mediaPlayer.value?.let { mp ->
            if (mp.isPlaying) { mp.pause(); isPlaying = false }
            else              { mp.start(); isPlaying = true  }
            return
        }
        val mp = MediaPlayer()
        mp.setDataSource(audioUrl)
        @Suppress("DEPRECATION")
        mp.setAudioStreamType(android.media.AudioManager.STREAM_MUSIC)
        mp.setOnPreparedListener  { it.start(); isPlaying = true }
        mp.setOnCompletionListener { isPlaying = false }
        mp.setOnErrorListener { player, what, extra ->
            errorMsg  = "Error de audio ($what, $extra)"
            isPlaying = false
            player.reset(); player.release()
            mediaPlayer.value = null
            true
        }
        mediaPlayer.value = mp
        mp.prepareAsync()
    }

    // ═══════════════════════════════════════════════════════════════════
    // UI
    // ═══════════════════════════════════════════════════════════════════
    Box(modifier = Modifier.fillMaxSize().background(BgColor)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color(0xFF7C5CBF), Color(0xFF9B7FD4))))
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) { Text("🎵", fontSize = 20.sp) }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("MelodyStudy", fontSize = 18.sp,
                            fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Generador de canciones educativas",
                            fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                }
            }

            // ── Selector de género ────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(Modifier.height(20.dp))

                Text("Género musical", fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold, color = InkLight)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GENEROS.forEach { (nombre, emoji) ->
                        val sel = generoSeleccionado == nombre
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (sel) PrimaryClr else PrimaryLight)
                                .clickable(enabled = !isLoading) { generoSeleccionado = nombre }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(emoji, fontSize = 18.sp)
                                Spacer(Modifier.height(2.dp))
                                Text(nombre, fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (sel) Color.White else PrimaryClr)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Pantalla inicial ──────────────────────────────────
                if (!hasResult && !isLoading && errorMsg.isEmpty() && temaActual.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier.size(72.dp).clip(CircleShape)
                                    .background(PrimaryLight),
                                contentAlignment = Alignment.Center
                            ) { Text("🎵", fontSize = 32.sp) }
                            Spacer(Modifier.height(16.dp))
                            Text("¡Hola! Soy MelodyStudy",
                                fontSize = 20.sp, fontWeight = FontWeight.Bold, color = InkColor)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Convierte cualquier tema de estudio\nen una canción memorable.",
                                fontSize = 14.sp, color = InkLight,
                                textAlign = TextAlign.Center, lineHeight = 22.sp
                            )
                            Spacer(Modifier.height(24.dp))
                            Text("Prueba con:", fontSize = 12.sp, color = InkLight)
                            Spacer(Modifier.height(8.dp))
                            listOf("el sistema solar", "la célula", "el ciclo del agua")
                                .forEach { s ->
                                    SugerenciaChip(s) { inputText = "Genera una canción sobre $s" }
                                    Spacer(Modifier.height(6.dp))
                                }
                        }
                    }
                }

                // ── Burbuja del mensaje enviado ───────────────────────
                if (temaActual.isNotEmpty() && (isLoading || hasResult || errorMsg.isNotEmpty())) {
                    AnimatedVisibility(
                        visible = true,
                        enter   = fadeIn() + slideInVertically { it / 2 }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp, 4.dp, 18.dp, 18.dp))
                                    .background(PrimaryClr)
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                                    .widthIn(max = 260.dp)
                            ) {
                                Column {
                                    Text(temaActual, color = Color.White, fontSize = 14.sp)
                                    Text(generoSeleccionado,
                                        color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // ── Panel de carga con progreso ───────────────────────
                AnimatedVisibility(
                    visible = isLoading,
                    enter   = fadeIn() + slideInVertically { it / 2 },
                    exit    = fadeOut()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment     = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape)
                                .background(PrimaryLight),
                            contentAlignment = Alignment.Center
                        ) { Text("🎵", fontSize = 16.sp) }
                        Spacer(Modifier.width(10.dp))

                        Card(
                            shape     = RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp),
                            colors    = CardDefaults.cardColors(containerColor = CardColor),
                            elevation = CardDefaults.cardElevation(2.dp),
                            modifier  = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {

                                val pct = (animProgreso * 100).toInt().coerceIn(0, 100)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment     = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "${ETAPAS.getOrNull(etapaActual)?.emoji ?: "⏳"} $etapaLabel",
                                        fontSize   = 13.sp,
                                        color      = InkColor,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        "$pct%",
                                        fontSize   = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color      = PrimaryClr
                                    )
                                }

                                Spacer(Modifier.height(10.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(PrimaryLight)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(animProgreso)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(Color(0xFF7C5CBF), Color(0xFFBB86FC))
                                                )
                                            )
                                    )
                                }

                                Spacer(Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    ETAPAS.forEachIndexed { i, etapa ->
                                        val acum = ETAPAS.take(i + 1).sumOf { it.peso } / 100f
                                        val completada = animProgreso >= acum - 0.01f
                                        val activa    = i == etapaActual && isLoading

                                        val pulso by rememberInfiniteTransition(label = "p$i")
                                            .animateFloat(
                                                0.5f, 1f,
                                                infiniteRepeatable(tween(700), RepeatMode.Reverse),
                                                label = "p$i"
                                            )

                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        when {
                                                            completada -> PrimaryClr
                                                            activa     -> PrimaryLight
                                                            else       -> Color(0xFFEEEEEE)
                                                        }
                                                    )
                                                    .alpha(if (activa) pulso else 1f),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    if (completada) "✓" else etapa.emoji,
                                                    fontSize = if (completada) 12.sp else 10.sp,
                                                    color = if (completada) Color.White
                                                    else if (activa) PrimaryClr
                                                    else InkLight
                                                )
                                            }
                                            Spacer(Modifier.height(3.dp))
                                            Text(
                                                etapa.label.split(" ").first(),
                                                fontSize  = 8.sp,
                                                color     = if (completada) PrimaryClr else InkLight,
                                                textAlign = TextAlign.Center,
                                                maxLines  = 1
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(10.dp))
                                Text(
                                    "La música tarda un poco — vale la pena 🎶",
                                    fontSize   = 11.sp,
                                    color      = InkLight,
                                    textAlign  = TextAlign.Center,
                                    modifier   = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // ── Error ─────────────────────────────────────────────
                if (errorMsg.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Card(
                        colors   = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        shape    = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("⚠️ $errorMsg",
                            modifier = Modifier.padding(16.dp),
                            color    = ErrorClr,
                            fontSize = 13.sp, lineHeight = 20.sp)
                    }
                }

                // ── Resultado ─────────────────────────────────────────
                AnimatedVisibility(
                    visible = hasResult,
                    enter   = fadeIn() + slideInVertically { it / 2 }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment     = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier.size(32.dp).clip(CircleShape)
                                .background(PrimaryLight),
                            contentAlignment = Alignment.Center
                        ) { Text("🎵", fontSize = 14.sp) }
                        Spacer(Modifier.width(8.dp))
                        Card(
                            shape     = RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp),
                            colors    = CardDefaults.cardColors(containerColor = CardColor),
                            elevation = CardDefaults.cardElevation(2.dp),
                            modifier  = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Badge("🎵 Canción lista", PrimaryLight, PrimaryClr)
                                    Badge(
                                        GENEROS.firstOrNull { it.first == generoSeleccionado }
                                            ?.let { "${it.second} ${it.first}" }
                                            ?: generoSeleccionado,
                                        Color(0xFFE8F5E9), Color(0xFF2E7D32)
                                    )
                                }

                                Spacer(Modifier.height(12.dp))
                                Text(letraCancion, fontSize = 14.sp,
                                    color = InkColor, lineHeight = 24.sp)
                                Spacer(Modifier.height(16.dp))
                                HorizontalDivider(color = Color(0xFFEEEEEE))
                                Spacer(Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Escuchar
                                    ActionButton(
                                        icon  = { Text(if (isPlaying) "⏸" else "🔊", fontSize = 14.sp) },
                                        label = if (isPlaying) "Pausar" else "Escuchar",
                                        color = PrimaryClr,
                                        modifier = Modifier.weight(1f)
                                    ) { escuchar() }

                                    // Regenerar
                                    ActionButton(
                                        icon = { Icon(Icons.Default.Refresh, null,
                                            modifier = Modifier.size(16.dp)) },
                                        label    = "Regenerar",
                                        color    = Color(0xFF2196F3),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        inputText = temaActual
                                        hasResult = false
                                        guardado  = false
                                        enviar()
                                    }

                                    // Guardar
                                    ActionButton(
                                        icon = {
                                            if (isGuardando) CircularProgressIndicator(
                                                modifier = Modifier.size(14.dp),
                                                color = Color.White, strokeWidth = 2.dp
                                            ) else Icon(Icons.Default.Add, null,
                                                modifier = Modifier.size(16.dp))
                                        },
                                        label = when {
                                            isGuardando -> "Guardando…"
                                            guardado    -> "Guardado ✓"
                                            else        -> "Guardar"
                                        },
                                        color = if (isGuardando || guardado)
                                            Color(0xFF888888) else SuccessClr,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        if (!guardado && !isGuardando) {
                                            scope.launch {
                                                isGuardando = true
                                                errorMsg    = ""
                                                try {
                                                    // CAMBIO 6: pasar audioKey (UUID) en lugar de memId (Long)
                                                    val (dbId, preguntas) = guardarCancion(
                                                        temaActual, generoSeleccionado,
                                                        letraCancion, audioKey
                                                    )
                                                    guardado = true
                                                    onGuardar(
                                                        TemaGuardado(
                                                            id          = dbId,
                                                            titulo      = temaActual,
                                                            genero      = generoSeleccionado,
                                                            letra       = letraCancion,
                                                            audioUrl    = "http://192.168.1.150:7000/audio/cancion/$dbId",
                                                            preguntas   = preguntas,
                                                            tieneExamen = preguntas.isNotEmpty()
                                                        )
                                                    )
                                                } catch (e: Exception) {
                                                    errorMsg = "Error al guardar: ${e.message}"
                                                } finally {
                                                    isGuardando = false
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }

            // ── Barra de input ────────────────────────────────────────
            Surface(tonalElevation = 8.dp, color = CardColor,
                modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {

                    if (txtCargado && nombreArchivo.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFE8F5E9))
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("📄", fontSize = 12.sp)
                            Text(nombreArchivo, fontSize = 11.sp, color = Color(0xFF2E7D32),
                                modifier = Modifier.weight(1f), maxLines = 1)
                            Text("✓ Analizado", fontSize = 11.sp,
                                color = Color(0xFF2E7D32), fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (analizandoTxt || isLoading) Color(0xFFEEEEEE)
                                    else Color(0xFFEDE8FF)
                                )
                                .clickable(enabled = !analizandoTxt && !isLoading) {
                                    fileLauncher.launch(
                                        Intent(Intent.ACTION_GET_CONTENT).apply {
                                            type = "text/plain"
                                            addCategory(Intent.CATEGORY_OPENABLE)
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (analizandoTxt) CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = PrimaryClr, strokeWidth = 2.dp
                            ) else Text("📄", fontSize = 18.sp)
                        }

                        OutlinedTextField(
                            value         = inputText,
                            onValueChange = { inputText = it; if (it.isBlank()) txtCargado = false },
                            placeholder   = {
                                Text(
                                    if (isLoading) "Generando canción…"
                                    else "¿De qué tema deseas generar la canción?",
                                    fontSize = 13.sp, color = InkLight
                                )
                            },
                            enabled   = !isLoading,
                            modifier  = Modifier.weight(1f),
                            shape     = RoundedCornerShape(24.dp),
                            colors    = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor      = if (txtCargado) SuccessClr else PrimaryClr,
                                unfocusedBorderColor    = if (txtCargado) Color(0xFF81C784) else Color(0xFFDDD8F0),
                                focusedContainerColor   = BgColor,
                                unfocusedContainerColor = BgColor,
                                disabledContainerColor  = Color(0xFFF0EEFF),
                                disabledBorderColor     = Color(0xFFDDD8F0),
                                cursorColor             = PrimaryClr
                            ),
                            textStyle = TextStyle(fontSize = 14.sp, color = InkColor),
                            maxLines  = 3
                        )

                        val canSend = inputText.isNotBlank() && !isLoading && !analizandoTxt
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (canSend)
                                        Brush.linearGradient(listOf(Color(0xFF7C5CBF), Color(0xFF9B7FD4)))
                                    else
                                        Brush.linearGradient(listOf(Color(0xFFDDD8F0), Color(0xFFDDD8F0)))
                                )
                                .clickable(enabled = canSend) { enviar() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Send, "Enviar",
                                tint     = if (canSend) Color.White else InkLight,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Componentes auxiliares ────────────────────────────────────────────────────

@Composable
private fun Badge(text: String, bgColor: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ActionButton(
    icon: @Composable () -> Unit,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick        = onClick,
        modifier       = modifier.height(36.dp),
        shape          = RoundedCornerShape(20.dp),
        colors         = ButtonDefaults.buttonColors(containerColor = color),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        icon()
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SugerenciaChip(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(PrimaryLight)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(text, color = PrimaryClr, fontSize = 13.sp)
    }
}

suspend fun analizarTxt(contenido: String): Pair<String, String> =
    withContext(Dispatchers.IO) {
        val url  = URL(Constants.ANALYZE_TXT)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            doOutput       = true
            connectTimeout = 10_000
            readTimeout    = 60_000
        }
        val texto    = if (contenido.length > 3000) contenido.substring(0, 3000) else contenido
        val bodyJson = JSONObject().apply { put("contenido", texto) }.toString()
        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(bodyJson) }
        val code = conn.responseCode
        if (code != 200) throw Exception("HTTP $code")
        val body = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
        val json = JSONObject(body)
        Pair(json.getString("tema"), json.getString("resumen"))
    }