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
    }

    public void setAllowScroll(boolean allow) {
        this.allowScroll = allow;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean handled = true;
        MotionEvent trackedEvent = MotionEvent.obtain(event);
        final int action = event.getActionMasked();

        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }
        velocityTracker.addMovement(event);

        if (action == MotionEvent.ACTION_DOWN) {
            nestedOffsetY = 0;
            lastTouchY = (int) event.getY();
            startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH);
            // 让 WebView 接管手势（以便中途在吸顶瞬间无缝切换到自身滚动）
            handled = super.onTouchEvent(trackedEvent);
        } else if (action == MotionEvent.ACTION_MOVE) {
            int y = (int) event.getY();
            int dy = lastTouchY - y; // up is positive

            // 父容器预消费（用于折叠/展开 AppBar）
            if (dispatchNestedPreScroll(0, dy, scrollConsumed, scrollOffset, ViewCompat.TYPE_TOUCH)) {
                dy -= scrollConsumed[1];
                trackedEvent.offsetLocation(0, scrollOffset[1]);
                nestedOffsetY += scrollOffset[1];
            }

            int scrolledByY = 0;
            if (allowScroll) {
                int oldY = getScrollY();
                super.onTouchEvent(trackedEvent);
                scrolledByY = getScrollY() - oldY;
            }

            int unconsumedY = dy - scrolledByY;
            // 将未消费部分交还父容器
            dispatchNestedScroll(0, scrolledByY, 0, unconsumedY, scrollOffset, ViewCompat.TYPE_TOUCH);

            lastTouchY = y - scrollOffset[1];
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            super.onTouchEvent(trackedEvent);
            stopNestedScroll(ViewCompat.TYPE_TOUCH);
            if (velocityTracker != null) {
                velocityTracker.recycle();
                velocityTracker = null;
            }
        } else {
            handled = super.onTouchEvent(trackedEvent);
        }

        trackedEvent.recycle();
        return handled;
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