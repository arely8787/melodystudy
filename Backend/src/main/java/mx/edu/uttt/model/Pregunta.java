package mx.edu.uttt.model;

public class Pregunta {
    private int    id;
    private String pregunta;
    private String opcion1;
    private String opcion2;
    private String opcion3;
    private String opcion4;
    private int    opcionCorrecta;
    private int    idExamen;

    public Pregunta() {}

    public Pregunta(int id, String pregunta, String opcion1, String opcion2,
                    String opcion3, String opcion4, int opcionCorrecta, int idExamen) {
        this.id             = id;
        this.pregunta       = pregunta;
        this.opcion1        = opcion1;
        this.opcion2        = opcion2;
        this.opcion3        = opcion3;
        this.opcion4        = opcion4;
        this.opcionCorrecta = opcionCorrecta;
        this.idExamen       = idExamen;
    }

    public int    getId()                        { return id; }
    public void   setId(int id)                  { this.id = id; }

    public String getPregunta()                      { return pregunta; }
    public void   setPregunta(String pregunta)       { this.pregunta = pregunta; }

    public String getOpcion1()                   { return opcion1; }
    public void   setOpcion1(String opcion1)     { this.opcion1 = opcion1; }

    public String getOpcion2()                   { return opcion2; }
    public void   setOpcion2(String opcion2)     { this.opcion2 = opcion2; }

    public String getOpcion3()                   { return opcion3; }
    public void   setOpcion3(String opcion3)     { this.opcion3 = opcion3; }

    public String getOpcion4()                   { return opcion4; }
    public void   setOpcion4(String opcion4)     { this.opcion4 = opcion4; }

    public int    getOpcionCorrecta()                      { return opcionCorrecta; }
    public void   setOpcionCorrecta(int opcionCorrecta)    { this.opcionCorrecta = opcionCorrecta; }

    public int    getIdExamen()                  { return idExamen; }
    public void   setIdExamen(int idExamen)      { this.idExamen = idExamen; }
}