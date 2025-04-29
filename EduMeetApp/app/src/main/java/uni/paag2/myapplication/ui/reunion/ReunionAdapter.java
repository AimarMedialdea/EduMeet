package uni.paag2.myapplication.ui.reunion;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import uni.paag2.myapplication.R;
import uni.paag2.myapplication.model.Reunion;

public class ReunionAdapter extends RecyclerView.Adapter<ReunionAdapter.ViewHolder> {

    private List<Reunion> reuniones;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onClick(Reunion reunion);
    }

    public ReunionAdapter(List<Reunion> reuniones, OnItemClickListener listener) {
        this.reuniones = reuniones;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tema, fechaHora, sala;

        public ViewHolder(View view) {
            super(view);
            tema = view.findViewById(R.id.temaTextView);
            fechaHora = view.findViewById(R.id.fechaHoraTextView);
            sala = view.findViewById(R.id.salaTextView);
        }

        public void bind(Reunion reunion, OnItemClickListener listener) {
            // Usar los getters para acceder a los valores de la reunión
            tema.setText(reunion.getTema()); // Usar el getter
            fechaHora.setText(reunion.getFecha() + " " + reunion.getHoraInicio()); // Usar los getters
            sala.setText(reunion.getSala()); // Usar el getter
            itemView.setOnClickListener(v -> listener.onClick(reunion));
        }

    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reunion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(reuniones.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return reuniones.size();
    }
}

