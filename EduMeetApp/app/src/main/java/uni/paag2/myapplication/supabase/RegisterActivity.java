package uni.paag2.myapplication.supabase;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;

import java.util.ArrayList;
import java.util.List;

import uni.paag2.myapplication.BaseActivity;
import uni.paag2.myapplication.R;

public class RegisterActivity extends BaseActivity {

    private SupabaseHelper supabaseHelper;
    private EditText editTextNombre;
    private EditText editTextEmail;
    private EditText editTextPassword;
    private EditText editTextConfirmPassword;
    private Spinner dptSpinner;
    private List<Departamento> departamentoList = new ArrayList<>();

    class Departamento {
        int id;
        String nombre;

        Departamento(int id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }

        @Override
        public String toString() {
            return nombre;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        supabaseHelper = new SupabaseHelper();

        editTextNombre = findViewById(R.id.editTextNombre);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        dptSpinner = findViewById(R.id.dptSpinner);
        Button buttonRegister = findViewById(R.id.buttonRegister);

        buttonRegister.setOnClickListener(v -> attemptRegister());

        cargarDepartamentos();
    }

    private void cargarDepartamentos() {
        supabaseHelper.obtenerDepartamentos(new SupabaseHelper.SupabaseCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        JSONArray jsonArray = new JSONArray(response);
                        departamentoList.clear();

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject obj = jsonArray.getJSONObject(i);
                            int id = obj.getInt("id_departamento");
                            String nombre = obj.getString("nombre");
                            departamentoList.add(new Departamento(id, nombre));
                        }

                        ArrayAdapter<Departamento> adapter = new ArrayAdapter<>(RegisterActivity.this, android.R.layout.simple_spinner_item, departamentoList);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        dptSpinner.setAdapter(adapter);

                    } catch (Exception e) {
                        Toast.makeText(RegisterActivity.this, "Error procesando departamentos", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> Toast.makeText(RegisterActivity.this, "Error al cargar departamentos", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void attemptRegister() {
        String nombre = editTextNombre.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String confirmPassword = editTextConfirmPassword.getText().toString().trim();

        if (nombre.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
            return;
        }

        // Hashear la contraseña
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        Departamento dptoSeleccionado = (Departamento) dptSpinner.getSelectedItem();
        int idDepartamento = dptoSeleccionado.id;

        supabaseHelper.registerUser(nombre, email, hashedPassword, idDepartamento, new SupabaseHelper.SupabaseCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    Toast.makeText(RegisterActivity.this, "Registro exitoso", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> Toast.makeText(RegisterActivity.this, error, Toast.LENGTH_SHORT).show());
            }
        });
    }
}
