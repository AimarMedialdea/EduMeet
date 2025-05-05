package uni.paag2.myapplication.ui.reunion;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import uni.paag2.myapplication.R;

public class ReunionesActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reuniones);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            if (item.getItemId() == R.id.menu_mis_reuniones) {
                selectedFragment = new MisReunionesFragment();
            } else if (item.getItemId() == R.id.menu_reuniones_unidas) {
                selectedFragment = new ReunionesUnidasFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.reunion_fragment_container, selectedFragment)
                        .commit();
            }

            return true;
        });

// Selección por defecto
        bottomNavigationView.setSelectedItemId(R.id.menu_mis_reuniones);

    }
}
