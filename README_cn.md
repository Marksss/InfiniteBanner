[English](README.md)  |  中文文档

# InfiniteBanner
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Platform](https://img.shields.io/badge/platform-android-green.svg)](http://developer.android.com/index.html)
[![API](https://img.shields.io/badge/API-14%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=14)

InfiniteBanner是一个能够无限轮播的banner，它的用法简单而且与ViewPager非常类似，但它也解决了ViewPager无法无限轮播的痛点。同时，它的内部是以子控件复用的形式实现，在无限轮播的同时并不会消耗过多的内存。

![demo-gif](https://github.com/Marksss/InfiniteBanner/blob/master/gif/demo.gif)
## 用法
### 将以下依赖添加到 `build.gradle` 文件中
``` implementation 'com.github.markshawn:infinite-banner:0.9.1' ```
### XML中的代码
```
<com.github.infinitebanner.InfiniteBannerView
    android:layout_width="match_parent"
    android:layout_height="200dp"
    app:bannerAutoScroll="true"/>
```
如果想让banner自动开启轮播，只需将bannerAutoScroll设置为true即可。其他可能会用到的属性值如下：
- bannerDividerWidth：每个子View之间的间隔距离；
- bannerInitialPage：每次刷新后的起始位置；
- bannerForegroundWidthPercent：居中显示的子View的宽度占容器总宽度的比值（默认是1，即静止的时候只能看到一个子View）；
- bannerLoopInterval：自动轮播的时间间隔（微秒）；
- bannerManuallyScrollLock：是否静止手划操作；
- bannerScrollReverse：是否反向轮播。

### Java中的代码
实现自己的BannerAdapter：
```
public class MyBannerAdapter extends AbsBannerAdapter {
    @Override
    public int getCount() {
        //总数
        ...
    }

    @Override
    protected View makeView(Context context) {
        // 创建子view
        ...
    }

    @Override
    protected void bind(View view, int position) {
        // 子view与数据绑定并展示相应内容
        ...
    }
}
```
```
infiniteBannerView.setAdapter(new MyBannerAdapter());
```
当需要刷新数据时：

```
myBannerAdapter.notifyDataSetChanged()
```

### 点击事件
```
infiniteBannerView.setOnItemClickListener(new InfiniteBannerView.OnItemClickListener() {
    @Override
    public void click(InfiniteBannerView view, int position) {
        ...
    }
});
```

### 动画效果
和ViewPager类似，只要添加transformer即可：
```
infiniteBannerView.setPageTransformer(new InfiniteBannerView.PageTransformer() {
    @Override
    public void transformPage(@NonNull View view, float offset) {
        ...
    }
});
```

### 指示器
本库中并没有添加任何indicator，如果想加indicator，可以用如下类似ViewPager的方式很轻松地添加自己喜欢的indicator（Demo中的indicator是一个开源库[PageIndicatorView](https://github.com/romandanylyk/PageIndicatorView "PageIndicatorView")）：
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

## 许可证
InfiniteBanner基于 [Apache License Version 2.0](LICENSE) 发布。