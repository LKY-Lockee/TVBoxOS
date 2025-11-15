package com.github.tvbox.osc.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.bean.IJKCode;
import com.github.tvbox.osc.bean.SettingItem;
import com.github.tvbox.osc.bean.SourceBean;
import com.github.tvbox.osc.ui.adapter.ApiHistoryDialogAdapter;
import com.github.tvbox.osc.ui.adapter.SelectDialogAdapter;
import com.github.tvbox.osc.ui.adapter.SettingM3Adapter;
import com.github.tvbox.osc.ui.dialog.AboutDialog;
import com.github.tvbox.osc.ui.dialog.ApiDialog;
import com.github.tvbox.osc.ui.dialog.ApiHistoryDialog;
import com.github.tvbox.osc.ui.dialog.BackupDialog;
import com.github.tvbox.osc.ui.dialog.SearchRemoteTvDialog;
import com.github.tvbox.osc.ui.dialog.SelectDialog;
import com.github.tvbox.osc.ui.dialog.XWalkInitDialog;
import com.github.tvbox.osc.util.AppManager;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.HistoryHelper;
import com.github.tvbox.osc.util.OkGoHelper;
import com.github.tvbox.osc.util.PlayerHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.orhanobut.hawk.Hawk;

import org.greenrobot.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author pj567
 * @date :2020/12/23
 * @description:
 */
public class SettingActivity extends BaseActivity {
    public static DevModeCallback callback = null;

    // 远程TV搜索相关静态变量（从 ModelSettingFragment 迁移）
    public static SearchRemoteTvDialog loadingSearchRemoteTvDialog;
    public static List<String> remoteTvHostList;
    public static boolean foundRemoteTv;

    private final Handler mHandler = new Handler();
    String devMode = "";
    private final Runnable mDevModeRun = () -> devMode = "";

    // Material 3 组件
    private RecyclerView recyclerView;
    private SettingM3Adapter adapter;
    private MaterialToolbar toolbar;
    private final List<SettingItem> settingItems = new ArrayList<>();

    private String homeSourceKey;
    private String currentApi;
    private int homeRec;
    private int dnsOpt;
    private String currentLiveApi;

    @Override
    protected int getLayoutResID() {
        return R.layout.activity_setting_m3;
    }

    @Override
    protected void init() {
        initView();
        initData();
    }

    private void initView() {
        toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SettingM3Adapter();
        recyclerView.setAdapter(adapter);
    }

    private void initData() {
        currentApi = Hawk.get(HawkConfig.API_URL, "");
        homeSourceKey = ApiConfig.get().getHomeSourceBean().getKey();
        homeRec = Hawk.get(HawkConfig.HOME_REC, 0);
        dnsOpt = Hawk.get(HawkConfig.DOH_URL, 0);
        currentLiveApi = Hawk.get(HawkConfig.LIVE_API_URL, "");

        buildSettingItems();
        adapter.setItems(settingItems);
    }

    private void buildSettingItems() {
        settingItems.clear();

        // 数据源设置
        settingItems.add(SettingItem.createCategory("数据源"));

        settingItems.add(SettingItem.createPreference(
                "配置地址",
                Hawk.get(HawkConfig.API_URL, ""),
                item -> showApiDialog()
        ).setSummary("设置应用数据源配置地址"));

        settingItems.add(SettingItem.createPreference(
                "配置历史",
                "",
                item -> showApiHistoryDialog()
        ).setSummary("查看和选择历史配置"));

        settingItems.add(SettingItem.createPreference(
                "首页站源",
                ApiConfig.get().getHomeSourceBean().getName(),
                item -> showHomeSourceDialog()
        ).setSummary("选择默认首页数据源"));

        settingItems.add(SettingItem.createPreference(
                "安全DNS",
                OkGoHelper.dnsHttpsList.get(Hawk.get(HawkConfig.DOH_URL, 0)),
                item -> showDnsDialog()
        ).setSummary("选择DNS解析方式"));

        // 播放设置
        settingItems.add(SettingItem.createCategory("播放"));

        settingItems.add(SettingItem.createPreference(
                "默认播放器",
                PlayerHelper.getPlayerName(Hawk.get(HawkConfig.PLAY_TYPE, 0)),
                item -> showPlayerDialog()
        ).setSummary("选择视频播放器"));

        settingItems.add(SettingItem.createPreference(
                "IJK解码方式",
                Hawk.get(HawkConfig.IJK_CODEC, "硬解码"),
                item -> showCodecDialog()
        ).setSummary("IJK播放器解码方式"));

        settingItems.add(SettingItem.createPreference(
                "渲染方式",
                PlayerHelper.getRenderName(Hawk.get(HawkConfig.PLAY_RENDER, 0)),
                item -> showRenderDialog()
        ).setSummary("视频画面渲染方式"));

        settingItems.add(SettingItem.createPreference(
                "画面缩放",
                PlayerHelper.getScaleName(Hawk.get(HawkConfig.PLAY_SCALE, 0)),
                item -> showScaleDialog()
        ).setSummary("默认画面缩放比例"));

        settingItems.add(SettingItem.createSwitch(
                "IJK缓存播放",
                Hawk.get(HawkConfig.IJK_CACHE_PLAY, false),
                item -> {
                    Hawk.put(HawkConfig.IJK_CACHE_PLAY, item.isSwitchState());
                    Toast.makeText(this, item.isSwitchState() ? "已开启" : "已关闭", Toast.LENGTH_SHORT).show();
                }
        ).setSummary("开启后可边下边播"));

        settingItems.add(SettingItem.createSwitch(
                "去广告",
                Hawk.get(HawkConfig.M3U8_PURIFY, false),
                item -> {
                    Hawk.put(HawkConfig.M3U8_PURIFY, item.isSwitchState());
                    Toast.makeText(this, item.isSwitchState() ? "已开启" : "已关闭", Toast.LENGTH_SHORT).show();
                }
        ).setSummary("过滤M3U8视频广告"));

        // 界面设置
        settingItems.add(SettingItem.createCategory("界面"));

        settingItems.add(SettingItem.createPreference(
                "首页推荐",
                getHomeRecName(Hawk.get(HawkConfig.HOME_REC, 0)),
                item -> showHomeRecDialog()
        ).setSummary("首页显示推荐内容"));

        settingItems.add(SettingItem.createPreference(
                "下次进入",
                Hawk.get(HawkConfig.DEFAULT_LOAD_LIVE, false) ? "直播" : "点播",
                item -> showDefaultLoadDialog()
        ).setSummary("设置启动后默认页面"));

        settingItems.add(SettingItem.createPreference(
                "搜索展示",
                getSearchView(Hawk.get(HawkConfig.SEARCH_VIEW, 0)),
                item -> showSearchViewDialog()
        ).setSummary("搜索结果展示方式"));

        settingItems.add(SettingItem.createPreference(
                "历史记录数",
                HistoryHelper.getHistoryNumName(Hawk.get(HawkConfig.HISTORY_NUM, 0)),
                item -> showHistoryNumDialog()
        ).setSummary("保留历史记录的数量"));

        settingItems.add(SettingItem.createSwitch(
                "显示预览",
                Hawk.get(HawkConfig.SHOW_PREVIEW, true),
                item -> {
                    Hawk.put(HawkConfig.SHOW_PREVIEW, item.isSwitchState());
                }
        ).setSummary("显示视频缩略图预览"));

        settingItems.add(SettingItem.createSwitch(
                "聚合搜索",
                Hawk.get(HawkConfig.FAST_SEARCH_MODE, false),
                item -> {
                    Hawk.put(HawkConfig.FAST_SEARCH_MODE, item.isSwitchState());
                }
        ).setSummary("开启多源聚合搜索"));

        // 高级设置
        settingItems.add(SettingItem.createCategory("高级"));

        settingItems.add(SettingItem.createPreference(
                "WebView类型",
                Hawk.get(HawkConfig.PARSE_WEBVIEW, true) ? "系统自带" : "XWalkView",
                item -> showWebViewDialog()
        ).setSummary("解析使用的WebView类型"));

        settingItems.add(SettingItem.createSwitch(
                "调试模式",
                Hawk.get(HawkConfig.DEBUG_OPEN, false),
                item -> {
                    Hawk.put(HawkConfig.DEBUG_OPEN, item.isSwitchState());
                }
        ).setSummary("开启应用调试信息"));

        // 其他
        settingItems.add(SettingItem.createCategory("其他"));

        settingItems.add(SettingItem.createPreference(
                "备份与恢复",
                "",
                item -> showBackupDialog()
        ).setSummary("备份或恢复应用数据"));

        settingItems.add(SettingItem.createPreference(
                "关于",
                "",
                item -> showAboutDialog()
        ).setSummary("应用版本和信息"));
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            mHandler.removeCallbacks(mDataRunnable);
            int keyCode = event.getKeyCode();
            if (keyCode == KeyEvent.KEYCODE_0) {
                mHandler.removeCallbacks(mDevModeRun);
                devMode += "0";
                mHandler.postDelayed(mDevModeRun, 200);
                if (devMode.length() >= 4) {
                    if (callback != null) {
                        callback.onChange();
                    }
                }
            }
        } else if (event.getAction() == KeyEvent.ACTION_UP) {
            mHandler.postDelayed(mDataRunnable, 200);
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onBackPressed() {
        if (currentApi.equals(Hawk.get(HawkConfig.API_URL, ""))) {
            if (dnsOpt != Hawk.get(HawkConfig.DOH_URL, 0)) {
                AppManager.getInstance().finishAllActivity();
                jumpActivity(HomeActivity.class);
            } else if ((homeSourceKey != null && !homeSourceKey.equals(Hawk.get(HawkConfig.HOME_API, ""))) || homeRec != Hawk.get(HawkConfig.HOME_REC, 0)) {
                jumpActivity(HomeActivity.class, createBundle());
            } else if (!currentLiveApi.equals(Hawk.get(HawkConfig.LIVE_API_URL, ""))) {
                jumpActivity(HomeActivity.class);
            }
        } else {
            AppManager.getInstance().finishAllActivity();
            jumpActivity(HomeActivity.class);
        }
        super.onBackPressed();
    }

    private Bundle createBundle() {
        Bundle bundle = new Bundle();
        bundle.putBoolean("useCache", true);
        return bundle;
    }

    // ========== 对话框显示方法 ==========

    private void showApiDialog() {
        ApiDialog dialog = new ApiDialog(this);
        EventBus.getDefault().register(dialog);
        dialog.setOnListener(api -> {
            Hawk.put(HawkConfig.API_URL, api);
            updateSettingItem("配置地址", api);
        });
        dialog.setOnDismissListener(dialog1 -> EventBus.getDefault().unregister(dialog1));
        dialog.show();
    }

    private void showApiHistoryDialog() {
        ArrayList<String> history = Hawk.get(HawkConfig.API_HISTORY, new ArrayList<>());
        if (history.isEmpty()) {
            Toast.makeText(this, "暂无历史配置", Toast.LENGTH_SHORT).show();
            return;
        }
        String current = Hawk.get(HawkConfig.API_URL, "");
        int idx = history.contains(current) ? history.indexOf(current) : 0;

        ApiHistoryDialog dialog = new ApiHistoryDialog(this);
        dialog.setTip("历史配置列表");
        dialog.setAdapter(new ApiHistoryDialogAdapter.SelectDialogInterface() {
            @Override
            public void click(String value) {
                Hawk.put(HawkConfig.API_URL, value);
                Hawk.put(HawkConfig.LIVE_API_URL, value);
                HistoryHelper.setLiveApiHistory(value);
                updateSettingItem("配置地址", value);
                dialog.dismiss();
            }

            @Override
            public void del(String value, ArrayList<String> data) {
                Hawk.put(HawkConfig.API_HISTORY, data);
            }
        }, history, idx);
        dialog.show();
    }

    private void showHomeSourceDialog() {
        List<SourceBean> sites = ApiConfig.get().getSwitchSourceBeanList();
        if (sites.isEmpty()) {
            Toast.makeText(this, "无可切换数据源", Toast.LENGTH_SHORT).show();
            return;
        }

        SelectDialog<SourceBean> dialog = new SelectDialog<>(this);
        dialog.setTip("请选择首页数据源");
        int select = sites.indexOf(ApiConfig.get().getHomeSourceBean());
        if (select < 0) select = 0;

        dialog.setAdapter(new SelectDialogAdapter.SelectDialogInterface<SourceBean>() {
            @Override
            public void click(SourceBean value, int pos) {
                ApiConfig.get().setSourceBean(value);
                updateSettingItem("首页站源", value.getName());

                Intent intent = new Intent(SettingActivity.this, HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                Bundle bundle = new Bundle();
                bundle.putBoolean("useCache", true);
                intent.putExtras(bundle);
                startActivity(intent);
            }

            @Override
            public String getDisplay(SourceBean val) {
                return val.getName();
            }
        }, new DiffUtil.ItemCallback<SourceBean>() {
            @Override
            public boolean areItemsTheSame(@NonNull @NotNull SourceBean oldItem, @NonNull @NotNull SourceBean newItem) {
                return oldItem == newItem;
            }

            @Override
            public boolean areContentsTheSame(@NonNull @NotNull SourceBean oldItem, @NonNull @NotNull SourceBean newItem) {
                return oldItem.getKey().equals(newItem.getKey());
            }
        }, sites, select);
        dialog.show();
    }

    private void showDnsDialog() {
        int dohUrl = Hawk.get(HawkConfig.DOH_URL, 0);
        SelectDialog<String> dialog = new SelectDialog<>(this);
        dialog.setTip("请选择安全DNS");
        dialog.setAdapter(new SelectDialogAdapter.SelectDialogInterface<String>() {
            @Override
            public void click(String value, int pos) {
                Hawk.put(HawkConfig.DOH_URL, pos);
                updateSettingItem("安全DNS", OkGoHelper.dnsHttpsList.get(pos));
            }

            @Override
            public String getDisplay(String val) {
                return val;
            }
        }, new DiffUtil.ItemCallback<String>() {
            @Override
            public boolean areItemsTheSame(@NonNull @NotNull String oldItem, @NonNull @NotNull String newItem) {
                return oldItem.equals(newItem);
            }

            @Override
            public boolean areContentsTheSame(@NonNull @NotNull String oldItem, @NonNull @NotNull String newItem) {
                return oldItem.equals(newItem);
            }
        }, OkGoHelper.dnsHttpsList, dohUrl);
        dialog.show();
    }

    private void showPlayerDialog() {
        int playerType = Hawk.get(HawkConfig.PLAY_TYPE, 0);
        int defaultPos = 0;
        ArrayList<Integer> players = PlayerHelper.getExistPlayerTypes();
        ArrayList<Integer> renders = new ArrayList<>();
        for (int p = 0; p < players.size(); p++) {
            renders.add(p);
            if (players.get(p) == playerType) {
                defaultPos = p;
            }
        }

        SelectDialog<Integer> dialog = new SelectDialog<>(this);
        dialog.setTip("请选择默认播放器");
        dialog.setAdapter(new SelectDialogAdapter.SelectDialogInterface<Integer>() {
            @Override
            public void click(Integer value, int pos) {
                Integer thisPlayerType = players.get(pos);
                Hawk.put(HawkConfig.PLAY_TYPE, thisPlayerType);
                updateSettingItem("默认播放器", PlayerHelper.getPlayerName(thisPlayerType));
                PlayerHelper.init();
            }

            @Override
            public String getDisplay(Integer val) {
                Integer playerType = players.get(val);
                return PlayerHelper.getPlayerName(playerType);
            }
        }, new DiffUtil.ItemCallback<Integer>() {
            @Override
            public boolean areItemsTheSame(@NonNull @NotNull Integer oldItem, @NonNull @NotNull Integer newItem) {
                return oldItem.intValue() == newItem.intValue();
            }

            @Override
            public boolean areContentsTheSame(@NonNull @NotNull Integer oldItem, @NonNull @NotNull Integer newItem) {
                return oldItem.intValue() == newItem.intValue();
            }
        }, renders, defaultPos);
        dialog.show();
    }

    private void showCodecDialog() {
        List<IJKCode> ijkCodes = ApiConfig.get().getIjkCodes();
        if (ijkCodes == null || ijkCodes.isEmpty()) {
            Toast.makeText(this, "无可用解码方式", Toast.LENGTH_SHORT).show();
            return;
        }

        int defaultPos = 0;
        String ijkSel = Hawk.get(HawkConfig.IJK_CODEC, "硬解码");
        for (int j = 0; j < ijkCodes.size(); j++) {
            if (ijkSel.equals(ijkCodes.get(j).getName())) {
                defaultPos = j;
                break;
            }
        }

        SelectDialog<IJKCode> dialog = new SelectDialog<>(this);
        dialog.setTip("请选择IJK解码");
        dialog.setAdapter(new SelectDialogAdapter.SelectDialogInterface<IJKCode>() {
            @Override
            public void click(IJKCode value, int pos) {
                value.selected(true);
                updateSettingItem("IJK解码方式", value.getName());
            }

            @Override
            public String getDisplay(IJKCode val) {
                return val.getName();
            }
        }, new DiffUtil.ItemCallback<IJKCode>() {
            @Override
            public boolean areItemsTheSame(@NonNull @NotNull IJKCode oldItem, @NonNull @NotNull IJKCode newItem) {
                return oldItem == newItem;
            }

            @Override
            public boolean areContentsTheSame(@NonNull @NotNull IJKCode oldItem, @NonNull @NotNull IJKCode newItem) {
                return oldItem.getName().equals(newItem.getName());
            }
        }, ijkCodes, defaultPos);
        dialog.show();
    }

    public interface DevModeCallback {
        void onChange();
    }

    // ========== 更多对话框方法 ==========

    private void showRenderDialog() {
        int defaultPos = Hawk.get(HawkConfig.PLAY_RENDER, 0);
        ArrayList<Integer> renders = new ArrayList<>();
        renders.add(0);
        renders.add(1);

        SelectDialog<Integer> dialog = new SelectDialog<>(this);
        dialog.setTip("请选择默认渲染方式");
        dialog.setAdapter(new SelectDialogAdapter.SelectDialogInterface<Integer>() {
            @Override
            public void click(Integer value, int pos) {
                Hawk.put(HawkConfig.PLAY_RENDER, value);
                updateSettingItem("渲染方式", PlayerHelper.getRenderName(value));
                PlayerHelper.init();
            }

            @Override
            public String getDisplay(Integer val) {
                return PlayerHelper.getRenderName(val);
            }
        }, new DiffUtil.ItemCallback<Integer>() {
            @Override
            public boolean areItemsTheSame(@NonNull @NotNull Integer oldItem, @NonNull @NotNull Integer newItem) {
                return oldItem.intValue() == newItem.intValue();
            }

            @Override
            public boolean areContentsTheSame(@NonNull @NotNull Integer oldItem, @NonNull @NotNull Integer newItem) {
                return oldItem.intValue() == newItem.intValue();
            }
        }, renders, defaultPos);
        dialog.show();
    }

    private void showScaleDialog() {
        int defaultPos = Hawk.get(HawkConfig.PLAY_SCALE, 0);
        ArrayList<Integer> scales = new ArrayList<>();
        scales.add(0);
        scales.add(1);
        scales.add(2);
        scales.add(3);
        scales.add(4);
        scales.add(5);

        SelectDialog<Integer> dialog = new SelectDialog<>(this);
        dialog.setTip("请选择默认画面缩放");
        dialog.setAdapter(new SelectDialogAdapter.SelectDialogInterface<Integer>() {
            @Override
            public void click(Integer value, int pos) {
                Hawk.put(HawkConfig.PLAY_SCALE, value);
                updateSettingItem("画面缩放", PlayerHelper.getScaleName(value));
            }

            @Override
            public String getDisplay(Integer val) {
                return PlayerHelper.getScaleName(val);
            }
        }, new DiffUtil.ItemCallback<Integer>() {
            @Override
            public boolean areItemsTheSame(@NonNull @NotNull Integer oldItem, @NonNull @NotNull Integer newItem) {
                return oldItem.intValue() == newItem.intValue();
            }

            @Override
            public boolean areContentsTheSame(@NonNull @NotNull Integer oldItem, @NonNull @NotNull Integer newItem) {
                return oldItem.intValue() == newItem.intValue();
            }
        }, scales, defaultPos);
        dialog.show();
    }

    private void showHomeRecDialog() {
        int defaultPos = Hawk.get(HawkConfig.HOME_REC, 0);
        ArrayList<Integer> types = new ArrayList<>();
        types.add(0);
        types.add(1);
        types.add(2);

        SelectDialog<Integer> dialog = new SelectDialog<>(this);
        dialog.setTip("请选择首页列表数据");
        dialog.setAdapter(new SelectDialogAdapter.SelectDialogInterface<Integer>() {
            @Override
            public void click(Integer value, int pos) {
                Hawk.put(HawkConfig.HOME_REC, value);
                updateSettingItem("首页推荐", getHomeRecName(value));
            }

            @Override
            public String getDisplay(Integer val) {
                return getHomeRecName(val);
            }
        }, new DiffUtil.ItemCallback<Integer>() {
            @Override
            public boolean areItemsTheSame(@NonNull @NotNull Integer oldItem, @NonNull @NotNull Integer newItem) {
                return oldItem.intValue() == newItem.intValue();
            }

            @Override
            public boolean areContentsTheSame(@NonNull @NotNull Integer oldItem, @NonNull @NotNull Integer newItem) {
                return oldItem.intValue() == newItem.intValue();
            }
        }, types, defaultPos);
        dialog.show();
    }

    private void showDefaultLoadDialog() {
        boolean currentState = Hawk.get(HawkConfig.DEFAULT_LOAD_LIVE, false);
        Hawk.put(HawkConfig.DEFAULT_LOAD_LIVE, !currentState);
        updateSettingItem("下次进入", !currentState ? "直播" : "点播");
    }

    private void showSearchViewDialog() {
        int defaultPos = Hawk.get(HawkConfig.SEARCH_VIEW, 0);
        ArrayList<Integer> types = new ArrayList<>();
        types.add(0);
        types.add(1);

        SelectDialog<Integer> dialog = new SelectDialog<>(this);
        dialog.setTip("请选择搜索视图");
        dialog.setAdapter(new SelectDialogAdapter.SelectDialogInterface<Integer>() {
            @Override
            public void click(Integer value, int pos) {
                Hawk.put(HawkConfig.SEARCH_VIEW, value);
                updateSettingItem("搜索展示", getSearchView(value));
            }

            @Override
            public String getDisplay(Integer val) {
                return getSearchView(val);
            }
        }, new DiffUtil.ItemCallback<Integer>() {
            @Override
            public boolean areItemsTheSame(@NonNull @NotNull Integer oldItem, @NonNull @NotNull Integer newItem) {
                return oldItem.intValue() == newItem.intValue();
            }

            @Override
            public boolean areContentsTheSame(@NonNull @NotNull Integer oldItem, @NonNull @NotNull Integer newItem) {
                return oldItem.intValue() == newItem.intValue();
            }
        }, types, defaultPos);
        dialog.show();
    }

    private void showHistoryNumDialog() {
        int defaultPos = Hawk.get(HawkConfig.HISTORY_NUM, 0);
        ArrayList<Integer> types = new ArrayList<>();
        types.add(0);
        types.add(1);
        types.add(2);

        SelectDialog<Integer> dialog = new SelectDialog<>(this);
        dialog.setTip("保留历史记录数量");
        dialog.setAdapter(new SelectDialogAdapter.SelectDialogInterface<Integer>() {
            @Override
            public void click(Integer value, int pos) {
                Hawk.put(HawkConfig.HISTORY_NUM, value);
                updateSettingItem("历史记录数", HistoryHelper.getHistoryNumName(value));
            }

            @Override
            public String getDisplay(Integer val) {
                return HistoryHelper.getHistoryNumName(val);
            }
        }, new DiffUtil.ItemCallback<Integer>() {
            @Override
            public boolean areItemsTheSame(@NonNull @NotNull Integer oldItem, @NonNull @NotNull Integer newItem) {
                return oldItem.intValue() == newItem.intValue();
            }

            @Override
            public boolean areContentsTheSame(@NonNull @NotNull Integer oldItem, @NonNull @NotNull Integer newItem) {
                return oldItem.intValue() == newItem.intValue();
            }
        }, types, defaultPos);
        dialog.show();
    }

    private void showWebViewDialog() {
        boolean useSystem = !Hawk.get(HawkConfig.PARSE_WEBVIEW, true);
        Hawk.put(HawkConfig.PARSE_WEBVIEW, useSystem);
        updateSettingItem("WebView类型", useSystem ? "系统自带" : "XWalkView");

        if (!useSystem) {
            Toast.makeText(this, "注意: XWalkView只适用于部分低Android版本，Android5.0以上推荐使用系统自带", Toast.LENGTH_LONG).show();
            XWalkInitDialog dialog = new XWalkInitDialog(this);
            dialog.setOnListener(() -> {
            });
            dialog.show();
        }
    }

    private void showBackupDialog() {
        BackupDialog dialog = new BackupDialog(this);
        dialog.show();
    }

    private void showAboutDialog() {
        AboutDialog dialog = new AboutDialog(this);
        dialog.show();
    }

    // ========== 辅助方法 ==========

    private String getHomeRecName(int rec) {
        switch (rec) {
            case 1:
                return "豆瓣推荐";
            case 2:
                return "仅当前源";
            default:
                return "推荐+豆瓣";
        }
    }

    private String getSearchView(int view) {
        return view == 1 ? "文字列表" : "缩略图";
    }

    private void updateSettingItem(String title, String newValue) {
        for (int i = 0; i < settingItems.size(); i++) {
            SettingItem item = settingItems.get(i);
            if (item.getTitle().equals(title)) {
                item.setValue(newValue);
                adapter.updateItem(i);
                break;
            }
        }
    }

    private final Runnable mDataRunnable = new Runnable() {
        @Override
        public void run() {
            // 处理数据变化
        }
    };
}