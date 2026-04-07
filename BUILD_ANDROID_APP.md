# 安卓APP打包指南

## 方法一：使用Cordova

### 步骤1：安装必要工具
1. 安装Node.js (https://nodejs.org/)
2. 安装Cordova：`npm install -g cordova`
3. 安装Android SDK (https://developer.android.com/studio)

### 步骤2：创建Cordova项目
```bash
# 创建项目
cordova create VideoPlayer com.example.videoplayer VideoPlayer

# 进入项目目录
cd VideoPlayer

# 添加安卓平台
cordova platform add android
```

### 步骤3：替换项目文件
1. 将以下文件复制到 `www` 目录：
   - video_player.html → index.html
   - style.css
   - script.js

2. 修改 `index.html` 中的引用路径（如果需要）

### 步骤4：配置权限
在 `config.xml` 文件中添加必要的权限：
```xml
<platform name="android">
    <config-file parent="/*" target="AndroidManifest.xml">
        <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
        <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
        <uses-permission android:name="android.permission.INTERNET" />
        <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    </config-file>
</platform>
```

### 步骤5：构建APK
```bash
# 构建调试版APK
cordova build android

# 构建发布版APK
cordova build android --release
```

构建完成后，APK文件会在 `platforms/android/app/build/outputs/apk/` 目录中。

## 方法二：使用PhoneGap Build

1. 访问 https://build.phonegap.com/
2. 创建一个新应用
3. 上传包含以下文件的ZIP包：
   - index.html (原video_player.html)
   - style.css
   - script.js
   - config.xml

4. 等待构建完成，下载APK文件

## 方法三：使用Android Studio创建WebView应用

### 步骤1：创建新项目
1. 打开Android Studio
2. 创建一个新的Empty Activity项目

### 步骤2：修改布局文件
在 `activity_main.xml` 中添加WebView：
```xml
<WebView
    android:id="@+id/webview"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
/>
```

### 步骤3：修改MainActivity.java
```java
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webview);
        webView.setWebViewClient(new WebViewClient());

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);

        // 加载本地HTML文件
        webView.loadUrl("file:///android_asset/index.html");
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
```

### 步骤4：添加文件
将以下文件复制到 `app/src/main/assets/` 目录：
- index.html (原video_player.html)
- style.css
- script.js

### 步骤5：添加权限
在 `AndroidManifest.xml` 中添加权限：
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

### 步骤6：构建APK
1. 点击 "Build" → "Build Bundle(s) / APK(s)" → "Build APK(s)"
2. 构建完成后，APK文件会在 `app/build/outputs/apk/debug/` 目录中

## 注意事项

1. **权限处理**：
   - Android 6.0+ 需要运行时权限请求
   - 对于Android 10+，需要在 `AndroidManifest.xml` 中添加 `android:requestLegacyExternalStorage="true"`

2. **文件访问**：
   - 本地文件访问需要适当的权限
   - 局域网文件访问需要确保设备在同一网络中

3. **性能优化**：
   - 对于低性能设备，建议限制视频分辨率
   - 可以考虑添加硬件加速配置

4. **兼容性**：
   - 测试不同Android版本和设备
   - 确保WebView版本支持所需的HTML5特性

## 功能说明

- **自动播放**：打开APP后自动开始播放视频
- **下滑切换**：在视频区域下滑手指切换到下一个视频
- **本地文件**：通过文件选择器选择本地视频文件夹
- **局域网文件**：支持输入局域网视频路径（如NAS服务器）
- **响应式设计**：适配各种手机屏幕尺寸
