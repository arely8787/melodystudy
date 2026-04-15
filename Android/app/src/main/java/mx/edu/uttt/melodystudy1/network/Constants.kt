package mx.edu.uttt.melodystudy1.network

object Constants {
    //private const val HOST = "172.20.10.3" //tel romina
    private const val HOST = "192.168.1.150" //casa
    const val BASE_URL = "http://$HOST:7000"

    // ── Canciones ─────────────────────────────────────────────────────
    const val GENERATE_SONG = "$BASE_URL/generate-song"
    const val SAVE_SONG     = "$BASE_URL/save-song"
    const val GET_SONGS     = "$BASE_URL/songs"
    const val GET_AUDIO     = "$BASE_URL/audio"
    const val DELETE_SONG   = "$BASE_URL/songs"

    // ── Examen ────────────────────────────────────────────────────────
    const val GENERATE_EXAM = "$BASE_URL/generate-exam"


    // ── Canciones guardadas del usuario ───────────────────────────────
    /** Usar como: "${CANCIONES_USUARIO}/123" */
    const val CANCIONES_USUARIO = "$BASE_URL/canciones"

    /** Usar como: "${EXAMEN_CANCION}/123" */
    const val EXAMEN_CANCION = "$BASE_URL/examen"

    // ── Calificación del examen ───────────────────────────────────────
    /** PUT "${CALIFICACION_EXAMEN}/123/calificacion"  body: {"calificacion": 85} */
    const val CALIFICACION_EXAMEN = "$BASE_URL/examenes"

    // ── Usuarios ──────────────────────────────────────────────────────
    const val REGISTER = "$BASE_URL/register"
    const val LOGIN    = "$BASE_URL/login"

    /** PUT "${USUARIO_XP}/123/xp"  body: {"nivel": 2, "progreso": 0.35} */
    const val USUARIO_XP = "$BASE_URL/usuarios"

    const val ANALYZE_TXT = "$BASE_URL/analyze-txt"
}