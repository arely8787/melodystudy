package mx.edu.uttt.model;

public class Usuario {
    private int    id;
    private String apodo;
    private String contrasena;   // almacena el HASH, nunca texto plano
    private String avatar;
    private String nacimiento;   // "YYYY-MM-DD"
    private int    nivel;
    private float  progreso;

    public Usuario() {}

    public Usuario(int id, String apodo, String avatar,
                   String nacimiento, int nivel, float progreso) {
        this.id          = id;
        this.apodo       = apodo;
        this.avatar      = avatar;
        this.nacimiento  = nacimiento;
        this.nivel       = nivel;
        this.progreso    = progreso;
    }

    // ── Getters / Setters ────────────────────────────────────────────
    public int    getId()          { return id; }
    public void   setId(int id)    { this.id = id; }

    public String getApodo()             { return apodo; }
    public void   setApodo(String a)     { this.apodo = a; }

    public String getContrasena()        { return contrasena; }
    public void   setContrasena(String c){ this.contrasena = c; }

    public String getAvatar()            { return avatar; }
    public void   setAvatar(String a)    { this.avatar = a; }

    public String getNacimiento()        { return nacimiento; }
    public void   setNacimiento(String n){ this.nacimiento = n; }

    public int    getNivel()             { return nivel; }
    public void   setNivel(int n)        { this.nivel = n; }

    public float  getProgreso()          { return progreso; }
    public void   setProgreso(float p)   { this.progreso = p; }
}