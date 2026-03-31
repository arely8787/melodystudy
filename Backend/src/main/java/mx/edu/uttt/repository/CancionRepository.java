package mx.edu.uttt.repository;

import mx.edu.uttt.model.Cancion;
import java.sql.*;
import java.util.*;

public class CancionRepository {

    private Connection getConnection() throws Exception {
        return DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/melodystudy",
                "root",
                "1234"
        );
    }

    public List<Cancion> obtenerPorUsuario(int idUsuario) {
        List<Cancion> lista = new ArrayList<>();

        try (Connection conn = getConnection()) {

            String sql = "SELECT * FROM canciones WHERE id_usuario = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idUsuario);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Cancion c = new Cancion();
                c.setId(rs.getInt("id"));
                c.setTema(rs.getString("tema"));
                c.setGenero(rs.getString("genero"));
                c.setLetra(rs.getString("letra"));
                c.setAudioUrl(rs.getString("audio_url"));
                c.setIdUsuario(rs.getInt("id_usuario"));

                lista.add(c);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return lista;
    }
}