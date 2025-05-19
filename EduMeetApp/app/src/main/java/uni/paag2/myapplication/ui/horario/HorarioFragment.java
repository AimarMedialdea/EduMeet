package uni.paag2.myapplication.ui.horario;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.*;

import uni.paag2.myapplication.BaseFragment;
import uni.paag2.myapplication.R;
import uni.paag2.myapplication.supabase.SupabaseHelper;

public class HorarioFragment extends BaseFragment {

    private Spinner spinnerAsignaturas, spinnerDia;
    private Button btnHoraInicio, btnHoraFin, btnGuardar;
    private RecyclerView recyclerHorario;
    private HorarioViewModel horarioViewModel;
    private SupabaseHelper supabaseHelper;
    private String horaInicio = "", horaFin = "";
    private int idProfesor;
    private HorarioAdapter horarioAdapter;

    private Map<String, Integer> asignaturaMap = new HashMap<>();
    private String asignaturaSeleccionada = "";

    // Mapa bidireccional para traducción entre euskera y castellano
    private static final Map<String, String> DIAS_CASTELLANO = new HashMap<String, String>() {{
        // Euskera a castellano
        put("astelehena", "lunes");
        put("asteartea", "martes");
        put("asteazkena", "miercoles");
        put("osteguna", "jueves");
        put("ostirala", "viernes");
        put("larunbata", "sabado");
        put("igandea", "domingo");

        // Castellano a castellano (normalizado)
        put("lunes", "lunes");
        put("martes", "martes");
        put("miércoles", "miercoles");
        put("miercoles", "miercoles");
        put("jueves", "jueves");
        put("viernes", "viernes");
        put("sábado", "sabado");
        put("sabado", "sabado");
        put("domingo", "domingo");
    }};

    // Mapa inverso para mostrar los días en la UI según el idioma actual
    private static final Map<String, String> DIAS_EUSKERA = new HashMap<String, String>() {{
        put("lunes", "astelehena");
        put("martes", "asteartea");
        put("miercoles", "asteazkena");
        put("jueves", "osteguna");
        put("viernes", "ostirala");
        put("sabado", "larunbata");
        put("domingo", "igandea");
    }};

    // Convierte el día seleccionado en el spinner al formato normalizado para la BD (siempre en castellano)
    private String obtenerDiaParaBaseDeDatos(String diaSpinner) {
        String diaNormalizado = diaSpinner.toLowerCase(Locale.getDefault());
        return DIAS_CASTELLANO.getOrDefault(diaNormalizado, "lunes");
    }

    // Convierte el día de la BD al formato de visualización según el idioma actual de la app
    private String obtenerDiaParaUI(String diaBD) {
        boolean isEuskera = esIdiomaEuskera();
        if (isEuskera) {
            return DIAS_EUSKERA.getOrDefault(diaBD, "astelehena");
        } else {
            return diaBD; // Ya está en castellano en la BD
        }
    }

    // Determina si la app está en euskera
    private boolean esIdiomaEuskera() {
        Locale locale = getResources().getConfiguration().locale;
        String language = locale.getLanguage();
        Log.d("HorarioFragment", "Idioma detectado: " + language);
        return language.equals("eu");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_horario, container, false);

        spinnerAsignaturas = root.findViewById(R.id.spinner_asignaturas);
        spinnerDia = root.findViewById(R.id.spinner_dia);
        btnHoraInicio = root.findViewById(R.id.btn_hora_inicio);
        btnHoraFin = root.findViewById(R.id.btn_hora_fin);
        btnGuardar = root.findViewById(R.id.btn_guardar);
        recyclerHorario = root.findViewById(R.id.lista_horario);

        supabaseHelper = new SupabaseHelper();
        horarioViewModel = new ViewModelProvider(this).get(HorarioViewModel.class);

        recyclerHorario.setLayoutManager(new LinearLayoutManager(requireContext()));
        horarioAdapter = new HorarioAdapter(new ArrayList<>());
        recyclerHorario.setAdapter(horarioAdapter);

        setupSwipeToDelete();

        SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        idProfesor = prefs.getInt("id_profesor", -1);
        Log.d("HorarioFragment", "idProfesor cargado: " + idProfesor);

        if (idProfesor == -1) {
            requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(),
                    getString(R.string.error_sesion), Toast.LENGTH_LONG).show());
            return root;
        }

        setupDiaSpinner();
        cargarAsignaturas();
        configurarBotones();
        observarHorario();
        obtenerHorario();

        if (savedInstanceState != null) {
            horaInicio = savedInstanceState.getString("horaInicio", "");
            horaFin = savedInstanceState.getString("horaFin", "");
            if (!horaInicio.isEmpty()) {
                btnHoraInicio.setText(getString(R.string.inicio) + " " + horaInicio);
            }
            if (!horaFin.isEmpty()) {
                btnHoraFin.setText(getString(R.string.fin) + " " + horaFin);
            }
        }

        return root;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("horaInicio", horaInicio);
        outState.putString("horaFin", horaFin);
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

                int idToDelete = itemToDelete.getIdProfesorAsignatura();
                Log.d("HorarioFragment", "Intentando eliminar horario con ID: " + idToDelete);

                new AlertDialog.Builder(requireContext())
                        .setTitle(getString(R.string.titulo_eliminar_horario))
                        .setMessage(getString(R.string.confirmacion_eliminar_horario))
                        .setPositiveButton(getString(R.string.si), (dialog, which) -> {
                            supabaseHelper.eliminarHorario(idToDelete, new SupabaseHelper.SupabaseCallback() {
                                @Override
                                public void onSuccess(String response) {
                                    requireActivity().runOnUiThread(() -> {
                                        Toast.makeText(requireContext(), getString(R.string.horario_eliminado), Toast.LENGTH_SHORT).show();
                                        List<HorarioItem> currentItems = new ArrayList<>(horarioAdapter.getHorarioItems());
                                        currentItems.remove(position);
                                        horarioViewModel.setHorario(currentItems);
                                    });
                                }

                                @Override
                                public void onFailure(String error) {
                                    Log.e("HorarioFragment", "Error al eliminar horario: " + error);
                                    requireActivity().runOnUiThread(() -> {
                                        Toast.makeText(requireContext(), getString(R.string.error_eliminar_horario, error), Toast.LENGTH_SHORT).show();
                                        horarioAdapter.notifyItemChanged(position);
                                    });
                                }
                            });
                        })
                        .setNegativeButton(getString(R.string.no), (dialog, which) -> horarioAdapter.notifyItemChanged(position))
                        .setOnCancelListener(dialog -> horarioAdapter.notifyItemChanged(position))
                        .show();
            }
        };

        new ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerHorario);
    }

    private void setupDiaSpinner() {
        // En lugar de usar el recurso array directamente, creamos una lista de los días
        // en el idioma actual de la aplicación
        List<String> diasEnIdiomaActual = new ArrayList<>();

        if (esIdiomaEuskera()) {
            // Si la app está en euskera, usamos nombres de días en euskera
            diasEnIdiomaActual.add("Astelehena");
            diasEnIdiomaActual.add("Asteartea");
            diasEnIdiomaActual.add("Asteazkena");
            diasEnIdiomaActual.add("Osteguna");
            diasEnIdiomaActual.add("Ostirala");
        } else {
            // Si la app está en castellano, usamos nombres de días en castellano
            diasEnIdiomaActual.add("Lunes");
            diasEnIdiomaActual.add("Martes");
            diasEnIdiomaActual.add("Miércoles");
            diasEnIdiomaActual.add("Jueves");
            diasEnIdiomaActual.add("Viernes");
        }

        // Creamos el adaptador con la lista que hemos generado
        ArrayAdapter<String> diaAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                diasEnIdiomaActual
        );

        diaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDia.setAdapter(diaAdapter);

        // Registrar log para verificar el idioma
        Log.d("HorarioFragment", "Idioma actual: " + (esIdiomaEuskera() ? "Euskera" : "Castellano"));
    }

    private void cargarAsignaturas() {
        supabaseHelper.obtenerAsignaturas(new SupabaseHelper.SupabaseCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    Log.d("HorarioFragment", "Respuesta de obtenerAsignaturas: " + response);
                    JSONArray array = new JSONArray(response);
                    List<String> nombres = new ArrayList<>();
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject asignatura = array.getJSONObject(i);
                        String nombre = asignatura.optString("nombre", getString(R.string.asignatura_desconocida));
                        int id = asignatura.optInt("id_asignatura", -1);
                        nombres.add(nombre);
                        asignaturaMap.put(nombre, id);
                    }

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
                    }

                } catch (Exception e) {
                    Log.e("HorarioFragment", "Error cargando asignaturas", e);
                }
            }

            @Override
            public void onFailure(String error) {
                Log.e("HorarioFragment", "Error en obtenerAsignaturas: " + error);
                if (isAdded() && getContext() != null && getActivity() != null) {
                    requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), getString(R.string.error_cargar_asignaturas), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void configurarBotones() {
        btnHoraInicio.setOnClickListener(v -> mostrarTimePicker(true));
        btnHoraFin.setOnClickListener(v -> mostrarTimePicker(false));

        btnGuardar.setOnClickListener(v -> {
            if (asignaturaSeleccionada.isEmpty()) {
                requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), getString(R.string.debes_seleccionar_asignatura), Toast.LENGTH_SHORT).show());
                return;
            }

            int idAsignatura = asignaturaMap.get(asignaturaSeleccionada);
            String dia = obtenerDiaParaBaseDeDatos(spinnerDia.getSelectedItem().toString());

            supabaseHelper.insertarHorario(idProfesor, idAsignatura, horaInicio, horaFin, dia, new SupabaseHelper.SupabaseCallback() {
                @Override
                public void onSuccess(String response) {
                    obtenerHorario();
                    Log.d("HorarioFragment", "Horario guardado correctamente");
                    requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), getString(R.string.horario_guardado), Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onFailure(String error) {
                    Log.e("HorarioFragment", "Error al guardar horario: " + error);
                    requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), getString(R.string.error_guardar_horario), Toast.LENGTH_SHORT).show());
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
                btnHoraInicio.setText(getString(R.string.inicio) + " " + hora);
            } else {
                horaFin = hora;
                btnHoraFin.setText(getString(R.string.fin) + " " + hora);
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
                    getString(R.string.error_id_profesor), Toast.LENGTH_SHORT).show());
            return;
        }

        Log.d("HorarioFragment", "Obteniendo horario del profesor con id: " + idProfesor);
        supabaseHelper.obtenerHorarioProfesor(idProfesor, new SupabaseHelper.SupabaseCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    Log.d("HorarioFragment", "Respuesta completa de obtenerHorarioProfesor: " + response);
                    JSONArray array = new JSONArray(response);
                    List<HorarioItem> items = new ArrayList<>();

                    for (int i = 0; i < array.length(); i++) {
                        JSONObject o = array.getJSONObject(i);
                        int idRelacion = o.optInt("id_relacion", -1);

                        // Obtener el día en formato de BD (castellano)
                        String diaBD = o.optString("dia", "lunes");

                        // Convertir el día para la UI según el idioma
                        String diaUI = obtenerDiaParaUI(diaBD);

                        String inicio = o.optString("hora_inicio", "08:00");
                        String fin = o.optString("hora_fin", "09:00");

                        String nombreAsignatura = getString(R.string.asignatura);
                        if (o.has("asignatura") && !o.isNull("asignatura")) {
                            JSONObject asignaturaObj = o.getJSONObject("asignatura");
                            nombreAsignatura = asignaturaObj.optString("nombre", getString(R.string.asignatura));
                        }

                        // Usamos diaUI para la visualización pero guardamos también el día original
                        HorarioItem item = new HorarioItem(nombreAsignatura, inicio, fin, diaUI, diaBD, idRelacion);
                        items.add(item);
                    }

                    // Ordenar por día de la semana (lunes primero) y luego por hora
                    Collections.sort(items, (item1, item2) -> {
                        // Primero convertimos los días de UI a los días de BD para ordenar correctamente
                        String dia1BD = item1.getDiaBD(); // Usamos el día de BD almacenado
                        String dia2BD = item2.getDiaBD(); // Usamos el día de BD almacenado

                        // Índices de los días (lunes = 0, martes = 1, etc.)
                        int diaIndex1 = getOrdenDia(dia1BD);
                        int diaIndex2 = getOrdenDia(dia2BD);

                        if (diaIndex1 != diaIndex2) {
                            return Integer.compare(diaIndex1, diaIndex2);
                        } else {
                            return item1.getHoraInicio().compareTo(item2.getHoraInicio());
                        }
                    });

                    horarioViewModel.setHorario(items);

                } catch (Exception e) {
                    Log.e("HorarioFragment", "Error al parsear el horario", e);
                }
            }

            @Override
            public void onFailure(String error) {
                Log.e("HorarioFragment", "Error en obtenerHorario: " + error);
                requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), getString(R.string.error_cargar_horario), Toast.LENGTH_SHORT).show());
            }
        });
    }

    // Método auxiliar para obtener un índice numérico para cada día de la semana
    private int getOrdenDia(String dia) {
        switch (dia.toLowerCase()) {
            case "lunes": return 0;
            case "martes": return 1;
            case "miercoles": return 2;
            case "jueves": return 3;
            case "viernes": return 4;
            case "sabado": return 5;
            case "domingo": return 6;
            default: return 99; // Para cualquier otro valor
        }
    }
}