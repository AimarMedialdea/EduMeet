package uni.paag2.myapplication.ui.reunion;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import uni.paag2.myapplication.BaseFragment;
import uni.paag2.myapplication.R;

public class ReunionFragment extends BaseFragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reunion, container, false);

        BottomNavigationView bottomNavigationView = view.findViewById(R.id.bottom_navigation);

        // Fragmento inicial
        getChildFragmentManager().beginTransaction()
                .replace(R.id.reunion_fragment_container, new MisReunionesFragment())
                .commit();

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            if (item.getItemId() == R.id.menu_mis_reuniones) {
                selectedFragment = new MisReunionesFragment();
            } else if (item.getItemId() == R.id.menu_reuniones_unidas) {
                selectedFragment = new ReunionesUnidasFragment();
            }

            if (selectedFragment != null) {
                getChildFragmentManager().beginTransaction()
                        .replace(R.id.reunion_fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });

        return view;
    }
}
