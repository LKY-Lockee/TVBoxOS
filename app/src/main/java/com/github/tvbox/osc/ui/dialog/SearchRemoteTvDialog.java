package com.github.tvbox.osc.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.callback.EmptyCallback;
import com.github.tvbox.osc.callback.LoadingCallback;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.player.thirdparty.RemoteTVBox;
import com.github.tvbox.osc.ui.activity.SettingsActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.kingja.loadsir.callback.Callback;
import com.kingja.loadsir.core.LoadService;
import com.kingja.loadsir.core.LoadSir;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;


public class SearchRemoteTvDialog {
    private final Dialog dialog;
    private final Context context;
    private final View view;
    private LoadService<?> mLoadService;

    public SearchRemoteTvDialog(@NonNull @NotNull Context context) {
        this.context = context;
        view = LayoutInflater.from(context).inflate(R.layout.dialog_search_remotetv, null);
        dialog = new MaterialAlertDialogBuilder(context)
                .setView(view)
                .create();
        EventBus.getDefault().register(this);

        Button btnCancel = view.findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(v -> dismiss());
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
        if (event.type == RefreshEvent.TYPE_SETTING_SEARCH_TV) {
            showRemoteTvDialog(SettingsActivity.foundRemoteTv);
        }
    }

    public void setTip(String tip) {
        ((TextView) view.findViewById(R.id.title)).setText(tip);
        setLoadSir(view.findViewById(R.id.list));
        showLoading();
    }

    private void showRemoteTvDialog(boolean found) {
        if (!found) {
            if (SettingsActivity.loadingSearchRemoteTvDialog != null) {
                SettingsActivity.loadingSearchRemoteTvDialog.showEmpty();
            }
            Toast.makeText(context, "未找到附近TVBox", Toast.LENGTH_SHORT).show();
            return;
        }
        if (SettingsActivity.loadingSearchRemoteTvDialog != null) {
            SettingsActivity.loadingSearchRemoteTvDialog.dismiss();
        }
        if (SettingsActivity.remoteTvHostList == null) {
            return;
        }
        RemoteTVBox.setAvailable(SettingsActivity.remoteTvHostList.get(0));

        String[] hosts = SettingsActivity.remoteTvHostList.toArray(new String[0]);
        new MaterialAlertDialogBuilder(context)
                .setTitle("附近TVBox")
                .setSingleChoiceItems(hosts, 0, (dlg, which) -> {
                    RemoteTVBox.setAvailable(hosts[which]);
                    Toast.makeText(context, "设置成功", Toast.LENGTH_SHORT).show();
                    dlg.dismiss();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    protected void setLoadSir(View view) {
        if (mLoadService == null) {
            mLoadService = LoadSir.getDefault().register(view, (Callback.OnReloadListener) v -> {
            });
        }
    }

    public void showLoading() {
        if (mLoadService != null) {
            mLoadService.showCallback(LoadingCallback.class);
        }
    }

    public void showEmpty() {
        if (null != mLoadService) {
            mLoadService.showCallback(EmptyCallback.class);
        }
    }

    public void showSuccess() {
        if (null != mLoadService) {
            mLoadService.showSuccess();
        }
    }

}
