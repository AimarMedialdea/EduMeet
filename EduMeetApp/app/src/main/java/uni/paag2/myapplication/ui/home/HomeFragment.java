package uni.paag2.myapplication.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import uni.paag2.myapplication.R;
import uni.paag2.myapplication.databinding.FragmentHomeBinding;
import uni.paag2.myapplication.model.Reunion;
import uni.paag2.myapplication.supabase.SupabaseHelper;
import uni.paag2.myapplication.ui.reunion.ReunionAdapter;
import uni.paag2.myapplication.ui.reunion.ReunionCardAdapter;

public class HomeFragment extends Fragment {

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

        cargarTodasLasReuniones();

        return view;
    }

    private void cargarTodasLasReuniones() {
        SupabaseHelper helper = new SupabaseHelper();
        helper.obtenerTodasLasReuniones(new SupabaseHelper.SupabaseCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONArray array = new JSONArray(response);
                    listaReuniones.clear();

                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        int id = obj.getInt("id_reunion");
                        String tema = obj.getString("tema");
                        String fecha = obj.getString("fecha");
                        String hora = obj.getString("hora_inicio").substring(0, 5);
                        String sala = obj.getString("sala");
                        int idProfesor = obj.getInt("id_profesor");

                        Reunion reunion = new Reunion(id, tema, fecha, hora, sala, idProfesor);
                        listaReuniones.add(reunion);
                    }

                    requireActivity().runOnUiThread(() -> adapter.notifyDataSetChanged());

                } catch (Exception e) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Error al procesar las reuniones", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onFailure(String error) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Error al obtener reuniones: " + error, Toast.LENGTH_SHORT).show());
            }
        });
    }
}
