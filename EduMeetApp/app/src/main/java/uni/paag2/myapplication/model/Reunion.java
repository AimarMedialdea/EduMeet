package uni.paag2.myapplication.model;

public class Reunion {
    private int id_reunion;
    private String tema;
    private String fecha;
    private String hora_inicio;
    private String sala;
    private int id_profesor; // ← Añadido

    // Constructor
    public Reunion(int id_reunion, String tema, String fecha, String hora_inicio, String sala, int id_profesor) {
        this.id_reunion = id_reunion;
        this.tema = tema;
        this.fecha = fecha;
        this.hora_inicio = hora_inicio;
        this.sala = sala;
        this.id_profesor = id_profesor;
    }

    // Constructor vacío (por si lo necesitas)
    public Reunion() {}

    // Getters
    public int getIdReunion() {
        return id_reunion;
    }

    public String getTema() {
        return tema;
    }

    public String getFecha() {
        return fecha;
    }

    public String getHoraInicio() {
        return hora_inicio;
    }

    public String getSala() {
        return sala;
    }

    public int getIdProfesor() {
        return id_profesor;
    }

    // Setters
    public void setIdReunion(int id_reunion) {
        this.id_reunion = id_reunion;
    }

    public void setTema(String tema) {
        this.tema = tema;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public void setHoraInicio(String hora_inicio) {
        this.hora_inicio = hora_inicio;
    }

    public void setSala(String sala) {
        this.sala = sala;
    }

    public void setIdProfesor(int id_profesor) {
        this.id_profesor = id_profesor;
    }
}
