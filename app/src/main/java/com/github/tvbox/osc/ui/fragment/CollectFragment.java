package com.github.tvbox.osc.ui.fragment;

import android.os.Bundle;
import android.view.View;
import android.view.animation.BounceInterpolator;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.BackPressProvider;
import com.github.tvbox.osc.base.BaseLazyFragment;
import com.github.tvbox.osc.base.ToolbarMenuProvider;
import com.github.tvbox.osc.cache.RoomDataManger;
import com.github.tvbox.osc.cache.VodCollect;
import com.github.tvbox.osc.ui.activity.DetailActivity;
import com.github.tvbox.osc.ui.adapter.CollectAdapter;
import com.github.tvbox.osc.ui.dialog.ConfirmClearDialog;
import com.github.tvbox.osc.util.FastClickCheckUtil;
import com.github.tvbox.osc.util.HawkConfig;
import com.owen.tvrecyclerview.widget.TvRecyclerView;
import com.owen.tvrecyclerview.widget.V7GridLayoutManager;

import java.util.ArrayList;
import java.util.List;

public class CollectFragment extends BaseLazyFragment implements ToolbarMenuProvider, BackPressProvider {
    public static CollectAdapter collectAdapter;
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
        return R.menu.collect_toolbar_menu;
    }

    @Override
    public String getToolbarTitle() {
        return "收藏";
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
        collectAdapter.notifyDataSetChanged();
        delMode = !delMode;
    }

    private void showClearDialog() {
        ConfirmClearDialog dialog = new ConfirmClearDialog(mContext, "Collect");
        dialog.show();
    }

    private void initView() {
        TvRecyclerView mGridView = rootView.findViewById(R.id.mGridView);
        mGridView.setHasFixedSize(true);
        mGridView.setLayoutManager(new V7GridLayoutManager(mContext, isBaseOnWidth() ? 5 : 6));
        collectAdapter = new CollectAdapter();
        mGridView.setAdapter(collectAdapter);
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
        collectAdapter.setOnItemClickListener((adapter, view, position) -> {
            FastClickCheckUtil.check(view);
            VodCollect vodInfo = collectAdapter.getData().get(position);
            if (vodInfo != null) {
                if (delMode) {
                    collectAdapter.remove(position);
                    RoomDataManger.deleteVodCollect(vodInfo.getId());
                } else {
                    if (ApiConfig.get().getSource(vodInfo.sourceKey) != null) {
                        Bundle bundle = new Bundle();
                        bundle.putString("id", vodInfo.vodId);
                        bundle.putString("sourceKey", vodInfo.sourceKey);
                        bundle.putString("picture", vodInfo.pic);
                        jumpActivity(DetailActivity.class, bundle);
                    } /*else {
                        Intent newIntent = new Intent(mContext, SearchActivity.class);
                        newIntent.putExtra("title", vodInfo.name);
                        newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        mContext.startActivity(newIntent);
                    }*/
                }
            }
        });
        collectAdapter.setOnItemLongClickListener((adapter, view, position) -> {
            toggleDelMode();
            return true;
        });
    }

    private void initData() {
        List<VodCollect> allVodRecord = RoomDataManger.getAllVodCollect();
        List<VodCollect> vodInfoList = new ArrayList<>(allVodRecord);
        collectAdapter.setNewData(vodInfoList);
    }
}

