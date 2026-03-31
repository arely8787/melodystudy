package mx.edu.uttt.melodystudy1.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import mx.edu.uttt.melodystudy1.model.NivelConfig
import mx.edu.uttt.melodystudy1.model.TemaGuardado
import mx.edu.uttt.melodystudy1.ui.components.AppHeader
import mx.edu.uttt.melodystudy1.ui.screens.*
import mx.edu.uttt.melodystudy1.viewmodel.UsuarioViewModel
import okhttp3.OkHttpClient

@Composable
fun MelodyStudyApp(client: OkHttpClient) {
    val navController  = rememberNavController()
    var selectedScreen by remember { mutableStateOf("inicio") }
    val scope          = rememberCoroutineScope()

    val temasGuardados = remember { mutableStateListOf<TemaGuardado>() }
    var temasRemotos   by remember { mutableStateOf<List<TemaGuardado>>(emptyList()) }

    // Lista unificada: remotos primero, luego locales no duplicados
    val todosLosTemas = remember(temasRemotos, temasGuardados.toList()) {
        val remotosIds = temasRemotos.map { it.id }.toSet()
        temasRemotos + temasGuardados.filter { it.id !in remotosIds }
    }

    val usuarioViewModel: UsuarioViewModel = viewModel()
    val sesion by usuarioViewModel.sesion.collectAsState()

    LaunchedEffect(sesion?.id) {
        val id = sesion?.id ?: return@LaunchedEffect
        if (id <= 0) return@LaunchedEffect
        try {
            temasRemotos = cargarCancionesBD(id)
        } catch (_: Exception) { }
    }

    val rutasSinNav  = listOf("splash", "login", "registro")
    val currentRoute by navController.currentBackStackEntryAsState()
    val mostrarNav   = currentRoute?.destination?.route?.let { ruta ->
        rutasSinNav.none { ruta.startsWith(it) }
    } ?: false

    Scaffold(
        topBar = {
            if (mostrarNav) {
                AppHeader(
                    nombreApp       = sesion?.apodo  ?: "Usuario",
                    avatar          = sesion?.avatar ?: "🎵",
                    nivel           = "Nivel ${sesion?.nivel ?: 1} · ${sesion?.titulo ?: "Aprendiz"}",
                    puntosActuales  = sesion?.xpActual ?: 0,
                    puntosTotales   = sesion?.xpMax    ?: 100
                )
            }
        },
        bottomBar = {
            if (mostrarNav) {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedScreen == "inicio",
                        onClick  = {
                            selectedScreen = "inicio"
                            navController.navigate("inicio") { popUpTo("inicio") }
                        },
                        label = { Text("Inicio") },
                        icon  = {
                            Icon(
                                if (selectedScreen == "inicio") Icons.Filled.Home
                                else Icons.Outlined.Home,
                                contentDescription = "Inicio"
                            )
                        }
                    )
                    NavigationBarItem(
                        selected = selectedScreen == "crear",
                        onClick  = {
                            selectedScreen = "crear"
                            navController.navigate("crear") { popUpTo("crear") }
                        },
                        label = { Text("Crear") },
                        icon  = { Icon(Icons.Outlined.Add, contentDescription = "Crear") }
                    )
                    NavigationBarItem(
                        selected = selectedScreen == "estudiar",
                        onClick  = {
                            selectedScreen = "estudiar"
                            navController.navigate("estudiar") { popUpTo("estudiar") }
                        },
                        label = { Text("Estudiar") },
                        icon  = { Icon(Icons.Outlined.MenuBook, contentDescription = "Estudiar") }
                    )
                    NavigationBarItem(
                        selected = selectedScreen == "perfil",
                        onClick  = {
                            selectedScreen = "perfil"
                            navController.navigate("perfil") { popUpTo("perfil") }
                        },
                        label = { Text("Perfil") },
                        icon  = {
                            Icon(
                                if (selectedScreen == "perfil") Icons.Filled.Person
                                else Icons.Outlined.Person,
                                contentDescription = "Perfil"
                            )
                        }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController    = navController,
            startDestination = "splash",
            modifier         = Modifier.padding(padding)
        ) {

            // ── Splash ────────────────────────────────────────────────
            composable("splash") {
                SplashScreen(
                    onSplashFinished = {
                        navController.navigate("login") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                )
            }

            // ── Login ─────────────────────────────────────────────────
            composable("login") {
                LoginScreen(
                    viewModel = usuarioViewModel,
                    onEntrar  = {
                        navController.navigate("inicio") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onRegistrarse = { navController.navigate("registro") }
                )
            }

            // ── Registro ──────────────────────────────────────────────
            composable("registro") {
                RegistroScreen(
                    viewModel = usuarioViewModel,
                    onGuardar = { _ ->
                        navController.navigate("login") {
                            popUpTo("login") { inclusive = false }
                        }
                    }
                )
            }

            // ── Inicio ────────────────────────────────────────────────
            composable("inicio") {
                selectedScreen = "inicio"
                InicioScreen(
                    apodo       = sesion?.apodo ?: "Usuario",
                    temas       = todosLosTemas,
                    irACrear    = {
                        selectedScreen = "crear"
                        navController.navigate("crear")
                    },
                    irAEstudiar = {
                        selectedScreen = "estudiar"
                        navController.navigate("estudiar")
                    },
                    irAExamen   = { tema ->           // ← nuevo
                        selectedScreen = "estudiar"
                        navController.navigate("examen/${tema.id}")
                    }
                )
            }

            // ── Crear ─────────────────────────────────────────────────
            composable("crear") {
                selectedScreen = "crear"
                CrearScreen(
                    idUsuario = sesion?.id ?: 0,
                    onGuardar = { tema ->
                        temasGuardados.add(tema)

                        // XP por canción guardada — actualizar local + BD
                        usuarioViewModel.ganarXP(NivelConfig.XP_CANCION_GUARDADA)
                        val s = usuarioViewModel.sesion.value
                        if (s != null) {
                            scope.launch {
                                try { actualizarXPUsuarioBD(s.id, s.nivel, s.progreso) }
                                catch (_: Exception) { }
                            }
                        }

                        selectedScreen = "estudiar"
                        navController.navigate("estudiar")
                    }
                )
            }

            // ── Estudiar ──────────────────────────────────────────────
            composable("estudiar") {
                selectedScreen = "estudiar"
                EstudiarScreen(
                    idUsuario            = sesion?.id ?: 0,
                    temasLocales         = temasGuardados,
                    temasRemotos         = temasRemotos,
                    onTemasRemotosLoaded = { temasRemotos = it },
                    onMusicaClick        = { tema -> navController.navigate("musica/${tema.id}") },
                    onEstudiarClick      = { tema -> navController.navigate("examen/${tema.id}") }
                )
            }

            // ── Música ────────────────────────────────────────────────
            composable(
                "musica/{temaId}",
                arguments = listOf(navArgument("temaId") { type = NavType.LongType })
            ) { backStack ->
                val id   = backStack.arguments?.getLong("temaId") ?: 0L
                val tema = todosLosTemas.find { it.id == id }

                if (tema != null) {
                    MusicaScreen(tema = tema, onVolver = { navController.popBackStack() })
                } else {
                    Text("Tema no encontrado")
                }
            }

            // ── Examen ────────────────────────────────────────────────
            // Recibe el ID del tema (Long), carga las preguntas internamente
            composable(
                "examen/{temaId}",
                arguments = listOf(navArgument("temaId") { type = NavType.LongType })
            ) { backStack ->
                val id   = backStack.arguments?.getLong("temaId") ?: 0L
                val tema = todosLosTemas.find { it.id == id }

                if (tema != null) {
                    ExamenScreen(
                        tema = tema,
                        onIniciarCuestionario = { temaConPreguntas ->
                            // Guardamos en la lista para que CuestionarioScreen lo encuentre
                            val idxRemoto = temasRemotos.indexOfFirst { it.id == temaConPreguntas.id }
                            if (idxRemoto >= 0) {
                                temasRemotos = temasRemotos.toMutableList().also {
                                    it[idxRemoto] = temaConPreguntas
                                }
                            } else {
                                val idxLocal = temasGuardados.indexOfFirst { it.id == temaConPreguntas.id }
                                if (idxLocal >= 0) temasGuardados[idxLocal] = temaConPreguntas
                            }
                            navController.navigate("cuestionario/${temaConPreguntas.id}")
                        },
                        onVolver = { navController.popBackStack() }
                    )
                } else {
                    Text("Tema no encontrado")
                }
            }

            // ── Cuestionario ──────────────────────────────────────────
            composable(
                "cuestionario/{temaId}",
                arguments = listOf(navArgument("temaId") { type = NavType.LongType })
            ) { backStack ->
                val id   = backStack.arguments?.getLong("temaId") ?: 0L
                val tema = todosLosTemas.find { it.id == id }

                if (tema != null) {
                    CuestionarioScreen(
                        tema      = tema,
                        idUsuario = sesion?.id ?: 0,
                        onTerminar = { calificacion ->
                            // 1. Calcular XP según calificación (escala 0–100)
                            val xpGanado = NivelConfig.xpDeExamen(calificacion)

                            // 2. Actualizar sesión local
                            if (xpGanado > 0) usuarioViewModel.ganarXP(xpGanado)

                            // 3. Sincronizar nivel/progreso con BD del servidor
                            val s = usuarioViewModel.sesion.value
                            if (xpGanado > 0 && s != null) {
                                scope.launch {
                                    try { actualizarXPUsuarioBD(s.id, s.nivel, s.progreso) }
                                    catch (_: Exception) { }
                                }
                            }

                            // 4. Actualizar calificación en la lista local (remotos y locales)
                            val idxRemoto = temasRemotos.indexOfFirst { it.id == id }
                            if (idxRemoto >= 0) {
                                temasRemotos = temasRemotos.toMutableList().also {
                                    it[idxRemoto] = it[idxRemoto].copy(ultimaCalificacion = calificacion)
                                }
                            } else {
                                val idxLocal = temasGuardados.indexOfFirst { it.id == id }
                                if (idxLocal >= 0) {
                                    temasGuardados[idxLocal] = temasGuardados[idxLocal]
                                        .copy(ultimaCalificacion = calificacion)
                                }
                            }

                            navController.popBackStack() // vuelve al examen
                            navController.popBackStack() // vuelve a estudiar
                        },
                        onVolver = { navController.popBackStack() }
                    )
                } else {
                    Text("Tema no encontrado")
                }
            }

            // ── Perfil ────────────────────────────────────────────────
            composable("perfil") {
                selectedScreen = "perfil"
                PerfilScreen(
                    apodo         = sesion?.apodo   ?: "Usuario",
                    avatar        = sesion?.avatar  ?: "🎵",
                    nivel         = sesion?.nivel   ?: 1,
                    tituloNivel   = sesion?.titulo  ?: "Aprendiz",
                    xpActual      = sesion?.xpActual ?: 0,
                    xpMax         = sesion?.xpMax   ?: 100,
                    onCerrarSesion = {
                        usuarioViewModel.cerrarSesion()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}