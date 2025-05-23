package uni.paag2.myapplication.ui.reunion;

import android.os.Bundle;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.List;

import uni.paag2.myapplication.R;

public class ReunionesActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private ReunionPagerAdapter pagerAdapter;
    private ImageButton buttonLeft, buttonRight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reuniones);

        viewPager = findViewById(R.id.viewPager);
        buttonLeft = findViewById(R.id.button_left);
        buttonRight = findViewById(R.id.button_right);

        List<Fragment> fragmentList = new ArrayList<>();
        fragmentList.add(new MisReunionesFragment());
        fragmentList.add(new ReunionesUnidasFragment());

        pagerAdapter = new ReunionPagerAdapter(this, fragmentList);
        viewPager.setAdapter(pagerAdapter);

        buttonLeft.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            if (current > 0) viewPager.setCurrentItem(current - 1);
        });

        buttonRight.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            if (current < pagerAdapter.getItemCount() - 1) viewPager.setCurrentItem(current + 1);
        });
    }
}
