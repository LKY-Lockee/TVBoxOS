package com.github.tvbox.osc.ui.fragment;

import android.annotation.SuppressLint;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.bean.VodInfo;
import com.github.tvbox.osc.ui.adapter.SeriesAdapter;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DetailTabPlaylistFragment extends Fragment {
    private View contentView;
    private RecyclerView mGridView;
    private TabLayout mGridViewFlag;
    private LinearLayout tvSeriesGroup;
    private TextView tvSeriesSort;

    private SeriesAdapter seriesAdapter;
    private BaseQuickAdapter<String, BaseViewHolder> seriesGroupAdapter;
    private GridLayoutManager mGridViewLayoutMgr;
    private LinearSmoothScroller smoothScroller;

    private final ArrayList<String> seriesGroupOptions = new ArrayList<>();
    private View currentSeriesGroupView;
    private boolean isReverse = false;
    private int GroupCount = 30;

    private OnSeriesFlagSelectedListener onSeriesFlagSelectedListener;
    private OnSeriesSelectedListener onSeriesSelectedListener;

    public interface OnSeriesFlagSelectedListener {
        void onSeriesFlagSelected(String flagName, int position);
    }

    public interface OnSeriesSelectedListener {
        void onSeriesSelected(int position);
    }

    public void setOnSeriesFlagSelectedListener(OnSeriesFlagSelectedListener listener) {
        this.onSeriesFlagSelectedListener = listener;
    }

    public void setOnSeriesSelectedListener(OnSeriesSelectedListener listener) {
        this.onSeriesSelectedListener = listener;
    }

    public void setContentView(View view) {
        this.contentView = view;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (contentView != null) {
            if (contentView.getParent() != null) {
                ((ViewGroup) contentView.getParent()).removeView(contentView);
            }
            initViews(contentView);
            return contentView;
        }
        View view = inflater.inflate(R.layout.fragment_detail_tab_playlist, container, false);
        initViews(view);
        return view;
    }

    private void initViews(View view) {
        mGridView = view.findViewById(R.id.mGridView);
        mGridViewFlag = view.findViewById(R.id.mGridViewFlag);
        tvSeriesGroup = view.findViewById(R.id.mSeriesGroupTv);
        tvSeriesSort = view.findViewById(R.id.mSeriesSortTv);
        RecyclerView mSeriesGroupView = view.findViewById(R.id.mSeriesGroupView);

        mGridView.setHasFixedSize(false);
        mGridViewLayoutMgr = new GridLayoutManager(requireContext(), 6);
        mGridView.setLayoutManager(mGridViewLayoutMgr);

        smoothScroller = new LinearSmoothScroller(requireContext()) {
            @Override
            protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
                return 100f / displayMetrics.densityDpi;
            }

            @Override
            public PointF computeScrollVectorForPosition(int targetPosition) {
                return mGridViewLayoutMgr.computeScrollVectorForPosition(targetPosition);
            }
        };

        seriesAdapter = new SeriesAdapter(mGridViewLayoutMgr);
        mGridView.setAdapter(seriesAdapter);

        mGridViewFlag.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (onSeriesFlagSelectedListener != null && tab.getTag() != null) {
                    int position = (int) tab.getTag();
                    onSeriesFlagSelectedListener.onSeriesFlagSelected(tab.getText().toString(), position);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        mSeriesGroupView.setHasFixedSize(true);
        mSeriesGroupView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        seriesGroupAdapter = new BaseQuickAdapter<>(R.layout.item_series_group, seriesGroupOptions) {
            @Override
            protected void convert(BaseViewHolder helper, String item) {
                com.google.android.material.button.MaterialButton btnSeries = (com.google.android.material.button.MaterialButton) helper.itemView;
                btnSeries.setText(item);
                if (helper.getLayoutPosition() == getData().size() - 1) {
                    helper.itemView.setId(View.generateViewId());
                    helper.itemView.setNextFocusRightId(helper.itemView.getId());
                } else {
                    helper.itemView.setNextFocusRightId(View.NO_ID);
                }
            }
        };
        mSeriesGroupView.setAdapter(seriesGroupAdapter);

        seriesAdapter.setOnItemClickListener((adapter, itemView, position) -> {
            if (onSeriesSelectedListener != null) {
                onSeriesSelectedListener.onSeriesSelected(position);
            }
        });

        seriesGroupAdapter.setOnItemClickListener((adapter, itemView, position) -> {
            onSeriesGroupClick(itemView, position);
        });

        tvSeriesSort.setOnClickListener(v -> onSortClick());
    }

    @SuppressLint("NotifyDataSetChanged")
    private void onSortClick() {
        isReverse = !isReverse;
        tvSeriesSort.setText(isReverse ? "倒序" : "正序");

        if (onSeriesSelectedListener != null) {
            onSeriesSelectedListener.onSeriesSelected(-1);
        }
    }

    private void onSeriesGroupClick(View itemView, int position) {
        com.google.android.material.button.MaterialButton btnSeriesGroup = (com.google.android.material.button.MaterialButton) itemView;
        btnSeriesGroup.setSelected(true);

        int targetPos = position * GroupCount;
        customSeriesScrollPos(targetPos);

        if (currentSeriesGroupView != null && currentSeriesGroupView instanceof com.google.android.material.button.MaterialButton prevBtn) {
            prevBtn.setSelected(false);
        }
        currentSeriesGroupView = itemView;
    }

    public void setVodInfo(VodInfo vodInfo) {
        if (vodInfo == null || vodInfo.seriesMap == null || vodInfo.seriesMap.isEmpty()) {
            mGridViewFlag.setVisibility(View.GONE);
            mGridView.setVisibility(View.GONE);
            tvSeriesGroup.setVisibility(View.GONE);
            return;
        }

        mGridViewFlag.setVisibility(View.VISIBLE);
        mGridView.setVisibility(View.VISIBLE);

        isReverse = vodInfo.reverseSort;
        tvSeriesSort.setText(isReverse ? "倒序" : "正序");

        mGridViewFlag.removeAllTabs();
        int selectedTabIndex = 0;
        for (int j = 0; j < vodInfo.seriesFlags.size(); j++) {
            VodInfo.VodSeriesFlag flag = vodInfo.seriesFlags.get(j);
            TabLayout.Tab tab = mGridViewFlag.newTab();
            tab.setText(flag.name);
            tab.setTag(j);
            mGridViewFlag.addTab(tab);

            if (flag.name.equals(vodInfo.playFlag)) {
                selectedTabIndex = j;
                flag.selected = true;
            } else {
                flag.selected = false;
            }
        }

        if (selectedTabIndex < mGridViewFlag.getTabCount()) {
            TabLayout.Tab tab = mGridViewFlag.getTabAt(selectedTabIndex);
            if (tab != null) {
                tab.select();
            }
        }

        refreshList(vodInfo);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void refreshList(VodInfo vodInfo) {
        if (vodInfo == null || vodInfo.playFlag == null) return;

        if (vodInfo.seriesMap.get(vodInfo.playFlag).size() <= vodInfo.playIndex) {
            vodInfo.playIndex = 0;
        }

        List<VodInfo.VodSeries> list = vodInfo.seriesMap.get(vodInfo.playFlag);
        if (list != null) {
            boolean canSelect = true;
            for (int j = 0; j < list.size(); j++) {
                if (list.get(j).selected) {
                    canSelect = false;
                    break;
                }
            }
            if (canSelect && vodInfo.playIndex < list.size()) {
                list.get(vodInfo.playIndex).selected = true;
            }
        }

        DisplayMetrics displayMetrics = new DisplayMetrics();
        requireActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        float textSize = getResources().getDimension(R.dimen.ts_20);

        Paint pFont = new Paint();
        pFont.setTextSize(textSize);
        Rect rect = new Rect();

        int listSize = list.size();
        int maxTextWidth = 1;
        for (int i = 0; i < listSize; ++i) {
            String name = list.get(i).name;
            pFont.getTextBounds(name, 0, name.length(), rect);
            if (maxTextWidth < rect.width()) {
                maxTextWidth = rect.width();
            }
        }

        int marginPx = (int) (getResources().getDimension(R.dimen.vs_5) * 2);
        int chipPadding = (int) (40 * displayMetrics.density);
        int minItemWidth = maxTextWidth + chipPadding + marginPx + 50;

        int screenWidth = displayMetrics.widthPixels;
        int offset = screenWidth / minItemWidth;

        if (offset < 1) offset = 1;

        mGridViewLayoutMgr.setSpanCount(offset);
        seriesAdapter.setNewData(list);

        setSeriesGroupOptions(vodInfo, offset);

        mGridView.postDelayed(() -> customSeriesScrollPos(vodInfo.playIndex), 100);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void setSeriesGroupOptions(VodInfo vodInfo, int offset) {
        List<VodInfo.VodSeries> list = vodInfo.seriesMap.get(vodInfo.playFlag);
        if (list == null) return;

        int listSize = list.size();
        seriesGroupOptions.clear();
        int groupCount = (offset == 3 || offset == 6) ? 30 : 20;
        if (listSize > 100 && listSize <= 400) groupCount = 60;
        if (listSize > 400) groupCount = 120;

        GroupCount = groupCount;

        if (listSize > 1) {
            tvSeriesGroup.setVisibility(View.VISIBLE);
            int remainedOptionSize = listSize % groupCount;
            int optionSize = listSize / groupCount;

            for (int i = 0; i < optionSize; i++) {
                if (vodInfo.reverseSort) {
                    seriesGroupOptions.add(String.format(Locale.getDefault(), "%d - %d",
                            listSize - (i * groupCount + 1) + 1, listSize - (i * groupCount + groupCount) + 1));
                } else {
                    seriesGroupOptions.add(String.format(Locale.getDefault(), "%d - %d",
                            i * groupCount + 1, i * groupCount + groupCount));
                }
            }
            if (remainedOptionSize > 0) {
                if (vodInfo.reverseSort) {
                    seriesGroupOptions.add(String.format(Locale.getDefault(), "%d - %d",
                            listSize - (optionSize * groupCount + 1) + 1,
                            listSize - (optionSize * groupCount + remainedOptionSize) + 1));
                } else {
                    seriesGroupOptions.add(String.format(Locale.getDefault(), "%d - %d",
                            optionSize * groupCount + 1, optionSize * groupCount + remainedOptionSize));
                }
            }

            seriesGroupAdapter.notifyDataSetChanged();
        } else {
            tvSeriesGroup.setVisibility(View.GONE);
        }
    }

    private void customSeriesScrollPos(int targetPos) {
        mGridViewLayoutMgr.scrollToPositionWithOffset(targetPos, 0);
        mGridView.postDelayed(() -> {
            smoothScroller.setTargetPosition(targetPos);
            mGridViewLayoutMgr.startSmoothScroll(smoothScroller);
            mGridView.smoothScrollToPosition(targetPos);
        }, 50);
    }

    public void updateSeriesSelection(int oldIndex, int newIndex) {
        if (seriesAdapter.getData().size() > oldIndex && oldIndex >= 0) {
            seriesAdapter.getData().get(oldIndex).selected = false;
            seriesAdapter.notifyItemChanged(oldIndex);
        }
        if (seriesAdapter.getData().size() > newIndex && newIndex >= 0) {
            seriesAdapter.getData().get(newIndex).selected = true;
            seriesAdapter.notifyItemChanged(newIndex);
            customSeriesScrollPos(newIndex);
        }
    }

    public void setSeriesGroupVisibility(int visibility) {
        if (tvSeriesGroup != null) {
            tvSeriesGroup.setVisibility(visibility);
        }
    }

    public void setPlaylistVisibility(int visibility) {
        if (mGridView != null) {
            mGridView.setVisibility(visibility);
        }
        if (mGridViewFlag != null) {
            mGridViewFlag.setVisibility(visibility);
        }
    }

    public void requestGridFocus() {
        if (mGridView != null) {
            mGridView.requestFocus();
        }
    }

    public boolean hasFocus() {
        return mGridView != null && mGridView.hasFocus();
    }

    public void requestFlagFocus() {
        if (mGridViewFlag != null) {
            mGridViewFlag.requestFocus();
        }
    }
}

