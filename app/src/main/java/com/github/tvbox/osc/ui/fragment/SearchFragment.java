package com.github.tvbox.osc.ui.fragment;

import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.catvod.crawler.JsLoader;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.BackPressProvider;
import com.github.tvbox.osc.base.BaseLazyFragment;
import com.github.tvbox.osc.base.ToolbarMenuProvider;
import com.github.tvbox.osc.bean.AbsXml;
import com.github.tvbox.osc.bean.Movie;
import com.github.tvbox.osc.bean.SourceBean;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.event.ServerEvent;
import com.github.tvbox.osc.ui.activity.HomeActivity;
import com.github.tvbox.osc.ui.adapter.PinyinAdapter;
import com.github.tvbox.osc.util.FastClickCheckUtil;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.HistoryHelper;
import com.github.tvbox.osc.util.SearchHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.search.SearchBar;
import com.google.android.material.search.SearchView;
import com.google.android.material.tabs.TabLayout;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.AbsCallback;
import com.lzy.okgo.model.Response;
import com.orhanobut.hawk.Hawk;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class SearchFragment extends BaseLazyFragment implements BackPressProvider, ToolbarMenuProvider {
    private TabLayout mTabLayout;
    private SearchBar searchBar;
    private MaterialButton btnStopSearch;
    private CoordinatorLayout searchBarContainer;
    private SearchView searchView;
    private RecyclerView rvSearchWords;
    private PinyinAdapter wordAdapter;
    private static ArrayList<String> hots;
    private LinearProgressIndicator searchProgressIndicator;

    private SearchResultFragment resultFragment;
    private String currentSourceFilter = "all";

    private String searchTitle = "";
    private final HashMap<String, ArrayList<Movie.Video>> searchResults = new HashMap<>();
    private List<Runnable> pauseRunnable = null;
    private ExecutorService searchExecutorService = null;
    private final AtomicInteger allRunCount = new AtomicInteger(0);
    private int totalSourceCount = 0;
    private android.view.ViewTreeObserver.OnPreDrawListener preDrawListener = null;

    // --- BackPressProvider ---
    @Override
    public boolean handleBackPress() {
        if (!currentSourceFilter.equals("all")) {
            mTabLayout.selectTab(mTabLayout.getTabAt(0));
            return true;
        }
        return false;
    }
    // ----------------

    // --- BaseLazyFragment ---
    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_search;
    }

    @Override
    protected void init() {
        EventBus.getDefault().register(this);

        mTabLayout = rootView.findViewById(R.id.mTabLayout);
        searchBar = rootView.findViewById(R.id.search_bar);
        btnStopSearch = rootView.findViewById(R.id.btn_stop_search);
        searchBarContainer = rootView.findViewById(R.id.search_bar_container);
        searchView = rootView.findViewById(R.id.search_view);
        searchProgressIndicator = rootView.findViewById(R.id.search_progress);

        // 设置停止搜索按钮点击事件
        btnStopSearch.setOnClickListener(v -> cancel());

        // 动态定位搜索框到底部导航栏上方
        updateSearchBarPosition();

        searchView.addTransitionListener((searchView, previousState, newState) -> {
            if (getActivity() == null) return;

            if (mActivity instanceof HomeActivity homeActivity) {
                if (newState == SearchView.TransitionState.SHOWING) {
                    homeActivity.collapseBottomNav();
                } else if (newState == SearchView.TransitionState.HIDDEN) {
                    homeActivity.expandBottomNav();
                }
            }

            View spacerView = searchView.findViewById(R.id.open_search_view_status_bar_spacer);
            if (spacerView != null) {
                ViewGroup parent = (ViewGroup) spacerView.getParent();
                if (parent != null) {
                    parent.removeView(spacerView);
                }
            }
        });

        rvSearchWords = rootView.findViewById(R.id.rv_search_words);
        LinearLayout llSearchResult = rootView.findViewById(R.id.ll_search_result);

        initSearchViews();

        resultFragment = new SearchResultFragment();
        resultFragment.setOnRefreshListener(() -> {
            if (!TextUtils.isEmpty(searchTitle)) {
                search(searchTitle);
            }
        });
        getChildFragmentManager().beginTransaction()
                .replace(R.id.searchResultContainer, resultFragment)
                .commit();

        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                String sourceKey = (String) tab.getTag();
                if (sourceKey != null) {
                    filterBySource(sourceKey);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        mTabLayout.removeAllTabs();
        mTabLayout.setVisibility(View.GONE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cancel();
        try {
            if (searchExecutorService != null) {
                searchExecutorService.shutdownNow();
                searchExecutorService = null;
                JsLoader.stopAll();
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }

        // 移除监听器
        if (rootView != null && preDrawListener != null) {
            if (rootView.getViewTreeObserver().isAlive()) {
                rootView.getViewTreeObserver().removeOnPreDrawListener(preDrawListener);
            }
            preDrawListener = null;
        }

        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (pauseRunnable != null && !pauseRunnable.isEmpty()) {
            searchExecutorService = Executors.newFixedThreadPool(5);
            for (Runnable runnable : pauseRunnable) {
                searchExecutorService.execute(runnable);
            }
            pauseRunnable.clear();
            pauseRunnable = null;
        }
        // 确保搜索框位置正确
        updateSearchBarPosition();
    }
    // ----------------

    // --- ToolbarMenuProvider ---
    @Override
    public int getMenuResId() {
        return R.menu.search_fragment_menu;
    }

    @Override
    public boolean onMenuItemClick(int itemId) {
        if (itemId == R.id.action_clear_search_history) {
            showClearHistoryDialog();
            return true;
        }
        return false;
    }

    @Override
    public String getToolbarTitle() {
        return "搜索";
    }

    @Override
    public boolean enableAppBarScroll() {
        return true;
    }
    // ----------------

    private void initSearchViews() {
        rvSearchWords.setHasFixedSize(true);
        rvSearchWords.setLayoutManager(new LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false));
        wordAdapter = new PinyinAdapter();
        rvSearchWords.setAdapter(wordAdapter);

        wordAdapter.setOnItemClickListener((adapter, view, position) -> {
            FastClickCheckUtil.check(view);
            PinyinAdapter.SearchItem item = wordAdapter.getItem(position);
            if (item == null) return;
            String keyword = item.title;
            searchView.setText(keyword);
            searchView.hide();
            search(keyword);
        });

        // 设置长按监听器（仅对历史记录生效）
        wordAdapter.setOnItemLongClickListener((position, item) -> {
            if (item.type == 0) {
                showDeleteHistoryItemDialog(item.title, position);
            }
        });

        searchView.setupWithSearchBar(searchBar);
        searchView.getEditText().setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                String keyword = searchView.getText().toString().trim();
                searchView.hide();
                search(keyword);
                return true;
            }
            return false;
        });

        searchView.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String text = s.toString().trim();
                if (!text.isEmpty()) {
                    loadSearchSuggestions(text);
                } else {
                    loadHistoryAndHotWords();
                }
            }
        });

        searchView.addTransitionListener((searchView, previousState, newState) -> {
            if (newState == SearchView.TransitionState.SHOWING) {
                loadHistoryAndHotWords();
            }
        });
    }

    private void loadHistoryAndHotWords() {
        ArrayList<String> historyList = Hawk.get(HawkConfig.SEARCH_HISTORY, new ArrayList<>());

        ArrayList<PinyinAdapter.SearchItem> combinedList = new ArrayList<>();
        for (String s : historyList) {
            combinedList.add(new PinyinAdapter.SearchItem(s, 0));
        }

        if (hots != null && !hots.isEmpty()) {
            for (String s : hots) {
                combinedList.add(new PinyinAdapter.SearchItem(s, 1));
            }
            wordAdapter.setNewData(combinedList);
            return;
        }

        wordAdapter.setNewData(combinedList);

        //noinspection SpellCheckingInspection
        OkGo.<String>get("https://node.video.qq.com/x/api/hot_search")
                .params("channdlId", "0")
                .params("_", System.currentTimeMillis())
                .execute(new AbsCallback<>() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        try {
                            hots = new ArrayList<>();
                            JsonArray itemList = JsonParser.parseString(response.body())
                                    .getAsJsonObject().get("data").getAsJsonObject()
                                    .get("mapResult").getAsJsonObject()
                                    .get("0").getAsJsonObject()
                                    .get("listInfo").getAsJsonArray();
                            for (JsonElement ele : itemList) {
                                JsonObject obj = (JsonObject) ele;
                                hots.add(obj.get("title").getAsString().trim()
                                        .replaceAll("[<>《》\\-]", "").split(" ")[0]);
                            }
                            ArrayList<PinyinAdapter.SearchItem> updatedList = new ArrayList<>();
                            for (String s : historyList) {
                                updatedList.add(new PinyinAdapter.SearchItem(s, 0));
                            }
                            for (String s : hots) {
                                updatedList.add(new PinyinAdapter.SearchItem(s, 1));
                            }
                            wordAdapter.setNewData(updatedList);
                        } catch (Throwable th) {
                            th.printStackTrace();
                        }
                    }

                    @Override
                    public String convertResponse(okhttp3.Response response) throws Throwable {
                        return Objects.requireNonNull(response.body()).string();
                    }
                });
    }

    private void loadSearchSuggestions(String key) {
        OkGo.get("https://tv.aiseet.atianqi.com/i-tvbin/qtv_video/search/get_search_smart_box")
                .params("format", "json")
                .params("page_num", 0)
                .params("page_size", 20)
                .params("key", key)
                .execute(new AbsCallback<>() {
                    @Override
                    public void onSuccess(Response response) {
                        try {
                            ArrayList<PinyinAdapter.SearchItem> suggestions = new ArrayList<>();
                            String result = (String) response.body();
                            Gson gson = new Gson();
                            JsonElement json = gson.fromJson(result, JsonElement.class);
                            JsonArray groupDataArr = json.getAsJsonObject()
                                    .get("data").getAsJsonObject()
                                    .get("search_data").getAsJsonObject()
                                    .get("vecGroupData").getAsJsonArray()
                                    .get(0).getAsJsonObject()
                                    .get("group_data").getAsJsonArray();
                            for (JsonElement groupDataElement : groupDataArr) {
                                JsonObject groupData = groupDataElement.getAsJsonObject();
                                String keywordTxt = groupData.getAsJsonObject("dtReportInfo")
                                        .getAsJsonObject("reportData")
                                        .get("keyword_txt").getAsString();
                                suggestions.add(new PinyinAdapter.SearchItem(keywordTxt.trim(), 2));
                            }
                            wordAdapter.setNewData(suggestions);
                            rvSearchWords.smoothScrollToPosition(0);
                        } catch (Throwable th) {
                            th.printStackTrace();
                        }
                    }

                    @Override
                    public String convertResponse(okhttp3.Response response) throws Throwable {
                        return Objects.requireNonNull(response.body()).string();
                    }
                });
    }

    private void updateSearchBarPosition() {
        if (rootView == null || getActivity() == null) return;
        rootView.post(() -> {
            if (getActivity() == null) return;
            View bottomNav = getActivity().findViewById(R.id.bottom_navigation);
            if (bottomNav != null && searchBarContainer != null) {
                CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) searchBarContainer.getLayoutParams();
                params.gravity = android.view.Gravity.NO_GRAVITY;
                params.setMargins(params.leftMargin, 0, params.rightMargin, 0);
                searchBarContainer.setLayoutParams(params);

                if (preDrawListener != null) {
                    rootView.getViewTreeObserver().removeOnPreDrawListener(preDrawListener);
                }

                // 使用 OnPreDrawListener 监听每一帧
                preDrawListener = () -> {
                    if (searchBarContainer == null || getActivity() == null) return true;
                    if (rootView != null) {
                        // 获取底部导航栏在屏幕中的位置
                        int[] navLocation = new int[2];
                        bottomNav.getLocationInWindow(navLocation);
                        int navTopInScreen = navLocation[1];

                        // 获取rootView在屏幕中的位置
                        int[] rootLocation = new int[2];
                        rootView.getLocationInWindow(rootLocation);
                        int rootTopInScreen = rootLocation[1];

                        // 计算searchBarContainer应该在rootView中的Y坐标
                        int searchBarHeight = searchBarContainer.getHeight();
                        int targetY = navTopInScreen - rootTopInScreen - searchBarHeight;

                        // 使用setY设置绝对位置
                        if (Math.abs(searchBarContainer.getY() - targetY) > 0.5f) {
                            searchBarContainer.setY(targetY);
                        }
                    }
                    return true;
                };
                rootView.getViewTreeObserver().addOnPreDrawListener(preDrawListener);
            }
        });
    }

    private void showEmptyState() {
        mTabLayout.removeAllTabs();
        mTabLayout.setVisibility(View.GONE);
        currentSourceFilter = "all";
        if (searchProgressIndicator != null) {
            searchProgressIndicator.setVisibility(View.GONE);
        }
        if (resultFragment != null && resultFragment.isAdded()) {
            resultFragment.updateData(new ArrayList<>());
        }
    }

    private void showStopSearchButton() {
        if (btnStopSearch == null) return;

        btnStopSearch.setVisibility(View.VISIBLE);
        btnStopSearch.animate()
                .alpha(1f)
                .setDuration(200)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();
    }

    private void hideStopSearchButton() {
        if (btnStopSearch == null) return;

        btnStopSearch.animate()
                .alpha(0f)
                .setDuration(200)
                .setInterpolator(new android.view.animation.AccelerateInterpolator())
                .withEndAction(() -> {
                    if (btnStopSearch != null) {
                        btnStopSearch.setVisibility(View.GONE);
                    }
                })
                .start();
    }

    public void search(String keyword) {
        if (TextUtils.isEmpty(keyword)) {
            if (mContext != null && isAdded()) {
                Toast.makeText(mContext, "输入内容不能为空", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        if (!isAdded() || mContext == null || searchBar == null) {
            return;
        }

        this.searchTitle = keyword;
        searchBar.setText(keyword);

        HistoryHelper.setSearchHistory(keyword);

        searchResults.clear();
        showEmptyState();

        hideSoftInput();

        searchResult();
    }

    private void cancel() {
        OkGo.getInstance().cancelTag("search");

        try {
            if (searchExecutorService != null) {
                searchExecutorService.shutdownNow();
                searchExecutorService = null;
                JsLoader.stopAll();
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }

        allRunCount.set(0);

        if (searchProgressIndicator != null) {
            searchProgressIndicator.setVisibility(View.GONE);
        }

        hideStopSearchButton();
    }

    private void searchResult() {
        try {
            if (searchExecutorService != null) {
                searchExecutorService.shutdownNow();
                searchExecutorService = null;
                JsLoader.stopAll();
            }
        } catch (Throwable th) {
            th.printStackTrace();
        } finally {
            allRunCount.set(0);
        }

        searchExecutorService = Executors.newFixedThreadPool(5);
        List<SourceBean> searchRequestList = new ArrayList<>(ApiConfig.get().getSourceBeanList());
        SourceBean home = ApiConfig.get().getHomeSourceBean();
        searchRequestList.remove(home);
        searchRequestList.add(0, home);

        HashMap<String, String> mCheckSources = SearchHelper.getSourcesForSearch();
        ArrayList<String> siteKey = new ArrayList<>();
        for (SourceBean bean : searchRequestList) {
            if (!bean.isSearchable()) {
                continue;
            }
            if (mCheckSources != null && !mCheckSources.containsKey(bean.getKey())) {
                continue;
            }
            siteKey.add(bean.getKey());
            allRunCount.incrementAndGet();
        }

        if (siteKey.isEmpty()) {
            Toast.makeText(mContext, "没有指定搜索源", Toast.LENGTH_SHORT).show();
            return;
        }

        totalSourceCount = siteKey.size();
        if (searchProgressIndicator != null) {
            searchProgressIndicator.setMax(totalSourceCount);
            searchProgressIndicator.setProgress(0);
            searchProgressIndicator.setVisibility(View.VISIBLE);
        }

        showStopSearchButton();

        com.github.tvbox.osc.viewmodel.SourceViewModel sourceViewModel =
                new androidx.lifecycle.ViewModelProvider(requireActivity()).get(com.github.tvbox.osc.viewmodel.SourceViewModel.class);

        for (String key : siteKey) {
            searchExecutorService.execute(() -> sourceViewModel.getSearch(key, searchTitle));
        }
    }

    private void searchData(AbsXml absXml) {
        boolean hasNewResults = false;

        if (absXml != null && absXml.movie != null && absXml.movie.videoList != null && !absXml.movie.videoList.isEmpty()) {
            String sourceKey = absXml.movie.videoList.get(0).sourceKey;
            ArrayList<Movie.Video> sourceResults = searchResults.computeIfAbsent(sourceKey, k -> new ArrayList<>());

            int oldSize = sourceResults.size();
            for (Movie.Video video : absXml.movie.videoList) {
                if (matchSearchResult(video.name, searchTitle)) {
                    sourceResults.add(video);
                }
            }

            hasNewResults = sourceResults.size() > oldSize;
        }

        int count = allRunCount.decrementAndGet();

        if (searchProgressIndicator != null && totalSourceCount > 0) {
            int searchedCount = totalSourceCount - count;
            searchProgressIndicator.setProgress(searchedCount);
        }

        if (hasNewResults) {
            if (mTabLayout.getTabCount() <= 0) {
                createTabsFromResults();
            } else {
                updateTabsWithNewResults();
            }
        }

        if (count <= 0) {
            if (searchProgressIndicator != null) {
                searchProgressIndicator.setVisibility(View.GONE);
            }

            hideStopSearchButton();

            if (mTabLayout.getTabCount() <= 0) {
                createTabsFromResults();
            }
            cancel();
        }
    }

    private void filterBySource(String sourceKey) {
        currentSourceFilter = sourceKey;

        ArrayList<Movie.Video> filteredResults;
        if ("all".equals(sourceKey)) {
            filteredResults = new ArrayList<>();
            for (ArrayList<Movie.Video> videos : searchResults.values()) {
                filteredResults.addAll(videos);
            }
        } else {
            filteredResults = searchResults.get(sourceKey);
            if (filteredResults == null) {
                filteredResults = new ArrayList<>();
            }
        }

        if (resultFragment != null && resultFragment.isAdded()) {
            resultFragment.updateData(filteredResults);
        }
    }

    private boolean matchSearchResult(String name, String searchTitle) {
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(searchTitle)) return false;
        searchTitle = searchTitle.trim();
        String[] arr = searchTitle.split("\\s+");
        int matchNum = 0;
        for (String one : arr) {
            if (name.contains(one)) matchNum++;
        }
        return matchNum == arr.length;
    }

    private void createTabsFromResults() {
        mTabLayout.removeAllTabs();

        for (String sourceKey : searchResults.keySet()) {
            ArrayList<Movie.Video> videos = searchResults.get(sourceKey);
            if (videos != null && !videos.isEmpty()) {
                SourceBean source = ApiConfig.get().getSource(sourceKey);
                if (source != null) {
                    TabLayout.Tab tab = mTabLayout.newTab();
                    tab.setText(source.getName() + " (" + videos.size() + ")");
                    tab.setTag(sourceKey);
                    mTabLayout.addTab(tab);
                }
            }
        }

        if (mTabLayout.getTabCount() > 0) {
            TabLayout.Tab allTab = mTabLayout.newTab();
            allTab.setText("全部");
            allTab.setTag("all");
            mTabLayout.addTab(allTab, 0);
            mTabLayout.setVisibility(View.VISIBLE);

            TabLayout.Tab firstTab = mTabLayout.getTabAt(0);
            if (firstTab != null) {
                firstTab.select();
            }
            filterBySource("all");
        } else {
            mTabLayout.setVisibility(View.GONE);
        }
    }

    private void updateTabsWithNewResults() {
        for (String sourceKey : searchResults.keySet()) {
            boolean tabExists = false;
            for (int i = 0; i < mTabLayout.getTabCount(); i++) {
                TabLayout.Tab tab = mTabLayout.getTabAt(i);
                if (tab != null && sourceKey.equals(tab.getTag())) {
                    ArrayList<Movie.Video> videos = searchResults.get(sourceKey);
                    if (videos != null) {
                        SourceBean source = ApiConfig.get().getSource(sourceKey);
                        if (source != null) {
                            tab.setText(source.getName() + " (" + videos.size() + ")");
                        }
                    }
                    tabExists = true;
                    break;
                }
            }

            if (!tabExists) {
                ArrayList<Movie.Video> videos = searchResults.get(sourceKey);
                if (videos != null && !videos.isEmpty()) {
                    SourceBean source = ApiConfig.get().getSource(sourceKey);
                    if (source != null) {
                        TabLayout.Tab tab = mTabLayout.newTab();
                        tab.setText(source.getName() + " (" + videos.size() + ")");
                        tab.setTag(sourceKey);
                        mTabLayout.addTab(tab);
                    }
                }
            }
        }

        filterBySource(currentSourceFilter);
    }

    private void showDeleteHistoryItemDialog(String keyword, int position) {
        if (getActivity() == null) return;

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(getActivity())
                .setTitle("删除搜索记录")
                .setMessage("确定要删除「" + keyword + "」吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    // 从 Hawk 中获取历史记录
                    ArrayList<String> historyList = Hawk.get(HawkConfig.SEARCH_HISTORY, new ArrayList<>());
                    historyList.remove(keyword);
                    Hawk.put(HawkConfig.SEARCH_HISTORY, historyList);

                    // 从 Adapter 中移除
                    wordAdapter.remove(position);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showClearHistoryDialog() {
        if (getActivity() == null) return;

        ArrayList<String> historyList = Hawk.get(HawkConfig.SEARCH_HISTORY, new ArrayList<>());
        if (historyList.isEmpty()) {
            Toast.makeText(mContext, "暂无搜索记录", Toast.LENGTH_SHORT).show();
            return;
        }

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(getActivity())
                .setTitle("清空搜索记录")
                .setMessage("确定要清空所有搜索记录吗？")
                .setPositiveButton("清空", (dialog, which) -> {
                    Hawk.delete(HawkConfig.SEARCH_HISTORY);

                    ArrayList<PinyinAdapter.SearchItem> newList = new ArrayList<>();
                    if (hots != null && !hots.isEmpty()) {
                        for (String s : hots) {
                            newList.add(new PinyinAdapter.SearchItem(s, 1));
                        }
                    }
                    wordAdapter.setNewData(newList);

                    android.widget.Toast.makeText(mContext, "已清空搜索记录", android.widget.Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void hideSoftInput() {
        try {
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) mContext.getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null && searchView != null) {
                imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void server(ServerEvent event) {
        if (event.type == ServerEvent.SERVER_SEARCH) {
            String title = (String) event.obj;
            search(title);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void refresh(RefreshEvent event) {
        if (event.type == RefreshEvent.TYPE_SEARCH_RESULT) {
            try {
                searchData(event.obj == null ? null : (AbsXml) event.obj);
            } catch (Exception e) {
                searchData(null);
            }
        }
    }
}
