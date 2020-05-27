package com.infinitebanner;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.github.infinitebanner.AbsBannerAdapter;
import com.github.infinitebanner.InfiniteBannerView;

import java.util.ArrayList;
import java.util.List;

public class BannerAdapter extends AbsBannerAdapter {

    @Override
    public int getCount() {
        return 3;
    }

    @Override
    protected View makeView(InfiniteBannerView parent) {
        ImageView imageView = new ImageView(parent.getContext());
        imageView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        return imageView;
    }

    @Override
    protected void bind(View view, int position) {
        if (position == 0) {
            ((ImageView) view).setImageResource(R.drawable.img_0);
        } else if (position == 1) {
            ((ImageView) view).setImageResource(R.drawable.img_1);
        } else {
            ((ImageView) view).setImageResource(R.drawable.img_2);
        }
    }
}
