package uni.paag2.myapplication;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * Fragment base del que deben heredar todos los fragments de la aplicación
 * para asegurar la correcta gestión del idioma
 */
public abstract class BaseFragment extends Fragment {

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(LocaleHelper.setLocale(context));
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LocaleHelper.applyLocaleToFragment(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        LocaleHelper.applyLocaleToFragment(this);
    }
}