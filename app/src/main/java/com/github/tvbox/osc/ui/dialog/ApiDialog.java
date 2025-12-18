package com.github.tvbox.osc.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.server.ControlManager;
import com.github.tvbox.osc.ui.tv.QRCodeGen;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.HistoryHelper;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.orhanobut.hawk.Hawk;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;

import me.jessyan.autosize.utils.AutoSizeUtils;

/**
 * 描述
 *
 * @author pj567
 * @since 2020/12/27
 */
public class ApiDialog {
    private final Dialog dialog;
    private final Context context;
    private final ImageView ivQRCode;
    private final TextView tvAddress;
    private final EditText inputApi;
    private final EditText inputApiLive;
    private OnListener listener = null;

    public ApiDialog(@NonNull @NotNull Context context) {
        this.context = context;
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_api, null);

        ivQRCode = view.findViewById(R.id.ivQRCode);
        tvAddress = view.findViewById(R.id.tvAddress);
        inputApi = view.findViewById(R.id.input);
        inputApiLive = view.findViewById(R.id.inputLive);

        dialog = new MaterialAlertDialogBuilder(context)
                .setView(view)
                .create();

        // 初始化数据
        inputApi.setText(Hawk.get(HawkConfig.API_URL, ""));
        inputApiLive.setText(Hawk.get(HawkConfig.LIVE_API_URL, Hawk.get(HawkConfig.API_URL)));

        view.findViewById(R.id.inputSubmit).setOnClickListener(v -> {
            String newApi = inputApi.getText().toString().trim();
            String newLiveApi = inputApiLive.getText().toString().trim();

            // 保存点播配置
            if (!newApi.isEmpty()) {
                HistoryHelper.setApiHistory(newApi);
                Hawk.put(HawkConfig.API_URL, newApi);
            }

            // 保存直播配置
            if (!newLiveApi.isEmpty()) {
                HistoryHelper.setLiveApiHistory(newLiveApi);
                Hawk.put(HawkConfig.LIVE_API_URL, newLiveApi);
            } else if (!newApi.isEmpty()) {
                // 如果直播配置为空，使用点播配置
                Hawk.put(HawkConfig.LIVE_API_URL, newApi);
            }

            if (listener != null) {
                listener.onchange(newApi);
            }
            dialog.dismiss();
        });

        inputApi.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEARCH || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                view.findViewById(R.id.inputSubmit).performClick();
                return true;
            }
            return false;
        });

        inputApiLive.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEARCH || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                view.findViewById(R.id.inputSubmit).performClick();
                return true;
            }
            return false;
        });

        refreshQRCode();
        EventBus.getDefault().register(this);
    }

    public void show() {
        dialog.show();
    }

    public void dismiss() {
        EventBus.getDefault().unregister(this);
        dialog.dismiss();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void refresh(RefreshEvent event) {
        if (event.type == RefreshEvent.TYPE_API_URL_CHANGE) {
            inputApi.setText((String) event.obj);
            inputApiLive.setText((String) event.obj);
        }
    }

    private void refreshQRCode() {
        String address = ControlManager.get().getAddress(false);
        tvAddress.setText(String.format("扫描上方二维码或访问地址\n%s", address));
        ivQRCode.setImageBitmap(QRCodeGen.generateBitmap(address + "api.html", AutoSizeUtils.mm2px(context, 300), AutoSizeUtils.mm2px(context, 300)));
    }

    public void setOnListener(OnListener listener) {
        this.listener = listener;
    }

    public interface OnListener {
        void onchange(String api);
    }
}
