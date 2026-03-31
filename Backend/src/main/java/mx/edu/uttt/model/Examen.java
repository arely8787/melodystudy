package mx.edu.uttt.model;

public class Examen {
    private int id;
    private int idCancion;

    public Examen() {}

    public Examen(int id, int idCancion) {
        this.id        = id;
        this.idCancion = idCancion;
    }

    public int  getId()                  { return id; }
    public void setId(int id)            { this.id = id; }

    public int  getIdCancion()                 { return idCancion; }
    public void setIdCancion(int idCancion)    { this.idCancion = idCancion; }
}