package mx.edu.uttt.controller;

import io.javalin.Javalin;
import mx.edu.uttt.model.Usuario;
import mx.edu.uttt.service.UsuarioService;
import org.json.JSONObject;

public class UsuarioController {

    private final UsuarioService service = new UsuarioService();

    public void registrarRutas(Javalin app) {

        // ── POST /register ────────────────────────────────────────────
        app.post("/register", ctx -> {
            JSONObject body = new JSONObject(ctx.body());

            String apodo      = body.optString("apodo", "").trim();
            String contrasena = body.optString("contrasena", "");
            String avatar     = body.optString("avatar", "🎵");
            String nacimiento = body.optString("nacimiento", "");

            try {
                Usuario u = service.registrar(apodo, contrasena, avatar, nacimiento);
                ctx.status(201).contentType("application/json")
                        .result(usuarioToJson(u));
            } catch (IllegalArgumentException e) {
                ctx.status(400).result(errorJson(e.getMessage()));
            } catch (RuntimeException e) {
                ctx.status(500).result(errorJson("Error interno: " + e.getMessage()));
            }
        });

        // ── POST /login ───────────────────────────────────────────────
        app.post("/login", ctx -> {
            JSONObject body = new JSONObject(ctx.body());

            String apodo      = body.optString("apodo", "").trim();
            String contrasena = body.optString("contrasena", "");

            try {
                Usuario u = service.login(apodo, contrasena);
                ctx.status(200).contentType("application/json")
                        .result(usuarioToJson(u));
            } catch (IllegalArgumentException e) {
                ctx.status(401).result(errorJson(e.getMessage()));
            } catch (RuntimeException e) {
                ctx.status(500).result(errorJson("Error interno: " + e.getMessage()));
            }
        });
    }

    // ── Helpers JSON ─────────────────────────────────────────────────

    private String usuarioToJson(Usuario u) {
        return "{"
                + "\"id\":"         + u.getId()                + ","
                + "\"apodo\":"      + str(u.getApodo())        + ","
                + "\"avatar\":"     + str(u.getAvatar())       + ","
                + "\"nacimiento\":" + str(u.getNacimiento())   + ","
                + "\"nivel\":"      + u.getNivel()             + ","
                + "\"progreso\":"   + u.getProgreso()
                + "}";
    }

    private String errorJson(String msg) {
        return "{\"error\":" + str(msg) + "}";
    }

    private String str(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n") + "\"";
    }
}