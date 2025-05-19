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
import androidx.recyclerview.widget.ItemTouchHelper;
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
        View root = inflater.inflate(R.layout.fragment_mis_reuniones, container, false);
        recyclerReuniones = root.findViewById(R.id.recyclerReuniones);
        recyclerReuniones.setLayoutManager(new LinearLayoutManager(getContext()));
        reunionAdapter = new ReunionAdapter(listaReuniones, this::editarReunion);
        recyclerReuniones.setAdapter(reunionAdapter);

        // Configuro swipe-to-delete
        ItemTouchHelper.SimpleCallback swipeCallback =
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
                    @Override
                    public boolean onMove(@NonNull RecyclerView rv,
                                          @NonNull RecyclerView.ViewHolder vh,
                                          @NonNull RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                        final int pos = viewHolder.getAdapterPosition();
                        final Reunion r = listaReuniones.get(pos);

                        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                .setTitle(R.string.misreuniones_dialogo_titulo)
                                .setMessage(R.string.misreuniones_dialogo_mensaje)
                                .setPositiveButton(R.string.general_si, (dialog, which) -> {
                                    SupabaseHelper helper = new SupabaseHelper();

                                    helper.borrarRelacionesReunion(r.getIdReunion(), new SupabaseHelper.SupabaseCallback() {
                                        @Override
                                        public void onSuccess(String responseRel) {
                                            helper.borrarReunion(r.getIdReunion(), new SupabaseHelper.SupabaseCallback() {
                                                @Override
                                                public void onSuccess(String response) {
                                                    if (!isAdded()) return;
                                                    requireActivity().runOnUiThread(() -> {
                                                        listaReuniones.remove(pos);
                                                        reunionAdapter.notifyItemRemoved(pos);
                                                        Toast.makeText(getContext(),
                                                                R.string.misreuniones_eliminada,
                                                                Toast.LENGTH_SHORT).show();
                                                    });
                                                }

                                                @Override
                                                public void onFailure(String error) {
                                                    if (!isAdded()) return;
                                                    requireActivity().runOnUiThread(() -> {
                                                        reunionAdapter.notifyItemChanged(pos);
                                                        Toast.makeText(getContext(),
                                                                getString(R.string.misreuniones_error_eliminar) + ": " + error,
                                                                Toast.LENGTH_LONG).show();
                                                    });
                                                }
                                            });
                                        }

                                        @Override
                                        public void onFailure(String error) {
                                            if (!isAdded()) return;
                                            requireActivity().runOnUiThread(() -> {
                                                reunionAdapter.notifyItemChanged(pos);
                                                Toast.makeText(getContext(),
                                                        getString(R.string.misreuniones_error_eliminar_relaciones) + ": " + error,
                                                        Toast.LENGTH_LONG).show();
                                            });
                                        }
                                    });
                                })
                                .setNegativeButton(R.string.general_cancelar, (dialog, which) -> {
                                    reunionAdapter.notifyItemChanged(pos);
                                    dialog.dismiss();
                                })
                                .setCancelable(false)
                                .show();
                    }

                };
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerReuniones);

        SharedPreferences prefs = requireContext()
                .getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        idProfesor = prefs.getInt("id_profesor", -1);
        if (idProfesor == -1) {
            Toast.makeText(requireContext(),
                    getString(R.string.misreuniones_error_sesion),
                    Toast.LENGTH_LONG).show();
            return root;
        }
        cargarReuniones();
        return root;
    }

    private void cargarReuniones() {
        new SupabaseHelper().obtenerReunionesPorProfesor(idProfesor, new SupabaseHelper.ReunionesCallback() {
            @Override
            public void onSuccess(List<Reunion> reuniones) {
                if (!isAdded() || getActivity() == null) return;
                requireActivity().runOnUiThread(() -> {
                    listaReuniones.clear();
                    listaReuniones.addAll(reuniones);
                    reunionAdapter.notifyDataSetChanged();
                });
            }
            @Override
            public void onFailure(String error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    String msg = getString(R.string.misreuniones_error_carga) + ": " + error;
                    Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void editarReunion(Reunion reunion) {
        ReunionDialogFragment dialog = ReunionDialogFragment.nuevaParaEditar(reunion);
        dialog.show(getParentFragmentManager(), "EditarReunion");
    }
}