package uni.paag2.myapplication.ui.horario;

public class HorarioItem {
    private String nombreAsignatura;
    private String horaInicio;
    private String horaFin;
    private String dia;
    private int idProfesorAsignatura; // Used for delete operations
    // Añadimos el día original para mantener referencia al valor en la BD
    private String diaBD;

    public HorarioItem(String nombreAsignatura, String horaInicio, String horaFin, String dia, int idProfesorAsignatura) {
        this.nombreAsignatura = nombreAsignatura;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.dia = dia;
        this.idProfesorAsignatura = idProfesorAsignatura;
    }

    // Constructor adicional que también permite almacenar el día de la BD
    public HorarioItem(String nombreAsignatura, String horaInicio, String horaFin, String dia, String diaBD, int idProfesorAsignatura) {
        this.nombreAsignatura = nombreAsignatura;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.dia = dia;
        this.diaBD = diaBD;
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

    public String getDiaBD() {
        return diaBD != null ? diaBD : dia; // Si no hay diaBD específico, devuelve dia
    }

    public int getIdProfesorAsignatura() {
        return idProfesorAsignatura;
    }

    @Override
    public String toString() {
        return nombreAsignatura + " - " + dia + " " + horaInicio + " - " + horaFin + " (ID: " + idProfesorAsignatura + ")";
    }
}