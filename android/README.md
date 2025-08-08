# NestedWebViewDemo

一个演示“顶部原生 View 折叠后，将滚动权移交给 WebView”的最小 Android 示例。

- CoordinatorLayout + AppBarLayout 作为父容器
- 顶部是原生 View（可折叠）
- 底部是 WebView，初始不滚动，随父容器上移；当 WebView 顶到屏幕顶部且 AppBar 完全折叠后，WebView 开始滚动

打开方式：
1. 用 Android Studio 打开 `android` 目录
2. 同步 Gradle 后直接运行 `app`

关键点：
- `AppBarLayout` 使用 `scroll|exitUntilCollapsed`，优先消费向上滚动以折叠顶部原生区块
- `WebView` 设置 `app:layout_behavior="@string/appbar_scrolling_view_behavior"`
- 在 `MainActivity` 中通过 `AppBarLayout.OnOffsetChangedListener` + `WebView.setOnTouchListener` 精细控制事件拦截，实现滚动权在父容器与 WebView 之间的顺畅切换