package com.github.tvbox.osc.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.BackPressProvider;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.base.ToolbarMenuProvider;
import com.github.tvbox.osc.server.ControlManager;
import com.github.tvbox.osc.ui.dialog.TipDialog;
import com.github.tvbox.osc.ui.fragment.CollectFragment;
import com.github.tvbox.osc.ui.fragment.HistoryFragment;
import com.github.tvbox.osc.ui.fragment.HomeFragment;
import com.github.tvbox.osc.ui.fragment.LiveFragment;
import com.github.tvbox.osc.ui.fragment.SearchFragment;
import com.github.tvbox.osc.util.AppManager;
import com.github.tvbox.osc.util.HawkConfig;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.orhanobut.hawk.Hawk;

enum Page {
    Home,
    History,
    Search,
    Collect,
    Live
}

public class HomeActivity extends BaseActivity {
    // Fragments
    private HomeFragment homeFragment;
    private HistoryFragment historyFragment;
    private SearchFragment searchFragment;
    private CollectFragment collectFragment;
    private LiveFragment liveFragment;
    // ----------------

    private final Handler mHandler = new Handler();
    private boolean useCacheConfig = false;
    private BottomNavigationView mBottomNavigation;
    private ViewPager2 viewPager;
    private Page currentMainPage = Page.Home;
    private long mExitTime = 0;
    private boolean dataInitOk = false;
    private boolean jarInitOk = false;
    private MaterialToolbar topAppBar;
    private AppBarLayout appBarLayout;

    // --- BaseActivity ---
    @Override
    protected int getLayoutResID() {
        return R.layout.activity_home;
    }

    @Override
    protected void init() {
        ControlManager.get().startServer();
        initView();
        useCacheConfig = false;
        initData();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackPress();
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        if (intent != null && intent.getExtras() != null) {
            Bundle bundle = intent.getExtras();
            if (bundle.getBoolean("openSearch", false)) {
                String searchTitle = bundle.getString("searchTitle");
                mHandler.postDelayed(() -> {
                    showFragment(Page.Search, false);
                    if (searchTitle != null && !searchTitle.isEmpty()) {
                        mHandler.postDelayed(() -> {
                            if (searchFragment != null && searchFragment.isAdded() && searchFragment.getContext() != null) {
                                searchFragment.search(searchTitle);
                            } else {
                                mHandler.postDelayed(() -> {
                                    if (searchFragment != null && searchFragment.isAdded() && searchFragment.getContext() != null) {
                                        searchFragment.search(searchTitle);
                                    }
                                }, 500);
                            }
                        }, 500);
                    }
                }, 100);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AppManager.getInstance().appExit(0);
        ControlManager.get().stopServer();
    }
    // ----------------

    // --- FragmentActivity ---
    @Override
    protected void onPause() {
        super.onPause();
        mHandler.removeCallbacksAndMessages(null);
    }
    // ----------------

    private void initView() {
        // 菜单
        appBarLayout = findViewById(R.id.appBarLayout);
        topAppBar = findViewById(R.id.appBar);
        topAppBar.inflateMenu(R.menu.home_toolbar_menu);
        topAppBar.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_settings) {
                jumpActivity(SettingsActivity.class);
                return true;
            }

            // 将菜单点击事件委托给实现了 ToolbarMenuProvider 的 Fragment
            Fragment currentFragment = getCurrentFragment();
            if (currentFragment instanceof ToolbarMenuProvider) {
                return ((ToolbarMenuProvider) currentFragment).onMenuItemClick(itemId);
            }

            return false;
        });

        // 设置 ViewPager2
        viewPager = findViewById(R.id.contentLayout);
        viewPager.setUserInputEnabled(false);

        // ViewPager2 页面改变监听
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);

                Page page = Page.values()[position];
                currentMainPage = page;

                // 同步底部导航栏
                switch (page) {
                    case Home:
                        mBottomNavigation.setSelectedItemId(R.id.navigation_main);
                        break;
                    case History:
                        mBottomNavigation.setSelectedItemId(R.id.navigation_history);
                        break;
                    case Search:
                        mBottomNavigation.setSelectedItemId(R.id.navigation_search);
                        break;
                    case Collect:
                        mBottomNavigation.setSelectedItemId(R.id.navigation_favourite);
                        break;
                    case Live:
                        mBottomNavigation.setSelectedItemId(R.id.navigation_live);
                        break;
                }

                // 强制展开 AppBar 和底部导航
                expandUI();

                mHandler.post(() -> updateAppBarForCurrentPage());
            }
        });

        // 设置底部导航栏监听器
        this.mBottomNavigation = findViewById(R.id.bottom_navigation);
        this.mBottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_main) {
                showFragment(Page.Home, true);
                return true;
            } else if (itemId == R.id.navigation_history) {
                showFragment(Page.History, true);
                return true;
            } else if (itemId == R.id.navigation_search) {
                showFragment(Page.Search, true);
                return true;
            } else if (itemId == R.id.navigation_favourite) {
                showFragment(Page.Collect, true);
                return true;
            } else if (itemId == R.id.navigation_live) {
                showFragment(Page.Live, true);
                return true;
            }
            return false;
        });

        setLoadSir(viewPager);
    }

    private void initData() {
        if (dataInitOk && jarInitOk) {
            viewPager.setAdapter(new HomePagerAdapter(this));
            showFragment(Page.Home, false);
            showSuccess();
            if (!useCacheConfig && Hawk.get(HawkConfig.DEFAULT_LOAD_LIVE, false)) {
                jumpActivity(LivePlayActivity.class);
            }
            return;
        }
        showLoading();
        if (dataInitOk && !jarInitOk) {
            if (!ApiConfig.get().getSpider().isEmpty()) {
                ApiConfig.get().loadJar(useCacheConfig, ApiConfig.get().getSpider(), new ApiConfig.LoadConfigCallback() {
                    @Override
                    public void success() {
                        jarInitOk = true;
                        mHandler.postDelayed(() -> initData(), 50);
                    }

                    @Override
                    public void notice(String msg) {
                        mHandler.post(() -> Toast.makeText(HomeActivity.this, msg, Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void error(String msg) {
                        jarInitOk = true;
                        dataInitOk = true;
                        mHandler.postDelayed(() -> {
                            Toast.makeText(HomeActivity.this, msg + "; 尝试加载最近一次的jar", Toast.LENGTH_SHORT).show();
                            initData();
                        }, 50);
                    }
                });
            }
            return;
        }
        ApiConfig.get().loadConfig(useCacheConfig, new ApiConfig.LoadConfigCallback() {
            TipDialog dialog = null;

            @Override
            public void notice(String msg) {
                mHandler.post(() -> Toast.makeText(HomeActivity.this, msg, Toast.LENGTH_SHORT).show());
            }

            @Override
            public void success() {
                dataInitOk = true;
                if (ApiConfig.get().getSpider().isEmpty()) {
                    jarInitOk = true;
                }
                mHandler.postDelayed(() -> initData(), 50);
            }

            @Override
            public void error(String msg) {
                if (msg.equalsIgnoreCase("-1")) {
                    mHandler.post(() -> {
                        dataInitOk = true;
                        jarInitOk = true;
                        initData();
                    });
                    return;
                }
                mHandler.post(() -> {
                    if (dialog == null)
                        dialog = new TipDialog(HomeActivity.this, msg, "重试", "取消", new TipDialog.OnListener() {
                            @Override
                            public void left() {
                                mHandler.post(() -> {
                                    initData();
                                    dialog.hide();
                                });
                            }

                            @Override
                            public void right() {
                                dataInitOk = true;
                                jarInitOk = true;
                                mHandler.post(() -> {
                                    initData();
                                    dialog.hide();
                                });
                            }

                            @Override
                            public void cancel() {
                                dataInitOk = true;
                                jarInitOk = true;
                                mHandler.post(() -> {
                                    initData();
                                    dialog.hide();
                                });
                            }
                        });
                    if (!dialog.isShowing())
                        dialog.show();
                });
            }
        }, this);
    }

    private Fragment getCurrentFragment() {
        int position = viewPager.getCurrentItem();
        Page page = Page.values()[position];
        return switch (page) {
            case Home -> homeFragment;
            case History -> historyFragment;
            case Search -> searchFragment;
            case Collect -> collectFragment;
            case Live -> liveFragment;
        };
    }

    private void showFragment(Page page, boolean smoothScroll) {
        currentMainPage = page;
        viewPager.setCurrentItem(page.ordinal(), smoothScroll);

        // 强制展开 AppBar 和底部导航
        expandUI();
    }

    public void expandUI() {
        expandAppBar();
        expandBottomNav();
    }

    public void expandAppBar() {
        if (appBarLayout != null) {
            appBarLayout.setExpanded(true, true);
        }
    }

    public void expandBottomNav() {
        if (mBottomNavigation != null) {
            mBottomNavigation.setVisibility(View.VISIBLE);
            if (mBottomNavigation.getTranslationY() != 0) {
                mBottomNavigation.animate()
                        .translationY(0)
                        .setDuration(300)
                        .start();
            }
        }
    }

    public void collapseBottomNav() {
        if (mBottomNavigation != null && mBottomNavigation.getVisibility() == View.VISIBLE) {
            mBottomNavigation.animate()
                    .translationY(mBottomNavigation.getHeight())
                    .setDuration(300)
                    .withEndAction(() -> {
                        if (mBottomNavigation != null) {
                            mBottomNavigation.setVisibility(View.GONE);
                        }
                    })
                    .start();
        }
    }

    public void switchToSearchAndSearch(String keyword) {
        showFragment(Page.Search, false);
        mHandler.postDelayed(() -> {
            if (searchFragment != null && searchFragment.isAdded() && searchFragment.getContext() != null) {
                searchFragment.search(keyword);
            } else {
                // 如果 Fragment 还没准备好，等待一段时间后重试
                mHandler.postDelayed(() -> {
                    if (searchFragment != null && searchFragment.isAdded() && searchFragment.getContext() != null) {
                        searchFragment.search(keyword);
                    }
                }, 500);
            }
        }, 200);
    }

    private void updateAppBarForCurrentPage() {
        Fragment fragment = getCurrentFragment();
        if (fragment == null) {
            return;
        }

        boolean enableScroll = false;
        if (fragment instanceof ToolbarMenuProvider provider) {
            enableScroll = provider.enableAppBarScroll();
        }

        // 设置 AppBar 滚动行为
        AppBarLayout.LayoutParams params = (AppBarLayout.LayoutParams) topAppBar.getLayoutParams();
        if (enableScroll) {
            // 允许随滑动收起
            params.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS | AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP);
        } else {
            // 不允许随滑动收起
            params.setScrollFlags(0);
        }
        topAppBar.setLayoutParams(params);

        // 清除菜单
        topAppBar.getMenu().clear();

        if (fragment instanceof ToolbarMenuProvider provider) {
            // 设置标题
            String title = provider.getToolbarTitle();
            if (title != null) {
                topAppBar.setTitle(title);
            } else {
                topAppBar.setTitle(R.string.app_name);
            }

            // 加载 Fragment 特定的菜单（会在设置按钮之前添加）
            int menuResId = provider.getMenuResId();
            if (menuResId != 0) {
                topAppBar.inflateMenu(menuResId);
            }
        } else {
            topAppBar.setTitle(R.string.app_name);
        }

        // 加载通用菜单
        topAppBar.inflateMenu(R.menu.home_toolbar_menu);
    }

    private void handleBackPress() {
        // 检查当前页面是否有自定义返回处理
        Fragment currentFragment = getCurrentFragment();
        if (currentFragment instanceof BackPressProvider backPressProvider) {
            if (backPressProvider.handleBackPress()) {
                return;
            }
        }

        // 如果不在主页，先返回主页
        if (currentMainPage != Page.Home) {
            // 返回主页
            mBottomNavigation.setSelectedItemId(R.id.navigation_main);
            return;
        }

        // 如果两次返回间隔小于 2000 毫秒，则退出应用
        if (System.currentTimeMillis() - mExitTime < 2000) {
            AppManager.getInstance().finishAllActivity();
            ControlManager.get().stopServer();
            finish();
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
        } else {
            // 否则仅提示用户，再按一次退出应用
            mExitTime = System.currentTimeMillis();
            Toast.makeText(mContext, "再按一次返回键退出应用", Toast.LENGTH_SHORT).show();
        }
    }

    private class HomePagerAdapter extends FragmentStateAdapter {
        public HomePagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            Page page = Page.values()[position];
            return switch (page) {
                case Home -> {
                    if (homeFragment == null) {
                        homeFragment = new HomeFragment();
                    }
                    yield homeFragment;
                }
                case History -> {
                    if (historyFragment == null) {
                        historyFragment = new HistoryFragment();
                    }
                    yield historyFragment;
                }
                case Search -> {
                    if (searchFragment == null) {
                        searchFragment = new SearchFragment();
                    }
                    yield searchFragment;
                }
                case Collect -> {
                    if (collectFragment == null) {
                        collectFragment = new CollectFragment();
                    }
                    yield collectFragment;
                }
                case Live -> {
                    if (liveFragment == null) {
                        liveFragment = new LiveFragment();
                    }
                    yield liveFragment;
                }
            };
        }

        @Override
        public int getItemCount() {
            return Page.values().length;
        }
    }
}
