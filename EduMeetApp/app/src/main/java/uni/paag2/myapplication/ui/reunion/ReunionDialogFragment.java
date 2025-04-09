package uni.paag2.myapplication.ui.reunion;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.MultiAutoCompleteTextView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import uni.paag2.myapplication.R;
import uni.paag2.myapplication.supabase.SupabaseHelper;

public class ReunionDialogFragment extends DialogFragment {

    private EditText temaEditText;
    private TextView fechaTextView, horaTextView;
    private Button btnGuardar;
    private Spinner salaSpinner;
    private MultiAutoCompleteTextView participantesTextView;

    // Lista para almacenar nombres y IDs de profesores
    private ArrayList<String> nombresProfesores = new ArrayList<>();
    private ArrayList<Integer> idsProfesores = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    // Lista para mantener los profesores seleccionados
    private ArrayList<Integer> idsProfesoresSeleccionados = new ArrayList<>();

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_reunion, null);

        // Inicializar vistas
        temaEditText = view.findViewById(R.id.temaEditText);
        fechaTextView = view.findViewById(R.id.fechaTextView);
        horaTextView = view.findViewById(R.id.horaTextView);
        btnGuardar = view.findViewById(R.id.btnGuardar);
        salaSpinner = view.findViewById(R.id.salaSpinner);
        participantesTextView = view.findViewById(R.id.participantesTextView);

        // Configurar el spinner para la selección de sala
        configurarSpinner();

        // Obtener la lista de profesores de la base de datos
        obtenerNombresProfesores();

        // Configurar el botón para guardar la reunión
        btnGuardar.setOnClickListener(v -> guardarReunion());

        builder.setView(view);
        return builder.create();
    }

    private void configurarSpinner() {
        String[] opciones = new String[]{"Sala A", "Sala B", "Sala C", "Aula Magna"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, opciones);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        salaSpinner.setAdapter(adapter);
    }

    private void obtenerNombresProfesores() {
        SupabaseHelper supabaseHelper = new SupabaseHelper();
        supabaseHelper.obtenerNombresProfesores(new SupabaseHelper.SupabaseCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONArray jsonArray = new JSONArray(response);

                    // Limpiar las listas por si se recargan los datos
                    nombresProfesores.clear();
                    idsProfesores.clear();

                    // Procesar cada profesor y agregar su nombre e ID a las listas
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject profesor = jsonArray.getJSONObject(i);
                        String nombre = profesor.getString("nombre");
                        int id = profesor.getInt("id_profesor");

                        nombresProfesores.add(nombre);
                        idsProfesores.add(id);
                    }

                    // Configurar el adaptador para el MultiAutoCompleteTextView en el hilo de UI
                    getActivity().runOnUiThread(() -> {
                        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, nombresProfesores);
                        participantesTextView.setAdapter(adapter);
                        participantesTextView.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());

                        // Agregar un listener para capturar los profesores seleccionados
                        participantesTextView.setOnItemClickListener((parent, view, position, id) -> {
                            // No es necesario hacer nada aquí, se procesarán las selecciones al guardar
                        });
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Error al procesar datos de profesores", Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onFailure(String error) {
                System.err.println("Error al obtener nombres de profesores: " + error);
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Error al obtener profesores: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void guardarReunion() {
        String tema = temaEditText.getText().toString().trim();
        String fecha = fechaTextView.getText().toString().trim();
        String hora = horaTextView.getText().toString().trim();
        String participantesTexto = participantesTextView.getText().toString().trim();

        // Validar los campos
        if (tema.isEmpty()) {
            temaEditText.setError("El tema es obligatorio");
            return;
        }

        if (participantesTexto.isEmpty()) {
            participantesTextView.setError("Debes seleccionar al menos un participante");
            return;
        }

        // Procesar los profesores seleccionados
        String[] profesoresSeleccionados = participantesTexto.split(", ");
        idsProfesoresSeleccionados.clear();

        for (String nombreProfesor : profesoresSeleccionados) {
            nombreProfesor = nombreProfesor.trim();
            int index = nombresProfesores.indexOf(nombreProfesor);
            if (index != -1) {
                idsProfesoresSeleccionados.add(idsProfesores.get(index));
            }
        }

        if (idsProfesoresSeleccionados.isEmpty()) {
            Toast.makeText(getContext(), "No se encontraron profesores válidos", Toast.LENGTH_SHORT).show();
            return;
        }

        // Formato de fecha y hora para la BD
        String fechaHoraInicio = fecha + "T" + hora + ":00";

        // Mostrar un mensaje de carga
        Toast.makeText(getContext(), "Guardando reunión...", Toast.LENGTH_SHORT).show();

        // Guardar la reunión
        SupabaseHelper supabaseHelper = new SupabaseHelper();
        supabaseHelper.insertarReunionConParticipantes(tema, fechaHoraInicio, idsProfesoresSeleccionados, new SupabaseHelper.SupabaseCallback() {
            @Override
            public void onSuccess(String response) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Reunión guardada correctamente", Toast.LENGTH_SHORT).show();
                    dismiss(); // Cerrar el diálogo
                });
            }

            @Override
            public void onFailure(String error) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Error al guardar la reunión: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}