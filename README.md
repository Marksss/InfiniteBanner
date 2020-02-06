English  |  [中文文档](README_cn.md)

# InfiniteBanner
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Platform](https://img.shields.io/badge/platform-android-green.svg)](http://developer.android.com/index.html)
[![API](https://img.shields.io/badge/API-14%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=14)
[![Download](https://api.bintray.com/packages/markshawn/com.github.markshawn/infinite-banner/images/download.svg)](https://bintray.com/markshawn/com.github.markshawn/infinite-banner/_latestVersion)

InfiniteBanner is a view that can automatically or manually scroll in a loop for a banner infinitely, which is very easy to use and is similar to ViewPager. Meanwhile, it not only solves the problem that ViewPager cannot scroll infinitely, but also reuses its child views so that less memory will be token while scrolling.

![demo-gif](https://github.com/Marksss/InfiniteBanner/blob/master/gif/demo.gif)
## Usage
### Add the dependency to your project `build.gradle` file
``` implementation 'com.github.markshawn:infinite-banner:0.9.3' ```
### Code in XML
```
<com.github.infinitebanner.InfiniteBannerView
    android:layout_width="match_parent"
    android:layout_height="200dp"
    app:bannerAutoScroll="true"/>
```
If you want it to scroll automatically, just set bannerAutoScroll true. Some other attributes that may be used:
- bannerDividerWidth：distance between child views；
- bannerInitialPage：the initial position after refreshing；
- bannerForegroundWidthPercent：the width of child view that in the middle of container / the container's width（The default value is 1, which means that only one child view can be seen when scolling stops）；
- bannerLoopInterval：time interval（millisecond）；
- bannerManuallyScrollLock：if manual scrolling is allowed；
- bannerScrollReverse：reverse scrolling automatically.

### Code in Java

Create your own BannerAdapter：
```
public class MyBannerAdapter extends AbsBannerAdapter {
    @Override
    public int getCount() {
        //total count
        ...
    }

    @Override
    protected View makeView(Context context) {
        // create child view if needed
        ...
    }

    @Override
    protected void bind(View view, int position) {
        // bind view and data together and show whatever needed
        ...
    }
}
```
```
infiniteBannerView.setAdapter(new MyBannerAdapter());
```
When you need to refresh your data：

```
myBannerAdapter.notifyDataSetChanged()
```

### Click listener
```
infiniteBannerView.setOnItemClickListener(new InfiniteBannerView.OnItemClickListener() {
    @Override
    public void click(InfiniteBannerView view, int position) {
        ...
    }
});
```

### Animation
Similar to ViewPager，you just need to add transformer：
```
infiniteBannerView.setPageTransformer(new InfiniteBannerView.PageTransformer() {
    @Override
    public void transformPage(@NonNull View view, float offset) {
        ...
    }
});
```

### Indicator
There is no any indicators in this library. If you need one, you can add whatever indicator you want as follows（indicator in demo is another open source library [PageIndicatorView](https://github.com/romandanylyk/PageIndicatorView "PageIndicatorView")）：
```
infiniteBannerView_1.addOnPageChangeListener(new InfiniteBannerView.OnPageChangeListener() {
    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        ...
    }

    @Override
    public void onPageSelected(int position) {
        ...
    }
});
```
## License
InfiniteBanner is released under the [Apache License Version 2.0](LICENSE).
