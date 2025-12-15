package com.github.tvbox.osc.ui.fragment;

import android.os.Bundle;
import android.view.View;
import android.view.animation.BounceInterpolator;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.BackPressProvider;
import com.github.tvbox.osc.base.BaseLazyFragment;
import com.github.tvbox.osc.base.ToolbarMenuProvider;
import com.github.tvbox.osc.bean.SourceBean;
import com.github.tvbox.osc.bean.VodInfo;
import com.github.tvbox.osc.cache.RoomDataManger;
import com.github.tvbox.osc.ui.activity.DetailActivity;
import com.github.tvbox.osc.ui.activity.FastSearchActivity;
import com.github.tvbox.osc.ui.adapter.HistoryAdapter;
import com.github.tvbox.osc.ui.dialog.ConfirmClearDialog;
import com.github.tvbox.osc.ui.tv.widget.AutoFitGridLayoutManager;
import com.github.tvbox.osc.util.FastClickCheckUtil;
import com.github.tvbox.osc.util.HawkConfig;
import com.owen.tvrecyclerview.widget.TvRecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * @author pj567
 * @date :2021/1/7
 * @description:
 */
public class HistoryFragment extends BaseLazyFragment implements ToolbarMenuProvider, BackPressProvider {
    public static HistoryAdapter historyAdapter;
    private boolean delMode = false;

    // --- BackPressProvider ---
    @Override
    public boolean handleBackPress() {
        if (delMode) {
            toggleDelMode();
            return true;
        }
        return false;
    }
    // ----------------

    // --- BaseLazyFragment ---
    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_grid;
    }

    @Override
    protected void init() {
        initView();
        initData();
    }
    // ----------------

    // --- ToolbarMenuProvider ---
    @Override
    public int getMenuResId() {
        return R.menu.history_toolbar_menu;
    }

    @Override
    public String getToolbarTitle() {
        return "历史记录";
    }

    @Override
    public boolean onMenuItemClick(int itemId) {
        if (itemId == R.id.action_delete) {
            toggleDelMode();
            return true;
        } else if (itemId == R.id.action_clear) {
            showClearDialog();
            return true;
        }
        return false;
    }
    // ----------------

    private void toggleDelMode() {
        HawkConfig.hotVodDelete = !HawkConfig.hotVodDelete;
        historyAdapter.notifyDataSetChanged();
        delMode = !delMode;
    }

    private void showClearDialog() {
        ConfirmClearDialog dialog = new ConfirmClearDialog(mContext, "History");
        dialog.show();
    }

    private void initView() {
        TvRecyclerView mGridView = rootView.findViewById(R.id.mGridView);
        mGridView.setLayoutManager(new AutoFitGridLayoutManager(mContext, 150));
        historyAdapter = new HistoryAdapter();
        mGridView.setAdapter(historyAdapter);
        mGridView.setOnItemListener(new TvRecyclerView.OnItemListener() {
            @Override
            public void onItemPreSelected(TvRecyclerView parent, View itemView, int position) {
                itemView.animate().scaleX(1.0f).scaleY(1.0f).setDuration(300).setInterpolator(new BounceInterpolator()).start();
            }

            @Override
            public void onItemSelected(TvRecyclerView parent, View itemView, int position) {
                itemView.animate().scaleX(1.05f).scaleY(1.05f).setDuration(300).setInterpolator(new BounceInterpolator()).start();
            }

            @Override
            public void onItemClick(TvRecyclerView parent, View itemView, int position) {

            }
        });
        historyAdapter.setOnItemClickListener((adapter, view, position) -> {
            FastClickCheckUtil.check(view);
            if (position == -1) return;
            VodInfo vodInfo = historyAdapter.getData().get(position);

            if (vodInfo != null) {
                if (delMode) {
                    historyAdapter.remove(position);
                    RoomDataManger.deleteVodRecord(vodInfo.sourceKey, vodInfo);
                } else {
                    Bundle bundle = new Bundle();
                    bundle.putString("id", vodInfo.id);
                    bundle.putString("sourceKey", vodInfo.sourceKey);
                    SourceBean sourceBean = ApiConfig.get().getSource(vodInfo.sourceKey);
                    if (sourceBean != null) {
                        bundle.putString("picture", vodInfo.pic);
                        jumpActivity(DetailActivity.class, bundle);
                    } else {
                        bundle.putString("title", vodInfo.name);
                        jumpActivity(FastSearchActivity.class, bundle);
                    }
                }
            }
        });
        historyAdapter.setOnItemLongClickListener((adapter, view, position) -> {
            toggleDelMode();
            return true;
        });
    }

    private void initData() {
        List<VodInfo> allVodRecord = RoomDataManger.getAllVodRecord(100);
        List<VodInfo> vodInfoList = new ArrayList<>();
        for (VodInfo vodInfo : allVodRecord) {
            if (vodInfo.playNote != null && !vodInfo.playNote.isEmpty()) vodInfo.note = "上次看到" + vodInfo.playNote;
            vodInfoList.add(vodInfo);
        }
        historyAdapter.setNewData(vodInfoList);
    }
}
