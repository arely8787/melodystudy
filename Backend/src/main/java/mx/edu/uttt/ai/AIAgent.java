package mx.edu.uttt.ai;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;

public class AIAgent {

    private static final String GROQ_URL =
            "https://api.groq.com/openai/v1/chat/completions";

    private final String apiKey;
    private final HttpClient http = HttpClient.newHttpClient();

    public AIAgent(String apiKey) {
        this.apiKey = apiKey;
    }

    // ── Genera la letra de una canción con el género indicado ─────────
    public String ask(String prompt, String genero) throws Exception {

        // Validación de que es una petición de canción
        String lower = prompt.toLowerCase();
        boolean valido = lower.contains("canción") || lower.contains("cancion")
                || lower.contains("song")    || lower.contains("letra")
                || lower.contains("canta")   || lower.contains("genera")
                || lower.contains("escribe") || lower.contains("sobre")
                || lower.contains("acerca");

        if (!valido) {
            return "🎵 Solo genero letras de canciones. " +
                    "Ejemplo: 'Genera una canción sobre el sistema solar'";
        }

        // Instrucciones específicas por género
        String instruccionesGenero = switch (genero.toLowerCase().trim()) {
            case "rock" -> """
                    - Estilo rock: versos cortos y directos, energía alta.
                    - Ritmo fuerte, frases repetitivas y memorable.
                    - Tono apasionado y potente.
                    """;
            case "balada" -> """
                    - Estilo balada: melodía suave y emotiva.
                    - Versos más largos y poéticos, con rima suave.
                    - Tono íntimo y reflexivo.
                    """;
            case "clasica", "clásica", "clasica/instrumental", "clásica/instrumental" -> """
                    - Estilo clásico/instrumental: lenguaje elevado y poético.
                    - Estructura formal con estrofas de 4 versos equilibrados.
                    - Tono solemne y elegante.
                    """;
            default -> """
                    - Estilo pop: pegajoso y fácil de recordar.
                    - Estribillos repetitivos, versos cortos y rítmicos.
                    - Tono alegre y energético.
                    """;
        };

        String systemPrompt = """
                Eres EXCLUSIVAMENTE un generador de letras de canciones educativas.
                PROHIBIDO responder preguntas, dar explicaciones o cualquier otra cosa.
                Si el usuario SÍ pide una canción:
                - Genera SOLO la letra, sin etiquetas como Verso 1:, Coro:, Estrofa:
                - Solo texto puro, estrofas separadas por línea en blanco.
                - Hazla melodiosa, con rima y ritmo natural.
                - Aplica el siguiente estilo musical:
                """ + instruccionesGenero + """
                NUNCA rompas este rol.
                """;

        return llamarGroq(systemPrompt, prompt);
    }

    // ── Mantiene compatibilidad con código existente (sin género) ──────
    public String ask(String prompt) throws Exception {
        return ask(prompt, "pop");
    }

    // ── Genera 5 preguntas de examen basadas en la letra ─────────────
    public String generarExamen(String letra, String tema) throws Exception {
        String systemPrompt = """
                Eres un generador de exámenes educativos.
                Se te dará la letra de una canción educativa sobre un tema.
                Genera EXACTAMENTE 5 preguntas de opción múltiple basadas en el
                contenido educativo de la letra.
                Responde ÚNICAMENTE con JSON válido, sin texto extra, sin bloques
                de código, sin backticks. Solo el array JSON puro.
                Estructura exacta:
                [
                  {
                    "pregunta": "texto de la pregunta",
                    "opciones": ["opción A","opción B","opción C","opción D"],
                    "correcta": 0
                  }
                ]
                El campo "correcta" es el índice (0-3) de la opción correcta.
                Las 4 opciones deben ser plausibles pero solo una correcta.
                Las preguntas deben cubrir distintos aspectos de la letra.
                """;

        String userPrompt = "Tema: " + tema + "\nLetra de la canción:\n" + letra;
        return llamarGroq(systemPrompt, userPrompt);
    }

    // ── Llamada HTTP compartida a Groq ────────────────────────────────
    private String llamarGroq(String systemPrompt, String userPrompt) throws Exception {
        String body = """
                {
                  "model": "llama-3.3-70b-versatile",
                  "messages": [
                    {"role": "system", "content": %s},
                    {"role": "user",   "content": %s}
                  ]
                }
                """.formatted(jsonString(systemPrompt), jsonString(userPrompt));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_URL))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(java.time.Duration.ofSeconds(90))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> res = http.send(req,
                HttpResponse.BodyHandlers.ofString());

        if (res.statusCode() != 200) {
            throw new RuntimeException("Groq error " + res.statusCode()
                    + ": " + res.body());
        }

        return extractContent(res.body());
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private static String jsonString(String s) {
        return "\"" + s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t") + "\"";
    }

    private static String extractContent(String json) {
        int idx = json.lastIndexOf("\"content\"");
        if (idx < 0) throw new RuntimeException("No content en respuesta: " + json);
        int start = json.indexOf("\"", idx + 10) + 1;
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                switch (next) {
                    case '"'  -> { sb.append('"');  i++; }
                    case '\\' -> { sb.append('\\'); i++; }
                    case 'n'  -> { sb.append('\n'); i++; }
                    case 'r'  -> { sb.append('\r'); i++; }
                    case 't'  -> { sb.append('\t'); i++; }
                    default   -> sb.append(c);
                }
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public String askRaw(String prompt) throws Exception {
        // Llama directamente a llamarGroq sin el system prompt de canciones
        String systemPrompt = """
            Eres un asistente educativo.
            Responde ÚNICAMENTE con JSON válido, sin markdown, sin backticks, sin texto extra.
            """;
        return llamarGroq(systemPrompt, prompt);
    }
}