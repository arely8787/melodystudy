package mx.edu.uttt.tts;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class TTSService {

    private static final String TTS_URL = "http://localhost:5000/tts";
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .build();

    /**
     * Llama al microservicio Python TTS y guarda el MP3 en disco.
     *
     * @param text     Letra de la canción
     * @param audioKey UUID único de la canción (para el nombre del archivo)
     * @param genero   Género musical: "pop", "rock", "balada", "clasica"
     * @return ruta absoluta del archivo MP3, o null si falla
     */
    public Path textToSpeech(String text, String audioKey, String genero) {
        try {
            if (text.length() > 500) text = text.substring(0, 500);

            String body = "{\"text\":" + jsonString(text)
                    + ",\"genero\":" + jsonString(genero) + "}";

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(TTS_URL))
                    .header("Content-Type", "application/json")
                    .timeout(java.time.Duration.ofMinutes(5))
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<byte[]> res = http.send(req, HttpResponse.BodyHandlers.ofByteArray());

            System.out.println("TTS status: " + res.statusCode()
                    + " | bytes: " + res.body().length + " | género: " + genero);

            if (res.statusCode() == 200 && res.body().length > 0) {
                Path audioDir = Path.of("audio_files");
                Files.createDirectories(audioDir);
                Path mp3 = audioDir.resolve("temp_" + audioKey + ".mp3"); // ← UUID en lugar de long
                Files.write(mp3, res.body());
                System.out.println("🎵 TTS guardado en: " + mp3.toAbsolutePath());
                return mp3;
            }

            System.err.println("Error TTS: " + new String(res.body()));

        } catch (Exception e) {
            System.err.println("TTS error: " + e.getMessage());
        }
        return null;
    }

    /** Compatibilidad legacy — sin género usa pop por defecto. */
    public Path textToSpeech(String text, String audioKey) {
        return textToSpeech(text, audioKey, "pop");
    }

    private static String jsonString(String s) {
        return "\"" + s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r") + "\"";
    }
}