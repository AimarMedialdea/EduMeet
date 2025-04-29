package uni.paag2.myapplication.ui.horario;

public class HorarioItem {
    private String nombreAsignatura;
    private String horaInicio;
    private String horaFin;
    private String dia;
    private int idProfesorAsignatura; // Used for delete operations

    public HorarioItem(String nombreAsignatura, String horaInicio, String horaFin, String dia, int idProfesorAsignatura) {
        this.nombreAsignatura = nombreAsignatura;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.dia = dia;
        this.idProfesorAsignatura = idProfesorAsignatura;
    }

    public String getNombreAsignatura() {
        return nombreAsignatura;
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

    @Override
    public String toString() {
        return nombreAsignatura + " - " + dia + " " + horaInicio + " - " + horaFin + " (ID: " + idProfesorAsignatura + ")";
    }
}