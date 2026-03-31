package mx.edu.uttt.melodystudy1.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.edu.uttt.melodystudy1.model.UsuarioSesion
import mx.edu.uttt.melodystudy1.viewmodel.UsuarioViewModel

private val PrimaryClr   = Color(0xFF7C5CBF)
private val PrimaryLight = Color(0xFFEDE8FF)
private val BgColor      = Color(0xFFF7F5FF)
private val InkColor     = Color(0xFF1A1825)
private val InkLight     = Color(0xFF7B7490)

@Composable
fun LoginScreen(
    viewModel: UsuarioViewModel,
    onEntrar: (UsuarioSesion) -> Unit,
    onRegistrarse: () -> Unit
){
    val bgGradient = Brush.verticalGradient(
        listOf(Color(0xFF4A1A8A), Color(0xFF7C5CBF))
    )
    val cargando by viewModel.cargando.collectAsState()
    val error    by viewModel.error.collectAsState()

    var apodo        by remember { mutableStateOf("") }
    var contrasena   by remember { mutableStateOf("") }
    var verPassword  by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header con logo y título
            Column(
                modifier = Modifier.padding(top = 60.dp, bottom = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎵", fontSize = 34.sp)
                }
                Text(
                    text = "Login",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }

            // Card del formulario
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {

                    // Campo Apodo
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Apodo",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = InkLight
                        )
                        OutlinedTextField(
                            value = apodo,
                            onValueChange = { apodo = it },
                            placeholder = { Text("Tu apodo", color = InkLight.copy(alpha = 0.6f)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryClr,
                                unfocusedBorderColor = Color(0xFFDDD8F0),
                                focusedContainerColor = BgColor,
                                unfocusedContainerColor = BgColor,
                                cursorColor = PrimaryClr
                            ),
                            singleLine = true
                        )
                    }

                    // Campo Contraseña
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Contraseña",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = InkLight
                        )
                        OutlinedTextField(
                            value = contrasena,
                            onValueChange = { contrasena = it },
                            placeholder = { Text("Tu contraseña", color = InkLight.copy(alpha = 0.6f)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            visualTransformation = if (verPassword)
                                VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { verPassword = !verPassword }) {
                                    Icon(
                                        imageVector = if (verPassword)
                                            Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                        contentDescription = if (verPassword) "Ocultar" else "Mostrar",
                                        tint = InkLight
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryClr,
                                unfocusedBorderColor = Color(0xFFDDD8F0),
                                focusedContainerColor = BgColor,
                                unfocusedContainerColor = BgColor,
                                cursorColor = PrimaryClr
                            ),
                            singleLine = true
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    // Botón Entrar
                    Button(
                        onClick = {
                            viewModel.login(apodo, contrasena) { sesion ->
                                onEntrar(sesion)
                            }
                        },
                        enabled  = !cargando && apodo.isNotBlank() && contrasena.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = RoundedCornerShape(26.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = PrimaryClr)
                    ){
                        Text(
                            text = "Entrar",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (error.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text     = "⚠️ $error",
                            color    = Color(0xFFC62828),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Link Registrarse
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = buildAnnotatedString {
                                append("¿No tienes cuenta? ")
                                withStyle(
                                    SpanStyle(
                                        color = PrimaryClr,
                                        fontWeight = FontWeight.Bold
                                    )
                                ) {
                                    append("Registrarse")
                                }
                            },
                            fontSize = 14.sp,
                            color = InkLight,
                            modifier = Modifier.clickable { onRegistrarse() }
                        )
                    }
                }
            }
        }
    }
}