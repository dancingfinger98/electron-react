package com.example.nestedwebviewdemo;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.view.ViewParent;
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
    private final int touchSlop;
    private boolean isDragging = false;

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
        touchSlop = vc.getScaledTouchSlop();
    }

    public void setAllowScroll(boolean allow) {
        this.allowScroll = allow;
        if (!allow) {
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
            isDragging = false;
            startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH);
            super.onTouchEvent(tracked);
            tracked.recycle();
            return true;
        }

        if (action == MotionEvent.ACTION_MOVE) {
            int index = event.findPointerIndex(activePointerId);
            if (index < 0) index = 0;
            int y = (int) event.getY(index);
            int dyRaw = lastTouchY - y; // up positive

            if (!isDragging && Math.abs(dyRaw) > touchSlop) {
                isDragging = true;
            }
            if (!isDragging) {
                tracked.recycle();
                return true;
            }

            int dy = dyRaw;

            if (dispatchNestedPreScroll(0, dy, scrollConsumed, scrollOffset, ViewCompat.TYPE_TOUCH)) {
                dy -= scrollConsumed[1];
                tracked.offsetLocation(0, scrollOffset[1]);
                nestedOffsetY += scrollOffset[1];
            }

            // 当允许 WebView 滚动时，避免父容器抢夺事件；反之允许父容器拦截
            ViewParent parent = getParent();
            if (parent != null) {
                boolean atTop = !canScrollVertically(-1);
                boolean scrollingDown = dy < 0; // finger moves down, content tries to scroll down
                boolean disallow = allowScroll && !(scrollingDown && atTop);
                parent.requestDisallowInterceptTouchEvent(disallow);
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
            velocityTracker.computeCurrentVelocity(1000, maximumFlingVelocity);
            float vY = velocityTracker.getYVelocity(activePointerId);
            float velocityY = -vY; // up positive

            if (!allowScroll) {
                boolean parentConsumed = dispatchNestedPreFling(0, velocityY);
                dispatchNestedFling(0, velocityY, parentConsumed);
            } else {
                dispatchNestedPreFling(0, velocityY);
                super.onTouchEvent(tracked);
                dispatchNestedFling(0, velocityY, true);
            }

            stopNestedScroll(ViewCompat.TYPE_TOUCH);
            activePointerId = MotionEvent.INVALID_POINTER_ID;
            isDragging = false;
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