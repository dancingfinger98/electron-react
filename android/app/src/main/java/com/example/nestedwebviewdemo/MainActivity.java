package com.example.nestedwebviewdemo;

import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.AppBarLayout;

public class MainActivity extends AppCompatActivity {

    private NestedScrollWebView webView;
    private AppBarLayout appBarLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webview);
        appBarLayout = findViewById(R.id.app_bar);
        configureWebView(webView);
        hookScrollHandoff();
        loadDemoContent();
    }

    private void configureWebView(@NonNull NestedScrollWebView webView) {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.setAllowScroll(false);
    }

    private void hookScrollHandoff() {
        appBarLayout.addOnOffsetChangedListener((appBar, verticalOffset) -> {
            int total = appBar.getTotalScrollRange();
            boolean collapsed = total != 0 && Math.abs(verticalOffset) >= total;
            webView.setAllowScroll(collapsed);
        });
    }

    private void loadDemoContent() {
        StringBuilder html = new StringBuilder();
        html.append("<html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"></head><body>");
        html.append("<h2>下半部分为 WebView，内容较长</h2>");
        html.append("<p>开始时 WebView 不滚；当上方原生视图完全滑出（吸顶）后，WebView 才开始滚动内容。</p>");
        for (int i = 1; i <= 120; i++) {
            html.append("<p>段落 ").append(i).append(": 长页面内容用于测试联动。</p>");
        }
        html.append("</body></html>");
        webView.loadDataWithBaseURL(null, html.toString(), "text/html", "UTF-8", null);
    }
}