package uni.paag2.myapplication.supabase;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;

import uni.paag2.myapplication.BaseActivity;
import uni.paag2.myapplication.MainActivity;
import uni.paag2.myapplication.R;

public class LogInSupa extends BaseActivity {
    private SupabaseHelper supabaseHelper;
    private EditText editTextEmail;
    private EditText editTextPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        supabaseHelper = new SupabaseHelper();

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        Button buttonLogin = findViewById(R.id.buttonLogin);
        TextView textViewRegister = findViewById(R.id.textViewRegister);

        buttonLogin.setOnClickListener(v -> attemptLogin());

        textViewRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LogInSupa.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void attemptLogin() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        supabaseHelper.obtenerUsuarioPorEmail(email, new SupabaseHelper.SupabaseCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONArray jsonArray = new JSONArray(response);
                    if (jsonArray.length() > 0) {
                        JSONObject userObject = jsonArray.getJSONObject(0);
                        String storedHash = userObject.getString("contrasena");

                        if (BCrypt.checkpw(password, storedHash)) {
                            // Contraseña válida
                            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putString("user_email", email);
                            editor.apply();

                            obtenerIdProfesor(email);

                            runOnUiThread(() -> {
                                Toast.makeText(LogInSupa.this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(LogInSupa.this, MainActivity.class));
                                finish();
                            });
                        } else {
                            runOnUiThread(() -> Toast.makeText(LogInSupa.this, "Contraseña incorrecta", Toast.LENGTH_SHORT).show());
                        }
                    } else {
                        runOnUiThread(() -> Toast.makeText(LogInSupa.this, "Usuario no encontrado", Toast.LENGTH_SHORT).show());
                    }
                } catch (JSONException e) {
                    runOnUiThread(() -> Toast.makeText(LogInSupa.this, "Error al procesar respuesta", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> Toast.makeText(LogInSupa.this, "Error: " + error, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void obtenerIdProfesor(String email) {
        supabaseHelper.obtenerIdProfesorPorEmail(email, LogInSupa.this, new SupabaseHelper.SupabaseCallback() {
            @Override
            public void onSuccess(String idProfesor) {
                try {
                    int id = Integer.parseInt(idProfesor);
                    SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putInt("id_profesor", id);
                    editor.apply();
                } catch (NumberFormatException e) {
                    Log.e("LOGIN", "Error al convertir id_profesor a entero: " + idProfesor);
                }
            }

            @Override
            public void onFailure(String error) {
                Log.e("LOGIN", "Error al obtener id_profesor: " + error);
            }
        });
    }
}
