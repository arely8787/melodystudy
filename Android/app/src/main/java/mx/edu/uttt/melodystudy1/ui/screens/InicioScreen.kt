package mx.edu.uttt.melodystudy1.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.edu.uttt.melodystudy1.model.TemaGuardado

private val PrimaryClr = Color(0xFF7C5CBF)

@Composable
fun InicioScreen(
    irACrear:      () -> Unit,
    irAEstudiar:   () -> Unit = {},
    irAExamen:     (TemaGuardado) -> Unit = {},   // ← nuevo
    temas:         List<TemaGuardado> = emptyList(),
    apodo:         String = "Apodo"
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {

        // Bienvenida con apodo
        Text(
            text       = "Bienvenido $apodo",
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        // Pregunta
        Text(
            text  = "¿Desea estudiar algún tema nuevo?",
            style = MaterialTheme.typography.bodyLarge
        )

        // Botón Comenzar → va a Crear
        Button(
            onClick  = irACrear,
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(16.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = PrimaryClr)
        ) {
            Text("Comenzar 🎵", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        // Card de temas estudiados
        Card(
            modifier  = Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(3.dp)
        ) {
            Column(
                modifier            = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text       = "Temas estudiados",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (temas.isEmpty()) {
                    Text(
                        text  = "Aún no tienes temas guardados.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                } else {
                    // Acceso rápido a cada tema con examen
                    temas.filter { it.tieneExamen }.take(5).forEach { tema ->
                        val generoEmoji = when (tema.genero.lowercase()) {
                            "rock"               -> "🎸"
                            "balada"             -> "🎹"
                            "clásica", "clasica" -> "🎻"
                            else                 -> "🎵"
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { irAExamen(tema) }
                                .background(Color(0xFFF3F0FF))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Emoji género
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEDE8FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(generoEmoji, fontSize = 16.sp)
                            }

                            // Título y calificación
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text       = tema.titulo,
                                    fontSize   = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color      = Color(0xFF1A1825),
                                    maxLines   = 1
                                )
                                Text(
                                    text     = if (tema.ultimaCalificacion != null)
                                        "Última: ${tema.ultimaCalificacion}/100"
                                    else "Sin intentos",
                                    fontSize = 11.sp,
                                    color    = Color(0xFF7B7490)
                                )
                            }

                            // Badge ir al examen
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(PrimaryClr)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Examinar →", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }

                    // Si hay temas sin examen, muéstralos sin botón de acción
                    temas.filter { !it.tieneExamen }.take(3).forEach { tema ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("🎵", fontSize = 14.sp)
                            Text(
                                text  = tema.titulo,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF1A1825)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Botón Ir a estudiar
                Button(
                    onClick  = irAEstudiar,
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("Ir a estudiar 📚", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}