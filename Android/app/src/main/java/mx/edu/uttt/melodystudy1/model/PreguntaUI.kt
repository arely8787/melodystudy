package mx.edu.uttt.melodystudy1.model

data class PreguntaUI(
    val texto: String,
    val opciones: List<String>,
    val correcta: Int
)