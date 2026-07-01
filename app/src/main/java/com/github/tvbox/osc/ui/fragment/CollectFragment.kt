package com.github.tvbox.osc.ui.fragment;

import android.os.Bundle;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.BaseLazyFragment;
import com.github.tvbox.osc.base.ToolbarMenuProvider;
import com.github.tvbox.osc.cache.RoomDataManger;
import com.github.tvbox.osc.cache.VodCollect;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.ui.activity.DetailActivity;
import com.github.tvbox.osc.ui.activity.HomeActivity;
import com.github.tvbox.osc.ui.adapter.CollectAdapter;
import com.github.tvbox.osc.ui.tv.widget.AutoFitGridLayoutManager;
import com.github.tvbox.osc.util.FastClickCheckUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

public class CollectFragment extends BaseLazyFragment implements ToolbarMenuProvider {
    public static CollectAdapter collectAdapter;
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
        return R.menu.collect_fragment_menu;
    }

    @Override
    public String getToolbarTitle() {
        return "收藏";
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
        collectAdapter = new CollectAdapter();
        mGridView.setAdapter(collectAdapter);

        setLoadSir2(mGridView);

        mSwipe.setOnRefreshListener(this::initData);
        mSwipe.setOnChildScrollUpCallback((parent, child) -> mGridView.canScrollVertically(-1));

        collectAdapter.setOnItemClickListener((adapter, view, position) -> {
            FastClickCheckUtil.check(view);
            VodCollect vodInfo = collectAdapter.getData().get(position);
            if (vodInfo != null) {
                if (ApiConfig.get().getSource(vodInfo.sourceKey) != null) {
                    Bundle bundle = new Bundle();
                    bundle.putString("id", vodInfo.vodId);
                    bundle.putString("sourceKey", vodInfo.sourceKey);
                    bundle.putString("picture", vodInfo.pic);
                    jumpActivity(DetailActivity.class, bundle);
                } else {
                    if (mActivity instanceof HomeActivity homeActivity) {
                        homeActivity.switchToSearchAndSearch(vodInfo.name);
                    }
                }
            }
        });
        collectAdapter.setOnItemLongClickListener((adapter, view, position) -> {
            VodCollect vodCollect = collectAdapter.getData().get(position);
            if (vodCollect != null) {
                showDeleteCollectItemDialog(vodCollect, position);
            }
            return true;
        });
    }

    private void initData() {
        List<VodCollect> allVodRecord = RoomDataManger.getAllVodCollect();
        List<VodCollect> vodInfoList = new ArrayList<>(allVodRecord);
        collectAdapter.setNewData(vodInfoList);

        if (vodInfoList.isEmpty()) {
            showEmpty();
        } else {
            showSuccess();
        }

        if (mSwipe != null) {
            mSwipe.setRefreshing(false);
        }
    }

    private void showDeleteCollectItemDialog(VodCollect vodCollect, int position) {
        if (getActivity() == null || vodCollect == null) return;

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(getActivity())
                .setTitle("取消收藏")
                .setMessage("确定要取消收藏「" + vodCollect.name + "」吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    collectAdapter.remove(position);
                    RoomDataManger.deleteVodCollect(vodCollect.getId());
                    if (collectAdapter.getData().isEmpty()) {
                        showEmpty();
                    }
                    android.widget.Toast.makeText(mContext, "已删除", android.widget.Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showClearDialog() {
        if (getActivity() == null) return;

        if (collectAdapter.getData().isEmpty()) {
            android.widget.Toast.makeText(mContext, "暂无收藏", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(getActivity())
                .setTitle("清空收藏")
                .setMessage("确定要清空所有收藏吗？")
                .setPositiveButton("清空", (dialog, which) -> {
                    RoomDataManger.deleteVodCollectAll();
                    collectAdapter.setNewData(new ArrayList<>());
                    showEmpty();
                    android.widget.Toast.makeText(mContext, "已清空收藏", android.widget.Toast.LENGTH_SHORT).show();
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
