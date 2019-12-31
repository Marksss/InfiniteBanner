package com.infinitebanner;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.github.infinitebanner.InfiniteBannerView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        InfiniteBannerView infiniteBannerView = findViewById(R.id.infinite_banner_0);
        infiniteBannerView.setAdapter(new BannerAdapter());
        infiniteBannerView.setPageTransformer(new InfiniteBannerView.PageTransformer() {
            @Override
            public void transformPage(@NonNull View view, float offset) {
                view.setScaleY(0.8f + 0.2f * offset);
                view.setAlpha(0.5f + 0.5f * offset);
            }
        });
        infiniteBannerView.setOnItemClickListener(new InfiniteBannerView.OnItemClickListener() {
            @Override
            public void click(InfiniteBannerView view, int position) {
                Toast.makeText(MainActivity.this, ""+position, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
