package mx.edu.uttt.service;

import mx.edu.uttt.model.Cancion;
import mx.edu.uttt.repository.CancionRepository;
import java.util.List;

public class CancionService {

    private CancionRepository repo = new CancionRepository();

    public List<Cancion> obtenerCancionesUsuario(int idUsuario) {
        return repo.obtenerPorUsuario(idUsuario);
    }
}