package uni.paag2.myapplication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;
import java.util.Map;

import uni.paag2.myapplication.databinding.ActivityMainBinding;
import uni.paag2.myapplication.supabase.SupabaseHelper;
import uni.paag2.myapplication.ui.reunion.ReunionDialogFragment;

public class MainActivity extends BaseActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);

        // Verificar las SharedPreferences para depuración
        verificarSharedPreferences();

        // Configuración del FAB
        FloatingActionButton fab = binding.appBarMain.fab;

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        // Obtener la vista del header del sidebar
        View headerView = navigationView.getHeaderView(0);
        TextView nombreTextView = headerView.findViewById(R.id.nombreU);
        TextView emailTextView = headerView.findViewById(R.id.emailU);

        // Obtener el email del profesor desde SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String email = sharedPreferences.getString("user_email", "");

        if (!email.isEmpty()) {
            nombreTextView.setText("Cargando...");
            emailTextView.setText(email);

            SupabaseHelper supabaseHelper = new SupabaseHelper();
            supabaseHelper.obtenerDatosProfesorPorEmail(email, new SupabaseHelper.SupabaseCallback() {
                @Override
                public void onSuccess(String responseData) {
                    try {
                        JSONArray jsonArray = new JSONArray(responseData);
                        if (jsonArray.length() > 0) {
                            JSONObject profesor = jsonArray.getJSONObject(0);
                            String nombre = profesor.getString("nombre");
                            if (profesor.has("id_profesor")) {
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putInt("id_profesor", profesor.getInt("id_profesor"));
                                editor.apply();
                            }
                            runOnUiThread(() -> nombreTextView.setText(nombre));
                        } else {
                            runOnUiThread(() -> nombreTextView.setText("Usuario"));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        runOnUiThread(() -> nombreTextView.setText("Error"));
                    }
                }

                @Override
                public void onFailure(String errorMessage) {
                    runOnUiThread(() -> nombreTextView.setText("Error"));
                }
            });
        } else {
            nombreTextView.setText("Invitado");
            emailTextView.setText("No hay sesión iniciada");
            Toast.makeText(this, "No se encontró información de sesión", Toast.LENGTH_LONG).show();
        }

        // Configurar Navigation Component
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_reunion, R.id.nav_horario)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination.getId() == R.id.nav_reunion) {
                fab.show();
                fab.setOnClickListener(view -> {
                    ReunionDialogFragment dialog = new ReunionDialogFragment();
                    dialog.show(getSupportFragmentManager(), "ReunionDialog");
                });
            } else {
                fab.hide();
            }
        });

        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        recreate(); // Recrear la actividad para aplicar cambios de idioma
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(updateBaseContextLocale(newBase));
    }

    @Override
    public void applyOverrideConfiguration(Configuration overrideConfiguration) {
        if (overrideConfiguration != null) {
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            String lang = prefs.getString("app_lang", "eu");
            Locale locale = new Locale(lang);
            overrideConfiguration.setLocale(locale);
        }
        super.applyOverrideConfiguration(overrideConfiguration);
    }

    private void verificarSharedPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        Map<String, ?> allEntries = sharedPreferences.getAll();
        Log.d("SHARED_PREFS", "Contenido de SharedPreferences:");
        if (allEntries.isEmpty()) {
            Log.d("SHARED_PREFS", "No hay datos guardados");
        } else {
            for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                Log.d("SHARED_PREFS", entry.getKey() + ": " + entry.getValue().toString());
            }
        }
        if (sharedPreferences.contains("id_profesor")) {
            int idProfesor = sharedPreferences.getInt("id_profesor", -1);
            Log.d("SHARED_PREFS", "ID del profesor encontrado: " + idProfesor);
        } else {
            Log.d("SHARED_PREFS", "La clave 'id_profesor' no existe");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    // Redirige al Settings.java cuando se selecciona el ítem del menú
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            Intent intent = new Intent(MainActivity.this, Settings.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}