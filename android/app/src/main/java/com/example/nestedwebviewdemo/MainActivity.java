package com.example.nestedwebviewdemo;

import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private NestedScrollWebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webview);
        configureWebView(webView);
        loadDemoContent();
    }

    private void configureWebView(@NonNull NestedScrollWebView webView) {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
    }

    private void loadDemoContent() {
        StringBuilder html = new StringBuilder();
        html.append("<html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"></head><body>");
        html.append("<h2>下半部分为 WebView，内容较长</h2>");
        html.append("<p>向上滑动：先由外层折叠原生视图，WebView 吸顶后开始滚动其内容。</p>");
        for (int i = 1; i <= 80; i++) {
            html.append("<p>段落 ").append(i).append(": 这是示例文本，用于制造长页面效果。滚动联动应在此处生效。</p>");
        }
        html.append("</body></html>");
        webView.loadDataWithBaseURL(null, html.toString(), "text/html", "UTF-8", null);
    }
}