package uni.paag2.myapplication.ui.horario;

public class HorarioItem {
    private String nombre;
    private String horaInicio;
    private String horaFin;
    private String dia;
    private int idProfesorAsignatura; // Added for deletion purposes

    public HorarioItem(String nombre, String horaInicio, String horaFin, String dia, int idProfesorAsignatura) {
        this.nombre = nombre;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.dia = dia;
        this.idProfesorAsignatura = idProfesorAsignatura;
    }

    public String getNombre() {
        return nombre;
    }

    public String getHoraInicio() {
        return horaInicio;
    }

    public String getHoraFin() {
        return horaFin;
    }

    public String getDia() {
        return dia;
    }

    public int getIdProfesorAsignatura() {
        return idProfesorAsignatura;
    }
}