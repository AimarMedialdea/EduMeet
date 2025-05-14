package uni.paag2.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Locale;

public class Settings extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Button logoutButton = findViewById(R.id.buttonLogout);
        Button changeLanguageButton = findViewById(R.id.buttonChangeLanguage);

        logoutButton.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            prefs.edit().clear().apply(); // Limpia todos los datos guardados
            Intent intent = new Intent(Settings.this, uni.paag2.myapplication.supabase.LogInSupa.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        changeLanguageButton.setOnClickListener(v -> {
            toggleLanguage(); // Cambia entre 'es' y 'eu'
        });
    }

    private void toggleLanguage() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String currentLang = prefs.getString("app_lang", "es");
        String newLang = currentLang.equals("es") ? "eu" : "es";

        Locale newLocale = new Locale(newLang);
        Locale.setDefault(newLocale);

        getResources().getConfiguration().setLocale(newLocale);
        prefs.edit().putString("app_lang", newLang).apply();

        recreate(); // Recarga la actividad con el nuevo idioma
    }
}
