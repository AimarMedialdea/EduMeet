package uni.paag2.myapplication.ui.horario;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.*;
import uni.paag2.myapplication.R;
import uni.paag2.myapplication.supabase.SupabaseHelper;

public class HorarioFragment extends Fragment {

    private Spinner spinnerAsignaturas, spinnerDia;
    private Button btnHoraInicio, btnHoraFin, btnGuardar;
    private RecyclerView recyclerHorario; // Changed from LinearLayout to RecyclerView
    private HorarioViewModel horarioViewModel;
    private SupabaseHelper supabaseHelper;
    private String horaInicio = "", horaFin = "";
    private int idProfesor;
    private HorarioAdapter horarioAdapter; // Added adapter for RecyclerView

    private Map<String, Integer> asignaturaMap = new HashMap<>();
    private String asignaturaSeleccionada = "";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_horario, container, false);

        spinnerAsignaturas = root.findViewById(R.id.spinner_asignaturas);
        spinnerDia = root.findViewById(R.id.spinner_dia);
        btnHoraInicio = root.findViewById(R.id.btn_hora_inicio);
        btnHoraFin = root.findViewById(R.id.btn_hora_fin);
        btnGuardar = root.findViewById(R.id.btn_guardar);
        recyclerHorario = root.findViewById(R.id.lista_horario); // Changed ID from LinearLayout to RecyclerView

        supabaseHelper = new SupabaseHelper();
        horarioViewModel = new ViewModelProvider(this).get(HorarioViewModel.class);

        // Configure RecyclerView
        recyclerHorario.setLayoutManager(new LinearLayoutManager(requireContext()));
        horarioAdapter = new HorarioAdapter(new ArrayList<>());
        recyclerHorario.setAdapter(horarioAdapter);

        // Setup swipe-to-delete
        setupSwipeToDelete();

        // Obtener id_profesor desde SharedPreferences primero
        SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        idProfesor = prefs.getInt("id_profesor", -1);
        Log.d("HorarioFragment", "idProfesor cargado: " + idProfesor);

        if (idProfesor == -1) {
            requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(),
                    "Error: No se ha iniciado sesión correctamente", Toast.LENGTH_LONG).show());
            return root;
        }

        setupDiaSpinner();
        cargarAsignaturas();
        configurarBotones();
        observarHorario();

        // Obtener horario después de cargar idProfesor
        obtenerHorario();

        return root;
    }

    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                final int position = viewHolder.getAdapterPosition();
                final HorarioItem itemToDelete = horarioAdapter.getHorarioItems().get(position);

                new AlertDialog.Builder(requireContext())
                        .setTitle("Eliminar horario")
                        .setMessage("¿Estás seguro de que quieres eliminar este horario?")
                        .setPositiveButton("Sí", (dialog, which) -> {
                            supabaseHelper.eliminarHorario(itemToDelete.getIdProfesorAsignatura(), new SupabaseHelper.SupabaseCallback() {
                                @Override
                                public void onSuccess(String response) {
                                    requireActivity().runOnUiThread(() -> {
                                        Toast.makeText(requireContext(), "Horario eliminado correctamente", Toast.LENGTH_SHORT).show();
                                        // Remove item from adapter and notify
                                        List<HorarioItem> currentItems = new ArrayList<>(horarioAdapter.getHorarioItems());
                                        currentItems.remove(position);
                                        horarioViewModel.setHorario(currentItems);
                                    });
                                }

                                @Override
                                public void onFailure(String error) {
                                    Log.e("HorarioFragment", "Error al eliminar horario: " + error);
                                    requireActivity().runOnUiThread(() -> {
                                        Toast.makeText(requireContext(), "Error al eliminar horario", Toast.LENGTH_SHORT).show();
                                        // Restore the item in the UI
                                        horarioAdapter.notifyItemChanged(position);
                                    });
                                }
                            });
                        })
                        .setNegativeButton("No", (dialog, which) -> {
                            // Restore the item in the UI
                            horarioAdapter.notifyItemChanged(position);
                        })
                        .setOnCancelListener(dialog -> {
                            // Restore the item in the UI if dialog is canceled
                            horarioAdapter.notifyItemChanged(position);
                        })
                        .show();
            }
        };

        new ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerHorario);
    }

    private void setupDiaSpinner() {
        ArrayAdapter<String> diaAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item,
                Arrays.asList("lunes", "martes", "miercoles", "jueves", "viernes"));
        diaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDia.setAdapter(diaAdapter);
    }

    private void cargarAsignaturas() {
        supabaseHelper.obtenerAsignaturas(new SupabaseHelper.SupabaseCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    // Verificar que la respuesta sea un JSON válido
                    Log.d("HorarioFragment", "Respuesta de obtenerAsignaturas: " + response);
                    JSONArray array = new JSONArray(response);
                    List<String> nombres = new ArrayList<>();
                    for (int i = 0; i < array.length(); i++) {
                        try {
                            JSONObject asignatura = array.getJSONObject(i);
                            String nombre = asignatura.optString("nombre", "Desconocido");
                            int id = asignatura.optInt("id_asignatura", -1);
                            nombres.add(nombre);
                            asignaturaMap.put(nombre, id);
                        } catch (JSONException e) {
                            Log.e("HorarioFragment", "Error procesando asignatura en posición " + i, e);
                        }
                    }

                    // Verificar que la lista no esté vacía antes de actualizar la UI
                    if (!nombres.isEmpty() && isAdded() && getContext() != null && getActivity() != null) {
                        requireActivity().runOnUiThread(() -> {
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                                    android.R.layout.simple_spinner_item, nombres);
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            spinnerAsignaturas.setAdapter(adapter);
                            spinnerAsignaturas.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                @Override
                                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                    asignaturaSeleccionada = nombres.get(position);
                                    Log.d("HorarioFragment", "Asignatura seleccionada: " + asignaturaSeleccionada);
                                }

                                @Override
                                public void onNothingSelected(AdapterView<?> parent) {}
                            });
                        });
                    } else {
                        Log.e("HorarioFragment", "La lista de asignaturas está vacía o contexto es inválido");
                    }

                } catch (Exception e) {
                    Log.e("HorarioFragment", "Error cargando asignaturas", e);
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(String error) {
                Log.e("HorarioFragment", "Error en obtenerAsignaturas: " + error);
                if (isAdded() && getContext() != null && getActivity() != null) {
                    requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Error cargando asignaturas", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void configurarBotones() {
        btnHoraInicio.setOnClickListener(v -> mostrarTimePicker(true));
        btnHoraFin.setOnClickListener(v -> mostrarTimePicker(false));

        btnGuardar.setOnClickListener(v -> {
            if (asignaturaSeleccionada.isEmpty()) {
                requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Debes seleccionar una asignatura", Toast.LENGTH_SHORT).show());
                return;
            }

            int idAsignatura = asignaturaMap.get(asignaturaSeleccionada);
            String dia = spinnerDia.getSelectedItem().toString().toLowerCase();

            Log.d("HorarioFragment", "Guardando horario para asignatura " + asignaturaSeleccionada + ", día: " + dia);

            supabaseHelper.insertarHorario(idProfesor, idAsignatura, horaInicio, horaFin, dia, new SupabaseHelper.SupabaseCallback() {
                @Override
                public void onSuccess(String response) {
                    obtenerHorario();
                    Log.d("HorarioFragment", "Horario guardado correctamente");
                }

                @Override
                public void onFailure(String error) {
                    Log.e("HorarioFragment", "Error al guardar horario: " + error);
                    requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Error al guardar horario", Toast.LENGTH_SHORT).show());
                }
            });
        });
    }

    private void mostrarTimePicker(boolean esInicio) {
        Calendar c = Calendar.getInstance();
        int h = c.get(Calendar.HOUR_OF_DAY);
        int m = c.get(Calendar.MINUTE);
        new TimePickerDialog(requireContext(), (view, hourOfDay, minute) -> {
            String hora = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
            if (esInicio) {
                horaInicio = hora;
                btnHoraInicio.setText("Inicio: " + hora);
            } else {
                horaFin = hora;
                btnHoraFin.setText("Fin: " + hora);
            }
        }, h, m, true).show();
    }

    private void observarHorario() {
        horarioViewModel.getHorario().observe(getViewLifecycleOwner(), lista -> {
            horarioAdapter.setHorarioItems(lista);
            horarioAdapter.notifyDataSetChanged();
        });
    }

    private void obtenerHorario() {
        if (idProfesor == -1) {
            Log.e("HorarioFragment", "ID de profesor inválido (-1)");
            requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(),
                    "Error: ID de profesor no válido", Toast.LENGTH_SHORT).show());
            return;
        }

        Log.d("HorarioFragment", "Obteniendo horario del profesor con id: " + idProfesor);
        supabaseHelper.obtenerHorarioProfesor(idProfesor, new SupabaseHelper.SupabaseCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    Log.d("HorarioFragment", "Respuesta de obtenerHorarioProfesor: " + response);
                    JSONArray array = new JSONArray(response);
                    List<HorarioItem> items = new ArrayList<>();

                    for (int i = 0; i < array.length(); i++) {
                        JSONObject o = array.getJSONObject(i);

                        Log.d("HorarioFragment", "Item recibido: " + o.toString());

                        String dia = o.optString("dia", "lunes");
                        String inicio = o.optString("hora_inicio", "08:00");
                        String fin = o.optString("hora_fin", "09:00");
                        int idProfesorAsignatura = o.optInt("id", -1); // Assuming this is the ID for the record

                        String nombreAsignatura = "Asignatura";

                        if (o.has("asignatura") && !o.isNull("asignatura")) {
                            JSONObject asignaturaObj = o.getJSONObject("asignatura");
                            nombreAsignatura = asignaturaObj.optString("nombre", "Asignatura");
                        } else {
                            Log.e("HorarioFragment", "No se encontró objeto asignatura para item " + i);
                        }

                        HorarioItem item = new HorarioItem(nombreAsignatura, inicio, fin, dia, idProfesorAsignatura);
                        items.add(item);
                    }

                    // Ordenamos por día y hora
                    Collections.sort(items, Comparator.comparing(HorarioItem::getDia).thenComparing(HorarioItem::getHoraInicio));

                    horarioViewModel.setHorario(items);
                    Log.d("HorarioFragment", "Horario procesado correctamente");

                } catch (Exception e) {
                    Log.e("HorarioFragment", "Error al parsear el horario", e);
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(String error) {
                Log.e("HorarioFragment", "Error en obtenerHorario: " + error);
                requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Error cargando horario", Toast.LENGTH_SHORT).show());
            }
        });
    }
}