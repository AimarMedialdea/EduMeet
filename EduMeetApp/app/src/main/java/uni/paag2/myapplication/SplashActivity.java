package uni.paag2.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import uni.paag2.myapplication.supabase.LogInSupa;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String email = prefs.getString("user_email", null);

        if (email != null && !email.isEmpty()) {
            // Sesión activa, redirige al MainActivity
            startActivity(new Intent(this, MainActivity.class));
        } else {
            // No hay sesión, redirige a Login
            startActivity(new Intent(this, LogInSupa.class));
        }

        finish(); // Cierra esta actividad
    }
}
