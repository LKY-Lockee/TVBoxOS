package com.github.tvbox.osc.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.BounceInterpolator;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.base.BaseLazyFragment;
import com.github.tvbox.osc.bean.Movie;
import com.github.tvbox.osc.bean.MovieSort;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.ui.activity.DetailActivity;
import com.github.tvbox.osc.ui.activity.FastSearchActivity;
import com.github.tvbox.osc.ui.adapter.GridAdapter;
import com.github.tvbox.osc.ui.tv.widget.AutoFitGridLayoutManager;
import com.github.tvbox.osc.ui.tv.widget.LoadMoreView;
import com.github.tvbox.osc.util.FastClickCheckUtil;
import com.github.tvbox.osc.viewmodel.SourceViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.owen.tvrecyclerview.widget.TvRecyclerView;
import com.owen.tvrecyclerview.widget.V7LinearLayoutManager;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Stack;

/**
 * @author pj567
 * @date :2020/12/21
 * @description:
 */
public class GridFragment extends BaseLazyFragment {
    private final Stack<GridInfo> mGrids = new Stack<>(); //ui栈
    private MovieSort.SortData sortData = null;
    private TvRecyclerView mGridView;
    private SourceViewModel sourceViewModel;
    protected GridAdapter gridAdapter;
    private int page = 1;
    private int maxPage = 1;
    private boolean isLoad = false;
    private boolean isTop = true;
    private View focusedView = null;
    private HorizontalScrollView filterChipScrollView;
    private LinearLayout filterButtonContainer;
    private View divider;
    private BottomSheetDialog currentBottomSheet;
    private MovieSort.SortFilter currentExpandedFilter;

    public GridFragment(MovieSort.SortData sortData) {
        setArguments(sortData);
    }

    public GridFragment setArguments(MovieSort.SortData sortData) {
        this.sortData = sortData;
        return this;
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_grid;
    }

    @Override
    protected void init() {
        initView();
        initViewModel();
        initData();
    }

    private void changeView(String id, Boolean isFolder) {
        this.sortData.flag = isFolder ? "1" : "2";
        initView();
        this.sortData.id = id; // 修改sortData.id为新的ID
        initViewModel();
        initData();
    }

    public boolean isFolderMode() {
        return (getUITag() == '1');
    }

    // 获取当前页面UI的显示模式 ‘0’ 正常模式 '1' 文件夹模式 '2' 显示缩略图的文件夹模式
    public char getUITag() {
        return (sortData == null || sortData.flag == null || sortData.flag.isEmpty()) ? '0' : sortData.flag.charAt(0);
    }

    // 是否允许聚合搜索 sortData.flag的第二个字符为‘1’时允许聚搜
    public boolean enableFastSearch() {
        return sortData.flag == null || sortData.flag.length() < 2 || (sortData.flag.charAt(1) == '1');
    }

    // 保存当前页面
    private void saveCurrentView() {
        if (this.mGridView == null) return;
        GridInfo info = new GridInfo();
        info.sortID = this.sortData.id;
        info.mGridView = this.mGridView;
        info.gridAdapter = this.gridAdapter;
        info.page = this.page;
        info.maxPage = this.maxPage;
        info.isLoad = this.isLoad;
        info.focusedView = this.focusedView;
        this.mGrids.push(info);
    }

    // 丢弃当前页面，将页面还原成上一个保存的页面
    public boolean restoreView() {
        if (mGrids.empty()) return false;
        this.showSuccess();
        ((ViewGroup) mGridView.getParent()).removeView(this.mGridView); // 重父窗口移除当前控件
        GridInfo info = mGrids.pop();// 还原上次保存的控件
        this.sortData.id = info.sortID;
        this.mGridView = info.mGridView;
        this.gridAdapter = info.gridAdapter;
        this.page = info.page;
        this.maxPage = info.maxPage;
        this.isLoad = info.isLoad;
        this.focusedView = info.focusedView;
        this.mGridView.setVisibility(View.VISIBLE);
        if (mGridView != null) mGridView.requestFocus();
        return true;
    }

    // 更改当前页面
    private void createView() {
        this.saveCurrentView(); // 保存当前页面
        if (mGridView == null) { // 从layout中拿view
            mGridView = findViewById(R.id.mGridView);
        } else { // 复制当前view
            TvRecyclerView v3 = new TvRecyclerView(this.mContext);
            v3.setSpacingWithMargins(10, 10);
            v3.setLayoutParams(mGridView.getLayoutParams());
            v3.setPadding(mGridView.getPaddingLeft(), mGridView.getPaddingTop(), mGridView.getPaddingRight(), mGridView.getPaddingBottom());
            v3.setClipToPadding(mGridView.getClipToPadding());
            ((ViewGroup) mGridView.getParent()).addView(v3);
            mGridView.setVisibility(View.GONE);
            mGridView = v3;
            mGridView.setVisibility(View.VISIBLE);
        }
        mGridView.setHasFixedSize(true);
        gridAdapter = new GridAdapter(isFolderMode());
        this.page = 1;
        this.maxPage = 1;
        this.isLoad = false;
    }

    private void initView() {
        this.createView();

        // 初始化筛选 Split Button 组件
        filterChipScrollView = findViewById(R.id.filterChipScrollView);
        filterButtonContainer = findViewById(R.id.filterButtonContainer);
        divider = findViewById(R.id.divider);
        setupFilterChips();

        mGridView.setAdapter(gridAdapter);
        if (isFolderMode()) {
            mGridView.setLayoutManager(new V7LinearLayoutManager(this.mContext, 1, false));
        } else {
            // 使用自适应网格布局管理器
            int minColumnWidthDp = 150;
            mGridView.setLayoutManager(new AutoFitGridLayoutManager(mContext, minColumnWidthDp));
        }

        gridAdapter.setOnLoadMoreListener(() -> {
            gridAdapter.setEnableLoadMore(true);
            sourceViewModel.getList(sortData, page);
        }, mGridView);
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
        mGridView.setOnInBorderKeyEventListener((direction, focused) -> false);
        gridAdapter.setOnItemClickListener((adapter, view, position) -> {
            FastClickCheckUtil.check(view);
            Movie.Video video = gridAdapter.getData().get(position);
            if (video != null) {
                Bundle bundle = new Bundle();
                bundle.putString("id", video.id);
                bundle.putString("sourceKey", video.sourceKey);
                bundle.putString("title", video.name);
                if (video.tag != null && (video.tag.equals("folder") || video.tag.equals("cover"))) {
                    focusedView = view;
                    if (("12".indexOf(getUITag()) != -1)) {
                        changeView(video.id, video.tag.equals("folder"));
                    } else {
                        changeView(video.id, false);
                    }
                } else {
                    if (video.id == null || video.id.isEmpty() || video.id.startsWith("msearch:")) {
                        jumpActivity(FastSearchActivity.class, bundle);
                    } else {
                        bundle.putString("picture", video.pic);
                        jumpActivity(DetailActivity.class, bundle);
                    }
                }
            }
        });
        gridAdapter.setOnItemLongClickListener((adapter, view, position) -> {
            FastClickCheckUtil.check(view);
            Movie.Video video = gridAdapter.getData().get(position);
            if (video != null) {
                Bundle bundle = new Bundle();
                bundle.putString("id", video.id);
                bundle.putString("sourceKey", video.sourceKey);
                bundle.putString("title", video.name);
                jumpActivity(FastSearchActivity.class, bundle);
            }
            return true;
        });
        gridAdapter.setLoadMoreView(new LoadMoreView());
        setLoadSir2(mGridView);
    }

    private void initViewModel() {
        if (sourceViewModel != null) {
            return;
        }
        sourceViewModel = new ViewModelProvider(this).get(SourceViewModel.class);
        sourceViewModel.listResult.observe(this, absXml -> {
            if (absXml != null && absXml.movie != null && absXml.movie.videoList != null && !absXml.movie.videoList.isEmpty()) {
                if (page == 1) {
                    showSuccess();
                    isLoad = true;
                    gridAdapter.setNewData(absXml.movie.videoList);
                } else {
                    gridAdapter.addData(absXml.movie.videoList);
                }
                page++;
                maxPage = absXml.movie.pagecount;
                if (maxPage > 0 && page > maxPage) {
                    gridAdapter.loadMoreEnd();
                    gridAdapter.setEnableLoadMore(false);
                    if (page > 2) Toast.makeText(getContext(), "没有更多了", Toast.LENGTH_SHORT).show();
                } else {
                    gridAdapter.loadMoreComplete();
                    gridAdapter.setEnableLoadMore(true);
                }
            } else {
                if (page == 1) {
                    showEmpty();
                } else if (page > 2) {// 只有一页数据时不提示
                    Toast.makeText(getContext(), "没有更多了", Toast.LENGTH_SHORT).show();
                }
                gridAdapter.loadMoreEnd();
                gridAdapter.setEnableLoadMore(false);
            }
        });
    }

    public boolean isLoad() {
        return isLoad || !mGrids.empty(); //如果有缓存页的话也可以认为是加载了数据的
    }

    protected void initData() {
        showLoading();
        isLoad = false;
        scrollTop();
        setupFilterChips();
        toggleFilterColor();
        sourceViewModel.getList(sortData, page);
    }

    private void toggleFilterColor() {
        if (sortData != null && sortData.filters != null && !sortData.filters.isEmpty()) {
            int count = sortData.filterSelectCount();
            EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_FILTER_CHANGE, count));
        }
    }

    public boolean isTop() {
        return isTop;
    }

    public void scrollTop() {
        isTop = true;
        mGridView.scrollToPosition(0);
    }

    private void setupFilterChips() {
        if (sortData == null || sortData.filters == null || sortData.filters.isEmpty()) {
            filterChipScrollView.setVisibility(View.GONE);
            divider.setVisibility(View.GONE);
            return;
        }

        filterChipScrollView.setVisibility(View.VISIBLE);
        divider.setVisibility(View.VISIBLE);
        filterButtonContainer.removeAllViews();

        for (MovieSort.SortFilter filter : sortData.filters) {
            View splitButtonView = LayoutInflater.from(mContext).inflate(R.layout.item_filter_split_button, filterButtonContainer, false);

            MaterialButton mainButton = splitButtonView.findViewById(R.id.splitButtonMain);
            MaterialButton dropdownButton = splitButtonView.findViewById(R.id.splitButtonDropdown);

            String displayText = filter.name;
            String selectedValue = sortData.filterSelect.get(filter.key);

            if (selectedValue != null) {
                for (String key : filter.values.keySet()) {
                    String value = filter.values.get(key);
                    if (value != null && value.equals(selectedValue)) {
                        displayText = filter.name + ": " + key;
                        break;
                    }
                }
            }

            mainButton.setText(displayText);

            View.OnClickListener clickListener = v -> showFilterBottomSheet(filter);
            mainButton.setOnClickListener(clickListener);
            dropdownButton.setOnClickListener(clickListener);

            // 如果这个筛选项当前是展开状态，恢复其展开状态
            if (currentExpandedFilter != null && currentExpandedFilter.equals(filter)) {
                dropdownButton.setChecked(true);
            }

            filterButtonContainer.addView(splitButtonView);
        }
    }

    private void showFilterBottomSheet(MovieSort.SortFilter filter) {
        if (currentBottomSheet != null && currentBottomSheet.isShowing()) {
            currentBottomSheet.dismiss();
        }

        // 记录当前展开的筛选项
        currentExpandedFilter = filter;

        currentBottomSheet = new BottomSheetDialog(mContext);
        View view = LayoutInflater.from(mContext).inflate(R.layout.bottom_sheet_filter_options, null);

        TextView titleView = view.findViewById(R.id.filterTitle);
        titleView.setText(filter.name);

        ChipGroup chipGroup = view.findViewById(R.id.chipGroup);
        chipGroup.removeAllViews();

        String currentSelection = sortData.filterSelect.get(filter.key);

        // 添加选项 Chips
        ArrayList<String> displayValues = new ArrayList<>(filter.values.keySet());
        ArrayList<String> actualValues = new ArrayList<>(filter.values.values());

        for (int i = 0; i < displayValues.size(); i++) {
            String displayValue = displayValues.get(i);
            String actualValue = actualValues.get(i);

            Chip optionChip = new Chip(mContext);
            optionChip.setText(displayValue);
            optionChip.setCheckable(true);
            optionChip.setChecked(actualValue.equals(currentSelection));

            optionChip.setOnClickListener(v -> {
                if (actualValue.equals(sortData.filterSelect.get(filter.key))) {
                    sortData.filterSelect.remove(filter.key);
                } else {
                    sortData.filterSelect.put(filter.key, actualValue);
                }

                // 立即刷新数据，setupFilterChips 会自动恢复箭头展开状态
                setupFilterChips();
                forceRefresh();
            });

            chipGroup.addView(optionChip);
        }

        currentBottomSheet.setOnDismissListener(dialog -> {
            // 清除展开状态记录
            currentExpandedFilter = null;
            // 重新查找按钮（因为 setupFilterChips 可能已经重新创建了按钮）
            MaterialButton triggerButton = findDropdownButtonForFilter(filter);
            // 播放收起动画
            if (triggerButton != null) {
                triggerButton.setChecked(false);
            }
        });

        currentBottomSheet.setContentView(view);
        currentBottomSheet.show();

        // 播放展开动画
        MaterialButton triggerButton = findDropdownButtonForFilter(filter);
        if (triggerButton != null) {
            triggerButton.setChecked(true);
        }
    }

    @Nullable
    private MaterialButton findDropdownButtonForFilter(MovieSort.SortFilter filter) {
        if (sortData == null || sortData.filters == null) {
            return null;
        }

        int filterIndex = sortData.filters.indexOf(filter);
        if (filterIndex < 0 || filterIndex >= filterButtonContainer.getChildCount()) {
            return null;
        }

        View splitButtonView = filterButtonContainer.getChildAt(filterIndex);
        return splitButtonView.findViewById(R.id.splitButtonDropdown);
    }

    public void forceRefresh() {
        page = 1;
        initData();
    }

    private static class GridInfo {
        public String sortID = "";
        public TvRecyclerView mGridView;
        public GridAdapter gridAdapter;
        public int page = 1;
        public int maxPage = 1;
        public boolean isLoad = false;
        public View focusedView = null;
    }
}
