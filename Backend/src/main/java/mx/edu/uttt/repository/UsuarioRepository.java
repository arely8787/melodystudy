package mx.edu.uttt.repository;

import mx.edu.uttt.config.Database;
import mx.edu.uttt.model.Usuario;

import java.sql.*;

public class UsuarioRepository {

    /** Inserta un usuario nuevo. Devuelve el id generado. */
    public int insertar(Usuario u) throws SQLException {
        String sql = "INSERT INTO usuarios " +
                "(apodo, contrasena, avatar, nacimiento) " +
                "VALUES (?, ?, ?, ?)";

        try (Connection cn = Database.getConnection();
             PreparedStatement ps = cn.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, u.getApodo());
            ps.setString(2, u.getContrasena());   // ya viene hasheada
            ps.setString(3, u.getAvatar());
            ps.setString(4, u.getNacimiento());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("No se generó ID");
    }

    /** Busca por apodo (para login). Devuelve null si no existe. */
    public Usuario buscarPorApodo(String apodo) throws SQLException {
        String sql = "SELECT id, apodo, contrasena, avatar, " +
                "nacimiento, nivel, progreso " +
                "FROM usuarios WHERE apodo = ?";

        try (Connection cn = Database.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, apodo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Usuario u = new Usuario();
                    u.setId(rs.getInt("id"));
                    u.setApodo(rs.getString("apodo"));
                    u.setContrasena(rs.getString("contrasena"));
                    u.setAvatar(rs.getString("avatar"));
                    u.setNacimiento(rs.getString("nacimiento"));
                    u.setNivel(rs.getInt("nivel"));
                    u.setProgreso(rs.getFloat("progreso"));
                    return u;
                }
            }
        }
        return null;
    }

    /** Verifica si un apodo ya está registrado. */
    public boolean existeApodo(String apodo) throws SQLException {
        String sql = "SELECT 1 FROM usuarios WHERE apodo = ?";
        try (Connection cn = Database.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, apodo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}