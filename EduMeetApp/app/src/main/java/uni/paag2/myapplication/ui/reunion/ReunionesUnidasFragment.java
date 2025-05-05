package uni.paag2.myapplication.ui.reunion;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
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

public class ReunionesUnidasFragment extends Fragment {

    private RecyclerView recyclerReuniones;
    private ReunionAdapter reunionAdapter;
    private List<Reunion> listaReuniones = new ArrayList<>();
    private int idProfesor;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_mis_reuniones, container, false);
        recyclerReuniones = root.findViewById(R.id.recyclerReuniones);
        recyclerReuniones.setLayoutManager(new LinearLayoutManager(getContext()));
        reunionAdapter = new ReunionAdapter(listaReuniones, this::editarReunión);
        recyclerReuniones.setAdapter(reunionAdapter);

        SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        idProfesor = prefs.getInt("id_profesor", -1);

        if (idProfesor == -1) {
            Toast.makeText(requireContext(), "No se ha iniciado sesión correctamente", Toast.LENGTH_LONG).show();
            return root;
        }
        cargarReunionesUnidas();
        return root;
    }

    private void cargarReunionesUnidas() {
        SupabaseHelper helper = new SupabaseHelper();
        // Este método debe implementar la lógica para obtener las reuniones en las que el profesor se ha unido
        // utilizando, por ejemplo, un filtro "unirme = true" en la tabla profesor_reunion.
        helper.obtenerReunionesUnidasPorProfesor(idProfesor, new SupabaseHelper.ReunionesCallback() {
            @Override
            public void onSuccess(List<Reunion> reuniones) {
                requireActivity().runOnUiThread(() -> {
                    listaReuniones.clear();
                    listaReuniones.addAll(reuniones);
                    reunionAdapter.notifyDataSetChanged();
                });
            }

            @Override
            public void onFailure(String error) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Error al obtener reuniones unidas: " + error, Toast.LENGTH_LONG).show()
                );
            }
        });
    }

    private void editarReunión(Reunion reunion) {
        ReunionDialogFragment dialog = ReunionDialogFragment.nuevaParaEditar(reunion);
        dialog.show(getParentFragmentManager(), "EditarReunión");
    }
}
