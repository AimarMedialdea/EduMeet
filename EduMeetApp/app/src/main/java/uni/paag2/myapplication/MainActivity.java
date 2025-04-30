package uni.paag2.myapplication;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

import uni.paag2.myapplication.databinding.ActivityMainBinding;
import uni.paag2.myapplication.supabase.SupabaseHelper;
import uni.paag2.myapplication.ui.reunion.ReunionDialogFragment;

public class MainActivity extends AppCompatActivity {

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
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        String email = sharedPreferences.getString("user_email", "");

        if (!email.isEmpty()) {
            // Muestra valores temporales mientras se carga
            nombreTextView.setText("Cargando...");
            emailTextView.setText(email); // Mostrar el email inmediatamente

            SupabaseHelper supabaseHelper = new SupabaseHelper();
            supabaseHelper.obtenerDatosProfesorPorEmail(email, new SupabaseHelper.SupabaseCallback() {
                @Override
                public void onSuccess(String responseData) {
                    try {
                        JSONArray jsonArray = new JSONArray(responseData);
                        Log.d("SUPABASE", "Respuesta: " + responseData);

                        if (jsonArray.length() > 0) {
                            JSONObject profesor = jsonArray.getJSONObject(0);
                            String nombre = profesor.getString("nombre");
                            // Guardar el ID del profesor para futuras referencias
                            if (profesor.has("id_profesor")) {
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putInt("id_profesor", profesor.getInt("id_profesor"));
                                editor.apply();
                            }

                            runOnUiThread(() -> {
                                nombreTextView.setText(nombre);
                                Log.d("UI", "Nombre actualizado: " + nombre);
                            });
                        } else {
                            runOnUiThread(() -> {
                                nombreTextView.setText("Usuario");
                                Log.e("SUPABASE", "No se encontraron datos para el profesor con email: " + email);
                            });
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.e("JSON", "Error al parsear JSON: " + e.getMessage());
                        runOnUiThread(() -> {
                            nombreTextView.setText("Error");
                        });
                    }
                }

                @Override
                public void onFailure(String errorMessage) {
                    Log.e("SUPABASE", "Error al obtener datos: " + errorMessage);
                    runOnUiThread(() -> {
                        nombreTextView.setText("Error");
                    });
                }
            });
        } else {
            Log.e("SESIÓN", "Email del profesor no encontrado en SharedPreferences");
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

        // Configura el comportamiento del FAB según el destino de navegación
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination.getId() == R.id.nav_reunion) {
                fab.show();
                fab.setOnClickListener(view -> {
                    ReunionDialogFragment dialog = new ReunionDialogFragment();
                    dialog.show(getSupportFragmentManager(), "ReunionDialog");
                });
            } else {
                fab.hide(); // ✅ Oculta el botón en cualquier otro destino
            }

        });

        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
    }

    private void verificarSharedPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);

        // Obtener todos los valores guardados para depuración
        Map<String, ?> allEntries = sharedPreferences.getAll();

        Log.d("SHARED_PREFS", "Contenido de SharedPreferences:");
        if (allEntries.isEmpty()) {
            Log.d("SHARED_PREFS", "No hay datos guardados en SharedPreferences");
        } else {
            for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                Log.d("SHARED_PREFS", entry.getKey() + ": " + entry.getValue().toString());
            }
        }

        // Verificar específicamente el ID del profesor
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

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}