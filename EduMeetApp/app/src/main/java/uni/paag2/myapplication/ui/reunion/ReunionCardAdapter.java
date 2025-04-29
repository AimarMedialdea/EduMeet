package uni.paag2.myapplication.ui.reunion;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import uni.paag2.myapplication.R;
import uni.paag2.myapplication.model.Reunion;

public class ReunionCardAdapter extends RecyclerView.Adapter<ReunionCardAdapter.ViewHolder> {

    private final List<Reunion> reuniones;

    public ReunionCardAdapter(List<Reunion> reuniones) {
        this.reuniones = reuniones;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTema, tvFecha, tvHora, tvSala, tvParticipantes;
        Button btnUnirse;

        public ViewHolder(View view) {
            super(view);
            tvTema = view.findViewById(R.id.tvTema);
            tvFecha = view.findViewById(R.id.tvFecha);
            tvHora = view.findViewById(R.id.tvHora);
            tvSala = view.findViewById(R.id.tvSala);
            tvParticipantes = view.findViewById(R.id.tvParticipantes);
            btnUnirse = view.findViewById(R.id.btnUnirse);
        }

        public void bind(Reunion reunion) {
            tvTema.setText(reunion.getTema());
            tvFecha.setText("Fecha: " + reunion.getFecha());
            tvHora.setText("Hora: " + reunion.getHoraInicio());
            tvSala.setText("Sala: " + reunion.getSala());
            tvParticipantes.setText("Participantes: (ninguno)"); // aún no implementado

            btnUnirse.setOnClickListener(v -> {
                Toast.makeText(v.getContext(), "Funcionalidad aún no implementada", Toast.LENGTH_SHORT).show();
            });
        }
    }

    @NonNull
    @Override
    public ReunionCardAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_reunion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReunionCardAdapter.ViewHolder holder, int position) {
        holder.bind(reuniones.get(position));
    }

    @Override
    public int getItemCount() {
        return reuniones.size();
    }
}
