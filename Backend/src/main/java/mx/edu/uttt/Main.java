package mx.edu.uttt;

import io.javalin.Javalin;
import mx.edu.uttt.ai.AIAgent;
import mx.edu.uttt.config.Database;
import mx.edu.uttt.controller.UsuarioController;
import mx.edu.uttt.tts.TTSService;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class Main {

    record Song(long id, String audioKey, String tema, String genero, String letra, String audioPath) {}

    static final Map<Long, Song> songs = new ConcurrentHashMap<>();
    static final AtomicLong      idGen = new AtomicLong(1);

    static final Path AUDIO_DIR = Path.of("audio_files");

    static final String API_KEY = System.getenv("GROQ_KEY") != null
            ? System.getenv("GROQ_KEY")
            : System.getenv("OPENAI_KEY") != null
            ? System.getenv("OPENAI_KEY") : "";

    static final AIAgent    agent = API_KEY.isEmpty() ? null : new AIAgent(API_KEY);
    static final TTSService tts   = new TTSService();

    //static final String SERVER_HOST = "172.20.10.3";
    static final String SERVER_HOST = "192.168.1.150";

    public static void main(String[] args) throws IOException {

        Files.createDirectories(AUDIO_DIR);

        // Limpia temporales huérfanos al arrancar
        try (var stream = Files.list(AUDIO_DIR)) {
            stream.filter(p -> p.getFileName().toString().startsWith("temp_")
                            && p.getFileName().toString().endsWith(".mp3"))
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                            System.out.println("🗑️ Limpiado: " + p.getFileName());
                        } catch (IOException e) {
                            System.err.println("⚠️ No se pudo borrar " + p + ": " + e.getMessage());
                        }
                    });
        } catch (Exception e) {
            System.err.println("⚠️ Error limpiando temporales: " + e.getMessage());
        }

        Javalin app = Javalin.create(config ->
                config.plugins.enableCors(cors ->
                        cors.add(it -> it.anyHost())
                )
        ).start(7000);

        new UsuarioController().registrarRutas(app);

        app.get("/", ctx -> ctx.result("✅ MelodyStudy backend activo"));

        // ── POST /generate-song ───────────────────────────────────────
        app.post("/generate-song", ctx -> {
            JSONObject body;
            try { body = new JSONObject(ctx.body()); }
            catch (Exception e) {
                body = new JSONObject();
                body.put("tema", ctx.body().trim());
            }

            String tema   = body.optString("tema",   "música").trim();
            String genero = body.optString("genero", "pop").trim();
            if (tema.isEmpty()) tema = "música";

            String letra;
            if (agent != null) {
                try { letra = agent.ask("Genera una canción educativa sobre " + tema, genero); }
                catch (Exception e) { ctx.status(502).result("Error IA: " + e.getMessage()); return; }
            } else {
                letra = fallbackLetra(tema);
            }

            long   id       = idGen.getAndIncrement();
            String audioKey = java.util.UUID.randomUUID().toString(); // UUID único, nunca se repite
            songs.put(id, new Song(id, audioKey, tema, genero, letra, null));

            final long   songId        = id;
            final String textoFinal    = letra;
            final String generoFinal   = genero;
            final String audioKeyFinal = audioKey;

            Thread.ofVirtual().start(() -> {
                Path mp3 = tts.textToSpeech(textoFinal, audioKeyFinal, generoFinal);
                if (mp3 != null) {
                    Song s = songs.get(songId);
                    songs.put(songId, new Song(songId, audioKeyFinal,
                            s.tema(), s.genero(), s.letra(), mp3.toString()));
                    System.out.println("🎵 MP3 listo: " + mp3.toAbsolutePath());
                }
            });

            // Devuelve audioKey al cliente para que lo use en el polling y al guardar
            String audioUrl = "http://" + SERVER_HOST + ":7000/audio/temp/" + audioKey;
            ctx.contentType("application/json");
            ctx.result(buildSongJson(id, audioKey, tema, genero, letra, audioUrl, -1));
        });

        // ── POST /save-song ───────────────────────────────────────────
        app.post("/save-song", ctx -> {
            JSONObject body;
            try { body = new JSONObject(ctx.body()); }
            catch (Exception e) {
                ctx.status(400).result("{\"error\":\"Body JSON inválido\"}");
                return;
            }

            int    idUsuario = body.optInt   ("id_usuario", 0);
            String tema      = body.optString("tema",       "").trim();
            String genero    = body.optString("genero",     "pop").trim();
            String letra     = body.optString("letra",      "").trim();
            String audioKey  = body.optString("audio_key",  "").trim(); // UUID del temporal

            if (idUsuario <= 0 || tema.isEmpty() || letra.isEmpty()) {
                ctx.status(400).result("{\"error\":\"id_usuario, tema y letra son requeridos\"}");
                return;
            }

            // 1. Primero guardar en BD para obtener el dbCancionId
            long dbCancionId;
            try {
                dbCancionId = guardarCancionBD(idUsuario, tema, genero, letra, "");
            } catch (Exception e) {
                ctx.status(500).result("{\"error\":\"Error BD: " + e.getMessage() + "\"}");
                return;
            }

            // 2. Ahora mover temp_{uuid}.mp3 → cancion_{dbId}.mp3
            String audioUrlFinal = "";
            if (!audioKey.isEmpty()) {
                Path tempMp3  = AUDIO_DIR.resolve("temp_" + audioKey + ".mp3");
                Path finalMp3 = AUDIO_DIR.resolve("cancion_" + dbCancionId + ".mp3");
                try {
                    // Si el TTS todavía está procesando, esperar hasta 30 segundos
                    if (!Files.exists(tempMp3)) {
                        int intentos = 0;
                        while (!Files.exists(tempMp3) && intentos < 6) {
                            Thread.sleep(5000);
                            intentos++;
                        }
                    }
                    if (Files.exists(tempMp3)) {
                        Files.move(tempMp3, finalMp3, StandardCopyOption.REPLACE_EXISTING);
                        audioUrlFinal = "http://" + SERVER_HOST + ":7000/audio/cancion/" + dbCancionId;
                        System.out.println("✅ MP3 movido: " + finalMp3.toAbsolutePath());
                    } else {
                        System.err.println("⚠️ temp_" + audioKey + ".mp3 no encontrado tras espera");
                    }
                } catch (Exception e) {
                    System.err.println("⚠️ No se pudo mover MP3: " + e.getMessage());
                }
            }

            // 3. Actualizar audio_url en BD si se movió el archivo
            if (!audioUrlFinal.isEmpty()) {
                try { actualizarAudioUrlBD(dbCancionId, audioUrlFinal); }
                catch (Exception e) {
                    System.err.println("⚠️ No se pudo actualizar audio_url: " + e.getMessage());
                }
            }

            System.out.println("✅ Canción guardada en BD, id=" + dbCancionId + ", audio=" + audioUrlFinal);

            // 4. Generar y guardar examen
            String examJson;
            if (agent != null) {
                try {
                    examJson = agent.generarExamen(letra, tema);
                    examJson = examJson.trim()
                            .replaceAll("^```json\\s*", "")
                            .replaceAll("^```\\s*",     "")
                            .replaceAll("\\s*```$",     "")
                            .trim();
                    guardarExamenBD(dbCancionId, examJson);
                } catch (Exception e) {
                    System.err.println("⚠️ No se pudo guardar examen: " + e.getMessage());
                    examJson = fallbackExamen();
                }
            } else {
                examJson = fallbackExamen();
                try { guardarExamenBD(dbCancionId, examJson); } catch (Exception ignored) {}
            }

            ctx.contentType("application/json");
            ctx.result("{\"dbId\":" + dbCancionId
                    + ",\"audioUrl\":\"" + audioUrlFinal + "\""
                    + ",\"preguntas\":" + examJson + "}");
        });

        // ── GET /audio/temp/{audioKey} ────────────────────────────────
        // Ahora recibe UUID (String), no long
        app.get("/audio/temp/{audioKey}", ctx -> {
            String audioKey = ctx.pathParam("audioKey");
            if (audioKey.isBlank()) {
                ctx.status(400).result("Key inválida");
                return;
            }
            Path mp3 = AUDIO_DIR.resolve("temp_" + audioKey + ".mp3");
            servirMp3(ctx, mp3, "temp_" + audioKey);
        });

        // ── GET /audio/cancion/{dbId} ─────────────────────────────────
        app.get("/audio/cancion/{dbId}", ctx -> {
            long dbId;
            try { dbId = Long.parseLong(ctx.pathParam("dbId")); }
            catch (NumberFormatException e) { ctx.status(400).result("ID inválido"); return; }
            Path mp3 = AUDIO_DIR.resolve("cancion_" + dbId + ".mp3");
            servirMp3(ctx, mp3, "cancion_" + dbId);
        });

        // ── GET /audio/{id} (compatibilidad con canciones antiguas) ──
        app.get("/audio/{id}", ctx -> {
            long id;
            try { id = Long.parseLong(ctx.pathParam("id")); }
            catch (NumberFormatException e) { ctx.status(400).result("ID inválido"); return; }
            Song song = songs.get(id);
            if (song != null && song.audioPath() != null) {
                servirMp3(ctx, Path.of(song.audioPath()), "song_" + id);
                return;
            }
            ctx.status(404).result("Audio no disponible");
        });

        // ── GET /canciones/{id_usuario} ───────────────────────────────
        app.get("/canciones/{id_usuario}", ctx -> {
            int idUsuario;
            try { idUsuario = Integer.parseInt(ctx.pathParam("id_usuario")); }
            catch (NumberFormatException e) { ctx.status(400).result("ID inválido"); return; }
            try {
                ctx.contentType("application/json").result(obtenerCancionesBD(idUsuario));
            } catch (Exception e) {
                ctx.status(500).result("{\"error\":\"" + e.getMessage() + "\"}");
            }
        });

        // ── GET /examen/{id_cancion} ──────────────────────────────────
        app.get("/examen/{id_cancion}", ctx -> {
            long idCancion;
            try { idCancion = Long.parseLong(ctx.pathParam("id_cancion")); }
            catch (NumberFormatException e) { ctx.status(400).result("ID inválido"); return; }
            try {
                ctx.contentType("application/json").result(obtenerExamenBD(idCancion));
            } catch (Exception e) {
                ctx.status(500).result("{\"error\":\"" + e.getMessage() + "\"}");
            }
        });

        // ── PUT /examenes/{id_cancion}/calificacion ───────────────────
        app.put("/examenes/{id_cancion}/calificacion", ctx -> {
            long idCancion;
            try { idCancion = Long.parseLong(ctx.pathParam("id_cancion")); }
            catch (NumberFormatException e) {
                ctx.status(400).result("{\"error\":\"ID inválido\"}"); return;
            }

            JSONObject body;
            try { body = new JSONObject(ctx.body()); }
            catch (Exception e) {
                ctx.status(400).result("{\"error\":\"Body inválido\"}"); return;
            }

            int calificacion = body.optInt("calificacion", -1);
            if (calificacion < 0 || calificacion > 100) {
                ctx.status(400).result("{\"error\":\"calificacion debe ser 0-100\"}"); return;
            }

            try {
                String sql = "UPDATE examenes SET calificacion = ? WHERE id_cancion = ?";
                try (Connection cn = Database.getConnection();
                     PreparedStatement ps = cn.prepareStatement(sql)) {
                    ps.setInt (1, calificacion);
                    ps.setLong(2, idCancion);
                    ps.executeUpdate();
                }
                ctx.result("{\"ok\":true}");
            } catch (Exception e) {
                ctx.status(500).result("{\"error\":\"" + e.getMessage() + "\"}");
            }
        });

        // ── PUT /usuarios/{id}/xp ─────────────────────────────────────
        app.put("/usuarios/{id}/xp", ctx -> {
            int idUsuario;
            try { idUsuario = Integer.parseInt(ctx.pathParam("id")); }
            catch (NumberFormatException e) { ctx.status(400).result("ID inválido"); return; }

            JSONObject body;
            try { body = new JSONObject(ctx.body()); }
            catch (Exception e) { ctx.status(400).result("{\"error\":\"JSON inválido\"}"); return; }

            int   nivel    = body.optInt   ("nivel",    1);
            float progreso = (float) body.optDouble("progreso", 0.0);

            if (nivel < 1 || nivel > 10) {
                ctx.status(400).result("{\"error\":\"nivel debe ser 1–10\"}");
                return;
            }
            if (progreso < 0f || progreso > 1f) {
                ctx.status(400).result("{\"error\":\"progreso debe ser 0.0–1.0\"}");
                return;
            }

            try {
                actualizarNivelBD(idUsuario, nivel, progreso);
                ctx.contentType("application/json").result("{\"ok\":true}");
            } catch (Exception e) {
                ctx.status(500).result("{\"error\":\"" + e.getMessage() + "\"}");
            }
        });

        // ── DELETE /songs/{id} ────────────────────────────────────────
        app.delete("/songs/{id}", ctx -> {
            long id;
            try { id = Long.parseLong(ctx.pathParam("id")); }
            catch (NumberFormatException e) { ctx.status(400).result("ID inválido"); return; }
            songs.remove(id);
            ctx.result("Eliminado");
        });

        // ── POST /analyze-txt ─────────────────────────────────────────
        app.post("/analyze-txt", ctx -> {
            JSONObject body;
            try { body = new JSONObject(ctx.body()); }
            catch (Exception e) {
                ctx.status(400).result("{\"error\":\"Body inválido\"}"); return;
            }

            String contenido = body.optString("contenido", "").trim();
            if (contenido.isEmpty()) {
                ctx.status(400).result("{\"error\":\"contenido es requerido\"}"); return;
            }

            if (contenido.length() > 3000) contenido = contenido.substring(0, 3000);

            if (agent == null) {
                ctx.status(503).result("{\"error\":\"IA no disponible\"}"); return;
            }

            try {
                String prompt = """
                    Analiza el siguiente texto educativo y responde ÚNICAMENTE con un JSON válido sin markdown.
                    El JSON debe tener exactamente estos campos:
                    {
                      "tema": "título corto del tema principal (máximo 8 palabras)",
                      "resumen": "resumen educativo de 3-5 oraciones con los conceptos más importantes"
                    }
                    
                    Texto a analizar:
                    """ + contenido;

                String respuesta = agent.askRaw(prompt);

                respuesta = respuesta.trim()
                        .replaceAll("^```json\\s*", "")
                        .replaceAll("^```\\s*",     "")
                        .replaceAll("\\s*```$",     "")
                        .trim();

                int inicio = respuesta.indexOf('{');
                int fin    = respuesta.lastIndexOf('}');
                if (inicio >= 0 && fin > inicio) {
                    respuesta = respuesta.substring(inicio, fin + 1);
                }

                JSONObject resultado = new JSONObject(respuesta);

                if (!resultado.has("tema") || !resultado.has("resumen")) {
                    throw new Exception("Respuesta IA incompleta");
                }

                ctx.contentType("application/json").result(resultado.toString());

            } catch (Exception e) {
                ctx.status(500).result("{\"error\":\"Error IA: " + e.getMessage() + "\"}");
            }
        });
    }

    // ── Helper: servir MP3 ────────────────────────────────────────────
    static void servirMp3(io.javalin.http.Context ctx, Path mp3, String nombre) throws IOException {
        if (!Files.exists(mp3)) {
            ctx.status(404).result("Audio no disponible aún");
            return;
        }
        ctx.contentType("audio/mpeg");
        ctx.header("Content-Disposition", "inline; filename=\"" + nombre + ".mp3\"");
        long lastMod = Files.getLastModifiedTime(mp3).toMillis();
        ctx.header("Last-Modified", String.valueOf(lastMod));
        ctx.result(Files.newInputStream(mp3));
    }

    // ── BD: guardar canción ───────────────────────────────────────────
    static long guardarCancionBD(int idUsuario, String tema, String genero,
                                 String letra, String audioUrl) throws SQLException {
        String sql = "INSERT INTO canciones (tema, genero, letra, audio_url, id_usuario) VALUES (?,?,?,?,?)";
        try (Connection cn = Database.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, tema);
            ps.setString(2, genero);
            ps.setString(3, letra);
            ps.setString(4, audioUrl);
            ps.setInt   (5, idUsuario);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        throw new SQLException("No se generó ID de canción");
    }

    // ── BD: actualizar audio_url ──────────────────────────────────────
    static void actualizarAudioUrlBD(long idCancion, String audioUrl) throws SQLException {
        String sql = "UPDATE canciones SET audio_url = ? WHERE id = ?";
        try (Connection cn = Database.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, audioUrl);
            ps.setLong  (2, idCancion);
            ps.executeUpdate();
        }
    }

    // ── BD: guardar examen ────────────────────────────────────────────
    static void guardarExamenBD(long idCancion, String preguntasJson) throws Exception {
        long idExamen;
        try (Connection cn = Database.getConnection();
             PreparedStatement ps = cn.prepareStatement(
                     "INSERT INTO examenes (id_cancion) VALUES (?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, idCancion);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) idExamen = rs.getLong(1);
                else throw new SQLException("No se generó ID de examen");
            }
        }
        JSONArray preguntas = new JSONArray(preguntasJson);
        try (Connection cn = Database.getConnection();
             PreparedStatement ps = cn.prepareStatement(
                     "INSERT INTO preguntas (pregunta,opcion1,opcion2,opcion3,opcion4,opcion_correcta,id_examen) VALUES (?,?,?,?,?,?,?)")) {
            for (int i = 0; i < preguntas.length(); i++) {
                JSONObject p  = preguntas.getJSONObject(i);
                JSONArray  op = p.getJSONArray("opciones");
                ps.setString(1, p.getString("pregunta"));
                ps.setString(2, op.getString(0));
                ps.setString(3, op.getString(1));
                ps.setString(4, op.getString(2));
                ps.setString(5, op.getString(3));
                ps.setInt   (6, p.getInt("correcta") + 1);
                ps.setLong  (7, idExamen);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // ── BD: actualizar calificación del examen ────────────────────────
    static void actualizarCalificacionBD(long idCancion, int calificacion) throws SQLException {
        String sql = "UPDATE examenes SET calificacion = ? WHERE id_cancion = ?";
        try (Connection cn = Database.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt (1, calificacion);
            ps.setLong(2, idCancion);
            ps.executeUpdate();
        }
    }

    // ── BD: actualizar nivel y progreso del usuario ───────────────────
    static void actualizarNivelBD(int idUsuario, int nivel, float progreso) throws SQLException {
        String sql = "UPDATE usuarios SET nivel = ?, progreso = ? WHERE id = ?";
        try (Connection cn = Database.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt  (1, nivel);
            ps.setFloat(2, progreso);
            ps.setInt  (3, idUsuario);
            ps.executeUpdate();
        }
    }

    // ── BD: obtener canciones (incluye calificación) ──────────────────
    static String obtenerCancionesBD(int idUsuario) throws SQLException {
        String sql = """
            SELECT c.id, c.tema, c.genero, c.letra, c.audio_url,
                   CASE WHEN e.id IS NOT NULL THEN 1 ELSE 0 END AS tiene_examen,
                   e.calificacion
            FROM canciones c
            LEFT JOIN examenes e ON e.id_cancion = c.id
            WHERE c.id_usuario = ?
            ORDER BY c.id DESC
            """;
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        try (Connection cn = Database.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (!first) sb.append(",");
                    first = false;
                    int cal = rs.getInt("calificacion");
                    boolean calNull = rs.wasNull();
                    sb.append("{")
                            .append("\"id\":").append(rs.getLong("id")).append(",")
                            .append("\"tema\":").append(jsonStr(rs.getString("tema"))).append(",")
                            .append("\"genero\":").append(jsonStr(rs.getString("genero"))).append(",")
                            .append("\"letra\":").append(jsonStr(rs.getString("letra"))).append(",")
                            .append("\"audioUrl\":").append(jsonStr(rs.getString("audio_url"))).append(",")
                            .append("\"tieneExamen\":").append(rs.getInt("tiene_examen") == 1).append(",")
                            .append("\"ultimaCalificacion\":").append(calNull ? "null" : cal)
                            .append("}");
                }
            }
        }
        return sb.append("]").toString();
    }

    // ── BD: obtener preguntas de examen ───────────────────────────────
    static String obtenerExamenBD(long idCancion) throws SQLException {
        String sql = """
                SELECT p.pregunta, p.opcion1, p.opcion2, p.opcion3, p.opcion4, p.opcion_correcta
                FROM preguntas p
                JOIN examenes e ON e.id = p.id_examen
                WHERE e.id_cancion = ?
                ORDER BY p.id
                """;
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        try (Connection cn = Database.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, idCancion);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (!first) sb.append(",");
                    first = false;
                    int correcta = rs.getInt("opcion_correcta") - 1;
                    sb.append("{")
                            .append("\"pregunta\":").append(jsonStr(rs.getString("pregunta"))).append(",")
                            .append("\"opciones\":[")
                            .append(jsonStr(rs.getString("opcion1"))).append(",")
                            .append(jsonStr(rs.getString("opcion2"))).append(",")
                            .append(jsonStr(rs.getString("opcion3"))).append(",")
                            .append(jsonStr(rs.getString("opcion4")))
                            .append("],\"correcta\":").append(correcta)
                            .append("}");
                }
            }
        }
        return sb.append("]").toString();
    }

    // ── Helpers ───────────────────────────────────────────────────────
    // audioKey agregado al JSON para que el cliente lo use en /save-song
    static String buildSongJson(long id, String audioKey, String tema, String genero,
                                String letra, String audioUrl, long dbId) {
        return "{\"id\":"       + id                + ",\"dbId\":"    + dbId
                + ",\"audioKey\":" + jsonStr(audioKey) // ← cliente lo guarda y lo manda en save-song
                + ",\"tema\":"     + jsonStr(tema)
                + ",\"genero\":"   + jsonStr(genero)
                + ",\"letra\":"    + jsonStr(letra)
                + ",\"audioUrl\":" + jsonStr(audioUrl) + "}";
    }

    static String jsonStr(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }

    static String fallbackLetra(String tema) {
        return tema + " es importante estudiar,\ncon música lo vas a recordar,\n"
                + "aprende fácil, aprende mejor,\nMelodyStudy lo hace con amor.";
    }

    static String fallbackExamen() {
        return "[{\"pregunta\":\"¿Cuál es el tema principal?\","
                + "\"opciones\":[\"Opción A\",\"Opción B\",\"Opción C\",\"Opción D\"],\"correcta\":0},"
                + "{\"pregunta\":\"¿Qué concepto se menciona?\","
                + "\"opciones\":[\"Opción A\",\"Opción B\",\"Opción C\",\"Opción D\"],\"correcta\":1},"
                + "{\"pregunta\":\"¿Cuál es la idea central?\","
                + "\"opciones\":[\"Opción A\",\"Opción B\",\"Opción C\",\"Opción D\"],\"correcta\":0},"
                + "{\"pregunta\":\"¿Qué se aprende?\","
                + "\"opciones\":[\"Opción A\",\"Opción B\",\"Opción C\",\"Opción D\"],\"correcta\":2},"
                + "{\"pregunta\":\"¿Cómo se relaciona con el estudio?\","
                + "\"opciones\":[\"Opción A\",\"Opción B\",\"Opción C\",\"Opción D\"],\"correcta\":3}]";
    }
}