package com.github.tvbox.osc.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;
import java.util.List;

/**
 * @author pj567
 * @date :2021/1/5
 * @description:
 */
public class CustomWebReceiver extends BroadcastReceiver {
    public static final String action = "android.content.movie.custom.web.Action";
    public static final String REFRESH_LIVE = "live";
    public static final String REFRESH_PARSE = "parse";
    public static String REFRESH_SOURCE = "source";
    public static List<Callback> callback = new ArrayList<>();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (action.equals(intent.getAction()) && intent.getExtras() != null) {
            Object refreshObj = null;
            String action = intent.getExtras().getString("action");
        }
    }

    public interface Callback {
        void onChange(String action, Object obj);
    }
}