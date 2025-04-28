package uni.paag2.myapplication.ui.horario;

public class HorarioItem {
    private String nombre;
    private String horaInicio;
    private String horaFin;
    private String dia;

    public HorarioItem(String nombre, String horaInicio, String horaFin, String dia) {
        this.nombre = nombre;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.dia = dia;
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
}
