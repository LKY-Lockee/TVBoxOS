package com.github.tvbox.osc.player.thirdparty;

import android.app.Activity;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.bean.IpScanningVo;
import com.github.tvbox.osc.server.RemoteServer;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.IpScanning;
import com.orhanobut.hawk.Hawk;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RemoteTVBox {

    private static int availableFailNum;
    private static int availableSuccessNum;
    private static int availableIpNum;

    public static boolean run(Activity activity, String url, String title, String subtitle, HashMap<String, String> headers) {
        String actionUrl = getAvailableActionUrl();
        if (TextUtils.isEmpty(actionUrl)) {
            return false;
        }
        try {
            if (headers != null && !headers.isEmpty()) {
                url = url + "|";
                int idx = 0;
                StringBuilder urlBuilder = new StringBuilder(url);
                for (String hk : headers.keySet()) {
                    urlBuilder.append(hk).append("=").append(URLEncoder.encode(headers.get(hk), "UTF-8"));
                    if (idx < headers.size() - 1) {
                        urlBuilder.append("&");
                    }
                    idx++;
                }
                url = urlBuilder.toString();
            }
            Map<String, String> params = new HashMap<>();
            params.put("do", "push");
            params.put("url", url);
            post(actionUrl, params, new okhttp3.Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String pushResult = Objects.requireNonNull(response.body()).string();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    public static void searchAvailable(Callback callback) {
        availableFailNum = 0;
        availableSuccessNum = 0;
        String localIp = RemoteServer.getLocalIPAddress(App.getInstance());
        List<IpScanningVo> searchList = new IpScanning().search(localIp, false);
        availableIpNum = searchList.size();
        int port = 9978;
        for (IpScanningVo one : searchList) {
            String ip = one.getIp();
            if (ip.equals(localIp)) {
                availableIpNum--;
                continue;
            }
            String actionUrl = "http://" + ip + ":" + port + "/action";
            String viewHost = ip + ":" + port;
            try {
                post(actionUrl, null, new okhttp3.Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        availableFailNum++;
                        callback.fail(availableFailNum == availableIpNum, (availableSuccessNum + availableFailNum) == availableIpNum);
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        availableSuccessNum++;
                        String result = Objects.requireNonNull(response.body()).string();
                        if (result.equals("ok")) {
                            callback.found(viewHost, (availableSuccessNum + availableFailNum) == availableIpNum);
                        }
                    }
                });
            } catch (Exception ignored) {

            }
        }

    }

    public static String getAvailable() {
        return Hawk.get(HawkConfig.REMOTE_TVBOX, null);
    }

    public static void setAvailable(String viewHost) {
        Hawk.put(HawkConfig.REMOTE_TVBOX, viewHost);
    }

    public static String getAvailableActionUrl() {
        if (getAvailable() == null) {
            return "";
        }
        return "http://" + getAvailable() + "/action";
    }

    public static void post(String url, Map<String, String> params, okhttp3.Callback callback) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.readTimeout(1000, TimeUnit.MILLISECONDS);
        builder.writeTimeout(1000, TimeUnit.MILLISECONDS);
        builder.connectTimeout(1000, TimeUnit.MILLISECONDS);
        OkHttpClient client = builder.build();
        FormBody.Builder formBodyBuilder = new FormBody.Builder();
        if (params != null && !params.isEmpty()) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                formBodyBuilder.add(entry.getKey(), entry.getValue());
            }
        }
        FormBody formBody = formBodyBuilder.build();
        client.newCall(new Request.Builder().url(url).post(formBody).build()).enqueue(callback);
    }

    public abstract class Callback {
        public abstract void found(String viewHost, boolean end);

        public abstract void fail(boolean all, boolean end);
    }
}


