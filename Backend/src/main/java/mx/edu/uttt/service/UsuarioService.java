package mx.edu.uttt.service;

import mx.edu.uttt.model.Usuario;
import mx.edu.uttt.repository.UsuarioRepository;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;

public class UsuarioService {

    private final UsuarioRepository repo = new UsuarioRepository();

    /**
     * Registra un usuario nuevo.
     * @return el Usuario con id asignado
     * @throws IllegalArgumentException si el apodo ya existe o faltan datos
     * @throws RuntimeException         si falla la BD
     */
    public Usuario registrar(String apodo, String contrasena,
                             String avatar, String nacimiento) {

        if (apodo == null || apodo.isBlank())
            throw new IllegalArgumentException("El apodo no puede estar vacío");
        if (contrasena == null || contrasena.length() < 4)
            throw new IllegalArgumentException("La contraseña debe tener al menos 4 caracteres");

        try {
            if (repo.existeApodo(apodo.trim()))
                throw new IllegalArgumentException("El apodo '" + apodo + "' ya está en uso");

            // ── HASH de la contraseña ─────────────────────────────────
            String hash = BCrypt.hashpw(contrasena, BCrypt.gensalt(12));

            Usuario u = new Usuario();
            u.setApodo(apodo.trim());
            u.setContrasena(hash);
            u.setAvatar(avatar != null ? avatar : "🎵");
            u.setNacimiento(nacimiento);

            int id = repo.insertar(u);
            u.setId(id);
            u.setNivel(1);
            u.setProgreso(0f);
            return u;

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (SQLException e) {
            throw new RuntimeException("Error de base de datos: " + e.getMessage(), e);
        }
    }

    /**
     * Verifica credenciales.
     * @return el Usuario si las credenciales son correctas
     * @throws IllegalArgumentException si apodo no existe o contraseña incorrecta
     */
    public Usuario login(String apodo, String contrasena) {
        if (apodo == null || apodo.isBlank() || contrasena == null)
            throw new IllegalArgumentException("Credenciales vacías");

        try {
            Usuario u = repo.buscarPorApodo(apodo.trim());

            if (u == null)
                throw new IllegalArgumentException("Apodo no encontrado");

            // ── VERIFICACIÓN BCrypt ───────────────────────────────────
            if (!BCrypt.checkpw(contrasena, u.getContrasena()))
                throw new IllegalArgumentException("Contraseña incorrecta");

            return u;

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (SQLException e) {
            throw new RuntimeException("Error de base de datos: " + e.getMessage(), e);
        }
    }
}