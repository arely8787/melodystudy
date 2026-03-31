package mx.edu.uttt.melodystudy1.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mx.edu.uttt.melodystudy1.model.PreguntaUI
import mx.edu.uttt.melodystudy1.model.TemaGuardado
import mx.edu.uttt.melodystudy1.network.Constants
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll


@Composable
fun ExamenScreen(
    tema: TemaGuardado,
    onIniciarCuestionario: (TemaGuardado) -> Unit,  // lleva el tema YA con preguntas cargadas
    onVolver: () -> Unit
) {
    val PrimaryClr = Color(0xFF7C5CBF)
    val BgColor    = Color(0xFFF7F5FF)
    val InkLight   = Color(0xFF7B7490)
    val InkColor   = Color(0xFF1A1825)

    var temaConPreguntas by remember { mutableStateOf(tema) }
    var cargando         by remember { mutableStateOf(true) }
    var error            by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    // Cargar preguntas al abrir la pantalla (para que estén listas al presionar Intentar)
    LaunchedEffect(tema.id) {
        if (!tema.tieneExamen) { cargando = false; return@LaunchedEffect }
        cargando = true
        error    = ""
        try {
            val preguntas = cargarPreguntasExamen(tema.id)
            temaConPreguntas = tema.copy(preguntas = preguntas)
        } catch (e: Exception) {
            error = "No se pudieron cargar las preguntas:\n${e.message}"
        } finally {
            cargando = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(BgColor)) {

        // ── Header ────────────────────────────────────────────────────
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
                Text(
                    "Examen",
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White
                )
            }
        }

        // ── Contenido ─────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            Text("📝", fontSize = 64.sp)

            Text(
                text       = "Examen",
                fontSize   = 14.sp,
                color      = InkLight,
                fontWeight = FontWeight.Medium,
                textAlign  = TextAlign.Center
            )

            Text(
                text       = tema.titulo,
                fontSize   = 22.sp,
                fontWeight = FontWeight.Bold,
                color      = InkColor,
                textAlign  = TextAlign.Center
            )

            Text(
                text      = tema.genero,
                fontSize  = 13.sp,
                color     = InkLight,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(4.dp))

            // ── Última calificación (sobre 100) ───────────────────────
            Card(
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier  = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier            = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Última calificación", fontSize = 13.sp, color = InkLight)
                    Spacer(Modifier.height(8.dp))

                    if (tema.ultimaCalificacion != null) {
                        Text(
                            text       = "${tema.ultimaCalificacion}/100",
                            fontSize   = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color      = when {
                                tema.ultimaCalificacion >= 80 -> Color(0xFF4CAF50)
                                tema.ultimaCalificacion >= 60 -> Color(0xFFFF9800)
                                else                          -> Color(0xFFF44336)
                            }
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text     = when {
                                tema.ultimaCalificacion >= 80 -> "¡Muy bien!"
                                tema.ultimaCalificacion >= 60 -> "Aprobado"
                                else                          -> "Necesitas mejorar"
                            },
                            fontSize = 13.sp,
                            color    = InkLight
                        )
                    } else {
                        Text(
                            "—",
                            fontSize   = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Color(0xFFCCCCCC)
                        )
                        Text("Sin intentos aún", fontSize = 13.sp, color = InkLight)
                    }
                }
            }

            // ── Estado de carga de preguntas ──────────────────────────
            when {
                cargando -> {
                    Row(
                        verticalAlignment    = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            color       = PrimaryClr,
                            modifier    = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            "Preparando preguntas...",
                            fontSize = 13.sp,
                            color    = InkLight
                        )
                    }
                }
                error.isNotEmpty() -> {
                    Text(
                        "⚠️ $error",
                        color     = Color(0xFFC62828),
                        fontSize  = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
                else -> {
                    // Info de cuántas preguntas tiene
                    if (temaConPreguntas.preguntas.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = Color(0xFFEDE8FF),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "${temaConPreguntas.preguntas.size} preguntas listas ✓",
                                fontSize = 12.sp,
                                color    = PrimaryClr,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // ── Botón Intentar ────────────────────────────────────────
            Button(
                onClick  = { onIniciarCuestionario(temaConPreguntas) },
                enabled  = !cargando && error.isEmpty() && temaConPreguntas.preguntas.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape  = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryClr)
            ) {
                Text("Intentar", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── Carga preguntas desde GET /examen/{idCancion} ─────────────────────────
suspend fun cargarPreguntasExamen(idCancion: Long): List<PreguntaUI> {
    return withContext(Dispatchers.IO) {
        val url  = URL("${Constants.EXAMEN_CANCION}/$idCancion")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod  = "GET"
            connectTimeout = 10_000
            readTimeout    = 15_000
        }
        val code = conn.responseCode
        if (code != 200) throw Exception("HTTP $code")

        val body  = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
        val array = JSONArray(body)

        List(array.length()) { i ->
            val obj = array.getJSONObject(i)
            val ops = obj.getJSONArray("opciones")
            PreguntaUI(
                texto    = obj.getString("pregunta"),
                opciones = List(ops.length()) { j -> ops.getString(j) },
                correcta = obj.getInt("correcta")
            )
        }
    }
}