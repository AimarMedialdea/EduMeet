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
import uni.paag2.myapplication.supabase.SupabaseHelper;

public class ReunionAdapter extends RecyclerView.Adapter<ReunionAdapter.ViewHolder> {

    private List<Reunion> reuniones;
    private final OnItemClickListener listener;
    private final SupabaseHelper supabaseHelper = new SupabaseHelper();

    public interface OnItemClickListener {
        void onClick(Reunion reunion);
    }

    public ReunionAdapter(List<Reunion> reuniones, OnItemClickListener listener) {
        this.reuniones = reuniones;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tema, fechaHora, sala, participantes;

        public ViewHolder(View view) {
            super(view);
            tema = view.findViewById(R.id.temaTextView);
            fechaHora = view.findViewById(R.id.fechaHoraTextView);
            sala = view.findViewById(R.id.salaTextView);
            participantes = view.findViewById(R.id.participantesTextView);
        }

        public void bind(Reunion reunion, OnItemClickListener listener, SupabaseHelper helper) {
            tema.setText(reunion.getTema());
            fechaHora.setText(reunion.getFecha() + " " + reunion.getHoraInicio());
            sala.setText(reunion.getSala());
            itemView.setOnClickListener(v -> listener.onClick(reunion));

            participantes.setText("Cargando participantes...");
            helper.obtenerParticipantesPorReunion(reunion.getIdReunion(), new SupabaseHelper.ParticipantesCallback() {
                @Override
                public void onSuccess(List<String> nombres) {
                    String texto = nombres.size() + ": " + String.join(", ", nombres);
                    itemView.post(() -> participantes.setText(texto));
                }

                @Override
                public void onFailure(String error) {
                    itemView.post(() -> participantes.setText("Error al cargar participantes"));
                }
            });
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
        holder.bind(reuniones.get(position), listener, supabaseHelper);
    }

    @Override
    public int getItemCount() {
        return reuniones.size();
    }
}
