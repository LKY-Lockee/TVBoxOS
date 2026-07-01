package com.github.tvbox.osc.ui.fragment;

import android.os.Bundle;

import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.BaseLazyFragment;
import com.github.tvbox.osc.base.ToolbarMenuProvider;
import com.github.tvbox.osc.bean.SourceBean;
import com.github.tvbox.osc.bean.VodInfo;
import com.github.tvbox.osc.cache.RoomDataManger;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.ui.activity.DetailActivity;
import com.github.tvbox.osc.ui.activity.HomeActivity;
import com.github.tvbox.osc.ui.adapter.HistoryAdapter;
import com.github.tvbox.osc.ui.tv.widget.AutoFitGridLayoutManager;
import com.github.tvbox.osc.util.FastClickCheckUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

/**
 * @author pj567
 * @date :2021/1/7
 * @description:
 */
public class HistoryFragment extends BaseLazyFragment implements ToolbarMenuProvider {
    public static HistoryAdapter historyAdapter;
    private SwipeRefreshLayout mSwipe;

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

    // --- Fragment ---
    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
    // ----------------

    // --- ToolbarMenuProvider ---
    @Override
    public int getMenuResId() {
        return R.menu.history_fragment_menu;
    }

    @Override
    public String getToolbarTitle() {
        return "历史记录";
    }

    @Override
    public boolean onMenuItemClick(int itemId) {
        if (itemId == R.id.action_clear) {
            showClearDialog();
            return true;
        }
        return false;
    }
    // ----------------

    private void initView() {
        EventBus.getDefault().register(this);

        mSwipe = rootView.findViewById(R.id.mSwipe);
        RecyclerView mGridView = rootView.findViewById(R.id.mGridView);
        mGridView.setLayoutManager(new AutoFitGridLayoutManager(mContext, 150));
        historyAdapter = new HistoryAdapter();
        mGridView.setAdapter(historyAdapter);

        setLoadSir2(mGridView);

        mSwipe.setOnRefreshListener(this::initData);
        mSwipe.setOnChildScrollUpCallback((parent, child) -> mGridView.canScrollVertically(-1));

        historyAdapter.setOnItemClickListener((adapter, view, position) -> {
            FastClickCheckUtil.check(view);
            if (position == -1) return;
            VodInfo vodInfo = historyAdapter.getData().get(position);

            if (vodInfo != null) {
                Bundle bundle = new Bundle();
                bundle.putString("id", vodInfo.id);
                bundle.putString("sourceKey", vodInfo.sourceKey);
                SourceBean sourceBean = ApiConfig.get().getSource(vodInfo.sourceKey);
                if (sourceBean != null) {
                    bundle.putString("picture", vodInfo.pic);
                    jumpActivity(DetailActivity.class, bundle);
                } else {
                    if (mActivity instanceof HomeActivity homeActivity) {
                        homeActivity.switchToSearchAndSearch(vodInfo.name);
                    }
                }
            }
        });
        historyAdapter.setOnItemLongClickListener((adapter, view, position) -> {
            VodInfo vodInfo = historyAdapter.getData().get(position);
            if (vodInfo != null) {
                showDeleteHistoryItemDialog(vodInfo, position);
            }
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

        if (vodInfoList.isEmpty()) {
            showEmpty();
        } else {
            showSuccess();
        }

        if (mSwipe != null) {
            mSwipe.setRefreshing(false);
        }
    }

    private void showDeleteHistoryItemDialog(VodInfo vodInfo, int position) {
        if (getActivity() == null || vodInfo == null) return;

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(getActivity())
                .setTitle("删除历史记录")
                .setMessage("确定要删除「" + vodInfo.name + "」的观看记录吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    historyAdapter.remove(position);
                    RoomDataManger.deleteVodRecord(vodInfo.sourceKey, vodInfo);
                    if (historyAdapter.getData().isEmpty()) {
                        showEmpty();
                    }
                    android.widget.Toast.makeText(mContext, "已删除", android.widget.Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showClearDialog() {
        if (getActivity() == null) return;

        if (historyAdapter.getData().isEmpty()) {
            android.widget.Toast.makeText(mContext, "暂无历史记录", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(getActivity())
                .setTitle("清空历史记录")
                .setMessage("确定要清空所有观看记录吗？")
                .setPositiveButton("清空", (dialog, which) -> {
                    RoomDataManger.deleteVodRecordAll();
                    historyAdapter.setNewData(new ArrayList<>());
                    showEmpty();
                    android.widget.Toast.makeText(mContext, "已清空历史记录", android.widget.Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void refresh(RefreshEvent event) {
        if (event.type == RefreshEvent.TYPE_HISTORY_REFRESH) {
            initData();
        }
    }
}
