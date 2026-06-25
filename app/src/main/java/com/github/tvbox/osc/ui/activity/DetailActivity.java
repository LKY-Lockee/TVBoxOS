package com.github.tvbox.osc.ui.activity;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.bean.Movie;
import com.github.tvbox.osc.bean.VodInfo;
import com.github.tvbox.osc.cache.RoomDataManger;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.ui.fragment.DetailTabInfoFragment;
import com.github.tvbox.osc.ui.fragment.DetailTabPlaylistFragment;
import com.github.tvbox.osc.ui.fragment.PlayFragment;
import com.github.tvbox.osc.ui.fragment.SearchFragment;
import com.github.tvbox.osc.util.SubtitleHelper;
import com.github.tvbox.osc.viewmodel.SourceViewModel;
import com.google.android.material.tabs.TabLayout;
import com.lzy.okgo.OkGo;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

/**
 * @author pj567
 * @date :2020/12/22
 * @description:
 */
public class DetailActivity extends BaseActivity {
    public String vodId;
    public String sourceKey;
    public String firstSourceKey;
    // preview
    VodInfo previewVodInfo = null;
    private boolean isFullscreen = false;
    private FragmentContainerView llPlayerFragmentContainer;
    private View topLayout;
    private androidx.viewpager2.widget.ViewPager2 viewPager;
    private PlayFragment playFragment = null;
    private SourceViewModel sourceViewModel;
    private Movie.Video mVideo;
    private VodInfo vodInfo;
    private String preFlag = "";
    private String vod_picture = "";

    private TabLayout tabLayout;

    private com.github.tvbox.osc.ui.fragment.SearchFragment searchFragment;
    private DetailTabInfoFragment tabInfoFragment;
    private DetailTabPlaylistFragment tabPlaylistFragment;
    private boolean hasSearchedOnce = false;

    private void refreshFlag(View itemView, int position) {
        if (vodInfo == null || vodInfo.seriesFlags == null || position >= vodInfo.seriesFlags.size()) {
            return;
        }

        String newFlag = vodInfo.seriesFlags.get(position).name;
        if (!vodInfo.playFlag.equals(newFlag)) {
            for (int i = 0; i < vodInfo.seriesFlags.size(); i++) {
                VodInfo.VodSeriesFlag flag = vodInfo.seriesFlags.get(i);
                if (flag.name.equals(vodInfo.playFlag)) {
                    flag.selected = false;
                    break;
                }
            }
            VodInfo.VodSeriesFlag flag = vodInfo.seriesFlags.get(position);
            flag.selected = true;
            if (vodInfo.seriesMap.get(vodInfo.playFlag) != null &&
                    vodInfo.seriesMap.get(vodInfo.playFlag).size() > vodInfo.playIndex) {
                vodInfo.seriesMap.get(vodInfo.playFlag).get(vodInfo.playIndex).selected = false;
            }
            vodInfo.playFlag = newFlag;
            tabPlaylistFragment.refreshList(vodInfo);
        }
    }

    private void onSeriesSelected(int position) {
        if (vodInfo == null || vodInfo.seriesMap.get(vodInfo.playFlag) == null) {
            return;
        }

        if (position == -1) {
            vodInfo.reverseSort = !vodInfo.reverseSort;
            vodInfo.reverse();
            tabPlaylistFragment.refreshList(vodInfo);
            return;
        }

        List<VodInfo.VodSeries> seriesList = vodInfo.seriesMap.get(vodInfo.playFlag);
        if (seriesList.isEmpty() || position >= seriesList.size()) {
            return;
        }

        boolean reload = false;
        boolean isAllowFull = false;

        int oldIndex = vodInfo.playIndex;

        if (vodInfo.playIndex != position) {
            vodInfo.playIndex = position;
            reload = true;
        }

        if (!preFlag.isEmpty() && !vodInfo.playFlag.equals(preFlag)) {
            reload = true;
            isAllowFull = true;
        }

        tabPlaylistFragment.updateSeriesSelection(oldIndex, position);

        if (reload) {
            jumpToPlay();
        }
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.activity_detail;
    }

    @Override
    protected void init() {
        EventBus.getDefault().register(this);
        initView();
        initViewModel();
        initData();

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        View videoContainer = findViewById(R.id.topLayout);
        ViewCompat.setOnApplyWindowInsetsListener(videoContainer, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), insets.top, v.getPaddingRight(), v.getPaddingBottom());
            return windowInsets;
        });

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isFullscreen) {
                    if (playFragment.onBackPressed())
                        return;
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    List<VodInfo.VodSeries> list = vodInfo.seriesMap.get(vodInfo.playFlag);
                    if (list != null) {
                        tabPlaylistFragment.setSeriesGroupVisibility(list.size() > 1 ? View.VISIBLE : View.GONE);
                    }
                    tabPlaylistFragment.requestGridFocus();
                    return;
                }
                if (playFragment != null) playFragment.setPlayTitle(false);

                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
                setEnabled(true);
            }
        });
    }

    private void initView() {
        ConstraintLayout llLayout = findViewById(R.id.llLayout);
        topLayout = findViewById(R.id.topLayout);
        llPlayerFragmentContainer = findViewById(R.id.previewPlayer);

        View tabInfoView = getLayoutInflater().inflate(R.layout.fragment_detail_tab_info, null);
        View tabPlaylistView = getLayoutInflater().inflate(R.layout.fragment_detail_tab_playlist, null);

        preFlag = "";
        playFragment = new PlayFragment();
        getSupportFragmentManager().beginTransaction().add(R.id.previewPlayer, playFragment).commit();
        getSupportFragmentManager().beginTransaction().show(playFragment).commitAllowingStateLoss();

        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        tabInfoFragment = new DetailTabInfoFragment();
        tabPlaylistFragment = new DetailTabPlaylistFragment();
        searchFragment = new SearchFragment();

        tabInfoFragment.setContentView(tabInfoView);
        tabPlaylistFragment.setContentView(tabPlaylistView);

        tabPlaylistFragment.setOnSeriesFlagSelectedListener((flagName, position) -> refreshFlag(null, position));
        tabPlaylistFragment.setOnSeriesSelectedListener(this::onSeriesSelected);

        FragmentStateAdapter adapter = new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                return switch (position) {
                    case 0 -> tabInfoFragment;
                    case 1 -> tabPlaylistFragment;
                    case 2 -> searchFragment;
                    default -> new Fragment();
                };
            }

            @Override
            public int getItemCount() {
                return tabLayout.getTabCount();
            }

            @Override
            public int getItemViewType(int position) {
                return position;
            }
        };
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(adapter.getItemCount());

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
                viewPager.post(() -> {
                    if (tab.getPosition() == 2 && mVideo != null && searchFragment != null && !hasSearchedOnce) {
                        searchFragment.search(mVideo.name);
                        hasSearchedOnce = true;
                    }
                });
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        viewPager.registerOnPageChangeCallback(new androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                tabLayout.selectTab(tabLayout.getTabAt(position));
            }
        });

        setLoadSir(llLayout);
    }

    private void jumpToPlay() {
        if (vodInfo != null && !vodInfo.seriesMap.get(vodInfo.playFlag).isEmpty()) {
            preFlag = vodInfo.playFlag;
            //更新播放地址
            tabInfoFragment.setPlayUrl(vodInfo.seriesMap.get(vodInfo.playFlag).get(vodInfo.playIndex).url);
            Bundle bundle = new Bundle();
            //保存历史
            insertVod(firstSourceKey, vodInfo);
            bundle.putString("sourceKey", sourceKey);
            App.getInstance().vodInfo = vodInfo;
            if (previewVodInfo == null) {
                try {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(bos);
                    oos.writeObject(vodInfo);
                    oos.flush();
                    oos.close();
                    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
                    previewVodInfo = (VodInfo) ois.readObject();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (previewVodInfo != null) {
                previewVodInfo.playerCfg = vodInfo.playerCfg;
                previewVodInfo.playFlag = vodInfo.playFlag;
                previewVodInfo.playIndex = vodInfo.playIndex;
                previewVodInfo.seriesMap = vodInfo.seriesMap;
                App.getInstance().vodInfo = previewVodInfo;
            }
            playFragment.setData(bundle);
        }
    }

    private void initViewModel() {
        sourceViewModel = new ViewModelProvider(this).get(SourceViewModel.class);
        sourceViewModel.detailResult.observe(this, absXml -> {
            if (absXml != null && absXml.movie != null && absXml.movie.videoList != null && !absXml.movie.videoList.isEmpty()) {
                showSuccess();

                WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
                controller.setAppearanceLightStatusBars(false);

                if (!TextUtils.isEmpty(absXml.msg) && !absXml.msg.equals("数据列表")) {
                    Toast.makeText(DetailActivity.this, absXml.msg, Toast.LENGTH_SHORT).show();
                    showEmpty();
                    return;
                }
                mVideo = absXml.movie.videoList.get(0);
                mVideo.id = vodId;
                hasSearchedOnce = false;
                if (TextUtils.isEmpty(mVideo.name)) mVideo.name = "TVBox";
                vodInfo = new VodInfo();
                if ((mVideo.pic == null || mVideo.pic.isEmpty()) && !vod_picture.isEmpty()) {
                    mVideo.pic = vod_picture;
                }
                vodInfo.setVideo(mVideo);
                vodInfo.sourceKey = mVideo.sourceKey;
                sourceKey = mVideo.sourceKey;

                tabInfoFragment.setVideoInfo(mVideo, sourceKey, firstSourceKey, vodId);

                if (vodInfo.seriesMap != null && !vodInfo.seriesMap.isEmpty()) {

                    VodInfo vodInfoRecord = RoomDataManger.getVodInfo(sourceKey, vodId);
                    // 读取历史记录
                    if (vodInfoRecord != null) {
                        vodInfo.playIndex = Math.max(vodInfoRecord.playIndex, 0);
                        vodInfo.playFlag = vodInfoRecord.playFlag;
                        vodInfo.playerCfg = vodInfoRecord.playerCfg;
                        vodInfo.reverseSort = vodInfoRecord.reverseSort;
                    } else {
                        vodInfo.playIndex = 0;
                        vodInfo.playFlag = null;
                        vodInfo.playerCfg = "";
                        vodInfo.reverseSort = false;
                    }

                    if (vodInfo.reverseSort) {
                        vodInfo.reverse();
                    }

                    if (vodInfo.playFlag == null || !vodInfo.seriesMap.containsKey(vodInfo.playFlag))
                        vodInfo.playFlag = (String) vodInfo.seriesMap.keySet().toArray()[0];

                    //设置播放地址
                    tabInfoFragment.setPlayUrl(vodInfo.seriesMap.get(vodInfo.playFlag).get(0).url);

                    tabPlaylistFragment.setVodInfo(vodInfo);

                    jumpToPlay();
                    llPlayerFragmentContainer.setVisibility(View.VISIBLE);
                    toggleSubtitleTextSize();
                } else {
                    tabPlaylistFragment.setPlaylistVisibility(View.GONE);
                }
            } else {
                showEmpty();
                llPlayerFragmentContainer.setVisibility(View.GONE);
            }
        });
    }

    private void initData() {
        Intent intent = getIntent();
        if (intent != null && intent.getExtras() != null) {
            Bundle bundle = intent.getExtras();
            vod_picture = bundle.getString("picture", "");
            loadDetail(bundle.getString("id", null), bundle.getString("sourceKey", ""));
        }
    }

    private void loadDetail(String vid, String key) {
        if (vid != null) {
            vodId = vid;
            sourceKey = key;
            firstSourceKey = key;
            showLoading();
            sourceViewModel.getDetail(sourceKey, vodId);
            tabInfoFragment.updateCollectButton();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void refresh(RefreshEvent event) {
        if (event.type == RefreshEvent.TYPE_REFRESH) {
            if (event.obj != null) {
                if (event.obj instanceof Integer) {
                    int index = (int) event.obj;
                    int oldIndex = vodInfo.playIndex;
                    vodInfo.playIndex = index;
                    tabPlaylistFragment.updateSeriesSelection(oldIndex, index);
                    //保存历史
                    insertVod(firstSourceKey, vodInfo);
                } else if (event.obj instanceof JSONObject) {
                    vodInfo.playerCfg = event.obj.toString();
                    //保存历史
                    insertVod(firstSourceKey, vodInfo);
                } else if (event.obj instanceof String) {
                    String url = event.obj.toString();
                    //设置更新播放地址
                    tabInfoFragment.setPlayUrl(url);
                }
            }
        } else if (event.type == RefreshEvent.TYPE_QUICK_SEARCH_SELECT) {
            if (event.obj != null) {
                Movie.Video video = (Movie.Video) event.obj;
                loadDetail(video.id, video.sourceKey);
            }
        }
    }

    private void insertVod(String sourceKey, VodInfo vodInfo) {
        try {
            vodInfo.playNote = vodInfo.seriesMap.get(vodInfo.playFlag).get(vodInfo.playIndex).name;
        } catch (Throwable th) {
            vodInfo.playNote = "";
        }
        RoomDataManger.insertVodRecord(sourceKey, vodInfo);
        EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_HISTORY_REFRESH));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //noinspection SpellCheckingInspection
        OkGo.getInstance().cancelTag("fenci");
        OkGo.getInstance().cancelTag("detail");
        EventBus.getDefault().unregister(this);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event != null && playFragment != null && isFullscreen) {
            if (playFragment.dispatchKeyEvent(event)) {
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event != null && playFragment != null && isFullscreen) {
            if (playFragment.onKeyDown(keyCode, event)) {
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (event != null && playFragment != null && isFullscreen) {
            if (playFragment.onKeyUp(keyCode, event)) {
                return true;
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            isFullscreen = true;
            hideSystemUI();
            updatePlayerLayoutForFullscreen(true);
            toggleSubtitleTextSize();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            isFullscreen = false;
            showSystemUI();
            updatePlayerLayoutForFullscreen(false);
            toggleSubtitleTextSize();
        }
    }

    private void updatePlayerLayoutForFullscreen(boolean fullscreen) {
        if (llPlayerFragmentContainer == null || topLayout == null) return;

        if (fullscreen) {
            // 隐藏所有其他UI元素
            tabLayout.setVisibility(View.GONE);
            viewPager.setVisibility(View.GONE);

            // 修改topLayout的布局参数，使其填充整个屏幕
            ConstraintLayout.LayoutParams topParams =
                    (ConstraintLayout.LayoutParams) topLayout.getLayoutParams();
            topParams.width = ConstraintLayout.LayoutParams.MATCH_PARENT;
            topParams.height = ConstraintLayout.LayoutParams.MATCH_PARENT;
            topParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
            topParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
            topParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
            topParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
            topLayout.setLayoutParams(topParams);

            // 修改previewPlayer的布局参数，使其填充整个topLayout
            ConstraintLayout.LayoutParams playerParams =
                    (ConstraintLayout.LayoutParams) llPlayerFragmentContainer.getLayoutParams();
            playerParams.width = ConstraintLayout.LayoutParams.MATCH_PARENT;
            playerParams.height = ConstraintLayout.LayoutParams.MATCH_PARENT;
            playerParams.dimensionRatio = null;
            playerParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
            playerParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
            playerParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
            playerParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
            llPlayerFragmentContainer.setLayoutParams(playerParams);
        } else {
            // 恢复UI元素显示
            tabLayout.setVisibility(View.VISIBLE);
            viewPager.setVisibility(View.VISIBLE);

            // 恢复topLayout的布局参数
            ConstraintLayout.LayoutParams topParams =
                    (ConstraintLayout.LayoutParams) topLayout.getLayoutParams();
            topParams.width = ConstraintLayout.LayoutParams.MATCH_PARENT;
            topParams.height = ConstraintLayout.LayoutParams.WRAP_CONTENT;
            topParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
            topParams.bottomToBottom = ConstraintLayout.LayoutParams.UNSET;
            topParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
            topParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
            topLayout.setLayoutParams(topParams);

            // 恢复previewPlayer的布局参数
            ConstraintLayout.LayoutParams playerParams =
                    (ConstraintLayout.LayoutParams) llPlayerFragmentContainer.getLayoutParams();
            playerParams.width = 0;
            playerParams.height = 0;
            playerParams.dimensionRatio = "H,16:9";
            playerParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
            playerParams.bottomToBottom = ConstraintLayout.LayoutParams.UNSET;
            playerParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
            playerParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
            llPlayerFragmentContainer.setLayoutParams(playerParams);
        }

        topLayout.requestLayout();
        llPlayerFragmentContainer.requestLayout();
    }

    private void hideSystemUI() {
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }

    private void showSystemUI() {
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.show(WindowInsetsCompat.Type.systemBars());
        controller.setAppearanceLightStatusBars(false);
    }

    void toggleSubtitleTextSize() {
        int subtitleTextSize = SubtitleHelper.getTextSize(this);
        if (!isFullscreen) {
            subtitleTextSize = (int) (subtitleTextSize * 0.6);
        }
        EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_SUBTITLE_SIZE_CHANGE, subtitleTextSize));
    }
}
