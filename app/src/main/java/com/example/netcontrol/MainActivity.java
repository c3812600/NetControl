package com.example.netcontrol;

import android.app.Activity;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Base64;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.HttpAuthHandler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {
    private WebView webView;
    private SharedPreferences prefs;
    private String currentUrl;
    private LocalHttpServer server;
    private volatile String lastBasicUser;
    private volatile String lastBasicPass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        webView = findViewById(R.id.webView);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
                String u = prefs.getString("basic_user_" + host, null);
                String p = prefs.getString("basic_pass_" + host, null);
                if (u == null || p == null) {
                    u = prefs.getString("basic_user_" + host + ":1880", null);
                    p = prefs.getString("basic_pass_" + host + ":1880", null);
                }
                if (u == null && lastBasicUser != null) u = lastBasicUser;
                if (p == null && lastBasicPass != null) p = lastBasicPass;
                if (u != null && p != null) {
                    handler.proceed(u, p);
                } else {
                    handler.cancel();
                }
            }
        });
        prefs = getSharedPreferences("netcontrol", MODE_PRIVATE);
        String last = prefs.getString("last_url", null);
        if (last == null || last.isEmpty()) {
            String html = "<html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"/></head><body style=\"margin:0;display:flex;align-items:center;justify-content:center;height:100vh;font-family:sans-serif;color:#666;\">等待主机下发指令…</body></html>";
            webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
            currentUrl = "";
        } else {
            webView.loadUrl(last);
            currentUrl = last;
        }
        applyImmersiveMode();
        server = new LocalHttpServer(8080, new LocalHttpServer.UiController() {
            @Override
            public void setUrl(final String url, final Map<String, String> headers, final String basicUser, final String basicPass) {
                runOnUiThread(() -> {
                    currentUrl = url;
                    if (basicUser != null && basicPass != null) {
                        lastBasicUser = basicUser;
                        lastBasicPass = basicPass;
                        String host = null;
                        String hostWithPort = null;
                        try {
                            Uri u = Uri.parse(url);
                            host = u.getHost();
                            if (host != null && u.getPort() != -1) hostWithPort = host + ":" + u.getPort();
                        } catch (Exception ignored) {}
                        if (host != null) {
                            SharedPreferences.Editor ed = prefs.edit()
                                    .putString("basic_user_" + host, basicUser)
                                    .putString("basic_pass_" + host, basicPass);
                            if (hostWithPort != null) {
                                ed.putString("basic_user_" + hostWithPort, basicUser)
                                        .putString("basic_pass_" + hostWithPort, basicPass);
                            }
                            ed.apply();
                        }
                    }
                    final String loadUrl = percentEncodeNonAscii(url);
                    if (headers != null && !headers.isEmpty()) {
                        Map<String, String> h = new HashMap<>(headers);
                        if (basicUser != null && basicPass != null && !h.containsKey("Authorization")) {
                            String token = Base64.encodeToString((basicUser + ":" + basicPass).getBytes(), Base64.NO_WRAP);
                            h.put("Authorization", "Basic " + token);
                        }
                        webView.loadUrl(loadUrl, h);
                    } else {
                        if (basicUser != null && basicPass != null) {
                            Map<String, String> h = new HashMap<>();
                            String token = Base64.encodeToString((basicUser + ":" + basicPass).getBytes(), Base64.NO_WRAP);
                            h.put("Authorization", "Basic " + token);
                            webView.loadUrl(loadUrl, h);
                        } else {
                            webView.loadUrl(loadUrl);
                        }
                    }
                    currentUrl = loadUrl;
                    prefs.edit().putString("last_url", loadUrl).apply();
                });
            }

            @Override
            public String getCurrentUrl() {
                return currentUrl == null ? "" : currentUrl;
            }

            @Override
            public String getScreenResolution() {
                DisplayMetrics dm = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getRealMetrics(dm);
                return dm.widthPixels + "x" + dm.heightPixels;
            }

            @Override
            public boolean isFullscreen() {
                return true;
            }

            @Override
            public String getDeviceIp() {
                return getLocalIpv4();
            }
        });
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyImmersiveMode();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (server != null) {
            server.stop();
        }
    }

    private void applyImmersiveMode() {
        View decor = getWindow().getDecorView();
        int flags = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decor.setSystemUiVisibility(flags);
    }

    /** 将 URL 中的非 ASCII 字符按 UTF-8 百分号编码，避免中文路径加载失败或显示为 '?' */
    private String percentEncodeNonAscii(String raw) {
        if (raw == null) return "";
        boolean hasNonAscii = false;
        for (int i = 0; i < raw.length(); i++) {
            if (raw.charAt(i) > 127) {
                hasNonAscii = true;
                break;
            }
        }
        if (!hasNonAscii) {
            return raw;
        }
        StringBuilder sb = new StringBuilder(raw.length() + 16);
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c < 128) {
                sb.append(c);
            } else {
                byte[] bytes = String.valueOf(c).getBytes(java.nio.charset.StandardCharsets.UTF_8);
                for (byte b : bytes) {
                    sb.append(String.format("%%%02X", b & 0xFF));
                }
            }
        }
        return sb.toString();
    }

    private String getLocalIpv4() {
        try {
            for (NetworkInterface nif : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (InetAddress addr : Collections.list(nif.getInetAddresses())) {
                    if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (SocketException ignored) {
        }
        return "";
    }
}
