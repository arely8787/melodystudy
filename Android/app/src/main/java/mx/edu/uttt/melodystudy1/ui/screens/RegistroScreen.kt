package mx.edu.uttt.melodystudy1.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.edu.uttt.melodystudy1.model.UsuarioSesion
import mx.edu.uttt.melodystudy1.viewmodel.UsuarioViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val PrimaryClrR   = Color(0xFF7C5CBF)
private val PrimaryLightR = Color(0xFFEDE8FF)
private val BgColorR      = Color(0xFFF7F5FF)
private val InkLightR     = Color(0xFF7B7490)

private val AVATARES = listOf("🐱", "🐶", "🦊", "🐼", "🐨", "🦁", "🐸", "🐧", "🦄", "🐙")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistroScreen(
    viewModel: UsuarioViewModel,
    onGuardar: (UsuarioSesion) -> Unit
) {

    val bgGradient = Brush.verticalGradient(
        listOf(Color(0xFF4A1A8A), Color(0xFF7C5CBF))
    )

    val cargando by viewModel.cargando.collectAsState()
    val error    by viewModel.error.collectAsState()

    var avatarSeleccionado by remember { mutableStateOf(0) }
    var apodo              by remember { mutableStateOf("") }
    var contrasena         by remember { mutableStateOf("") }
    var verPassword        by remember { mutableStateOf(false) }

    // Date picker
    var mostrarCalendario  by remember { mutableStateOf(false) }
    var fechaNacimiento    by remember { mutableStateOf<LocalDate?>(null) }
    val datePickerState    = rememberDatePickerState()

    if (mostrarCalendario) {
        DatePickerDialog(
            onDismissRequest = { mostrarCalendario = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        fechaNacimiento = LocalDate.ofEpochDay(millis / 86_400_000)
                    }
                    mostrarCalendario = false
                }) { Text("Aceptar", color = PrimaryClrR) }
            },
            dismissButton = {
                TextButton(onClick = { mostrarCalendario = false }) {
                    Text("Cancelar", color = InkLightR)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Título
            Column(
                modifier = Modifier.padding(top = 52.dp, bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🎵", fontSize = 30.sp)
                Text(
                    text = "Registrarse",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "Crea tu cuenta para comenzar",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }

            // Card del formulario
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {

                    // Selector de avatar
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Elige tu avatar",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = InkLightR
                        )

                        // Avatar seleccionado grande
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryLightR)
                                    .border(3.dp, PrimaryClrR, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(AVATARES[avatarSeleccionado], fontSize = 36.sp)
                            }
                        }

                        // Grid de avatares
                        val filas = AVATARES.chunked(5)
                        filas.forEach { fila ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                fila.forEachIndexed { _, avatar ->
                                    val idx = AVATARES.indexOf(avatar)
                                    val seleccionado = avatarSeleccionado == idx
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (seleccionado) PrimaryLightR else Color(0xFFF5F5F5)
                                            )
                                            .border(
                                                width = if (seleccionado) 2.dp else 0.dp,
                                                color = if (seleccionado) PrimaryClrR else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable { avatarSeleccionado = idx },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(avatar, fontSize = 22.sp)
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFFEEEEEE))

                    // Campo Apodo
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Apodo",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = InkLightR
                        )
                        OutlinedTextField(
                            value = apodo,
                            onValueChange = { apodo = it },
                            placeholder = { Text("Tu apodo", color = InkLightR.copy(alpha = 0.6f)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryClrR,
                                unfocusedBorderColor = Color(0xFFDDD8F0),
                                focusedContainerColor = BgColorR,
                                unfocusedContainerColor = BgColorR,
                                cursorColor = PrimaryClrR
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
                            color = InkLightR
                        )
                        OutlinedTextField(
                            value = contrasena,
                            onValueChange = { contrasena = it },
                            placeholder = { Text("Tu contraseña", color = InkLightR.copy(alpha = 0.6f)) },
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
                                        tint = InkLightR
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryClrR,
                                unfocusedBorderColor = Color(0xFFDDD8F0),
                                focusedContainerColor = BgColorR,
                                unfocusedContainerColor = BgColorR,
                                cursorColor = PrimaryClrR
                            ),
                            singleLine = true
                        )
                    }

                    // Campo Fecha de cumpleaños
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Fecha de cumpleaños",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = InkLightR
                        )
                        OutlinedTextField(
                            value = fechaNacimiento?.format(
                                DateTimeFormatter.ofPattern("dd / MM / yyyy")
                            ) ?: "",
                            onValueChange = {},
                            readOnly = true,
                            placeholder = { Text("dd / mm / aaaa", color = InkLightR.copy(alpha = 0.6f)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { mostrarCalendario = true },
                            shape = RoundedCornerShape(14.dp),
                            trailingIcon = {
                                IconButton(onClick = { mostrarCalendario = true }) {
                                    Icon(
                                        imageVector = Icons.Filled.CalendarMonth,
                                        contentDescription = "Calendario",
                                        tint = PrimaryClrR
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryClrR,
                                unfocusedBorderColor = Color(0xFFDDD8F0),
                                focusedContainerColor = BgColorR,
                                unfocusedContainerColor = BgColorR,
                                cursorColor = PrimaryClrR,
                                disabledBorderColor = Color(0xFFDDD8F0),
                                disabledContainerColor = BgColorR
                            ),
                            enabled = false
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    // Botón Guardar
                    Button(
                        onClick = {
                            // nacimiento viene del DatePicker como LocalDate
                            // lo formateamos a "YYYY-MM-DD" para el backend
                            val nacimientoStr = fechaNacimiento
                                ?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                                ?: "2000-01-01"

                            viewModel.registrar(
                                apodo      = apodo,
                                contrasena = contrasena,
                                avatar     = AVATARES[avatarSeleccionado],
                                nacimiento = nacimientoStr
                            ) { sesion ->
                                onGuardar(sesion)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryClrR)
                    ) {
                        Text(
                            text = "Guardar",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (error.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text      = "⚠️ $error",
                            color     = Color(0xFFC62828),
                            fontSize  = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier  = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}