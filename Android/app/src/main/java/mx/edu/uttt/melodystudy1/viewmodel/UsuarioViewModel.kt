package mx.edu.uttt.melodystudy1.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mx.edu.uttt.melodystudy1.model.NivelConfig
import mx.edu.uttt.melodystudy1.model.UsuarioSesion
import mx.edu.uttt.melodystudy1.network.UsuarioApi

class UsuarioViewModel : ViewModel() {

    private val _sesion   = MutableStateFlow<UsuarioSesion?>(null)
    val sesion: StateFlow<UsuarioSesion?> = _sesion

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando

    private val _error    = MutableStateFlow("")
    val error: StateFlow<String> = _error

    // ── Login ─────────────────────────────────────────────────────────
    fun login(apodo: String, contrasena: String, onExito: (UsuarioSesion) -> Unit) {
        viewModelScope.launch {
            _cargando.value = true
            _error.value    = ""
            try {
                val resultado = UsuarioApi.login(apodo, contrasena)
                _sesion.value = resultado
                onExito(resultado)
            } catch (e: Exception) {
                _error.value = e.message ?: "Error desconocido"
            } finally {
                _cargando.value = false
            }
        }
    }

    // ── Registro ──────────────────────────────────────────────────────
    fun registrar(
        apodo: String, contrasena: String,
        avatar: String, nacimiento: String,
        onExito: (UsuarioSesion) -> Unit
    ) {
        viewModelScope.launch {
            _cargando.value = true
            _error.value    = ""
            try {
                val resultado = UsuarioApi.registrar(apodo, contrasena, avatar, nacimiento)
                _sesion.value = resultado
                onExito(resultado)
            } catch (e: Exception) {
                _error.value = e.message ?: "Error desconocido"
            } finally {
                _cargando.value = false
            }
        }
    }

    // ── Ganar XP (local, sin llamar al servidor por ahora) ────────────
    fun ganarXP(xp: Int) {
        val actual = _sesion.value ?: return
        val (nuevoNivel, nuevoProgreso) = NivelConfig.aplicarXP(
            nivelActual    = actual.nivel,
            progresoActual = actual.progreso,
            xpGanado       = xp
        )
        _sesion.value = actual.copy(nivel = nuevoNivel, progreso = nuevoProgreso)
        // TODO (siguiente fase): sincronizar con BD via PUT /usuarios/{id}/progreso
    }

    // ── Cerrar sesión ─────────────────────────────────────────────────
    fun cerrarSesion() {
        _sesion.value = null
        _error.value  = ""
    }
}