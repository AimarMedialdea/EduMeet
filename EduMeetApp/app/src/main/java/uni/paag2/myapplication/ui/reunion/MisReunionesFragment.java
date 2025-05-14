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

    public class MisReunionesFragment extends BaseFragment {

        private RecyclerView recyclerReuniones;
        private ReunionAdapter reunionAdapter;
        private List<Reunion> listaReuniones = new ArrayList<>();
        private int idProfesor;

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater,
                                 @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            View root = inflater.inflate(R.layout.fragment_mis_reuniones, container, false); // Asegúrate de que sea el layout correcto
            recyclerReuniones = root.findViewById(R.id.recyclerReuniones);
            recyclerReuniones.setLayoutManager(new LinearLayoutManager(getContext()));
            reunionAdapter = new ReunionAdapter(listaReuniones, this::editarReunion);
            recyclerReuniones.setAdapter(reunionAdapter);

            // Obtener ID del profesor
            SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            idProfesor = prefs.getInt("id_profesor", -1);

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
                    // Verifica que el fragmento esté adjunto antes de intentar actualizar la UI
                    if (isAdded() && getActivity() != null) {
                        requireActivity().runOnUiThread(() -> {
                            listaReuniones.clear();
                            listaReuniones.addAll(reuniones);
                            reunionAdapter.notifyDataSetChanged();
                        });
                    } else {
                        Log.e("MisReunionesFragment", "El fragmento no está adjunto a la actividad");
                    }
                }

                @Override
                public void onFailure(String error) {
                    // Verifica que el fragmento esté adjunto antes de mostrar el mensaje de error
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Error al obtener reuniones: " + error, Toast.LENGTH_LONG).show();
                        });
                    }
                }
            });
        }

        private void editarReunion(Reunion reunion) {
            ReunionDialogFragment dialog = ReunionDialogFragment.nuevaParaEditar(reunion);
            dialog.show(getParentFragmentManager(), "EditarReunion");
        }
    }
