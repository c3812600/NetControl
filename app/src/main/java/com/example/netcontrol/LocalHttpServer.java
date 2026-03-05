package com.example.netcontrol;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class LocalHttpServer extends NanoHTTPD {
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
        if (method == Method.GET && "/api/status".equals(uri)) {
            return handleStatus();
        }
        if (method == Method.POST && "/api/set_url".equals(uri)) {
            return handleSetUrl(session);
        }
        return NanoHTTPD.newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json", "{\"code\":404,\"msg\":\"not found\",\"data\":null}");
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
            return NanoHTTPD.newFixedLengthResponse(Response.Status.OK, "application/json", res.toString());
        } catch (JSONException e) {
            return NanoHTTPD.newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json", "{\"code\":500,\"msg\":\"error\",\"data\":null}");
        }
    }

    private Response handleSetUrl(IHTTPSession session) {
        Map<String, String> files = new HashMap<>();
        try {
            session.parseBody(files);
            String body = files.get("postData");
            if (body == null) {
                return NanoHTTPD.newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json", "{\"code\":400,\"msg\":\"bad request\",\"data\":null}");
            }
            JSONObject obj = new JSONObject(body);
            String url = obj.optString("url", "");
            url = sanitizeUrl(url);
            Map<String, String> headers = new HashMap<>();
            if (obj.has("headers")) {
                JSONObject h = obj.getJSONObject("headers");
                for (java.util.Iterator<String> it = h.keys(); it.hasNext();) {
                    String k = it.next();
                    headers.put(k, h.optString(k, ""));
                }
            }
            String basicUser = obj.optString("basic_user", "");
            String basicPass = obj.optString("basic_pass", "");
            if (url.isEmpty()) {
                return NanoHTTPD.newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json", "{\"code\":400,\"msg\":\"url required\",\"data\":null}");
            }
            ui.setUrl(url, headers.isEmpty() ? null : headers, basicUser.isEmpty() ? null : basicUser, basicPass.isEmpty() ? null : basicPass);
            return NanoHTTPD.newFixedLengthResponse(Response.Status.OK, "application/json", "{\"code\":200,\"msg\":\"Command received, loading URL...\",\"data\":null}");
        } catch (Exception e) {
            return NanoHTTPD.newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json", "{\"code\":500,\"msg\":\"error\",\"data\":null}");
        }
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
