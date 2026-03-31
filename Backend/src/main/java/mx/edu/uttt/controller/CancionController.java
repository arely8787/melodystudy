package mx.edu.uttt.controller;

import io.javalin.Javalin;
import mx.edu.uttt.service.CancionService;

public class CancionController {

    public static void registrarRutas(Javalin app) {

        CancionService service = new CancionService();

        app.get("/canciones/usuario/{id}", ctx -> {

            int idUsuario = Integer.parseInt(ctx.pathParam("id"));

            var canciones = service.obtenerCancionesUsuario(idUsuario);

            ctx.json(canciones);
        });
    }
}