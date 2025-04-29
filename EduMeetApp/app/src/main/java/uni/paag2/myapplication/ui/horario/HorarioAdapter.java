package uni.paag2.myapplication.ui.horario;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import uni.paag2.myapplication.R;

public class HorarioAdapter extends RecyclerView.Adapter<HorarioAdapter.HorarioViewHolder> {

    private List<HorarioItem> horarioItems;

    public HorarioAdapter(List<HorarioItem> horarioItems) {
        this.horarioItems = horarioItems;
    }

    public List<HorarioItem> getHorarioItems() {
        return horarioItems;
    }

    public void setHorarioItems(List<HorarioItem> horarioItems) {
        this.horarioItems = horarioItems;
    }

    @NonNull
    @Override
    public HorarioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_horario, parent, false);
        return new HorarioViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HorarioViewHolder holder, int position) {
        HorarioItem item = horarioItems.get(position);

        // Cortar horas para quitar segundos
        String horaInicio = item.getHoraInicio().length() >= 5 ?
                item.getHoraInicio().substring(0, 5) : item.getHoraInicio();
        String horaFin = item.getHoraFin().length() >= 5 ?
                item.getHoraFin().substring(0, 5) : item.getHoraFin();

        holder.tvAsignatura.setText(item.getNombre());
        holder.tvDiaHora.setText(item.getDia() + " - " + horaInicio + " a " + horaFin);
    }

    @Override
    public int getItemCount() {
        return horarioItems.size();
    }

    static class HorarioViewHolder extends RecyclerView.ViewHolder {
        TextView tvAsignatura;
        TextView tvDiaHora;

        HorarioViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAsignatura = itemView.findViewById(R.id.tv_asignatura);
            tvDiaHora = itemView.findViewById(R.id.tv_dia_hora);
        }
    }
}