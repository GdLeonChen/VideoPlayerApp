package com.videoplayer.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 视频播放器 MainActivity
 * - WebView 全屏壳，加载 assets/index.html
 * - 处理存储权限（Android 5 ~ 14 全版本兼容）
 * - JavaScript Bridge 供 HTML 感知原生环境
 * - 文件夹选择器（支持整个文件夹，批量加载视频）
 * - 持久化记忆功能（自动加载上次文件夹）
 */
public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private SharedPreferences prefs;

    // 文件夹选择 Uri（持久化）
    private Uri folderTreeUri;

    // 文件选择回调（WebChromeClient.onShowFileChooser 需要）
    private ValueCallback<Uri[]> filePathCallback;

    // 文件夹选择启动器（ACTION_OPEN_DOCUMENT_TREE）
    private final ActivityResultLauncher<Intent> folderPickerLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Uri treeUri = result.getData().getData();
                if (treeUri != null) {
                    // 持久化文件夹访问权限
                    getContentResolver().takePersistableUriPermission(
                        treeUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    );
                    folderTreeUri = treeUri;

                    // 保存到 SharedPreferences
                    prefs.edit()
                        .putString("last_folder_uri", treeUri.toString())
                        .apply();

                    // 扫描文件夹内的所有视频文件
                    List<Uri> videoUris = scanFolderForVideos(treeUri);
                    if (!videoUris.isEmpty()) {
                        // 将视频列表传递给 WebView
                        loadVideoList(videoUris);
                    }
                }
            }
        });

    // 文件选择启动器（兼容旧版多选方式）
    private final ActivityResultLauncher<Intent> fileChooserLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (filePathCallback == null) return;
            Uri[] uris = null;
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Intent data = result.getData();
                if (data.getClipData() != null) {
                    // 多选
                    int count = data.getClipData().getItemCount();
                    uris = new Uri[count];
                    for (int i = 0; i < count; i++) {
                        uris[i] = data.getClipData().getItemAt(i).getUri();
                    }
                } else if (data.getData() != null) {
                    // 单选
                    uris = new Uri[]{data.getData()};
                }
            }
            filePathCallback.onReceiveValue(uris != null ? uris : new Uri[0]);
            filePathCallback = null;
        });

    // 权限申请启动器
    private final ActivityResultLauncher<String[]> permissionLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            // 权限结果：无论授权与否，页面交互本身已可用（HTML 文件选择器会再次触发系统权限）
        });

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 初始化 SharedPreferences
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // 全屏：使用 WindowCompat（兼容 API 21~34）
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
            controller.hide(WindowInsetsCompat.Type.systemBars());
            controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        } else {
            // 旧版全屏（API 21-29）
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
        // 硬件加速（视频播放必需）
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        // 防止截屏/录屏泄漏视频内容（可按需移除）
        // getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);

        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        setupWebView();
        requestNecessaryPermissions();

        // 加载本地 HTML
        webView.loadUrl("file:///android_asset/index.html");

        // 返回键：优先让 WebView 执行 history.back()
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        // 检查是否有保存的文件夹，自动加载
        loadLastFolderIfNeeded();
    }

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    private void setupWebView() {
        WebSettings settings = webView.getSettings();

        // JavaScript
        settings.setJavaScriptEnabled(true);

        // 文件访问（本地 assets 和外部存储）
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);

        // 媒体无需用户手势即可自动播放
        settings.setMediaPlaybackRequiresUserGesture(false);

        // 允许混合内容（HTTP 资源在 HTTPS 页面内——NAS 场景）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        // 缓存 & 存储
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);

        // 视口
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

        // 隐藏滚动条 & 防橡皮筋
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        // JavaScript Bridge
        webView.addJavascriptInterface(new AndroidBridge(), "AndroidBridge");

        // WebViewClient
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;  // 所有导航在 WebView 内处理
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // 页面加载完毕后注入 Android 版本信息
                view.evaluateJavascript(
                    "if(window.__player)window.__player._androidReady=true;", null);
            }
        });

        // WebChromeClient — 文件选择 + 媒体权限 + 全屏
        webView.setWebChromeClient(new WebChromeClient() {

            // ── 文件夹选择器（改进：支持整个文件夹）──
            @Override
            public boolean onShowFileChooser(WebView webView,
                                             ValueCallback<Uri[]> filePathCb,
                                             FileChooserParams params) {
                // 启动文件夹选择器
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                folderPickerLauncher.launch(intent);
                return true;
            }

            // ── 媒体权限（摄像头/麦克风等，此处仅授予视频相关）──
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    request.grant(request.getResources());
                }
            }

            // ── 视频全屏回调（部分视频播放器会触发）──
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    WindowInsetsControllerCompat ctrl =
                        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
                    ctrl.hide(WindowInsetsCompat.Type.systemBars());
                } else {
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                }
            }

            @Override
            public void onHideCustomView() {
                // 退出视频全屏后恢复沉浸
            }
        });
    }

    // ─── JavaScript Bridge ───────────────────────────────────────────
    private class AndroidBridge {

        /** 返回设备 Android SDK 版本（供 HTML 判断权限策略） */
        @JavascriptInterface
        public int getAndroidVersion() {
            return Build.VERSION.SDK_INT;
        }

        /** 是否已获得视频读取权限 */
        @JavascriptInterface
        public boolean hasStoragePermission() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                return ContextCompat.checkSelfPermission(
                    MainActivity.this, Manifest.permission.READ_MEDIA_VIDEO
                ) == PackageManager.PERMISSION_GRANTED;
            } else {
                return ContextCompat.checkSelfPermission(
                    MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED;
            }
        }

        /** 触发原生权限申请（供 HTML 按需调用） */
        @JavascriptInterface
        public void requestStoragePermission() {
            runOnUiThread(() -> requestNecessaryPermissions());
        }

        /** 返回应用版本名 */
        @JavascriptInterface
        public String getAppVersion() {
            try {
                return getPackageManager()
                    .getPackageInfo(getPackageName(), 0).versionName;
            } catch (Exception e) {
                return "1.0";
            }
        }

        /** 选择文件夹（供 JavaScript 调用） */
        @JavascriptInterface
        public void openFolderPicker() {
            runOnUiThread(() -> {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                folderPickerLauncher.launch(intent);
            });
        }
    }

    // ─── 权限申请 ────────────────────────────────────────────────────
    private void requestNecessaryPermissions() {
        List<String> perms = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ — 细粒度媒体权限
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO)
                    != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.READ_MEDIA_VIDEO);
            }
        } else {
            // Android 12 及以下
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

        if (!perms.isEmpty()) {
            permissionLauncher.launch(perms.toArray(new String[0]));
        }
    }

    // ─── 文件夹扫描 ─────────────────────────────────────────────────────
    /**
     * 扫描文件夹内的所有视频文件
     */
    private List<Uri> scanFolderForVideos(Uri treeUri) {
        List<Uri> videoUris = new ArrayList<>();
        DocumentFile tree = DocumentFile.fromTreeUri(this, treeUri);

        if (tree != null && tree.canRead()) {
            scanFolderRecursive(tree, videoUris);
        }

        return videoUris;
    }

    /**
     * 递归扫描文件夹
     */
    private void scanFolderRecursive(DocumentFile folder, List<Uri> videoUris) {
        DocumentFile[] files = folder.listFiles();

        // 支持的视频 MIME 类型
        String[] videoMimeTypes = {
            "video/mp4",
            "video/quicktime",
            "video/x-matroska",  // MKV
            "video/webm",
            "video/3gpp",
            "video/x-msvideo",    // AVI
            "video/x-ms-wmv"      // WMV
        };

        for (DocumentFile file : files) {
            if (file.isDirectory()) {
                // 递归扫描子文件夹
                scanFolderRecursive(file, videoUris);
            } else if (file.isFile()) {
                // 检查是否为视频文件
                String mimeType = file.getType();
                if (mimeType != null && isVideoMime(mimeType, videoMimeTypes)) {
                    videoUris.add(file.getUri());
                }
            }
        }
    }

    /**
     * 判断是否为视频 MIME 类型
     */
    private boolean isVideoMime(String mimeType, String[] videoMimeTypes) {
        for (String videoType : videoMimeTypes) {
            if (mimeType.equals(videoType)) {
                return true;
            }
        }
        // 或者直接以 video/ 开头
        return mimeType.startsWith("video/");
    }

    // ─── 视频列表传递 ─────────────────────────────────────────────────────
    /**
     * 将视频列表传递给 WebView JavaScript
     */
    private void loadVideoList(List<Uri> videoUris) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < videoUris.size(); i++) {
            if (i > 0) json.append(",");
            json.append("{\"url\":\"").append(videoUris.get(i).toString()).append("\"}");
        }
        json.append("]");

        final String jsonString = json.toString();

        runOnUiThread(() -> {
            if (webView != null) {
                webView.evaluateJavascript(
                    "if(window.__player)window.__player.loadVideoList(" + jsonString + ");",
                    null
                );
            }
        });
    }

    // ─── 记忆功能 ─────────────────────────────────────────────────────────
    /**
     * 启动时检查并加载上次打开的文件夹
     */
    private void loadLastFolderIfNeeded() {
        String savedUri = prefs.getString("last_folder_uri", null);
        if (savedUri != null) {
            try {
                Uri lastFolderUri = Uri.parse(savedUri);
                // 检查权限是否仍然有效
                if (getContentResolver().getPersistedUriPermissions().stream()
                    .anyMatch(p -> p.getUri().equals(lastFolderUri))) {
                    folderTreeUri = lastFolderUri;
                    // 延迟加载，等待 WebView 准备就绪
                    webView.postDelayed(() -> {
                        List<Uri> videoUris = scanFolderForVideos(lastFolderUri);
                        if (!videoUris.isEmpty()) {
                            loadVideoList(videoUris);
                        }
                    }, 1000);
                }
            } catch (Exception e) {
                // URI 解析失败，清除保存的记录
                prefs.edit().remove("last_folder_uri").apply();
            }
        }
    }

    // ─── 生命周期 ─────────────────────────────────────────────────────
    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
            webView.resumeTimers();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) {
            webView.onPause();
            webView.pauseTimers();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.clearHistory();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
