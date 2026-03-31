package mx.edu.uttt.melodystudy1.model

data class TemaGuardado(
    val id:                 Long,
    val titulo:             String,
    val genero:             String,
    val letra:              String,
    val audioUrl:           String    = "",
    val tieneExamen:        Boolean   = false,
    val ultimaCalificacion: Int?      = null,   // ← null si nunca se ha hecho
    val preguntas:          List<PreguntaUI> = emptyList()
)
