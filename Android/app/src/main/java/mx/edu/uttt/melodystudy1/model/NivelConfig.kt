package mx.edu.uttt.melodystudy1.model

/**
 * Sistema de niveles completo.
 * La BD solo guarda: nivel (INT) y progreso (FLOAT 0.0–1.0)
 * Toda la lógica de XP vive aquí — sin tocar la BD.
 */
object NivelConfig {

    // ── Tabla de niveles: xpNecesario para COMPLETAR ese nivel ────────
    private val XP_POR_NIVEL = mapOf(
        1  to 100,
        2  to 200,
        3  to 350,
        4  to 500,
        5  to 700,
        6  to 950,
        7  to 1250,
        8  to 1600,
        9  to 2000,
        10 to 2000   // nivel máximo — no sube más
    )

    val NIVEL_MAXIMO = 10

    // ── Títulos por nivel ─────────────────────────────────────────────
    private val TITULO_POR_NIVEL = mapOf(
        1  to "Aprendiz",
        2  to "Estudiante",
        3  to "Curioso",
        4  to "Explorador",
        5  to "Melómano",
        6  to "Compositor",
        7  to "Maestro",
        8  to "Virtuoso",
        9  to "Leyenda",
        10 to "MelodyMaster"
    )

    // ── XP ganado por acción ──────────────────────────────────────────
    const val XP_EXAMEN_APROBADO   = 10   // calificación >= 6
    const val XP_EXAMEN_PERFECTO   = 20   // calificación == 10
    const val XP_CANCION_GUARDADA  = 5

    // ── Getters ───────────────────────────────────────────────────────

    /** XP necesario para completar el nivel actual */
    fun xpMaxDelNivel(nivel: Int): Int =
        XP_POR_NIVEL[nivel.coerceIn(1, NIVEL_MAXIMO)] ?: 100

    /** Título del nivel */
    fun tituloDelNivel(nivel: Int): String =
        TITULO_POR_NIVEL[nivel.coerceIn(1, NIVEL_MAXIMO)] ?: "Aprendiz"

    /**
     * Calcula cuánto XP ganó el usuario según su calificación.
     * Retorna 0 si reprobó (< 6).
     */
    fun xpDeExamen(calificacion: Int): Int = when {
        calificacion == 10 -> XP_EXAMEN_PERFECTO
        calificacion >= 6  -> XP_EXAMEN_APROBADO
        else               -> 0
    }

    /**
     * Aplica XP ganado al estado actual y devuelve el nuevo (nivel, progreso).
     *
     * @param nivelActual  nivel guardado en BD
     * @param progresoActual  progreso guardado en BD (0.0 – 1.0)
     * @param xpGanado  cuánto XP suma esta acción
     * @return Pair(nuevoNivel, nuevoProgreso)
     */
    fun aplicarXP(
        nivelActual: Int,
        progresoActual: Float,
        xpGanado: Int
    ): Pair<Int, Float> {
        if (xpGanado <= 0) return Pair(nivelActual, progresoActual)
        if (nivelActual >= NIVEL_MAXIMO) return Pair(NIVEL_MAXIMO, 1f)

        val xpMax      = xpMaxDelNivel(nivelActual)
        val xpActual   = (progresoActual * xpMax).toInt()
        var xpNuevo    = xpActual + xpGanado
        var nivel      = nivelActual

        // Subir de nivel si se supera el XP máximo
        while (xpNuevo >= xpMaxDelNivel(nivel) && nivel < NIVEL_MAXIMO) {
            xpNuevo -= xpMaxDelNivel(nivel)
            nivel++
        }

        // Si llegó al nivel máximo, bloquear en 1.0
        val progreso = if (nivel >= NIVEL_MAXIMO) 1f
        else (xpNuevo.toFloat() / xpMaxDelNivel(nivel)).coerceIn(0f, 1f)

        return Pair(nivel, progreso)
    }

    /**
     * XP actual como número entero (para mostrar en UI).
     * Ejemplo: nivel=2, progreso=0.35 → "70 / 200 XP"
     */
    fun xpActualInt(nivel: Int, progreso: Float): Int =
        (progreso * xpMaxDelNivel(nivel)).toInt()
}