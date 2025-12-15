package com.github.tvbox.osc.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.viewpager2.widget.ViewPager2;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.BackPressProvider;
import com.github.tvbox.osc.base.BaseLazyFragment;
import com.github.tvbox.osc.bean.AbsSortXml;
import com.github.tvbox.osc.bean.MovieSort;
import com.github.tvbox.osc.bean.SourceBean;
import com.github.tvbox.osc.ui.activity.HomeActivity;
import com.github.tvbox.osc.ui.adapter.HomePageAdapter;
import com.github.tvbox.osc.ui.adapter.SelectDialogAdapter;
import com.github.tvbox.osc.ui.dialog.SelectDialog;
import com.github.tvbox.osc.util.DefaultConfig;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.viewmodel.SourceViewModel;
import com.google.android.material.tabs.TabLayout;
import com.orhanobut.hawk.Hawk;
import com.owen.tvrecyclerview.widget.TvRecyclerView;
import com.owen.tvrecyclerview.widget.V7GridLayoutManager;

import java.util.ArrayList;
import java.util.List;

import me.jessyan.autosize.utils.AutoSizeUtils;

public class HomeFragment extends BaseLazyFragment implements BackPressProvider {
    private TabLayout mTabLayout;
    private ViewPager2 mViewPager;
    private final List<BaseLazyFragment> fragments = new ArrayList<>();
    private List<MovieSort.SortData> sortDataList = new ArrayList<>();
    private HomePageAdapter adapter;
    private int currentSelected = 0;
    private OnTabReselectedListener tabReselectedListener;
    private SelectDialog<SourceBean> mSiteSwitchDialog;
    private SourceViewModel sourceViewModel;

    public HomeFragment() {
        setOnTabReselectedListener((position, sortData, fragment) -> {
            // 如果是主页标签（id为"my0"），弹出站点切换
            if ("my0".equals(sortData.id)) {
                showSiteSwitch();
                return;
            }
            // 如果有筛选项，弹出筛选
            if ((fragment instanceof GridFragment) && !sortData.filters.isEmpty()) {
                ((GridFragment) fragment).showFilter();
            }
        });
    }

    // --- BackPressProvider ---
    @Override
    public boolean handleBackPress() {
        if (getCurrentFragment() instanceof GridFragment grid) {
            // 如果当前 Fragment 能恢复之前保存的 UI 状态，则直接返回
            if (grid.restoreView()) {
                return true;
            }
            // 如果当前不是第一个界面，则返回到第一项
            if (getCurrentPosition() != 0) {
                setCurrentPosition(0);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
    // ----------------

    // --- BaseLazyFragment ---
    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_home;
    }

    @Override
    protected void init() {
        mTabLayout = rootView.findViewById(R.id.mTabLayout);
        mViewPager = rootView.findViewById(R.id.mViewPager);
        mViewPager.setSaveEnabled(false);

        initViewModel();

        if (!fragments.isEmpty()) {
            setupViewPager();
        } else {
            loadData();
        }
    }
    // ----------------

    private void showSiteSwitch() {
        List<SourceBean> sites = ApiConfig.get().getSwitchSourceBeanList();
        if (sites.isEmpty()) return;
        int select = sites.indexOf(ApiConfig.get().getHomeSourceBean());
        if (select < 0 || select >= sites.size()) select = 0;
        if (mSiteSwitchDialog == null) {
            mSiteSwitchDialog = new SelectDialog<>(mContext);
            TvRecyclerView tvRecyclerView = mSiteSwitchDialog.findViewById(R.id.list);
            // 根据 sites 数量动态计算列数
            int spanCount = (int) Math.floor(sites.size() / 20.0);
            spanCount = Math.min(spanCount, 2);
            tvRecyclerView.setLayoutManager(new V7GridLayoutManager(mSiteSwitchDialog.getContext(), spanCount + 1));
            // 设置对话框宽度
            ConstraintLayout cl_root = mSiteSwitchDialog.findViewById(R.id.cl_root);
            ViewGroup.LayoutParams clp = cl_root.getLayoutParams();
            clp.width = AutoSizeUtils.mm2px(mSiteSwitchDialog.getContext(), 380 + 200 * spanCount);
            mSiteSwitchDialog.setTip("请选择首页数据源");
        }
        mSiteSwitchDialog.setAdapter(new SelectDialogAdapter.SelectDialogInterface<>() {
            @Override
            public void click(SourceBean value, int pos) {
                ApiConfig.get().setSourceBean(value);
                refreshHome();
            }

            @Override
            public String getDisplay(SourceBean val) {
                return val.getName();
            }
        }, new DiffUtil.ItemCallback<>() {
            @Override
            public boolean areItemsTheSame(@NonNull SourceBean oldItem, @NonNull SourceBean newItem) {
                return oldItem == newItem;
            }

            @Override
            public boolean areContentsTheSame(@NonNull SourceBean oldItem, @NonNull SourceBean newItem) {
                return oldItem.getKey().equals(newItem.getKey());
            }
        }, sites, select);
        mSiteSwitchDialog.show();
    }

    private void refreshHome() {
        Intent intent = new Intent(getContext(), HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        Bundle bundle = new Bundle();
        bundle.putBoolean("useCache", true);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    private void initViewModel() {
        sourceViewModel = new ViewModelProvider(requireActivity()).get(SourceViewModel.class);
        sourceViewModel.sortResult.observe(getViewLifecycleOwner(), this::setDataFromAbsSortXml);
    }

    public void loadData() {
        if (sourceViewModel != null) {
            sourceViewModel.getSort(ApiConfig.get().getHomeSourceBean().getKey());
        }
    }

    public void setDataFromAbsSortXml(AbsSortXml absXml) {
        fragments.clear();

        List<MovieSort.SortData> adjustedSortList;
        if (absXml != null && absXml.classes != null && absXml.classes.sortList != null) {
            adjustedSortList = DefaultConfig.adjustSort(
                    ApiConfig.get().getHomeSourceBean().getKey(),
                    absXml.classes.sortList,
                    true
            );
        } else {
            adjustedSortList = DefaultConfig.adjustSort(
                    ApiConfig.get().getHomeSourceBean().getKey(),
                    new ArrayList<>(),
                    true
            );
        }

        this.sortDataList = adjustedSortList;

        if (!sortDataList.isEmpty()) {
            for (MovieSort.SortData data : sortDataList) {
                if (data.id.equals("my0")) {
                    if (Hawk.get(HawkConfig.HOME_REC, 0) == 1 && absXml != null && absXml.videoList != null && !absXml.videoList.isEmpty()) {
                        fragments.add(new UserFragment(data));
                    } else {
                        fragments.add(new UserFragment(null));
                    }
                } else {
                    fragments.add(new GridFragment(data));
                }
            }
        }

        if (mViewPager != null && mTabLayout != null) {
            setupViewPager();
        }
    }

    private void setupViewPager() {
        if (getActivity() == null) {
            return;
        }

        // 只有在适配器为空时才创建新的适配器
        if (mViewPager.getAdapter() == null) {
            adapter = new HomePageAdapter(getActivity(), fragments);
            mViewPager.setAdapter(adapter);

            // ViewPager页面改变监听（只设置一次）
            mViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected(position);
                    if (mTabLayout != null && position >= 0 && position < mTabLayout.getTabCount()) {
                        TabLayout.Tab tab = mTabLayout.getTabAt(position);
                        if (tab != null && !tab.isSelected()) {
                            tab.select();
                        }
                        currentSelected = position;
                    }
                }
            });
        } else {
            // 如果适配器已存在，只需通知数据改变
            adapter.notifyDataSetChanged();
        }

        // 设置当前页面
        mViewPager.setCurrentItem(currentSelected, false);

        // 设置TabLayout
        updateTabLayout();

        // TabLayout监听（移除旧的监听器，避免重复）
        mTabLayout.clearOnTabSelectedListeners();
        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                currentSelected = position;
                mViewPager.setCurrentItem(position, true);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                if (position >= 0 && position < fragments.size() && position < sortDataList.size()) {
                    if (tabReselectedListener != null) {
                        tabReselectedListener.onTabReselected(position, sortDataList.get(position), fragments.get(position));
                    }
                }
            }
        });
    }

    private void updateTabLayout() {
        mTabLayout.removeAllTabs();
        for (MovieSort.SortData sortData : sortDataList) {
            TabLayout.Tab tab = mTabLayout.newTab();
            tab.setText(sortData.name);
            mTabLayout.addTab(tab);
        }

        if (currentSelected < mTabLayout.getTabCount()) {
            TabLayout.Tab tab = mTabLayout.getTabAt(currentSelected);
            if (tab != null) {
                tab.select();
            }
        }
    }

    public void setOnTabReselectedListener(OnTabReselectedListener listener) {
        this.tabReselectedListener = listener;
    }

    public int getCurrentPosition() {
        return currentSelected;
    }

    public void setCurrentPosition(int position) {
        this.currentSelected = position;
        if (mViewPager != null) {
            mViewPager.setCurrentItem(position, false);
        }
    }

    public BaseLazyFragment getCurrentFragment() {
        if (currentSelected >= 0 && currentSelected < fragments.size()) {
            return fragments.get(currentSelected);
        }
        return null;
    }

    public interface OnTabReselectedListener {
        void onTabReselected(int position, MovieSort.SortData sortData, BaseLazyFragment fragment);
    }
}

