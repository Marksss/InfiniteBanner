package com.github.infinitebanner;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.FloatRange;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Scroller;

import java.util.ArrayList;
import java.util.List;

/**
 * 可以无限循环的banner
 * Created by Marksss on 2019/8/12.
 */
public class InfiniteBannerView extends ViewGroup {
    private Pager mPager = new Pager();
    private long mLoopInterval = 3000;
    private AbsBannerAdapter mAdapter;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private int mDividerWidth = 0; // 两个page之间的空白的间隔
    private int mOutsideDepth = 1; // 预加载的可见层数（实际预加载层数）
    private float mOutSidePercent = 0f; // 当前page两边露出部分中单边的占比（0<=percent<=0.33）
    private boolean mIsReverse; // false:正向,从右往左；true:反向,从左往右；默认false
    private boolean mIsScrollLocked; // 是否禁止触摸拖动
    private boolean mIsTouching, mIsLoopingStart, mIsAttachToWindow;
    private long mLastTouchTime, mLastScrollTime;
    private int mLastX, mLastDownX, mLastDownY;
    private Scroller mScroller;
    private VelocityTracker mVelocityTracker;
    private int mTouchSlop, mMaxVelocity;
    private OnItemClickListener mOnItemClickListener;
    private List<OnPageChangeListener> mOnPageChangeListeners = new ArrayList<>();
    private ScrollIntercepter mScrollIntercepter;
    private PageTransformer mPageTransformer;
    private SparseArray<View> mViewsMap = new SparseArray<>();

    public InfiniteBannerView(Context context) {
        super(context);
        init(context, null);
    }

    public InfiniteBannerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public InfiniteBannerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public InfiniteBannerView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.InfiniteBannerView);
            mDividerWidth = a.getDimensionPixelSize(R.styleable.InfiniteBannerView_bannerDividerWidth, mDividerWidth);
            mPager.initPosition(a.getInt(R.styleable.InfiniteBannerView_bannerInitialPage, 0));
            mIsScrollLocked = a.getBoolean(R.styleable.InfiniteBannerView_bannerScrollLock, false);
            mIsReverse = a.getBoolean(R.styleable.InfiniteBannerView_bannerScrollReverse, false);
            mOutSidePercent = a.getFloat(R.styleable.InfiniteBannerView_bannerOutsidePercent, mOutSidePercent);
            if (mOutSidePercent > 0){
                mOutsideDepth = 2;
                mOutSidePercent = Math.min(mOutSidePercent, 0.33f);
            }
            String s = a.getString(R.styleable.InfiniteBannerView_bannerLoopInterval);
            if (s != null) {
                try {
                    mLoopInterval = Long.parseLong(s);
                } catch (NumberFormatException e) {
                    Log.e("InfiniteBannerView", e.getMessage());
                }
            }
            a.recycle();
        }
        mScroller = new Scroller(context);
        ViewConfiguration config = ViewConfiguration.get(context);
        mTouchSlop = config.getScaledPagingTouchSlop();
        mMaxVelocity = config.getScaledMinimumFlingVelocity();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mAdapter != null) {
            mAdapter.registerDataSetObserver(mDataObserver);
        }
        startLoopIfNeeded();
        mIsAttachToWindow = true;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAdapter != null) {
            mAdapter.unregisterDataSetObserver(mDataObserver);
        }
        dispose();
        mIsAttachToWindow = false;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int sizeHeight = MeasureSpec.getSize(heightMeasureSpec);
        int modeHeight = MeasureSpec.getMode(heightMeasureSpec);
        int maxHeight = sizeHeight - getPaddingTop() - getPaddingBottom();
        int childMaxWidth = (int) ((MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight()) * (1 - 2 * mOutSidePercent));
        int height = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            measureChild(child, MeasureSpec.makeMeasureSpec(childMaxWidth, MeasureSpec.EXACTLY), heightMeasureSpec);
            height = Math.min(Math.max(height, child.getMeasuredHeight()), maxHeight);
        }
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec),
                modeHeight == MeasureSpec.EXACTLY ? sizeHeight : height + getPaddingTop() + getPaddingBottom());
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (getChildCount() == 0 || mAdapter == null) {
            return;
        }
        int outsideLength = (int) (getMeasuredWidth() * mOutSidePercent);
        int periodLength = getPeriodLength(getMeasuredWidth());
        if (mPager.isSkippingPosition()) {
            // 重新刷新
            int head = mPager.mCurrentPosition - getDepthWithCache();
            int tail = mPager.mCurrentPosition + getDepthWithCache();
            while (head <= tail) {
                if (head == tail) {
                    childLayout(getViewOnPosition(head), periodLength, outsideLength, head);
                } else {
                    childLayout(getViewOnPosition(head), periodLength, outsideLength, head);
                    childLayout(getViewOnPosition(tail), periodLength, outsideLength, tail);
                }
                head++;
                tail--;
            }
        } else if (mPager.isPreviousPosition()) {
            // 前一页
            childLayout(getViewOnPosition(mPager.mPositionLastRelayout + getDepthWithCache()),
                    periodLength, outsideLength, mPager.mPositionLastRelayout - getDepthWithCache() - 1);
        } else if (mPager.isNextPosition()) {
            // 下一页
            childLayout(getViewOnPosition(mPager.mPositionLastRelayout - getDepthWithCache()),
                    periodLength, outsideLength, mPager.mPositionLastRelayout + getDepthWithCache() + 1);
        }
        mPager.recordWhenRelayout();
    }

    private void childLayout(final View child, int periodLength, int outsideLength, int page) {
        if (child == null) {
            return;
        }
        final int index = getLoopIndex(page);
        if (index >= 0) {
            int childX = outsideLength + page * periodLength;
            child.layout(
                    childX,
                    0,
                    childX + periodLength - mDividerWidth,
                    getMeasuredHeight());
            mAdapter.bind(child, index);
        } else {
            child.layout(0, 0, 0, 0);
        }
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            int scrollX = mScroller.getCurrX();
            scrollTo(scrollX, mScroller.getCurrY());
            invalidate();
        }
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);

        if (getItemCount() > 0) {
            int periodLength = getPeriodLength(getWidth());
            int page = l / periodLength;
            int positionOffsetPixels = l % periodLength;
            if (l < 0){
                page -= 1;
                positionOffsetPixels += periodLength;
            }
            float positionOffset = (float) positionOffsetPixels / (float) periodLength;
            onPageScrolledInternal(page, positionOffset, positionOffsetPixels);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!isScrollLocked()) {
            if (mVelocityTracker == null) {
                mVelocityTracker = VelocityTracker.obtain();
            }
            mVelocityTracker.addMovement(ev);
        }
        int x = (int) ev.getX();
        int y = (int) ev.getY();
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (!mScroller.isFinished() && !isScrollLocked()) {
                    mScroller.abortAnimation();
                }
                mLastX = mLastDownX = x;
                mLastDownY = y;
                mIsTouching = true;
                mPager.recordBeforeScroll();
                break;
            case MotionEvent.ACTION_MOVE:
                if (!isScrollLocked()) {
                    int dx = mLastX - x;
                    if (isCurrentScrollAllowed(mPager.mCurrentPosition, dx > 0)) {
                        scrollBy(dx, 0);
                    }
                }
                if (Math.abs(mLastX - x) > Math.abs(mLastDownY - y)) {
                    requestDisallowInterceptTouchEvent(true);
                }
                mLastX = x;
                break;
            case MotionEvent.ACTION_UP:
                if (!isScrollLocked()) {
                    mVelocityTracker.computeCurrentVelocity(1000);
                    int initVelocity = (int) mVelocityTracker.getXVelocity();
                    if (initVelocity > mMaxVelocity) {
                        // 快速的向右滑，显示前一页
                        goPreviousPage();
                    } else if (initVelocity < -mMaxVelocity) {
                        // 快速的向左滑，显示后一页
                        goNextPage();
                    } else {
                        // 慢慢的滑动
                        slowScrollToPage();
                    }
                    if (mVelocityTracker != null) {
                        mVelocityTracker.recycle();
                        mVelocityTracker = null;
                    }
                }
                handleClick(ev);
            default:
                mIsTouching = false;
                mLastTouchTime = System.currentTimeMillis();
                break;
        }
        return true;
    }

    private void handleClick(MotionEvent ev) {
        int upX = (int) ev.getX();
        int upY = (int) ev.getY();
        if (Math.abs(mLastDownX - upX) < 20
                && Math.abs(mLastDownY - upY) < 20
                && !performClick()) {
            if (upX < getWidth() * mOutSidePercent) {
                goPreviousPage();
            } else if (upX > getWidth() * (1 - mOutSidePercent)) {
                goNextPage();
            } else if (mOnItemClickListener != null && getItemCount() > 0) {
                int periodLength = getPeriodLength(getWidth());
                int page = getScrollX() / periodLength;
                if (getScrollX() % periodLength > periodLength / 2) {
                    page++;
                } else if (getScrollX() % periodLength < -periodLength / 2){
                    page--;
                }
                mOnItemClickListener.click(this, getLoopIndex(page));
            }
        }
    }

    private void slowScrollToPage() {
        int periodLength = getPeriodLength(getWidth());
        int whichPage = (getScrollX() + periodLength / 2) / periodLength;
        scrollToPage(whichPage);
    }

    private int getPeriodLength(int width) {
        return width - 2 * (int) (width * mOutSidePercent) + mDividerWidth;
    }

    private void scrollToPage(int indexPage) {
        mPager.recordBeforeScroll();
        int periodLength = getPeriodLength(getWidth());
        if (mPager.mCurrentPosition != indexPage) {
            long currentTime = System.currentTimeMillis();
            // 在一次scroll完成后加一点时间的缓冲，切换太快会有视觉异常
            // scroll坐标偏移异常，则返回原坐标
            if (Math.abs(getScrollX() - mPager.mCurrentPosition * periodLength) <= 2 * periodLength
                    && currentTime - mLastScrollTime > 300) {
                mPager.updateCurrentPosition(indexPage);
                mLastScrollTime = currentTime;
                if (!mOnPageChangeListeners.isEmpty()) {
                    for (OnPageChangeListener onPageChangeListener : mOnPageChangeListeners) {
                        onPageChangeListener.onPageSelected(getLoopIndex(indexPage));
                    }
                }
            }
        }
        int dx = mPager.mCurrentPosition * periodLength - getScrollX();
        if (Math.abs(dx) < periodLength * 3 / 2) {
            mScroller.startScroll(getScrollX(), 0, dx, 0, Math.abs(dx));
            invalidate();
        } else {
            scrollTo(mPager.mCurrentPosition * periodLength, 0);
        }
        requestLayout();
    }

    public void onPageScrolledInternal(int position, float positionOffset, int positionOffsetPixels) {
        if (!mOnPageChangeListeners.isEmpty()) {
            for (OnPageChangeListener onPageChangeListener : mOnPageChangeListeners) {
                onPageChangeListener.onPageScrolled(getLoopIndex(position), positionOffset, positionOffsetPixels);
            }
        }
        if (mPageTransformer != null) {
            transform(position, 1f - positionOffset);
            transform(position + 1, positionOffset);
            if (getDepthWithCache() == 2){
                transform(position - 1, 0);
                transform(position + 2, 0);
            }
        }
    }

    private void transform(int position, float transformPos) {
        View child = getViewOnPosition(position);
        if (child != null && getWidth() > 0) {
            mPageTransformer.transformPage(child, transformPos);
        }
    }

    public boolean isReverse() {
        return mIsReverse;
    }

    public void setReverse(boolean reverse) {
        mIsReverse = reverse;
    }

    private boolean isScrollLocked() {
        return mIsScrollLocked || getItemCount() <= 1;
    }

    /**
     * 是否禁止触摸拖动
     * @param locked
     */
    public void setScrollLocked(boolean locked) {
        mIsScrollLocked = locked;
    }

    /**
     * scroll到下一页
     */
    public void goNextPage() {
        if (isCurrentScrollAllowed(mPager.mCurrentPosition, true)) {
            scrollToPage(mPager.mCurrentPosition + 1);
        }
    }

    /**
     * scroll到前一页
     */
    public void goPreviousPage() {
        if (isCurrentScrollAllowed(mPager.mCurrentPosition, false)) {
            scrollToPage(mPager.mCurrentPosition - 1);
        }
    }

    /**
     * 自动循环播放
     */
    public void startLoop() {
        mIsLoopingStart = true;
        dispose();
        if (getItemCount() > 1) {
            loop();
        }
    }

    /**
     * 取消循环播放
     */
    public void cancelLoop() {
        mIsLoopingStart = false;
        dispose();
    }

    /**
     * @return true:相对于上一次静止的位置，目前的位置是向右划的过程中
     */
    public boolean isScrollPrevious() {
        return getScrollX() < getPeriodLength(getWidth()) * mPager.mPositionLastScroll;
    }

    /**
     * @return true:相对于上一次静止的位置，目前的位置是向左划的过程中
     */
    public boolean isScrollNext() {
        return getScrollX() > getPeriodLength(getWidth()) * mPager.mPositionLastScroll;
    }

    private boolean isCurrentScrollAllowed(int position, boolean forward) {
        return mScrollIntercepter == null || !mScrollIntercepter.onScrollIntercept(position, forward);
    }

    private View getViewOnPosition(int position) {
        int index = getLoopIndex(getChildCount(), position);
        return mViewsMap.get(index);
    }

    private void loop(){
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!mIsTouching && System.currentTimeMillis() - mLastTouchTime > mLoopInterval / 2) {
                    if (mIsReverse) {
                        goPreviousPage();
                    } else {
                        goNextPage();
                    }
                }
                loop();
            }
        }, mLoopInterval);
    }

    private void dispose() {
        mHandler.removeCallbacksAndMessages(null);
    }

    private void reset() {
        relayoutAllViews();
        mPager.reset();
        scrollToPage(mPager.mCurrentPosition);
        startLoopIfNeeded();
        post(new Runnable() {
            @Override
            public void run() {
                onPageScrolledInternal(mPager.mCurrentPosition, 0, 0);
            }
        });
    }

    private void startLoopIfNeeded() {
        if (getItemCount() > 1 && mIsLoopingStart) {
            startLoop();
        }
    }

    private void relayoutAllViews() {
        removeAllViewsInLayout();
        int count = getItemCount();
        for (int i = 0; i <= getDepthWithCache() * 2; i++) {
            View view = mViewsMap.get(i);
            if (view == null) {
                view = mAdapter.makeView(getContext(), i);
                mViewsMap.put(i, view);
            }
            super.addView(view);
        }
    }

    private int getDepthWithCache() {
        return mOutsideDepth;
    }

    private int getLoopIndex(int index) {
        return getLoopIndex(getItemCount(), index);
    }

    private int getLoopIndex(int totalCount, int index) {
        if (totalCount == 0){
            return -1;
        }
        return index >= 0 ? index % totalCount : totalCount + (index + 1) % totalCount - 1;
    }

    public AbsBannerAdapter getAdapter() {
        return mAdapter;
    }

    public void setAdapter(AbsBannerAdapter adapter) {
        if (mAdapter != null) {
            mAdapter.unregisterDataSetObserver(mDataObserver);
        }
        mAdapter = adapter;
        if (mIsAttachToWindow) {
            mAdapter.registerDataSetObserver(mDataObserver);
        }
        reset();
    }

    public ScrollIntercepter getScrollIntercepter() {
        return mScrollIntercepter;
    }

    public void setScrollIntercepter(ScrollIntercepter scrollIntercepter) {
        mScrollIntercepter = scrollIntercepter;
    }

    public void setPageTransformer(PageTransformer pageTransformer) {
        mPageTransformer = pageTransformer;
    }

    public OnItemClickListener getOnItemClickListener() {
        return mOnItemClickListener;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }

    public void addOnPageChangeListener(@NonNull OnPageChangeListener onPageChangeListener) {
        mOnPageChangeListeners.add(onPageChangeListener);
    }

    public void removeOnPageChangeListener(OnPageChangeListener onPageChangeListener) {
        mOnPageChangeListeners.remove(onPageChangeListener);
    }

    public int getDividerWidth() {
        return mDividerWidth;
    }

    public void setDividerWidth(int dividerWidth) {
        mDividerWidth = dividerWidth;
    }

    public float getOutSidePercent() {
        return mOutSidePercent;
    }

    public void setOutSidePercent(@FloatRange(from=0.0f, to=0.33f) float outSidePercent) {
        mOutSidePercent = outSidePercent;
    }

    public long getLoopInterval() {
        return mLoopInterval;
    }

    public void setLoopInterval(long loopInterval) {
        mLoopInterval = loopInterval;
    }

    private int getItemCount() {
        return mAdapter == null ? 0 : mAdapter.getCount();
    }

    private DataSetObserver mDataObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            reset();
        }

        @Override
        public void onInvalidated() {
            requestLayout();
        }
    };

    public static interface OnItemClickListener {
        void click(InfiniteBannerView view, int position);
    }

    public static interface OnPageChangeListener {
        void onPageScrolled(int position, float positionOffset, int positionOffsetPixels);
        void onPageSelected(int position);
    }

    public static interface ScrollIntercepter {
        /**
         * @param position 当前的位置
         * @param forward true：正向运动；false：反向运动
         * @return true：拦截
         */
        boolean onScrollIntercept(int position, boolean forward);
    }

    public interface PageTransformer {
        void transformPage(@NonNull View view, float arg);
    }

    private static class Pager {
        private int mInitialPosition = 0, mCurrentPosition, mPositionLastScroll;
        private int mPositionLastRelayout = Integer.MIN_VALUE / 2;

        private void initPosition(int position) {
            mInitialPosition = position;
            mCurrentPosition = mInitialPosition;
        }

        private void reset() {
            mPositionLastRelayout = Integer.MIN_VALUE / 2;
            mCurrentPosition = mInitialPosition;
        }

        private void updateCurrentPosition(int currentPosition) {
            mCurrentPosition = currentPosition;
        }

        private void recordBeforeScroll() {
            mPositionLastScroll = mCurrentPosition;
        }

        private void recordWhenRelayout() {
            mPositionLastRelayout = mCurrentPosition;
        }

        private boolean isSkippingPosition() {
            return Math.abs(mPositionLastRelayout - mCurrentPosition) > 1;
        }

        private boolean isNextPosition() {
            return mCurrentPosition - mPositionLastRelayout == 1;
        }

        private boolean isPreviousPosition() {
            return mCurrentPosition - mPositionLastRelayout == -1;
        }
    }
}
