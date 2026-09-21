package com.example.netcontrol;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class LocalHttpServer extends NanoHTTPD {
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final String JSON_UTF8 = "application/json; charset=UTF-8";

    public interface UiController {
        void setUrl(String url, Map<String, String> headers, String basicUser, String basicPass);
        String getCurrentUrl();
        String getScreenResolution();
        boolean isFullscreen();
        String getDeviceIp();
    }

    private final UiController ui;

    public LocalHttpServer(int port, UiController ui) {
        super(port);
        this.ui = ui;
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Method method = session.getMethod();

        if (method == Method.OPTIONS) {
            return withCors(NanoHTTPD.newFixedLengthResponse(Response.Status.NO_CONTENT, JSON_UTF8, ""));
        }

        if (method == Method.GET && "/api/status".equals(uri)) {
            return withCors(handleStatus());
        }
        if (method == Method.POST && "/api/set_url".equals(uri)) {
            return withCors(handleSetUrl(session));
        }
        return withCors(json(Response.Status.NOT_FOUND, "{\"code\":404,\"msg\":\"not found\",\"data\":null}"));
    }

    /** CORS + Chrome 私网访问（本机网页 → 局域网设备）所需响应头 */
    private Response withCors(Response r) {
        r.addHeader("Access-Control-Allow-Origin", "*");
        r.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        r.addHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With, Access-Control-Request-Private-Network");
        r.addHeader("Access-Control-Allow-Private-Network", "true");
        r.addHeader("Access-Control-Max-Age", "86400");
        r.addHeader("Access-Control-Expose-Headers", "Content-Type");
        return r;
    }

    private Response json(Response.Status status, String body) {
        return NanoHTTPD.newFixedLengthResponse(status, JSON_UTF8, body);
    }

    private Response handleStatus() {
        try {
            JSONObject data = new JSONObject();
            data.put("device_name", "NetControl_Terminal");
            data.put("device_ip", ui.getDeviceIp());
            data.put("current_url", ui.getCurrentUrl());
            data.put("screen_resolution", ui.getScreenResolution());
            data.put("is_fullscreen", ui.isFullscreen());
            JSONObject res = new JSONObject();
            res.put("code", 200);
            res.put("msg", "success");
            res.put("data", data);
            return json(Response.Status.OK, res.toString());
        } catch (JSONException e) {
            return json(Response.Status.INTERNAL_ERROR, "{\"code\":500,\"msg\":\"error\",\"data\":null}");
        }
    }

    private Response handleSetUrl(IHTTPSession session) {
        try {
            String body = readBodyUtf8(session);
            if (body == null || body.trim().isEmpty()) {
                return json(Response.Status.BAD_REQUEST, "{\"code\":400,\"msg\":\"bad request\",\"data\":null}");
            }
            JSONObject obj = new JSONObject(body);
            String url = sanitizeUrl(obj.optString("url", ""));
            if (url.isEmpty()) {
                return json(Response.Status.BAD_REQUEST, "{\"code\":400,\"msg\":\"url required\",\"data\":null}");
            }
            url = percentEncodeNonAscii(url);

            Map<String, String> headers = new HashMap<>();
            if (obj.has("headers") && !obj.isNull("headers")) {
                JSONObject h = obj.getJSONObject("headers");
                for (java.util.Iterator<String> it = h.keys(); it.hasNext(); ) {
                    String k = it.next();
                    headers.put(k, h.optString(k, ""));
                }
            }
            String basicUser = obj.optString("basic_user", "");
            String basicPass = obj.optString("basic_pass", "");
            ui.setUrl(
                    url,
                    headers.isEmpty() ? null : headers,
                    basicUser.isEmpty() ? null : basicUser,
                    basicPass.isEmpty() ? null : basicPass
            );
            JSONObject res = new JSONObject();
            res.put("code", 200);
            res.put("msg", "Command received, loading URL...");
            res.put("data", new JSONObject().put("accepted_url", url));
            return json(Response.Status.OK, res.toString());
        } catch (Exception e) {
            String msg = e.getMessage() == null ? "error" : e.getMessage().replace("\"", "'");
            return json(Response.Status.INTERNAL_ERROR, "{\"code\":500,\"msg\":\"" + msg + "\",\"data\":null}");
        }
    }

    /**
     * 读取 POST body，并按 UTF-8 解码。
     * NanoHTTPD parseBody 的 postData 在部分机型上会变成平台默认编码，导致中文 URL 变 '?'。
     */
    private String readBodyUtf8(IHTTPSession session) {
        Map<String, String> files = new HashMap<>();
        try {
            session.parseBody(files);
        } catch (Exception ignored) {
            // 继续尝试从输入流/已解析数据恢复
        }
        String postData = files.get("postData");
        if (postData != null && !postData.isEmpty()) {
            try {
                // 先按 ISO-8859-1 取回原始字节，再按 UTF-8 解码
                byte[] raw = postData.getBytes("ISO-8859-1");
                String decoded = new String(raw, UTF8);
                if (looksLikeJson(decoded)) {
                    return decoded;
                }
            } catch (Exception ignored) {
            }
            if (looksLikeJson(postData)) {
                return postData;
            }
            try {
                return new String(postData.getBytes(UTF8), UTF8);
            } catch (Exception ignored) {
                return postData;
            }
        }

        try {
            Map<String, String> headers = session.getHeaders();
            String cl = headers.get("content-length");
            if (cl == null) {
                cl = headers.get("Content-Length");
            }
            int len = cl == null ? 0 : Integer.parseInt(cl.trim());
            if (len <= 0 || len > 2 * 1024 * 1024) {
                return null;
            }
            InputStream in = session.getInputStream();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[Math.min(len, 4096)];
            int remaining = len;
            while (remaining > 0) {
                int n = in.read(buf, 0, Math.min(buf.length, remaining));
                if (n <= 0) break;
                bos.write(buf, 0, n);
                remaining -= n;
            }
            return new String(bos.toByteArray(), UTF8);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean looksLikeJson(String s) {
        if (s == null) return false;
        String t = s.trim();
        return t.startsWith("{") || t.startsWith("[");
    }

    /** 非 ASCII 字符按 UTF-8 百分号编码，避免 WebView/链路把中文路径变成 '?' */
    private String percentEncodeNonAscii(String raw) {
        if (raw == null || raw.isEmpty()) return "";
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
                byte[] bytes = String.valueOf(c).getBytes(UTF8);
                for (byte b : bytes) {
                    sb.append(String.format("%%%02X", b & 0xFF));
                }
            }
        }
        return sb.toString();
    }

    private String sanitizeUrl(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        if (s.startsWith("`") && s.endsWith("`") && s.length() >= 2) {
            s = s.substring(1, s.length() - 1).trim();
        }
        if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
            if (s.length() >= 2) s = s.substring(1, s.length() - 1).trim();
        }
        return s;
    }
}
