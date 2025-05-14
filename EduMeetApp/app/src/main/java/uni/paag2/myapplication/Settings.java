package uni.paag2.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import java.util.Locale;

public class Settings extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Button logoutButton = findViewById(R.id.buttonLogout);
        Button changeLanguageButton = findViewById(R.id.buttonChangeLanguage);

        // Mostrar el idioma actual
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String currentLang = prefs.getString("app_lang", "eu");
        updateLanguageButtonText(changeLanguageButton, currentLang);

        logoutButton.setOnClickListener(v -> {
            SharedPreferences userPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            // Guardar el idioma actual antes de limpiar las preferencias
            String savedLang = userPrefs.getString("app_lang", "eu");

            userPrefs.edit().clear().apply(); // Limpia todos los datos guardados

            // Restaurar solo la configuración de idioma después de limpiar
            userPrefs.edit().putString("app_lang", savedLang).apply();

            Intent intent = new Intent(Settings.this, uni.paag2.myapplication.supabase.LogInSupa.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        changeLanguageButton.setOnClickListener(v -> {
            toggleLanguageAndRestart();
        });
    }

    private void updateLanguageButtonText(Button button, String currentLang) {
        if (currentLang.equals("eu")) {
            button.setText("Cambiar a Español");
        } else {
            button.setText("Aldatu Euskarara");
        }
    }

    private void toggleLanguageAndRestart() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String currentLang = prefs.getString("app_lang", "eu");
        String newLang = currentLang.equals("eu") ? "es" : "eu";

        // Guardar el nuevo idioma en preferencias
        prefs.edit().putString("app_lang", newLang).apply();

        // Cambiar locale en tiempo real
        Locale locale = new Locale(newLang);
        Locale.setDefault(locale);
        Configuration config = getResources().getConfiguration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

        // Mostrar un mensaje con el idioma seleccionado
        String message = newLang.equals("eu") ? "Hizkuntza euskarara aldatu da" : "El idioma ha cambiado a español";
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

        // Reiniciar la aplicación para aplicar el cambio a toda la app
        Intent intent = new Intent(Settings.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}