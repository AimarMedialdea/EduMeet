package uni.paag2.myapplication.ui.reunion;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

import uni.paag2.myapplication.BaseFragment;
import uni.paag2.myapplication.R;
import uni.paag2.myapplication.model.Reunion;
import uni.paag2.myapplication.supabase.SupabaseHelper;

public class ReunionesUnidasFragment extends BaseFragment {

    private RecyclerView recyclerReuniones;
    private ReunionAdapter reunionAdapter;
    private List<Reunion> listaReuniones = new ArrayList<>();
    private int idProfesor;
    private SupabaseHelper supabaseHelper = new SupabaseHelper();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_mis_reuniones, container, false);
        recyclerReuniones = root.findViewById(R.id.recyclerReuniones);
        recyclerReuniones.setLayoutManager(new LinearLayoutManager(getContext()));
        reunionAdapter = new ReunionAdapter(listaReuniones, this::verReunion);
        recyclerReuniones.setAdapter(reunionAdapter);

        SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        idProfesor = prefs.getInt("id_profesor", -1);

        if (idProfesor == -1) {
            Toast.makeText(requireContext(), getString(R.string.reunionesunidas_error_sesion), Toast.LENGTH_LONG).show();
            return root;
        }

        cargarReunionesUnidas();

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (idProfesor != -1) {
            cargarReunionesUnidas();
        }
    }

    private void cargarReunionesUnidas() {
        supabaseHelper.obtenerReunionesUnidasPorProfesor(idProfesor, new SupabaseHelper.ReunionesCallback() {
            @Override
            public void onSuccess(List<Reunion> reuniones) {
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        try {
                            listaReuniones.clear();
                            listaReuniones.addAll(reuniones);
                            reunionAdapter.notifyDataSetChanged();

                            Log.d("ReunionesUnidasFragment", "Reuniones unidas cargadas: " + reuniones.size());
                            for (Reunion r : reuniones) {
                                Log.d("ReunionesUnidasFragment", "Reunión: " + r.getTema() + ", ID: " + r.getIdReunion());
                            }

                            if (reuniones.isEmpty() && getContext() != null) {
                                Toast.makeText(getContext(), getString(R.string.reunionesunidas_no_hay), Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Log.e("ReunionesUnidasFragment", "Error al actualizar UI: " + e.getMessage(), e);
                            if (getContext() != null) {
                                Toast.makeText(getContext(), getString(R.string.reunionesunidas_error_ui), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } else {
                    Log.e("ReunionesUnidasFragment", "El fragmento no está adjunto a una actividad");
                }
            }

            @Override
            public void onFailure(String error) {
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Log.e("ReunionesUnidasFragment", "Error al cargar reuniones: " + error);
                        if (getContext() != null) {
                            String mensaje = getString(R.string.misreuniones_error_carga) + ": " + error;
                            Toast.makeText(getContext(), mensaje, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
    }

    private void verReunion(Reunion reunion) {
        // lógica para ver los detalles de la reunión
    }
}