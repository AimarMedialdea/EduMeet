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

import uni.paag2.myapplication.R;
import uni.paag2.myapplication.model.Reunion;
import uni.paag2.myapplication.supabase.SupabaseHelper;

public class ReunionFragment extends Fragment {

    private RecyclerView recyclerReuniones;
    private ReunionAdapter reunionAdapter;
    private List<Reunion> listaReuniones = new ArrayList<>();
    private int idProfesor;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_reunion, container, false);
        recyclerReuniones = root.findViewById(R.id.recyclerReuniones);
        recyclerReuniones.setLayoutManager(new LinearLayoutManager(getContext()));
        reunionAdapter = new ReunionAdapter(listaReuniones, this::editarReunion);
        recyclerReuniones.setAdapter(reunionAdapter);

        // Obtener ID del profesor
        SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        idProfesor = prefs.getInt("id_profesor", -1);
        Log.d("ID Profesor", "ID Profesor: " + idProfesor);  // Verificar el ID
        if (idProfesor == -1) {
            Toast.makeText(requireContext(), "No se ha iniciado sesión correctamente", Toast.LENGTH_LONG).show();
            return root;
        }


        cargarReuniones();

        return root;
    }

    private void cargarReuniones() {
        SupabaseHelper helper = new SupabaseHelper();
        helper.obtenerReunionesPorProfesor(idProfesor, new SupabaseHelper.ReunionesCallback() {
            @Override
            public void onSuccess(List<Reunion> reuniones) {
                // Actualizar la lista de reuniones en el RecyclerView
                requireActivity().runOnUiThread(() -> {
                    listaReuniones.clear();
                    listaReuniones.addAll(reuniones);
                    reunionAdapter.notifyDataSetChanged(); // Notificar al adaptador
                });
            }

            @Override
            public void onFailure(String error) {
                requireActivity().runOnUiThread(() -> {
                    // Mostrar el error si no se obtienen reuniones
                    Toast.makeText(getContext(), "Error al obtener reuniones: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }


    private void editarReunion(Reunion reunion) {
        ReunionDialogFragment dialog = ReunionDialogFragment.nuevaParaEditar(reunion);
        dialog.show(getParentFragmentManager(), "EditarReunion");
    }
}
