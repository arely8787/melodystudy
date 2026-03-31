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

private val BgColor      = Color(0xFFF7F5FF)
private val CardColor    = Color(0xFFFFFFFF)
private val PrimaryClr   = Color(0xFF7C5CBF)
private val PrimaryLight = Color(0xFFEDE8FF)
private val InkColor     = Color(0xFF1A1825)
private val InkLight     = Color(0xFF7B7490)

private val GENEROS = listOf(
    "Pop"     to "🎤",
    "Rock"    to "🎸",
    "Balada"  to "🎹",
    "Clásica" to "🎻"
)

@Composable
fun CrearScreen(
    idUsuario: Int = 0,
    onGuardar: (TemaGuardado) -> Unit = {}
) {
    var inputText          by remember { mutableStateOf("") }
    var temaActual         by remember { mutableStateOf("") }
    var letraCancion       by remember { mutableStateOf("") }
    var audioUrl           by remember { mutableStateOf("") }
    // memId = ID en memoria del servidor (para /audio/{id}), NO es el ID de BD
    var memId              by remember { mutableStateOf(0L) }
    var generoSeleccionado by remember { mutableStateOf("Pop") }
    var isLoading          by remember { mutableStateOf(false) }
    var isGuardando        by remember { mutableStateOf(false) }
    var hasResult          by remember { mutableStateOf(false) }
    var guardado           by remember { mutableStateOf(false) }
    var errorMsg           by remember { mutableStateOf("") }
    var isPlaying          by remember { mutableStateOf(false) }
    val mediaPlayer        = remember { mutableStateOf<MediaPlayer?>(null) }
    val scope              = rememberCoroutineScope()
    val scrollState        = rememberScrollState()

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer.value?.release()
            mediaPlayer.value = null
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val dot1 by infiniteTransition.animateFloat(0.2f, 1f,
        infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "d1")
    val dot2 by infiniteTransition.animateFloat(0.2f, 1f,
        infiniteRepeatable(tween(600, delayMillis = 200), RepeatMode.Reverse), label = "d2")
    val dot3 by infiniteTransition.animateFloat(0.2f, 1f,
        infiniteRepeatable(tween(600, delayMillis = 400), RepeatMode.Reverse), label = "d3")

    // ── /generate-song: solo genera, no guarda en BD ─────────────────
    suspend fun generarCancion(tema: String, genero: String): Triple<Long, String, String> {
        return withContext(Dispatchers.IO) {
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
                // NO enviamos id_usuario aquí
            }.toString()

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(bodyJson) }

            val code = conn.responseCode
            if (code != 200) throw Exception("Error del servidor: $code")

            val body = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
            val json = JSONObject(body)

            val id    = json.getLong("id")           // ID en memoria
            val letra = json.getString("letra")
            val audio = json.getString("audioUrl")
            Triple(id, letra, audio)
        }
    }

    // ── /save-song: guarda en BD + genera examen, todo en un paso ────
    suspend fun guardarCancion(
        tema: String, genero: String, letra: String,
        tempId: Long   // ← agregar parámetro
    ): Pair<Long, List<PreguntaUI>> {
        return withContext(Dispatchers.IO) {
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
                put("temp_id",    tempId)      // ← CLAVE: ID temporal para encontrar el MP3
                put("tema",       tema)
                put("genero",     genero.lowercase())
                put("letra",      letra)
                // NO enviar audio_url, el backend la construye
            }.toString()

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(bodyJson) }

            val code = conn.responseCode
            if (code != 200) throw Exception("Error al guardar: $code")

            val body = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
            val json = JSONObject(body)

            val dbId      = json.getLong("dbId")
            val audioUrl  = json.optString("audioUrl", "")   // ← guardar la URL real que devuelve BD
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
            Pair(dbId, preguntas)  // audioUrl ya viene dentro del TemaGuardado via onGuardar
        }
    }

    fun enviar() {
        if (inputText.isBlank() || isLoading) return
        temaActual = inputText.trim()
        guardado   = false
        errorMsg   = ""
        memId      = 0L

        mediaPlayer.value?.stop()
        mediaPlayer.value?.release()
        mediaPlayer.value = null
        isPlaying = false

        scope.launch {
            isLoading    = true
            hasResult    = false
            letraCancion = ""
            audioUrl     = ""
            try {
                val (id, letra, url) = generarCancion(temaActual, generoSeleccionado)
                memId        = id
                letraCancion = letra
                audioUrl     = url
                hasResult    = true
                scrollState.animateScrollTo(scrollState.maxValue)
            } catch (e: Exception) {
                errorMsg = "No se pudo conectar al servidor:\n${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun escuchar() {
        if (audioUrl.isBlank()) return

        mediaPlayer.value?.let { mp ->
            if (mp.isPlaying) { mp.pause(); isPlaying = false }
            else              { mp.start(); isPlaying = true  }
            return
        }

        val mp = MediaPlayer()
        mp.setDataSource(audioUrl)
        mp.setAudioStreamType(android.media.AudioManager.STREAM_MUSIC)
        mp.setOnPreparedListener  { it.start(); isPlaying = true }
        mp.setOnCompletionListener { isPlaying = false }
        mp.setOnErrorListener { player, what, extra ->
            errorMsg  = "Error de audio ($what, $extra)"
            isPlaying = false
            player.reset()
            player.release()
            mediaPlayer.value = null
            true
        }
        mediaPlayer.value = mp
        mp.prepareAsync()
    }

    // ── UI ────────────────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize().background(BgColor)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Header
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.verticalGradient(
                        listOf(Color(0xFF7C5CBF), Color(0xFF9B7FD4))))
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

            Column(
                modifier = Modifier.weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(Modifier.height(20.dp))

                // Selector de género
                Text("Género musical", fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold, color = InkLight)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GENEROS.forEach { (nombre, emoji) ->
                        val seleccionado = generoSeleccionado == nombre
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (seleccionado) PrimaryClr else PrimaryLight)
                                .clickable { generoSeleccionado = nombre }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(emoji, fontSize = 18.sp)
                                Spacer(Modifier.height(2.dp))
                                Text(nombre, fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (seleccionado) Color.White else PrimaryClr)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Pantalla inicial
                if (!hasResult && !isLoading && errorMsg.isEmpty()) {
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

                // Error
                if (errorMsg.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Card(
                        colors   = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        shape    = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("⚠️ $errorMsg",
                            modifier = Modifier.padding(16.dp),
                            color    = Color(0xFFC62828),
                            fontSize = 13.sp, lineHeight = 20.sp)
                    }
                }

                // Loading
                if (isLoading) {
                    Spacer(Modifier.height(40.dp))
                    Box(modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.size(10.dp).clip(CircleShape)
                                    .background(PrimaryClr).alpha(dot1))
                                Box(Modifier.size(10.dp).clip(CircleShape)
                                    .background(PrimaryClr).alpha(dot2))
                                Box(Modifier.size(10.dp).clip(CircleShape)
                                    .background(PrimaryClr).alpha(dot3))
                            }
                            Spacer(Modifier.height(12.dp))
                            Text("Componiendo tu canción $generoSeleccionado...",
                                color = InkLight, fontSize = 14.sp)
                        }
                    }
                }

                // Resultado
                if (hasResult) {
                    Spacer(Modifier.height(8.dp))

                    // Burbuja usuario
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End) {
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
                    Spacer(Modifier.height(12.dp))

                    // Burbuja IA
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
                                // Badges
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(PrimaryLight)
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("🎵 Canción generada",
                                            color = PrimaryClr, fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color(0xFFE8F5E9))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            GENEROS.firstOrNull { it.first == generoSeleccionado }
                                                ?.let { "${it.second} ${it.first}" }
                                                ?: generoSeleccionado,
                                            color = Color(0xFF2E7D32), fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
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
                                        icon  = { Text(if (isPlaying) "⏸" else "🔊",
                                            fontSize = 14.sp) },
                                        label = if (isPlaying) "Pausar" else "Escuchar",
                                        color = PrimaryClr,
                                        modifier = Modifier.weight(1f)
                                    ) { escuchar() }

                                    // Regenerar
                                    ActionButton(
                                        icon  = { Icon(Icons.Default.Refresh, null,
                                            modifier = Modifier.size(16.dp)) },
                                        label = "Regenerar",
                                        color = Color(0xFF2196F3),
                                        modifier = Modifier.weight(1f)
                                    ) { inputText = temaActual; enviar() }

                                    // Guardar → llama a /save-song (BD + examen en un paso)
                                    ActionButton(
                                        icon = {
                                            if (isGuardando) {
                                                CircularProgressIndicator(
                                                    modifier    = Modifier.size(14.dp),
                                                    color       = Color.White,
                                                    strokeWidth = 2.dp
                                                )
                                            } else {
                                                Icon(Icons.Default.Add, null,
                                                    modifier = Modifier.size(16.dp))
                                            }
                                        },
                                        label = when {
                                            isGuardando -> "Guardando…"
                                            guardado    -> "Guardado ✓"
                                            else        -> "Guardar"
                                        },
                                        color = when {
                                            isGuardando -> Color(0xFF888888)
                                            guardado    -> Color(0xFF888888)
                                            else        -> Color(0xFF4CAF50)
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        if (!guardado && !isGuardando) {
                                            scope.launch {
                                                isGuardando = true
                                                errorMsg    = ""
                                                try {
                                                    val (dbId, preguntas) = guardarCancion(
                                                        temaActual, generoSeleccionado,
                                                        letraCancion,
                                                        memId        // ← pasar el ID temporal
                                                    )
                                                    guardado = true
                                                    onGuardar(
                                                        TemaGuardado(
                                                            id          = dbId,
                                                            titulo      = temaActual,
                                                            genero      = generoSeleccionado,
                                                            letra       = letraCancion,
                                                            audioUrl    = "http://10.0.2.2:7000/audio/cancion/$dbId",  // ← URL persistente
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
                    Spacer(Modifier.height(16.dp))
                }
                Spacer(Modifier.height(8.dp))
            }

            // Input bar
            Surface(tonalElevation = 8.dp, color = CardColor,
                modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value         = inputText,
                        onValueChange = { inputText = it },
                        placeholder   = {
                            Text("¿De qué tema deseas generar la canción?",
                                fontSize = 13.sp, color = InkLight)
                        },
                        modifier   = Modifier.weight(1f),
                        shape      = RoundedCornerShape(24.dp),
                        colors     = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor      = PrimaryClr,
                            unfocusedBorderColor    = Color(0xFFDDD8F0),
                            focusedContainerColor   = BgColor,
                            unfocusedContainerColor = BgColor,
                            cursorColor             = PrimaryClr
                        ),
                        textStyle  = TextStyle(fontSize = 14.sp, color = InkColor),
                        maxLines   = 3,
                        singleLine = false
                    )
                    val sendBg = if (inputText.isBlank() || isLoading)
                        Brush.linearGradient(listOf(Color(0xFFDDD8F0), Color(0xFFDDD8F0)))
                    else
                        Brush.linearGradient(listOf(Color(0xFF7C5CBF), Color(0xFF9B7FD4)))

                    Box(
                        modifier = Modifier.size(48.dp).clip(CircleShape)
                            .background(sendBg)
                            .clickable(enabled = inputText.isNotBlank() && !isLoading) { enviar() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Send, "Enviar",
                            tint     = if (inputText.isBlank() || isLoading) InkLight else Color.White,
                            modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
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