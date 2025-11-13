package com.github.tvbox.osc.ui.dialog;

import android.content.Context;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.bean.VodInfo;
import com.github.tvbox.osc.cache.RoomDataManger;
import com.github.tvbox.osc.cache.VodCollect;
import com.github.tvbox.osc.ui.activity.CollectActivity;
import com.github.tvbox.osc.ui.activity.HistoryActivity;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ConfirmClearDialog extends BaseDialog {

    public ConfirmClearDialog(@NonNull @NotNull Context context, String type) {
        super(context);
        setContentView(R.layout.dialog_confirm);
        setCanceledOnTouchOutside(true);
        TextView tvYes = findViewById(R.id.btnConfirm);
        TextView tvNo = findViewById(R.id.btnCancel);

        tvYes.setOnClickListener(v -> {
            // if removing all Favorites
            if (Objects.equals(type, "Collect")) {
                List<VodCollect> vodInfoList = new ArrayList<>();
                CollectActivity.collectAdapter.setNewData(vodInfoList);
                CollectActivity.collectAdapter.notifyDataSetChanged();
                RoomDataManger.deleteVodCollectAll();
                // if removing all History
            } else if (Objects.equals(type, "History")) {
                List<VodInfo> vodInfoList = new ArrayList<>();
                HistoryActivity.historyAdapter.setNewData(vodInfoList);
                HistoryActivity.historyAdapter.notifyDataSetChanged();
                RoomDataManger.deleteVodRecordAll();
            }

            ConfirmClearDialog.this.dismiss();
        });
        tvNo.setOnClickListener(v -> ConfirmClearDialog.this.dismiss());
    }

}