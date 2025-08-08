package com.example.nestedwebviewdemo;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.NestedScrollingChild2;
import androidx.core.view.NestedScrollingChildHelper;
import androidx.core.view.ViewCompat;

public class NestedScrollWebView extends WebView implements NestedScrollingChild2 {

    private final NestedScrollingChildHelper nestedScrollingChildHelper;

    private int lastTouchY;
    private int nestedOffsetY;
    private final int[] scrollConsumed = new int[2];
    private final int[] scrollOffset = new int[2];

    private VelocityTracker velocityTracker;
    private int activePointerId = MotionEvent.INVALID_POINTER_ID;
    private final int maximumFlingVelocity;

    private volatile boolean allowScroll = false;

    public NestedScrollWebView(@NonNull Context context) {
        this(context, null);
    }

    public NestedScrollWebView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, android.R.attr.webViewStyle);
    }

    public NestedScrollWebView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        nestedScrollingChildHelper = new NestedScrollingChildHelper(this);
        setNestedScrollingEnabled(true);
        setOverScrollMode(OVER_SCROLL_NEVER);
        ViewConfiguration vc = ViewConfiguration.get(context);
        maximumFlingVelocity = vc.getScaledMaximumFlingVelocity();
    }

    public void setAllowScroll(boolean allow) {
        this.allowScroll = allow;
        if (!allow) {
            // 确保内容不会意外保留滚动位置
            scrollTo(getScrollX(), 0);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        MotionEvent tracked = MotionEvent.obtain(event);
        final int action = event.getActionMasked();

        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }
        velocityTracker.addMovement(event);

        if (action == MotionEvent.ACTION_DOWN) {
            nestedOffsetY = 0;
            activePointerId = event.getPointerId(0);
            lastTouchY = (int) event.getY();
            startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH);
            // 仅建立手势，不在未允许时产生滚动
            super.onTouchEvent(tracked);
            tracked.recycle();
            return true;
        }

        if (action == MotionEvent.ACTION_MOVE) {
            int index = event.findPointerIndex(activePointerId);
            if (index < 0) index = 0;
            int y = (int) event.getY(index);
            int dy = lastTouchY - y; // up positive

            // 父容器预消费（用于折叠/展开 AppBar）
            if (dispatchNestedPreScroll(0, dy, scrollConsumed, scrollOffset, ViewCompat.TYPE_TOUCH)) {
                dy -= scrollConsumed[1];
                tracked.offsetLocation(0, scrollOffset[1]);
                nestedOffsetY += scrollOffset[1];
            }

            int scrolledByY = 0;
            if (allowScroll) {
                int before = getScrollY();
                super.onTouchEvent(tracked);
                scrolledByY = getScrollY() - before;
            }

            int unconsumedY = dy - scrolledByY;
            dispatchNestedScroll(0, scrolledByY, 0, unconsumedY, scrollOffset, ViewCompat.TYPE_TOUCH);

            lastTouchY = y - scrollOffset[1];
            tracked.recycle();
            return true;
        }

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            // 处理 fling：未允许滚动时，优先交给父容器
            velocityTracker.computeCurrentVelocity(1000, maximumFlingVelocity);
            float vY = velocityTracker.getYVelocity(activePointerId);
            float velocityY = -vY; // up positive

            if (!allowScroll) {
                boolean parentConsumed = dispatchNestedPreFling(0, velocityY);
                dispatchNestedFling(0, velocityY, parentConsumed);
                // 不把 fling 交给 WebView
            } else {
                // 允许滚动时，先询问父容器预消费，再交给 WebView 自己处理
                dispatchNestedPreFling(0, velocityY);
                super.onTouchEvent(tracked);
                dispatchNestedFling(0, velocityY, true);
            }

            stopNestedScroll(ViewCompat.TYPE_TOUCH);
            activePointerId = MotionEvent.INVALID_POINTER_ID;
            if (velocityTracker != null) {
                velocityTracker.recycle();
                velocityTracker = null;
            }
            tracked.recycle();
            return true;
        }

        boolean result = super.onTouchEvent(tracked);
        tracked.recycle();
        return result;
    }

    // NestedScrollingChild2 implementation
    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        nestedScrollingChildHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return nestedScrollingChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes, int type) {
        return nestedScrollingChildHelper.startNestedScroll(axes, type);
    }

    @Override
    public void stopNestedScroll(int type) {
        nestedScrollingChildHelper.stopNestedScroll(type);
    }

    @Override
    public boolean hasNestedScrollingParent(int type) {
        return nestedScrollingChildHelper.hasNestedScrollingParent(type);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow, int type) {
        return nestedScrollingChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type);
    }

    @Override
    public void dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed,
                                     int[] offsetInWindow, int type) {
        nestedScrollingChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
                offsetInWindow, type);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return nestedScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return nestedScrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }
}