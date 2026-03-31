package mx.edu.uttt.melodystudy1.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mx.edu.uttt.melodystudy1.model.TemaGuardado
import mx.edu.uttt.melodystudy1.network.Constants
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

// Modelo interno de pregunta para la UI
private data class Pregunta(
    val texto:    String,
    val opciones: List<String>,
    val correcta: Int
)

// Fallback por si el examen no tiene preguntas de IA
private fun preguntasFallback(tema: TemaGuardado): List<Pregunta> = listOf(
    Pregunta("¿Cuál es el tema principal de esta canción?",
        listOf("No está definido", tema.titulo, "Otro tema", "Ninguna"), 1),
    Pregunta("¿En qué género musical fue creada esta canción?",
        listOf("Jazz", "Reggaeton", tema.genero, "Blues"), 2),
    Pregunta("¿Para qué sirve aprender con canciones?",
        listOf("Solo para entretenerse", "Para recordar mejor el tema",
            "No tiene utilidad", "Para distraerse"), 1),
    Pregunta("¿Cómo se llama esta aplicación educativa?",
        listOf("MusicLearn", "TuneStudy", "MelodyStudy", "SongClass"), 2),
    Pregunta("¿Qué combina MelodyStudy para enseñar?",
        listOf("Juegos y videos", "Música y contenido educativo",
            "Imágenes y texto", "Animaciones y ejercicios"), 1)
)

@Composable
fun CuestionarioScreen(
    tema:       TemaGuardado,
    idUsuario:  Int,                   // para actualizar XP en BD
    onTerminar: (Int) -> Unit,         // devuelve calificación 0–100
    onVolver:   () -> Unit
) {
    val PrimaryClr   = Color(0xFF7C5CBF)
    val PrimaryLight = Color(0xFFEDE8FF)
    val BgColor      = Color(0xFFF7F5FF)

    val scope = rememberCoroutineScope()

    // Usa las preguntas de la IA si existen; si no, usa el fallback
    val preguntas = remember {
        if (tema.preguntas.isNotEmpty()) {
            tema.preguntas.map { p ->
                Pregunta(texto = p.texto, opciones = p.opciones, correcta = p.correcta)
            }
        } else {
            preguntasFallback(tema)
        }
    }

    val respuestas        = remember { mutableStateListOf(*Array(preguntas.size) { -1 }) }
    var mostrarResultado  by remember { mutableStateOf(false) }
    var calificacion      by remember { mutableStateOf(0) }
    var mostrarRespuestas by remember { mutableStateOf(false) }
    var guardando         by remember { mutableStateOf(false) }

    // ── Diálogo de resultado ──────────────────────────────────────────
    if (mostrarResultado) {
        Dialog(onDismissRequest = {}) {
            Card(
                shape     = RoundedCornerShape(24.dp),
                colors    = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier            = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (calificacion >= 60) "🎉" else "😓",
                        fontSize = 56.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Tu calificación", fontSize = 14.sp, color = Color(0xFF7B7490))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "$calificacion/100",
                        fontSize   = 52.sp,
                        fontWeight = FontWeight.Bold,
                        color      = when {
                            calificacion >= 80 -> Color(0xFF4CAF50)
                            calificacion >= 60 -> Color(0xFFFF9800)
                            else               -> Color(0xFFF44336)
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        when {
                            calificacion == 100 -> "¡Perfecto! ¡Dominas el tema!"
                            calificacion >= 80  -> "¡Excelente dominio del tema!"
                            calificacion >= 60  -> "Aprobado, pero puedes mejorar."
                            else                -> "Sigue estudiando, tú puedes."
                        },
                        fontSize = 14.sp,
                        color    = Color(0xFF7B7490)
                    )

                    // XP ganado
                    val xpGanado = when {
                        calificacion == 100 -> 20
                        calificacion >= 60  -> 10
                        else               -> 0
                    }
                    if (xpGanado > 0) {
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFFFFF9C4))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "+$xpGanado XP ganados ⭐",
                                color    = Color(0xFFF57F17),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (tema.preguntas.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFFE8F5E9))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "✨ Examen generado con IA",
                                color    = Color(0xFF2E7D32),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Ver respuestas correctas
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                try {
                                    guardarCalificacionBD(tema.id, calificacion)
                                } catch (_: Exception) { }
                            }
                            mostrarRespuestas = true
                            mostrarResultado  = false
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape    = RoundedCornerShape(22.dp),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryClr)
                    ) {
                        Text("Ver respuestas", fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(Modifier.height(10.dp))

                    // Continuar: guarda calificación + XP en BD y notifica al padre
                    Button(
                        onClick = {
                            guardando = true
                            scope.launch {
                                try {
                                    // 1. Guardar calificación del examen en BD
                                    guardarCalificacionBD(tema.id, calificacion)
                                } catch (e: Exception) {
                                    // No bloquear la UI si falla la red
                                }
                                guardando = false
                                // 2. Notificar al padre (él aplica XP local + BD de usuario)
                                onTerminar(calificacion)
                            }
                        },
                        enabled  = !guardando,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape    = RoundedCornerShape(24.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = PrimaryClr)
                    ) {
                        if (guardando) {
                            CircularProgressIndicator(
                                color       = Color.White,
                                modifier    = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Continuar", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }

    // ── Pantalla principal ────────────────────────────────────────────
    Column(modifier = Modifier.fillMaxSize().background(BgColor)) {

        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0xFF7C5CBF), Color(0xFF9B7FD4))))
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onVolver) {
                    Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                }
                Column {
                    Text(
                        "Cuestionario",
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White
                    )
                    if (tema.preguntas.isNotEmpty()) {
                        Text(
                            "Preguntas generadas con IA ✨",
                            fontSize = 11.sp,
                            color    = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                tema.titulo,
                fontSize   = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color      = Color(0xFF1A1825)
            )

            preguntas.forEachIndexed { idx, pregunta ->
                Card(
                    shape     = RoundedCornerShape(16.dp),
                    colors    = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier  = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "${idx + 1}. ${pregunta.texto}",
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color      = Color(0xFF1A1825),
                            lineHeight = 22.sp
                        )
                        Spacer(Modifier.height(12.dp))

                        pregunta.opciones.forEachIndexed { opIdx, opcion ->
                            val seleccionada = respuestas[idx] == opIdx
                            val esCorrecta   = opIdx == pregunta.correcta

                            val bgColor = when {
                                mostrarRespuestas && esCorrecta                -> Color(0xFFE8F5E9)
                                mostrarRespuestas && seleccionada && !esCorrecta -> Color(0xFFFFEBEE)
                                seleccionada                                   -> PrimaryLight
                                else                                           -> Color.Transparent
                            }
                            val textColor = when {
                                mostrarRespuestas && esCorrecta                -> Color(0xFF2E7D32)
                                mostrarRespuestas && seleccionada && !esCorrecta -> Color(0xFFC62828)
                                seleccionada                                   -> PrimaryClr
                                else                                           -> Color(0xFF1A1825)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(bgColor)
                                    .then(
                                        if (!mostrarRespuestas)
                                            Modifier.clickable { respuestas[idx] = opIdx }
                                        else Modifier
                                    )
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                RadioButton(
                                    selected = seleccionada,
                                    onClick  = if (!mostrarRespuestas) {
                                        { respuestas[idx] = opIdx }
                                    } else null,
                                    colors   = RadioButtonDefaults.colors(
                                        selectedColor = if (mostrarRespuestas && !esCorrecta && seleccionada)
                                            Color(0xFFC62828)
                                        else PrimaryClr
                                    )
                                )
                                Text(opcion, fontSize = 14.sp, color = textColor)
                                if (mostrarRespuestas && esCorrecta) {
                                    Spacer(Modifier.weight(1f))
                                    Text("✓", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            if (!mostrarRespuestas) {
                val contestadas = respuestas.count { it != -1 }
                Text(
                    "$contestadas / ${preguntas.size} preguntas contestadas",
                    fontSize = 13.sp,
                    color    = Color(0xFF9E9CA8),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.height(4.dp))

                Button(
                    onClick = {
                        val correctas = preguntas.indices.count { i ->
                            respuestas[i] == preguntas[i].correcta
                        }
                        // Calificación de 0 a 100
                        calificacion     = (correctas * 100) / preguntas.size
                        mostrarResultado = true
                    },
                    enabled  = respuestas.none { it == -1 },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(26.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = PrimaryClr)
                ) {
                    Text("Enviar respuestas", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Button(
                    onClick = {
                        guardando = true
                        scope.launch {
                            try {
                                guardarCalificacionBD(tema.id, calificacion)
                            } catch (_: Exception) { }
                            guardando = false
                            onTerminar(calificacion)
                        }
                    },
                    enabled  = !guardando,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(26.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = PrimaryClr)
                ) {
                    if (guardando) {
                        CircularProgressIndicator(
                            color       = Color.White,
                            modifier    = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Terminar", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Guarda la calificación del examen en BD ───────────────────────────────
suspend fun guardarCalificacionBD(idCancion: Long, calificacion: Int) {
    withContext(Dispatchers.IO) {
        val url  = URL("${Constants.CALIFICACION_EXAMEN}/$idCancion/calificacion")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod  = "PUT"
            doOutput       = true
            connectTimeout = 10_000
            readTimeout    = 15_000
            setRequestProperty("Content-Type", "application/json")
        }
        val body = "{\"calificacion\":$calificacion}"
        conn.outputStream.use { it.write(body.toByteArray()) }
        conn.responseCode // ejecuta la petición
        conn.disconnect()
    }
}

// ── Actualiza nivel y progreso del usuario en BD ──────────────────────────
suspend fun actualizarXPUsuarioBD(idUsuario: Int, nivel: Int, progreso: Float) {
    withContext(Dispatchers.IO) {
        val url  = URL("${Constants.USUARIO_XP}/$idUsuario/xp")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "PUT"
            doOutput      = true
            connectTimeout = 10_000
            readTimeout    = 15_000
            setRequestProperty("Content-Type", "application/json")
        }
        OutputStreamWriter(conn.outputStream).use {
            it.write("{\"nivel\":$nivel,\"progreso\":$progreso}")
        }
        conn.responseCode
    }
}