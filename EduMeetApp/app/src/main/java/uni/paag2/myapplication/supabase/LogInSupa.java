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

import org.json.JSONException;
import org.json.JSONObject;

import uni.paag2.myapplication.MainActivity;
import uni.paag2.myapplication.R;

public class LogInSupa extends AppCompatActivity {
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
            Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        supabaseHelper.loginUser(email, password, LogInSupa.this, new SupabaseHelper.SupabaseCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject jsonResponse = new JSONObject(response);

                    SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("user_email", email);

                    if (jsonResponse.has("access_token")) {
                        String accessToken = jsonResponse.getString("access_token");
                        editor.putString("access_token", accessToken);
                    }

                    editor.apply();
                    Log.d("LOGIN", "Email guardado en SharedPreferences: " + email);

                    // Obtener el ID del profesor después del login exitoso
                    obtenerIdProfesor(email);

                } catch (JSONException e) {
                    Log.e("LOGIN", "Error al parsear respuesta JSON: " + e.getMessage());
                    SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("user_email", email);
                    editor.apply();

                    // También intenta obtener el ID del profesor aunque falle el parseo
                    obtenerIdProfesor(email);
                }

                runOnUiThread(() -> {
                    Toast.makeText(LogInSupa.this, "Login exitoso", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(LogInSupa.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() ->
                        Toast.makeText(LogInSupa.this, error, Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void obtenerIdProfesor(String email) {
        supabaseHelper.obtenerIdProfesorPorEmail(email, LogInSupa.this, new SupabaseHelper.SupabaseCallback() {
            @Override
            public void onSuccess(String idProfesor) {
                SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt("id_profesor", Integer.parseInt(idProfesor));
                editor.apply();
                Log.d("LOGIN", "ID profesor guardado en SharedPreferences: " + idProfesor);
            }

            @Override
            public void onFailure(String error) {
                Log.e("LOGIN", "Error al obtener id_profesor: " + error);
            }
        });
    }
}
