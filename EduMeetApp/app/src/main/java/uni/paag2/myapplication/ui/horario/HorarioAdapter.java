package uni.paag2.myapplication.ui.horario;

import android.util.Log;
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

    @NonNull
    @Override
    public HorarioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_horario, parent, false);
        return new HorarioViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HorarioViewHolder holder, int position) {
        HorarioItem item = horarioItems.get(position);

        // Añadir debug log para ver el ID del item
        Log.d("HorarioAdapter", "Dibujando item: " + item.toString());

        holder.tvAsignatura.setText(item.getNombreAsignatura());

        // Formatear el día y hora en un solo TextView
        String diaHora = capitalizeFirstLetter(item.getDia()) + " - " + item.getHoraInicio() + " a " + item.getHoraFin();
        holder.tvDiaHora.setText(diaHora);

        // Almacenar el ID en el tag de la vista para recuperarlo en eventos de clic
        holder.itemView.setTag(item.getIdProfesorAsignatura());
    }

    private String capitalizeFirstLetter(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }

    @Override
    public int getItemCount() {
        return horarioItems.size();
    }

    public void setHorarioItems(List<HorarioItem> horarioItems) {
        this.horarioItems = horarioItems;
        notifyDataSetChanged();
    }

    public List<HorarioItem> getHorarioItems() {
        return horarioItems;
    }

    static class HorarioViewHolder extends RecyclerView.ViewHolder {
        TextView tvAsignatura, tvDiaHora;

        public HorarioViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAsignatura = itemView.findViewById(R.id.tv_asignatura);
            tvDiaHora = itemView.findViewById(R.id.tv_dia_hora);
        }
    }
}