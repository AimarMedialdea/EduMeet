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
                    // Intentar parsear la respuesta para obtener información del usuario
                    JSONObject jsonResponse = new JSONObject(response);

                    // Guardar el email del usuario en SharedPreferences
                    SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("user_email", email);

                    // Intentar obtener el token de acceso si está disponible
                    if (jsonResponse.has("access_token")) {
                        String accessToken = jsonResponse.getString("access_token");
                        editor.putString("access_token", accessToken);
                    }

                    // Aplicar los cambios
                    editor.apply();

                    Log.d("LOGIN", "Email guardado en SharedPreferences: " + email);
                } catch (JSONException e) {
                    // Si falla el parseo, al menos guardar el email
                    Log.e("LOGIN", "Error al parsear respuesta JSON: " + e.getMessage());
                    SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("user_email", email);
                    editor.apply();
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
}