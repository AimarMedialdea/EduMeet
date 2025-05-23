package uni.paag2.myapplication.ui.reunion;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.List;

import me.relex.circleindicator.CircleIndicator3;
import uni.paag2.myapplication.R;

public class ReunionFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reunion, container, false);

        ViewPager2 viewPager = view.findViewById(R.id.viewPager);
        CircleIndicator3 indicator = view.findViewById(R.id.indicator);
        TextView reunionTitle = view.findViewById(R.id.reunionTitle); // 🔴 Asegúrate de tener este TextView en el XML

        List<Fragment> fragments = new ArrayList<>();
        fragments.add(new MisReunionesFragment());
        fragments.add(new ReunionesUnidasFragment());

        ReunionPagerAdapter adapter = new ReunionPagerAdapter(requireActivity(), fragments);
        viewPager.setAdapter(adapter);

        indicator.setViewPager(viewPager);

        // ✅ Cambia el título según la página del ViewPager
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (position == 0) {
                    reunionTitle.setText(R.string.reuniones_creadas);
                } else if (position == 1) {
                    reunionTitle.setText(R.string.reuniones_unidas);
                }
            }
        });

        return view;
    }
}
