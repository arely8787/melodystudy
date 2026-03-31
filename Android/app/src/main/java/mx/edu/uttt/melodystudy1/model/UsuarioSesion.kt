package mx.edu.uttt.melodystudy1.model

data class UsuarioSesion(
    val id:         Int,
    val apodo:      String,
    val avatar:     String,
    val nacimiento: String,
    val nivel:      Int,
    val progreso:   Float   // 0.0 – 1.0 tal como viene de la BD
) {
    // Helpers calculados — no se guardan en BD
    val titulo: String get() = NivelConfig.tituloDelNivel(nivel)
    val xpActual: Int  get() = NivelConfig.xpActualInt(nivel, progreso)
    val xpMax: Int     get() = NivelConfig.xpMaxDelNivel(nivel)
}