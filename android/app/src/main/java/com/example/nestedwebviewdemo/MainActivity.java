package com.example.nestedwebviewdemo;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.AppBarLayout;
import androidx.core.view.ViewCompat;

public class MainActivity extends AppCompatActivity {

    private AppBarLayout appBarLayout;
    private WebView webView;
    private boolean appBarCollapsed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        appBarLayout = findViewById(R.id.app_bar);
        webView = findViewById(R.id.webview);

        configureWebView(webView);
        setupScrollHandoff();
        loadDemoContent();
    }

    private void configureWebView(@NonNull WebView webView) {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        ViewCompat.setNestedScrollingEnabled(webView, true);
    }

    private void setupScrollHandoff() {
        appBarLayout.addOnOffsetChangedListener((appBar, verticalOffset) -> {
            int total = appBar.getTotalScrollRange();
            appBarCollapsed = total != 0 && Math.abs(verticalOffset) >= total;
        });

        webView.setOnTouchListener(new View.OnTouchListener() {
            int lastY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        lastY = (int) event.getY();
                        // 当 AppBar 未折叠，允许父容器拦截处理（由外层滑动）
                        v.getParent().requestDisallowInterceptTouchEvent(appBarCollapsed);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        int y = (int) event.getY();
                        int dy = y - lastY; // 手指下移为正（页面向下滚）
                        lastY = y;

                        boolean scrollingDown = dy > 0;
                        boolean canScrollUp = webView.canScrollVertically(-1); // 内容是否可向下滚（即是否已向上滚动过）

                        if (!appBarCollapsed) {
                            // 顶部未折叠，交给父容器（CoordinatorLayout/AppBarLayout）优先消费
                            v.getParent().requestDisallowInterceptTouchEvent(false);
                        } else if (scrollingDown && !canScrollUp) {
                            // WebView 已回到顶部且向下滑，交给父容器以展开 AppBar
                            v.getParent().requestDisallowInterceptTouchEvent(false);
                        } else {
                            // 其他情况交给 WebView 自己滚动
                            v.getParent().requestDisallowInterceptTouchEvent(true);
                        }
                        break;
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP:
                        // 结束手势，允许父容器按需拦截
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                        break;
                }
                // 返回 false 让 WebView 继续接收事件，是否真正滚动由上面的拦截开关决定
                return false;
            }
        });
    }

    private void loadDemoContent() {
        // 生成超出一屏的长内容，避免外网依赖
        StringBuilder html = new StringBuilder();
        html.append("<html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"></head><body>");
        html.append("<h2>下半部分为 WebView，内容较长</h2>");
        html.append("<p>向上滑动：先折叠上半部分原生视图，WebView 固定在顶部；折叠完毕后开始滚动 WebView 内容。</p>");
        for (int i = 1; i <= 80; i++) {
            html.append("<p>段落 ").append(i).append(": 这是示例文本，用于制造长页面效果。滚动联动应在此处生效。</p>");
        }
        html.append("</body></html>");
        webView.loadDataWithBaseURL(null, html.toString(), "text/html", "UTF-8", null);
    }
}