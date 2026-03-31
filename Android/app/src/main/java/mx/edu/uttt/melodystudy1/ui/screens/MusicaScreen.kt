package mx.edu.uttt.melodystudy1.ui.screens

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import mx.edu.uttt.melodystudy1.model.TemaGuardado
import java.net.HttpURLConnection
import java.net.URL

private val SpotifyBg    = Color(0xFF121212)
private val SpotifyCard  = Color(0xFF1E1E1E)
private val SpotifyLight = Color(0xFFB3B3B3)
private val SpotifyWhite = Color(0xFFFFFFFF)
private val AccentPurple = Color(0xFF7C5CBF)

@Composable
fun MusicaScreen(tema: TemaGuardado, onVolver: () -> Unit) {

    val mediaPlayer       = remember { mutableStateOf<MediaPlayer?>(null) }
    var isReady           by remember { mutableStateOf(false) }
    var isPlaying         by remember { mutableStateOf(false) }
    var isBuffering       by remember { mutableStateOf(false) }
    var errorMsg          by remember { mutableStateOf("") }
    var duracionMs        by remember { mutableStateOf(1) }
    var posicionMs        by remember { mutableStateOf(0) }
    var progreso          by remember { mutableStateOf(0f) }
    var arrastrandoSlider by remember { mutableStateOf(false) }
    var audioDisponible   by remember { mutableStateOf<Boolean?>(null) }

    // UN SOLO scroll para toda la pantalla
    val scrollState = rememberScrollState()

    val generoEmoji = when (tema.genero.lowercase()) {
        "rock"               -> "🎸"
        "balada"             -> "🎹"
        "clásica", "clasica" -> "🎻"
        else                 -> "🎵"
    }

    // ── 1. Verificar audio en servidor ───────────────────────────────
    LaunchedEffect(tema.audioUrl) {
        if (tema.audioUrl.isBlank()) {
            audioDisponible = false
            errorMsg = "No hay URL de audio para este tema."
            return@LaunchedEffect
        }
        audioDisponible = null
        isBuffering     = true
        errorMsg        = ""

        var intentos = 0
        var listo    = false
        while (intentos < 12 && !listo) {
            listo = withContext(Dispatchers.IO) {
                try {
                    val conn = URL(tema.audioUrl).openConnection() as HttpURLConnection
                    conn.requestMethod  = "HEAD"
                    conn.connectTimeout = 5_000
                    conn.readTimeout    = 5_000
                    val code = conn.responseCode
                    conn.disconnect()
                    code == 200
                } catch (e: Exception) { false }
            }
            if (!listo) { intentos++; delay(5_000) }
        }
        audioDisponible = listo
        isBuffering     = !listo
        if (!listo) errorMsg = "El audio aún no está disponible. Inténtalo en unos segundos."
    }

    // ── 2. Inicializar MediaPlayer ────────────────────────────────────
    LaunchedEffect(audioDisponible) {
        if (audioDisponible != true) return@LaunchedEffect
        isBuffering = true
        errorMsg    = ""
        val mp = MediaPlayer()
        try {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            mp.setDataSource(tema.audioUrl)
            mp.setOnPreparedListener { player ->
                duracionMs  = player.duration.coerceAtLeast(1)
                isReady     = true
                isBuffering = false
                player.start()
                isPlaying = true
            }
            mp.setOnCompletionListener {
                isPlaying  = false
                posicionMs = 0
                progreso   = 0f
            }
            mp.setOnErrorListener { _, what, extra ->
                errorMsg    = "Error reproduciendo audio ($what/$extra)."
                isBuffering = false
                isPlaying   = false
                isReady     = false
                true
            }
            mediaPlayer.value = mp
            mp.prepareAsync()
        } catch (e: Exception) {
            errorMsg    = "No se pudo cargar el audio: ${e.message}"
            isBuffering = false
            mp.release()
        }
    }

    // ── 3. Liberar al salir ───────────────────────────────────────────
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer.value?.stop()
            mediaPlayer.value?.release()
            mediaPlayer.value = null
        }
    }

    // ── 4. Barra de progreso ──────────────────────────────────────────
    LaunchedEffect(isPlaying) {
        while (isActive && isPlaying) {
            delay(500)
            val mp = mediaPlayer.value
            if (mp != null && mp.isPlaying && !arrastrandoSlider) {
                posicionMs = mp.currentPosition
                progreso   = posicionMs.toFloat() / duracionMs.toFloat()
            }
        }
    }

    fun formatTime(ms: Int): String {
        val s = ms / 1000
        return "%d:%02d".format(s / 60, s % 60)
    }

    fun togglePlay() {
        val mp = mediaPlayer.value ?: return
        if (!isReady) return
        if (mp.isPlaying) { mp.pause(); isPlaying = false }
        else              { mp.start(); isPlaying = true  }
    }

    // ── UI: Box raíz con fondo, barra superior fija encima del scroll ─
    Box(modifier = Modifier.fillMaxSize().background(SpotifyBg)) {

        // ── Contenido scrolleable ─────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)           // ← ÚNICO scroll
        ) {

            // Espacio para la barra superior flotante
            Spacer(Modifier.height(64.dp))

            // Gradiente de portada (parte visual superior)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF3D1A6E), Color(0xFF1A0D3D), SpotifyBg)
                        )
                    )
                    .padding(top = 16.dp, bottom = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    // Portada / artwork
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .shadow(24.dp, RoundedCornerShape(20.dp))
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF7C5CBF), Color(0xFF9B7FD4), Color(0xFFBB86FC))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(generoEmoji, fontSize = 80.sp)
                    }

                    Spacer(Modifier.height(24.dp))

                    // Título
                    Text(
                        text       = tema.titulo,
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color      = SpotifyWhite,
                        textAlign  = TextAlign.Center,
                        modifier   = Modifier.padding(horizontal = 32.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text     = "${tema.genero.replaceFirstChar { it.uppercase() }} · MelodyStudy",
                        fontSize = 14.sp,
                        color    = SpotifyLight
                    )
                }
            }

            // ── Player pegado debajo de la portada ────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SpotifyBg)
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(Modifier.height(8.dp))

                // Slider de progreso
                Slider(
                    value         = progreso,
                    onValueChange = { v ->
                        arrastrandoSlider = true
                        progreso          = v
                        posicionMs        = (v * duracionMs).toInt()
                    },
                    onValueChangeFinished = {
                        mediaPlayer.value?.seekTo(posicionMs)
                        arrastrandoSlider = false
                    },
                    enabled = isReady,
                    colors  = SliderDefaults.colors(
                        thumbColor               = SpotifyWhite,
                        activeTrackColor         = SpotifyWhite,
                        inactiveTrackColor       = Color(0xFF535353),
                        disabledThumbColor       = Color(0xFF535353),
                        disabledActiveTrackColor = Color(0xFF535353)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Tiempos
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatTime(posicionMs), fontSize = 11.sp, color = SpotifyLight)
                    Text(formatTime(duracionMs), fontSize = 11.sp, color = SpotifyLight)
                }

                Spacer(Modifier.height(20.dp))

                // Botón play central
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    when {
                        isBuffering || audioDisponible == null -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    color       = SpotifyWhite,
                                    modifier    = Modifier.size(64.dp),
                                    strokeWidth = 3.dp
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    if (audioDisponible == null) "Esperando audio del servidor…"
                                    else "Cargando…",
                                    color    = SpotifyLight,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        else -> {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(if (isReady) SpotifyWhite else Color(0xFF535353))
                                    .clickable(enabled = isReady) { togglePlay() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector        = if (isPlaying) Icons.Default.Pause
                                    else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                                    tint               = SpotifyBg,
                                    modifier           = Modifier.size(36.dp)
                                )
                            }
                        }
                    }
                }

                // Error
                if (errorMsg.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "⚠️ $errorMsg",
                        color     = Color(0xFFFF5252),
                        fontSize  = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier  = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(28.dp))

                // ── Sección letra: SIN scroll propio, fluye con la página ──
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(20.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(AccentPurple)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Letra",
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color      = SpotifyWhite
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Texto de letra completo, sin scroll interno
                if (tema.letra.isBlank()) {
                    Text(
                        "Sin letra disponible.",
                        fontSize = 14.sp,
                        color    = SpotifyLight
                    )
                } else {
                    // Cada línea con leve separación para legibilidad
                    tema.letra.split("\n").forEach { linea ->
                        if (linea.isBlank()) {
                            Spacer(Modifier.height(12.dp))
                        } else {
                            Text(
                                text       = linea,
                                fontSize   = 15.sp,
                                color      = if (linea.startsWith("[") || linea.startsWith("("))
                                    AccentPurple.copy(alpha = 0.85f)
                                else
                                    SpotifyLight,
                                fontWeight = if (linea.startsWith("[") || linea.startsWith("("))
                                    FontWeight.SemiBold
                                else
                                    FontWeight.Normal,
                                lineHeight = 26.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(48.dp))
            }
        }

        // ── Barra superior flotante (siempre visible al hacer scroll) ─
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(SpotifyBg, SpotifyBg.copy(alpha = 0f))
                    )
                )
                .padding(horizontal = 4.dp, vertical = 8.dp)
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onVolver) {
                Icon(Icons.Default.ArrowBack, "Volver", tint = SpotifyWhite)
            }
            Text(
                "Reproduciendo",
                fontSize  = 13.sp,
                color     = SpotifyLight,
                modifier  = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.size(48.dp))
        }
    }
}