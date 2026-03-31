package mx.edu.uttt.melodystudy1.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mx.edu.uttt.melodystudy1.model.TemaGuardado
import mx.edu.uttt.melodystudy1.network.Constants
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

private val PrimaryClr   = Color(0xFF7C5CBF)
private val PrimaryLight = Color(0xFFEDE8FF)
private val BgColor      = Color(0xFFF7F5FF)
private val InkColor     = Color(0xFF1A1825)
private val InkLight     = Color(0xFF7B7490)

@Composable
fun EstudiarScreen(
    idUsuario: Int = 0,
    temasLocales: List<TemaGuardado> = emptyList(),
    temasRemotos: List<TemaGuardado> = emptyList(),               // ← nuevo
    onTemasRemotosLoaded: (List<TemaGuardado>) -> Unit = {},      // ← nuevo
    onMusicaClick: (TemaGuardado) -> Unit = {},
    onEstudiarClick: (TemaGuardado) -> Unit = {}
) {
    var cargando     by remember { mutableStateOf(false) }
    var errorCarga   by remember { mutableStateOf("") }

    // Cargar desde el backend al entrar
    LaunchedEffect(idUsuario) {
        if (idUsuario <= 0) return@LaunchedEffect
        cargando   = true
        errorCarga = ""
        try {
            val cargados = cargarCancionesBD(idUsuario)
            onTemasRemotosLoaded(cargados)   // ← sube el estado al padre
        } catch (e: Exception) {
            errorCarga = "No se pudieron cargar tus canciones: ${e.message}"
        } finally {
            cargando = false
        }
    }

    // Mezcla: remotos primero, luego locales que no tengan ID en remotos
    val remotosIds = temasRemotos.map { it.id }.toSet()
    val soloLocales = temasLocales.filter { it.id !in remotosIds }
    val temas = temasRemotos + soloLocales

    Column(modifier = Modifier.fillMaxSize().background(BgColor)) {

        // Header
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(
                    listOf(Color(0xFF7C5CBF), Color(0xFF9B7FD4))))
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Mis canciones", fontSize = 20.sp,
                    fontWeight = FontWeight.Bold, color = Color.White)
                if (temas.isNotEmpty()) {
                    Text("${temas.size} tema${if (temas.size != 1) "s" else ""}",
                        fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                }
            }
        }

        when {
            cargando -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = PrimaryClr)
                        Spacer(Modifier.height(12.dp))
                        Text("Cargando tus canciones...", fontSize = 13.sp, color = InkLight)
                    }
                }
            }

            errorCarga.isNotEmpty() -> {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center) {
                    Text("⚠️ $errorCarga", color = Color(0xFFC62828),
                        fontSize = 13.sp, textAlign = TextAlign.Center)
                }
            }

            temas.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📚", fontSize = 48.sp)
                        Spacer(Modifier.height(16.dp))
                        Text("Aún no tienes canciones guardadas",
                            fontSize = 16.sp, color = InkLight, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                        Text("Ve a Crear, genera y presiona Guardar",
                            fontSize = 13.sp, color = InkLight, textAlign = TextAlign.Center)
                    }
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    temas.forEach { tema ->
                        TemaCard(
                            tema           = tema,
                            onMusicaClick  = { onMusicaClick(tema) },
                            onEstudiarClick = { onEstudiarClick(tema) }
                        )
                    }
                }
            }
        }
    }
}

// ── Carga canciones del backend ───────────────────────────────────────────────

suspend fun cargarCancionesBD(idUsuario: Int): List<TemaGuardado> {
    return withContext(Dispatchers.IO) {
        val url  = URL("${Constants.CANCIONES_USUARIO}/$idUsuario")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod  = "GET"
            connectTimeout = 10_000
            readTimeout    = 15_000
        }

        val code = conn.responseCode
        if (code != 200) throw Exception("HTTP $code")

        val body = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
        val array = JSONArray(body)

        List(array.length()) { i ->
            val obj = array.getJSONObject(i)
            TemaGuardado(
                id                 = obj.getLong("id"),
                titulo             = obj.getString("tema"),
                genero             = obj.getString("genero"),
                letra              = obj.getString("letra"),
                audioUrl           = obj.optString("audioUrl", ""),
                tieneExamen        = obj.optBoolean("tieneExamen", false),
                ultimaCalificacion = if (obj.isNull("ultimaCalificacion")) null
                else obj.getInt("ultimaCalificacion"),  // ← nuevo
                preguntas          = emptyList()
            )
        }
    }
}

// ── TemaCard ──────────────────────────────────────────────────────────────────

@Composable
fun TemaCard(
    tema: TemaGuardado,
    onMusicaClick: () -> Unit,
    onEstudiarClick: () -> Unit
) {
    val generoEmoji = when (tema.genero.lowercase()) {
        "rock"              -> "🎸"
        "balada"            -> "🎹"
        "clásica", "clasica" -> "🎻"
        else                -> "🎵"
    }

    Card(
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp),
        modifier  = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Icono circular con emoji de género
            Box(
                modifier = Modifier.size(52.dp).clip(CircleShape).background(PrimaryLight),
                contentAlignment = Alignment.Center
            ) { Text(generoEmoji, fontSize = 22.sp) }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = tema.titulo,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = InkColor,
                    maxLines   = 2
                )
                // Género como subtítulo
                Text(
                    text     = tema.genero,
                    fontSize = 12.sp,
                    color    = InkLight
                )
                tema.ultimaCalificacion?.let {
                    Spacer(Modifier.height(2.dp))
                    Text("Última calificación: $it/10", fontSize = 12.sp, color = InkLight)
                }

                Spacer(Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                    // Botón Música — siempre visible si tiene audioUrl
                    if (tema.audioUrl.isNotBlank()) {
                        Button(
                            onClick        = onMusicaClick,
                            shape          = RoundedCornerShape(20.dp),
                            colors         = ButtonDefaults.buttonColors(containerColor = PrimaryClr),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier       = Modifier.height(32.dp)
                        ) { Text("🎵 Música", fontSize = 12.sp) }
                    }

                    // Botón Estudiar — solo si tiene examen
                    if (tema.tieneExamen) {
                        Button(
                            onClick        = onEstudiarClick,
                            shape          = RoundedCornerShape(20.dp),
                            colors         = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier       = Modifier.height(32.dp)
                        ) { Text("📚 Estudiar", fontSize = 12.sp) }
                    }

                    // Si no tiene examen, muestra badge informativo
                    if (!tema.tieneExamen) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFFF5F5F5))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("Sin examen", fontSize = 11.sp, color = InkLight)
                        }
                    }
                }
            }
        }
    }
}