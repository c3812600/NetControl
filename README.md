# NetControl

NetControl 是一款基于 Android 原生开发的轻量级局域网网页展示终端：启动后全屏无边框显示指定网页，并在后台运行本地 HTTP 服务，供局域网内的“中控主机/服务端”查询设备状态与下发 URL 切换指令。

## 功能特性

- 全屏沉浸式 WebView 展示网页（适配不同分辨率）
- 本地 HTTP 监听服务（默认端口 8080）
- 断电/重启后自动恢复上一次下发的 URL（SharedPreferences 持久化）
- 支持需要登录弹窗的网页（HTTP Basic Auth）
- 支持首次主文档请求附带自定义请求头（headers）

## 环境要求

- Android：minSdk 23（Android 6.0+）
- JDK：建议 17（本项目本机打包使用 JDK 17）
- Android Studio / Android SDK（可用命令行 Gradle 构建）

## 构建与安装

### Debug 包

PowerShell（Windows）示例：

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
.\gradlew assembleDebug
```

生成路径：

- `app/build/outputs/apk/debug/app-debug.apk`

### Release 包（可选）

Release 签名通过环境变量或 Gradle 属性注入（仓库默认忽略 keystore/签名配置文件）：

- `RELEASE_STORE_FILE`
- `RELEASE_STORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

示例（PowerShell）：

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
$env:RELEASE_STORE_FILE = "C:\path\to\your.jks"
$env:RELEASE_STORE_PASSWORD = "******"
$env:RELEASE_KEY_ALIAS = "your_alias"
$env:RELEASE_KEY_PASSWORD = "******"
.\gradlew assembleRelease
```

生成路径：

- `app/build/outputs/apk/release/app-release.apk`

## 本地 HTTP API（给服务端/中控调用）

设备在局域网内固定监听：

- `http://<设备IP>:8080`

### 1) 设备在线查询：GET /api/status

请求：

- `GET http://<设备IP>:8080/api/status`

返回（200）示例：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "device_name": "NetControl_Terminal",
    "device_ip": "192.168.1.105",
    "current_url": "http://192.168.1.100/display1.html",
    "screen_resolution": "1920x1080",
    "is_fullscreen": true
  }
}
```

### 2) 下发网页切换：POST /api/set_url

请求：

- `POST http://<设备IP>:8080/api/set_url`
- `Content-Type: application/json`

Body 示例（最简）：

```json
{
  "url": "http://192.168.1.100/new_dashboard.html"
}
```

注意：

- `url` 支持 `http://` / `https://`
- 服务端侧如果会额外包裹引号/反引号，App 端会做首尾清理（trim + 去除外层引号/反引号）

#### 可选：Basic Auth（处理网页弹窗账号密码）

```json
{
  "url": "http://192.168.18.119:1880/static/",
  "basic_user": "admin",
  "basic_pass": "admin"
}
```

#### 可选：自定义请求头（仅对首次主文档请求生效）

```json
{
  "url": "http://192.168.1.100/new_dashboard.html",
  "headers": {
    "X-Auth-Token": "xxxx",
    "Cookie": "a=b; c=d"
  }
}
```

返回（200）示例：

```json
{
  "code": 200,
  "msg": "Command received, loading URL...",
  "data": null
}
```

### curl 调用示例（Windows）

PowerShell 中建议使用 `curl.exe`：

```powershell
curl.exe "http://192.168.1.105:8080/api/status"
curl.exe -X POST "http://192.168.1.105:8080/api/set_url" -H "Content-Type: application/json" -d "{\"url\":\"http://192.168.1.100/new_dashboard.html\"}"
```

## 运行逻辑（简述）

- App 启动：进入全屏沉浸式 WebView
- 若本地无历史 URL：显示“等待主机下发指令…”
- 启动本地 HTTP 服务监听 8080
- 收到 `/api/set_url`：保存 URL 并在 UI 线程加载，重启后自动恢复

## 图标

- 根目录 `ico.png` 用于生成 launcher 图标资源（不同密度 mipmap + 自适应前景图）

## 相关实现位置（源码入口）

- WebView 全屏与持久化逻辑：`app/src/main/java/com/example/netcontrol/MainActivity.java`
- 本地 HTTP 服务与 API：`app/src/main/java/com/example/netcontrol/LocalHttpServer.java`
- Android 配置：`app/src/main/AndroidManifest.xml`
