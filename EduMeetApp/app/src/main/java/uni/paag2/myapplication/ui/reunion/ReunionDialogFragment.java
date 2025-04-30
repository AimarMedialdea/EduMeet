package uni.paag2.myapplication.ui.reunion;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.util.List;

import uni.paag2.myapplication.R;
import uni.paag2.myapplication.model.Reunion;
import uni.paag2.myapplication.supabase.SupabaseHelper;

public class ReunionDialogFragment extends DialogFragment {

    private EditText temaEditText;
    private TextView fechaTextView, horaTextView;
    private Button btnGuardar;
    private Spinner salaSpinner;

    // Para edición
    private Integer idReunion = null;

    public void setReunionData(int idReunion, String tema, String fecha, String hora, String sala) {
        this.idReunion = idReunion;
        Bundle args = new Bundle();
        args.putString("tema", tema);
        args.putString("fecha", fecha);
        args.putString("hora", hora);
        args.putString("sala", sala);
        setArguments(args);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_reunion, null);

        temaEditText = view.findViewById(R.id.temaEditText);
        fechaTextView = view.findViewById(R.id.fechaEditText);
        horaTextView = view.findViewById(R.id.horaEditText);
        btnGuardar = view.findViewById(R.id.btnGuardar);
        salaSpinner = view.findViewById(R.id.salaSpinner);

        configurarSpinner();

        // Si hay datos para edición
        if (getArguments() != null) {
            idReunion = getArguments().getInt("id_reunion", -1);
            if (idReunion == -1) idReunion = null;

            temaEditText.setText(getArguments().getString("tema"));
            fechaTextView.setText(getArguments().getString("fecha"));
            horaTextView.setText(getArguments().getString("hora"));

            String sala = getArguments().getString("sala");
            ArrayAdapter adapter = (ArrayAdapter) salaSpinner.getAdapter();
            int pos = adapter.getPosition(sala);
            salaSpinner.setSelection(pos);
        }

        btnGuardar.setOnClickListener(v -> guardarReunion());

        builder.setView(view);
        return builder.create();
    }

    private void configurarSpinner() {
        SupabaseHelper supabaseHelper = new SupabaseHelper();
        supabaseHelper.obtenerAulas(new SupabaseHelper.SupabaseCallbackAulas() {
            @Override
            public void onSuccess(List<String> aulas) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, aulas);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    salaSpinner.setAdapter(adapter);

                    // Si estamos editando, seleccionamos el aula correspondiente
                    if (getArguments() != null) {
                        String sala = getArguments().getString("sala");
                        int pos = adapter.getPosition(sala);
                        if (pos >= 0) salaSpinner.setSelection(pos);
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(getContext(), "Error al cargar aulas: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void guardarReunion() {
        String tema = temaEditText.getText().toString().trim();
        String fecha = fechaTextView.getText().toString().trim();
        String hora = horaTextView.getText().toString().trim();
        String sala = salaSpinner.getSelectedItem().toString();

        if (tema.isEmpty()) {
            temaEditText.setError("El tema es obligatorio");
            return;
        }
        if (fecha.isEmpty() || hora.isEmpty()) {
            Toast.makeText(getContext(), "Debes seleccionar fecha y hora", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        int idProfesor = prefs.getInt("id_profesor", -1);
        if (idProfesor == -1) {
            Toast.makeText(getContext(), "Error: No se ha iniciado sesión correctamente", Toast.LENGTH_LONG).show();
            return;
        }

        SupabaseHelper supabaseHelper = new SupabaseHelper();

        if (idReunion == null) {
            // Nueva reunión
            Toast.makeText(getContext(), "Guardando reunión...", Toast.LENGTH_SHORT).show();
            supabaseHelper.insertarReunion(tema, fecha, hora + ":00", sala, idProfesor, callback());
        } else {
            // Actualizar reunión existente
            Toast.makeText(getContext(), "Actualizando reunión...", Toast.LENGTH_SHORT).show();
            supabaseHelper.actualizarReunion(idReunion, tema, fecha, hora + ":00", sala, idProfesor, callback());
        }
    }

    private SupabaseHelper.SupabaseCallback callback() {
        return new SupabaseHelper.SupabaseCallback() {
            @Override
            public void onSuccess(String response) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Operación exitosa", Toast.LENGTH_SHORT).show();
                    dismiss();
                });
            }

            @Override
            public void onFailure(String error) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        };
    }

    public static ReunionDialogFragment nuevaParaEditar(Reunion reunion) {
        ReunionDialogFragment fragment = new ReunionDialogFragment();

        Bundle args = new Bundle();
        args.putInt("id_reunion", reunion.getIdReunion());
        args.putString("tema", reunion.getTema());
        args.putString("fecha", reunion.getFecha());
        args.putString("hora", reunion.getHoraInicio());
        args.putString("sala", reunion.getSala());

        fragment.setArguments(args);
        return fragment;
    }

}
