package mx.edu.uttt.model;

public class Cancion {
    private int    id;
    private String tema;
    private String genero;
    private String letra;
    private String audioUrl;
    private int    idUsuario;

    public Cancion() {}

    public Cancion(int id, String tema, String genero,
                   String letra, String audioUrl, int idUsuario) {
        this.id        = id;
        this.tema      = tema;
        this.genero    = genero;
        this.letra     = letra;
        this.audioUrl  = audioUrl;
        this.idUsuario = idUsuario;
    }

    public int    getId()                    { return id; }
    public void   setId(int id)              { this.id = id; }

    public String getTema()                  { return tema; }
    public void   setTema(String tema)       { this.tema = tema; }

    public String getGenero()                { return genero; }
    public void   setGenero(String genero)   { this.genero = genero; }

    public String getLetra()                 { return letra; }
    public void   setLetra(String letra)     { this.letra = letra; }

    public String getAudioUrl()                    { return audioUrl; }
    public void   setAudioUrl(String audioUrl)     { this.audioUrl = audioUrl; }

    public int    getIdUsuario()                   { return idUsuario; }
    public void   setIdUsuario(int idUsuario)      { this.idUsuario = idUsuario; }
}