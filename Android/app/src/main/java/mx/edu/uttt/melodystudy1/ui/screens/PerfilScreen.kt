package mx.edu.uttt.melodystudy1.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PrimaryClr   = Color(0xFF7C5CBF)
private val PrimaryLight = Color(0xFFEDE8FF)
private val BgColor      = Color(0xFFF7F5FF)
private val InkColor     = Color(0xFF1A1825)
private val InkLight     = Color(0xFF7B7490)

@Composable
fun PerfilScreen(
    apodo:          String = "Usuario",
    avatar:         String = "🎵",
    nivel:          Int    = 1,
    tituloNivel:    String = "Aprendiz",   // ← nuevo
    xpActual:       Int    = 0,            // ← reemplaza puntosActuales
    xpMax:          Int    = 100,          // ← reemplaza puntosTotales
    onCerrarSesion: () -> Unit = {}
) {
    val gradiente = Brush.verticalGradient(
        listOf(Color(0xFF7C5CBF), Color(0xFF9B7FD4))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradiente)
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Text(
                text = "Perfil",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // ── Avatar con emoji del registro ─────────────────────────
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(PrimaryLight),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text     = avatar,   // emoji guardado en BD
                    fontSize = 48.sp
                )
            }

            // ── Apodo ─────────────────────────────────────────────────
            Text(
                text      = apodo,
                fontSize  = 24.sp,
                fontWeight = FontWeight.Bold,
                color     = InkColor,
                textAlign = TextAlign.Center
            )

            // ── Nivel ─────────────────────────────────────────────────
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment    = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment    = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("⭐", fontSize = 22.sp)
                        Text("Nivel", fontSize = 15.sp, color = InkLight)
                    }
                    Text(
                        text       = "$nivel",
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color      = PrimaryClr
                    )
                }
            }

            // ── Puntuación + barra de progreso ────────────────────────
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment    = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment    = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("🏆", fontSize = 22.sp)
                            Text("Puntuación", fontSize = 15.sp, color = InkLight)
                        }
                        Text(
                            text       = "$xpActual / $xpMax XP",
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color      = PrimaryClr
                        )
                    }

                    LinearProgressIndicator(
                        progress  = { xpActual.toFloat() / xpMax.toFloat() },
                        modifier  = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50)),
                        color      = PrimaryClr,
                        trackColor = PrimaryLight
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // ── Cerrar sesión ─────────────────────────────────────────
            Button(
                onClick  = onCerrarSesion,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(26.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
            ) {
                Text("Cerrar sesión", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}