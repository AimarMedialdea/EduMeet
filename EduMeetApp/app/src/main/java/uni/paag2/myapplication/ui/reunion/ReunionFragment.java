package uni.paag2.myapplication.ui.reunion;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import uni.paag2.myapplication.R;

public class ReunionFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Simplemente infla el layout sin buscar el FAB
        View root = inflater.inflate(R.layout.fragment_reunion, container, false);
        return root;
    }

    // Este método puede permanecer para ser llamado desde la actividad si es necesario
    public void abrirDialogoReunion() {
        ReunionDialogFragment dialog = new ReunionDialogFragment();
        dialog.show(getParentFragmentManager(), "ReunionDialog");
    }
}