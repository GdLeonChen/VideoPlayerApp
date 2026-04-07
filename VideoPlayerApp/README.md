# 📱 视频播放器 Android App — 打包指南

## 目录结构

```
VideoPlayerApp/
├── app/
│   ├── src/main/
│   │   ├── assets/
│   │   │   └── index.html          ← 播放器主页面（已内嵌 CSS + JS）
│   │   ├── java/com/videoplayer/app/
│   │   │   └── MainActivity.java   ← 原生 WebView 壳
│   │   ├── res/
│   │   │   ├── layout/activity_main.xml
│   │   │   ├── values/styles.xml   ← 全屏黑色主题
│   │   │   └── xml/file_paths.xml  ← FileProvider 路径配置
│   │   └── AndroidManifest.xml     ← 权限声明
│   ├── build.gradle
│   └── proguard-rules.pro
├── build.gradle
├── settings.gradle
└── gradle.properties
```

---

## 🚀 快速打包（5 分钟）

### 第一步：安装 Android Studio

下载地址：https://developer.android.com/studio

安装时选择默认选项，会自动安装 JDK + Android SDK。

### 第二步：打开项目

1. 启动 Android Studio
2. 点击 **Open** → 选择 `VideoPlayerApp` 文件夹
3. 等待 Gradle 同步完成（首次需下载依赖，约 5~10 分钟，需要网络）

### 第三步：构建 APK

菜单栏 → **Build** → **Build Bundle(s) / APK(s)** → **Build APK(s)**

构建完成后，右下角会弹出提示，点击 **locate** 找到 APK 文件：

```
app/build/outputs/apk/debug/app-debug.apk
```

### 第四步：安装到手机

- 用 USB 数据线连接手机，开启 USB 调试，直接从 Android Studio 点击 ▶ Run
- 或者将 `app-debug.apk` 复制到手机，用文件管理器安装（需开启"允许安装未知来源应用"）

---

## ✅ App 功能说明

| 功能 | 说明 |
|------|------|
| 📁 本地文件夹 | 选择手机存储中的视频文件夹，自动扫描并播放 |
| 🌐 局域网/NAS | 输入 HTTP 视频直链，支持多个 URL 逐行输入 |
| 👆 上划切换 | 手指向上滑动 → 下一个视频（TikTok 逻辑） |
| 👇 下划切换 | 手指向下滑动 → 上一个视频 |
| 🔇/🔊 静音切换 | 初始静音（保证自动播放），点击视频取消静音 |
| 📊 进度条 | 点击进度条任意位置跳转 |
| 🎬 播放结束 | 自动播放下一个 |
| 📱 全屏 | 竖屏全屏，无状态栏 |

---

## 🔧 支持的视频格式

| 格式 | 支持 |
|------|------|
| MP4 (H.264/H.265) | ✅ 最佳 |
| MKV | ✅ |
| MOV | ✅ |
| WebM (VP8/VP9) | ✅ |
| 3GP | ✅ |
| M3U8 (HLS 流) | ✅ |
| AVI / WMV / FLV | ❌ 移动端 WebView 不支持 |

---

## 🌐 NAS / 局域网使用方法

### Jellyfin / Emby
- 在 Jellyfin Web 界面找到视频，右键复制直链地址
- 格式：`http://192.168.1.100:8096/Videos/xxx/stream.mp4?api_key=xxx`

### HTTP 文件服务器（推荐）
- 在 NAS 上开启 HTTP 文件浏览服务
- 格式：`http://192.168.1.100:8080/movies/film.mp4`

### 群晖 NAS
- 开启 Web Station 或 HTTP 共享
- 直接输入 SMB 路径的 HTTP 等价地址

---

## ⚙️ 常见问题

**Q: Gradle 同步失败？**
A: 检查网络，或在 `gradle.properties` 中配置代理。

**Q: 安装时提示"解析包时出现问题"？**
A: 确认手机 Android 版本 ≥ 5.0 (API 21)。

**Q: 视频播放没有声音？**
A: 首次加载为静音模式（保证自动播放），点击视频画面即可解除静音。

**Q: 无法访问局域网视频？**
A: 确保手机和 NAS 在同一 Wi-Fi 网络，检查 NAS 防火墙设置。

**Q: 本地视频选不到？**
A: App 启动时会申请存储权限，在手机"设置 → 应用 → 视频播放器 → 权限"中手动授权。
