package uni.paag2.myapplication.ui.reunion;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import uni.paag2.myapplication.R;
import uni.paag2.myapplication.model.Reunion;

public class ReunionCardAdapter extends RecyclerView.Adapter<ReunionCardAdapter.ViewHolder> {

    private final List<Reunion> reuniones;
    private final OkHttpClient client = new OkHttpClient();
    // Modifica estas constantes con tus valores reales de Supabase
    private final String SUPABASE_URL = "https://trjiewwhjoeytkdwkvlm.supabase.co";
    private final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InRyamlld3doam9leXRrZHdrdmxtIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDM0OTY1ODQsImV4cCI6MjA1OTA3MjU4NH0.YS6EF001LPQq4RyJEGLLbQc8DSu4lidDRQMAjbjBOrw";

    public ReunionCardAdapter(List<Reunion> reuniones) {
        this.reuniones = reuniones;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTema, tvFecha, tvHora, tvSala, tvParticipantes;
        Button btnUnirse;
        boolean estaUnido = false;
        private HorarioManager horarioManager;

        public ViewHolder(View view) {
            super(view);
            tvTema = view.findViewById(R.id.tvTema);
            tvFecha = view.findViewById(R.id.tvFecha);
            tvHora = view.findViewById(R.id.tvHora);
            tvSala = view.findViewById(R.id.tvSala);
            tvParticipantes = view.findViewById(R.id.tvParticipantes);
            btnUnirse = view.findViewById(R.id.btnUnirse);
        }

        public void bind(Reunion reunion, OkHttpClient client, String supabaseUrl, String supabaseKey) {
            tvTema.setText(reunion.getTema());
            tvFecha.setText("Fecha: " + reunion.getFecha());
            tvHora.setText("Hora: " + reunion.getHoraInicio());
            tvSala.setText("Sala: " + reunion.getSala());

            // Inicializar el HorarioManager
            horarioManager = new HorarioManager(itemView.getContext(), supabaseUrl, supabaseKey);

            // Obtener el ID del profesor actual (puedes obtenerlo de SharedPreferences o de tu sistema de sesión)
            int idProfesor = obtenerIdProfesorActual();

            // Verificar si el profesor ya está unido a esta reunión
            verificarParticipacion(idProfesor, reunion.getIdReunion(), client, supabaseUrl, supabaseKey);

            // Obtener participantes de la reunión
            obtenerParticipantes(reunion.getIdReunion(), client, supabaseUrl, supabaseKey);

            btnUnirse.setOnClickListener(v -> {
                if (estaUnido) {
                    // Si ya está unido, salirse de la reunión
                    salirDeReunion(idProfesor, reunion.getIdReunion(), client, supabaseUrl, supabaseKey);
                } else {
                    // Si no está unido, unirse a la reunión
                    unirseAReunion(idProfesor, reunion.getIdReunion(), client, supabaseUrl, supabaseKey, reunion);
                }
            });
        }

        // Método para obtener el ID del profesor actual (desde SharedPreferences o tu sistema de sesión)
        private int obtenerIdProfesorActual() {
            SharedPreferences prefs = itemView.getContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            return prefs.getInt("id_profesor", -1); // -1 si no se ha iniciado sesión correctamente
        }


        private void verificarParticipacion(int idProfesor, int idReunion, OkHttpClient client, String supabaseUrl, String supabaseKey) {
            String url = supabaseUrl + "/rest/v1/profesor_reunion?id_profesor=eq." + idProfesor +
                    "&id_reunion=eq." + idReunion + "&unirme=eq.true";

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Authorization", "Bearer " + supabaseKey)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, java.io.IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws java.io.IOException {
                    if (response.isSuccessful()) {
                        String responseData = response.body().string();
                        try {
                            // Si hay resultados, significa que el profesor ya está unido
                            JSONArray jsonArray = new JSONArray(responseData);
                            final boolean participando = jsonArray.length() > 0;

                            // Actualizar la UI en el hilo principal
                            itemView.post(() -> {
                                estaUnido = participando;
                                btnUnirse.setText(participando ? "Salir" : "Unirse");
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }

        private void obtenerParticipantes(int idReunion, OkHttpClient client, String supabaseUrl, String supabaseKey) {
            String url = supabaseUrl + "/rest/v1/profesor_reunion?id_reunion=eq." + idReunion +
                    "&unirme=eq.true&select=id_profesor";

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Authorization", "Bearer " + supabaseKey)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, java.io.IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws java.io.IOException {
                    if (response.isSuccessful()) {
                        String responseData = response.body().string();
                        try {
                            // Contar el número de profesores unidos
                            JSONArray jsonArray = new JSONArray(responseData);
                            final int numParticipantes = jsonArray.length();

                            // Actualizar la UI en el hilo principal
                            itemView.post(() -> {
                                if (numParticipantes > 0) {
                                    tvParticipantes.setText("Participantes: " + numParticipantes);
                                } else {
                                    tvParticipantes.setText("Participantes: (ninguno)");
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }

        private void unirseAReunion(int idProfesor, int idReunion, OkHttpClient client, String supabaseUrl, String supabaseKey, Reunion reunion) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String fechaActual = sdf.format(new Date());

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("id_profesor", idProfesor);
                jsonObject.put("id_reunion", idReunion);
                jsonObject.put("unirme", true);
                jsonObject.put("fecha_union", fechaActual);
                jsonObject.put("fecha", fechaActual);

                RequestBody body = RequestBody.create(
                        MediaType.parse("application/json"),
                        jsonObject.toString()
                );

                String url = supabaseUrl + "/rest/v1/profesor_reunion";

                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("apikey", supabaseKey)
                        .addHeader("Authorization", "Bearer " + supabaseKey)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Prefer", "resolution=merge-duplicates") // Habilita UPSERT
                        .post(body)
                        .build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, java.io.IOException e) {
                        itemView.post(() -> {
                            Toast.makeText(itemView.getContext(), "Error al unirse: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws java.io.IOException {
                        if (response.isSuccessful()) {
                            itemView.post(() -> {
                                estaUnido = true;
                                btnUnirse.setText("Salir");
                                Toast.makeText(itemView.getContext(), "Te has unido a la reunión", Toast.LENGTH_SHORT).show();
                                obtenerParticipantes(idReunion, client, supabaseUrl, supabaseKey);

                                // Llamar al método para buscar horarios disponibles
                                buscarYActualizarHorario(idReunion);
                            });
                        } else {
                            itemView.post(() -> {
                                Toast.makeText(itemView.getContext(), "Error al unirse: " + response.code(), Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
                });
            } catch (Exception e) {
                itemView.post(() -> {
                    Toast.makeText(itemView.getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }

        private void buscarYActualizarHorario(int idReunion) {
            // Mostrar mensaje de procesamiento
            Toast.makeText(itemView.getContext(), "Buscando horarios disponibles para todos...", Toast.LENGTH_SHORT).show();

            // Llamar al HorarioManager para buscar horarios libres
            horarioManager.buscarHorarioDisponible(idReunion, new HorarioManager.HorarioCallback() {
                @Override
                public void onSuccess(String nuevoHorario) {
                    itemView.post(() -> {
                        if (nuevoHorario != null) {
                            // Mostrar el nuevo horario
                            String horaFormateada = nuevoHorario.substring(0, 5);
                            tvHora.setText("Hora: " + horaFormateada + " (actualizada)");
                            Toast.makeText(itemView.getContext(), "Horario actualizado a: " + horaFormateada, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(itemView.getContext(), "No se encontraron horarios disponibles para todos", Toast.LENGTH_LONG).show();
                        }
                    });
                }

                @Override
                public void onFailure(String error) {
                    itemView.post(() -> {
                        Toast.makeText(itemView.getContext(), "Error al buscar horario: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        }

        private void salirDeReunion(int idProfesor, int idReunion, OkHttpClient client, String supabaseUrl, String supabaseKey) {
            String url = supabaseUrl + "/rest/v1/profesor_reunion?id_profesor=eq." + idProfesor + "&id_reunion=eq." + idReunion;

            // Crear el cuerpo de la petición para actualizar el campo 'unirme' a false
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("unirme", false);
            } catch (Exception e) {
                itemView.post(() -> Toast.makeText(itemView.getContext(), "Error al preparar salida: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                return;
            }

            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"),
                    jsonObject.toString()
            );

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Authorization", "Bearer " + supabaseKey)
                    .addHeader("Content-Type", "application/json")
                    .method("PATCH", body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, java.io.IOException e) {
                    itemView.post(() -> {
                        Toast.makeText(itemView.getContext(), "Error al salir: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws java.io.IOException {
                    if (response.isSuccessful()) {
                        itemView.post(() -> {
                            estaUnido = false;
                            btnUnirse.setText("Unirse");
                            Toast.makeText(itemView.getContext(), "Has salido de la reunión", Toast.LENGTH_SHORT).show();
                            obtenerParticipantes(idReunion, client, supabaseUrl, supabaseKey);
                        });
                    } else {
                        itemView.post(() -> {
                            Toast.makeText(itemView.getContext(), "Error al salir: " + response.code(), Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            });
        }
    }

    @NonNull
    @Override
    public ReunionCardAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_reunion, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ReunionCardAdapter.ViewHolder holder, int position) {
        holder.bind(reuniones.get(position), client, SUPABASE_URL, SUPABASE_KEY);
    }

    @Override
    public int getItemCount() {
        return reuniones.size();
    }
}
