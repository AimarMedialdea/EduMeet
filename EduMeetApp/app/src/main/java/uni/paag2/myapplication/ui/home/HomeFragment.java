package uni.paag2.myapplication.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import uni.paag2.myapplication.BaseFragment;
import uni.paag2.myapplication.R;
import uni.paag2.myapplication.model.Reunion;
import uni.paag2.myapplication.supabase.SupabaseHelper;
import uni.paag2.myapplication.ui.reunion.ReunionCardAdapter;

public class HomeFragment extends BaseFragment {

    private RecyclerView recyclerView;
    private ReunionCardAdapter adapter;
    private final List<Reunion> listaReuniones = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        recyclerView = view.findViewById(R.id.recyclerReuniones);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ReunionCardAdapter(listaReuniones);
        recyclerView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refrescar y verificar reuniones cada vez que el fragmento se vuelve visible
        cargarTodasLasReuniones();
    }

    private void cargarTodasLasReuniones() {
        SupabaseHelper helper = new SupabaseHelper();
        helper.obtenerTodasLasReuniones(new SupabaseHelper.SupabaseCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONArray array = new JSONArray(response);
                    listaReuniones.clear();
                    List<Reunion> reunionesVencidas = new ArrayList<>();

                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        int id = obj.getInt("id_reunion");
                        String tema = obj.getString("tema");
                        String fecha = obj.getString("fecha");
                        String hora = obj.getString("hora_inicio").substring(0, 5);
                        String sala = obj.getString("sala");
                        int idProfesor = obj.getInt("id_profesor");

                        Reunion reunion = new Reunion(id, tema, fecha, hora, sala, idProfesor);

                        // Comprobar si la reunión ya ha pasado
                        if (reunionHaPasado(fecha, hora)) {
                            reunionesVencidas.add(reunion);
                        } else {
                            listaReuniones.add(reunion);
                        }
                    }

                    requireActivity().runOnUiThread(() -> adapter.notifyDataSetChanged());

                    // Eliminar las reuniones que ya han pasado
                    if (!reunionesVencidas.isEmpty()) {
                        eliminarReunionesVencidas(reunionesVencidas);
                    }

                } catch (Exception e) {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(), "Error al procesar las reuniones", Toast.LENGTH_SHORT).show());
                    }
                }
            }

            @Override
            public void onFailure(String error) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Error al obtener reuniones: " + error, Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private boolean reunionHaPasado(String fechaStr, String horaStr) {
        try {
            // Formato para parsear la fecha y hora
            SimpleDateFormat formatoFecha = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat formatoHora = new SimpleDateFormat("HH:mm", Locale.getDefault());

            // Obtener la fecha y hora actual
            Date ahora = new Date();

            // Parsear la fecha y hora de la reunión
            Date fechaReunion = formatoFecha.parse(fechaStr);
            Date horaReunion = formatoHora.parse(horaStr);

            // Combinar fecha y hora de la reunión
            Calendar calReunion = Calendar.getInstance();
            calReunion.setTime(fechaReunion);

            Calendar calHora = Calendar.getInstance();
            calHora.setTime(horaReunion);

            calReunion.set(Calendar.HOUR_OF_DAY, calHora.get(Calendar.HOUR_OF_DAY));
            calReunion.set(Calendar.MINUTE, calHora.get(Calendar.MINUTE));

            // Comparar con la fecha y hora actual
            return calReunion.getTime().before(ahora);

        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void eliminarReunionesVencidas(List<Reunion> reunionesVencidas) {
        SupabaseHelper helper = new SupabaseHelper();

        for (Reunion reunion : reunionesVencidas) {
            helper.borrarRelacionesReunion(reunion.getIdReunion(), new SupabaseHelper.SupabaseCallback() {
                @Override
                public void onSuccess(String responseRel) {
                    helper.borrarReunion(reunion.getIdReunion(), new SupabaseHelper.SupabaseCallback() {
                        @Override
                        public void onSuccess(String response) {
                            if (isAdded()) {
                                requireActivity().runOnUiThread(() -> {
                                    Toast.makeText(getContext(),
                                            getString(R.string.misreuniones_eliminada) + ": " + reunion.getTema(),
                                            Toast.LENGTH_SHORT).show();
                                });
                            }
                        }

                        @Override
                        public void onFailure(String error) {
                            if (isAdded()) {
                                requireActivity().runOnUiThread(() -> {
                                    Toast.makeText(getContext(),
                                            getString(R.string.misreuniones_error_eliminar) + ": " + error,
                                            Toast.LENGTH_LONG).show();
                                });
                            }
                        }
                    });
                }

                @Override
                public void onFailure(String error) {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(),
                                    getString(R.string.misreuniones_error_eliminar_relaciones) + ": " + error,
                                    Toast.LENGTH_LONG).show();
                        });
                    }
                }
            });
        }
    }
}