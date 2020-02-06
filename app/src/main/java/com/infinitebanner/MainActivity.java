package com.infinitebanner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.github.infinitebanner.InfiniteBannerView;
import com.rd.PageIndicatorView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        InfiniteBannerView infiniteBannerView_0 = findViewById(R.id.infinite_banner_0);
        infiniteBannerView_0.setAdapter(new BannerAdapter());
        infiniteBannerView_0.setPageTransformer(new InfiniteBannerView.PageTransformer() {
            @Override
            public void transformPage(@NonNull View view, float offset) {
                view.setScaleY(0.8f + 0.2f * offset);
                view.setAlpha(0.5f + 0.5f * offset);
            }
        });
        infiniteBannerView_0.setOnItemClickListener(new InfiniteBannerView.OnItemClickListener() {
            @Override
            public void click(InfiniteBannerView view, int position) {
                Toast.makeText(MainActivity.this, ""+position, Toast.LENGTH_SHORT).show();
            }
        });
        infiniteBannerView_0.setInitPosition(1);

        InfiniteBannerView infiniteBannerView_1 = findViewById(R.id.infinite_banner_1);
        infiniteBannerView_1.setAdapter(new BannerAdapter());
        final PageIndicatorView pageIndicatorView = findViewById(R.id.pageIndicatorView);
        pageIndicatorView.setCount(3);
        infiniteBannerView_1.addOnPageChangeListener(new InfiniteBannerView.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                pageIndicatorView.setSelection(position);
            }
        });
    }
}
