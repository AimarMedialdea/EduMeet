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
            Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        supabaseHelper.loginUser(email, password, LogInSupa.this, new SupabaseHelper.SupabaseCallback() {
            @Override
            public void onSuccess(String response) {
                SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("user_email", email);

                try {
                    // Intentar parsear como JSON
                    JSONObject jsonResponse = new JSONObject(response);

                    if (jsonResponse.has("access_token")) {
                        String accessToken = jsonResponse.getString("access_token");
                        editor.putString("access_token", accessToken);
                        Log.d("LOGIN", "Access token guardado.");
                    }

                } catch (JSONException e) {
                    // Si no es JSON, asumimos que fue solo un texto como "Login correcto"
                    Log.d("LOGIN", "Respuesta no es JSON. Login aparentemente exitoso: " + response);
                }

                editor.apply();
                Log.d("LOGIN", "Email guardado en SharedPreferences: " + email);

                // Obtener el ID del profesor por email
                obtenerIdProfesor(email);

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
                try {
                    int id = Integer.parseInt(idProfesor);
                    SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putInt("id_profesor", id);
                    editor.apply();
                    Log.d("LOGIN", "ID profesor guardado en SharedPreferences: " + idProfesor);
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
