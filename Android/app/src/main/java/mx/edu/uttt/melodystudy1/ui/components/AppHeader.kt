package mx.edu.uttt.melodystudy1.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AppHeader(
    nombreApp:     String = "Usuario",
    avatar:        String = "🎵",        // ← nuevo
    nivel:         String = "Nivel 1",
    puntosActuales: Int   = 50,
    puntosTotales:  Int   = 100
) {

    val progreso = puntosActuales.toFloat() / puntosTotales.toFloat()

    // 🎨 Nuevo gradiente morado-azul
    val gradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF7E57C2), // morado
            Color(0xFF5C6BC0), // morado azulado
            Color(0xFF42A5F5)  // azul suave
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(gradient)
            .statusBarsPadding()
            .padding(16.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // 🔹 Columna izquierda
            Column(
                modifier = Modifier.weight(0.4f)
            ) {

                Row(verticalAlignment = Alignment.CenterVertically) {

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(avatar)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = nombreApp,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = nivel,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.2f))

            // 🔹 Columna derecha
            Column(
                modifier = Modifier.weight(0.4f),
                horizontalAlignment = Alignment.End
            ) {

                Text(
                    text = "$puntosActuales/$puntosTotales pts",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = progreso,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
            }
        }
    }
}