package com.github.tvbox.osc.ui.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.view.KeyEvent;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.bean.IJKCode;
import com.github.tvbox.osc.bean.SettingItem;
import com.github.tvbox.osc.bean.SourceBean;
import com.github.tvbox.osc.data.AppDataManager;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.player.thirdparty.RemoteTVBox;
import com.github.tvbox.osc.ui.adapter.SettingM3Adapter;
import com.github.tvbox.osc.ui.dialog.ApiDialog;
import com.github.tvbox.osc.ui.dialog.SearchRemoteTvDialog;
import com.github.tvbox.osc.util.AppManager;
import com.github.tvbox.osc.util.FileUtils;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.HistoryHelper;
import com.github.tvbox.osc.util.OkGoHelper;
import com.github.tvbox.osc.util.PlayerHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.orhanobut.hawk.Hawk;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONObject;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * @author pj567
 * @date :2020/12/23
 * @description:
 */
public class SettingsActivity extends BaseActivity {
    public static DevModeCallback callback = null;

    public static SearchRemoteTvDialog loadingSearchRemoteTvDialog;
    public static List<String> remoteTvHostList;
    public static boolean foundRemoteTv;

    private final Handler mHandler = new Handler();
    String devMode = "";
    private final Runnable mDevModeRun = () -> devMode = "";

    private SettingM3Adapter adapter;
    private final List<SettingItem> settingItems = new ArrayList<>();

    private String homeSourceKey;
    private String currentApi;
    private int homeRec;
    private int dnsOpt;
    private String currentLiveApi;

    private ActivityResultLauncher<String[]> storagePermissionLauncher;
    private ActivityResultLauncher<Intent> settingsLauncher;

    @Override
    protected int getLayoutResID() {
        return R.layout.activity_settings;
    }

    @Override
    protected void init() {
        initPermissionLaunchers();
        initView();
        initData();
    }

    private void initPermissionLaunchers() {
        // 存储权限申请器
        storagePermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean allGranted = true;
                    for (Boolean granted : result.values()) {
                        if (!granted) {
                            allGranted = false;
                            break;
                        }
                    }

                    if (allGranted) {
                        Toast.makeText(this, "已获得存储权限", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "存储权限被拒绝", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // 设置页面返回监听器
        settingsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // 从设置页面返回，检查权限状态
                    if (checkStoragePermission()) {
                        Toast.makeText(this, "已获得存储权限", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void initView() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
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
                "点播历史配置",
                "",
                item -> showApiHistoryDialog()
        ).setSummary("查看和选择点播历史配置"));

        settingItems.add(SettingItem.createPreference(
                "直播历史配置",
                "",
                item -> showLiveHistoryDialog()
        ).setSummary("查看和选择直播历史配置"));

        settingItems.add(SettingItem.createPreference(
                "首页站源",
                ApiConfig.get().getHomeSourceBean().getName(),
                item -> showHomeSourceDialog()
        ).setSummary("选择默认首页数据源"));

        settingItems.add(SettingItem.createPreference(
                "安全DNS",
                OkGoHelper.dnsHttpsList.get(Hawk.get(HawkConfig.DOH_URL, 0)),
                item -> showDnsDialog()
        ).setSummary("选择DNS解析服务"));

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
                }
        ).setSummary("开启IJK缓存"));

        settingItems.add(SettingItem.createSwitch(
                "去广告",
                Hawk.get(HawkConfig.M3U8_PURIFY, false),
                item -> {
                    Hawk.put(HawkConfig.M3U8_PURIFY, item.isSwitchState());
                }
        ).setSummary("过滤M3U8视频广告"));

        // 界面设置
        settingItems.add(SettingItem.createCategory("界面"));

        settingItems.add(SettingItem.createPreference(
                "首页推荐",
                getHomeRecName(Hawk.get(HawkConfig.HOME_REC, 0)),
                item -> showHomeRecDialog()
        ).setSummary("设置首页推荐内容"));

        settingItems.add(SettingItem.createPreference(
                "启动方式",
                Hawk.get(HawkConfig.DEFAULT_LOAD_LIVE, false) ? "直播" : "点播",
                item -> showDefaultLoadDialog()
        ).setSummary("设置启动后默认页面"));

        settingItems.add(SettingItem.createPreference(
                "保留历史记录",
                HistoryHelper.getHistoryNumName(Hawk.get(HawkConfig.HISTORY_NUM, 0)),
                item -> showHistoryNumDialog()
        ).setSummary("保留历史记录的数量"));

        settingItems.add(SettingItem.createSwitch(
                "显示预览",
                Hawk.get(HawkConfig.SHOW_PREVIEW, true),
                item -> Hawk.put(HawkConfig.SHOW_PREVIEW, item.isSwitchState())
        ).setSummary("显示视频缩略图预览"));

        // 高级设置
        settingItems.add(SettingItem.createCategory("高级"));

        settingItems.add(SettingItem.createPreference(
                "存储权限",
                "",
                item -> requestStoragePermission()
        ).setSummary("请求存储权限用于备份恢复"));

        settingItems.add(SettingItem.createPreference(
                "搜索附近TVBox",
                "",
                item -> showSearchRemoteTvDialog()
        ).setSummary("搜索局域网内的其他TVBox设备"));

        settingItems.add(SettingItem.createPreference(
                "清空缓存",
                "",
                item -> clearCache()
        ).setSummary("清空播放缓存和JAR缓存"));

        settingItems.add(SettingItem.createSwitch(
                "调试模式",
                Hawk.get(HawkConfig.DEBUG_OPEN, false),
                item -> Hawk.put(HawkConfig.DEBUG_OPEN, item.isSwitchState())
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

    // ========== 设置项功能 ==========
    // 数据源
    private void showApiDialog() {
        ApiDialog dialog = new ApiDialog(this);
        dialog.setOnListener(api -> {
            Hawk.put(HawkConfig.API_URL, api);
            updateSettingsItem("配置地址", api);
        });
        dialog.show();
    }

    private void showApiHistoryDialog() {
        ArrayList<String> history = Hawk.get(HawkConfig.API_HISTORY, new ArrayList<>());
        if (history.isEmpty()) {
            Toast.makeText(this, "暂无点播配置历史", Toast.LENGTH_SHORT).show();
            return;
        }
        String current = Hawk.get(HawkConfig.API_URL, "");
        int idx = history.contains(current) ? history.indexOf(current) : 0;

        String[] historyArray = history.toArray(new String[0]);

        new MaterialAlertDialogBuilder(this)
                .setTitle("点播配置历史")
                .setSingleChoiceItems(historyArray, idx, (dialog, which) -> {
                    String selectedUrl = historyArray[which];
                    Hawk.put(HawkConfig.API_URL, selectedUrl);
                    Hawk.put(HawkConfig.LIVE_API_URL, selectedUrl);
                    HistoryHelper.setLiveApiHistory(selectedUrl);
                    updateSettingsItem("配置地址", selectedUrl);
                    dialog.dismiss();
                })
                .setNeutralButton("清空历史", (dialog, which) -> new MaterialAlertDialogBuilder(this)
                        .setTitle("确认清空")
                        .setMessage("确定要清空所有点播配置历史吗？")
                        .setPositiveButton("确定", (d, w) -> {
                            Hawk.put(HawkConfig.API_HISTORY, new ArrayList<String>());
                            Toast.makeText(this, "已清点播空历史配置", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("取消", null)
                        .show())
                .setNegativeButton("取消", null)
                .show();
    }

    private void showLiveHistoryDialog() {
        ArrayList<String> history = Hawk.get(HawkConfig.LIVE_API_HISTORY, new ArrayList<>());
        if (history.isEmpty()) {
            Toast.makeText(this, "暂无直播历史配置", Toast.LENGTH_SHORT).show();
            return;
        }
        String current = Hawk.get(HawkConfig.LIVE_API_URL, "");
        int idx = history.contains(current) ? history.indexOf(current) : 0;

        String[] historyArray = history.toArray(new String[0]);

        new MaterialAlertDialogBuilder(this)
                .setTitle("直播历史配置")
                .setSingleChoiceItems(historyArray, idx, (dialog, which) -> {
                    String selectedUrl = historyArray[which];
                    Hawk.put(HawkConfig.LIVE_API_URL, selectedUrl);
                    Toast.makeText(this, "已选择直播配置", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .setNeutralButton("清空历史", (dialog, which) -> new MaterialAlertDialogBuilder(this)
                        .setTitle("确认清空")
                        .setMessage("确定要清空所有直播历史配置吗？")
                        .setPositiveButton("确定", (d, w) -> {
                            Hawk.put(HawkConfig.LIVE_API_HISTORY, new ArrayList<String>());
                            Toast.makeText(this, "已清空直播历史配置", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("取消", null)
                        .show())
                .setNegativeButton("取消", null)
                .show();
    }

    private void showHomeSourceDialog() {
        List<SourceBean> sites = ApiConfig.get().getSwitchSourceBeanList();
        if (sites.isEmpty()) {
            Toast.makeText(this, "无可用数据源", Toast.LENGTH_SHORT).show();
            return;
        }

        int select = sites.indexOf(ApiConfig.get().getHomeSourceBean());
        if (select < 0 || select >= sites.size()) select = 0;

        String[] siteNames = new String[sites.size()];
        for (int i = 0; i < sites.size(); i++) {
            siteNames[i] = sites.get(i).getName();
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("请选择首页数据源")
                .setSingleChoiceItems(siteNames, select, (dialog, which) -> {
                    SourceBean selectedSite = sites.get(which);
                    ApiConfig.get().setSourceBean(selectedSite);
                    updateSettingsItem("首页站源", selectedSite.getName());
                    dialog.dismiss();

                    Intent intent = new Intent(SettingsActivity.this, HomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    Bundle bundle = new Bundle();
                    bundle.putBoolean("useCache", true);
                    intent.putExtras(bundle);
                    startActivity(intent);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showDnsDialog() {
        int dohUrl = Hawk.get(HawkConfig.DOH_URL, 0);
        String[] dnsOptions = OkGoHelper.dnsHttpsList.toArray(new String[0]);

        new MaterialAlertDialogBuilder(this)
                .setTitle("请选择安全DNS")
                .setSingleChoiceItems(dnsOptions, dohUrl, (dialog, which) -> {
                    Hawk.put(HawkConfig.DOH_URL, which);
                    updateSettingsItem("安全DNS", OkGoHelper.dnsHttpsList.get(which));
                    dialog.dismiss();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // 播放
    private void showPlayerDialog() {
        int playerType = Hawk.get(HawkConfig.PLAY_TYPE, 0);
        int defaultPos = 0;
        ArrayList<Integer> players = PlayerHelper.getExistPlayerTypes();

        String[] playerNames = new String[players.size()];
        for (int p = 0; p < players.size(); p++) {
            playerNames[p] = PlayerHelper.getPlayerName(players.get(p));
            if (players.get(p) == playerType) {
                defaultPos = p;
            }
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("请选择默认播放器")
                .setSingleChoiceItems(playerNames, defaultPos, (dialog, which) -> {
                    Integer thisPlayerType = players.get(which);
                    Hawk.put(HawkConfig.PLAY_TYPE, thisPlayerType);
                    updateSettingsItem("默认播放器", PlayerHelper.getPlayerName(thisPlayerType));
                    PlayerHelper.init();
                    dialog.dismiss();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showCodecDialog() {
        List<IJKCode> ijkCodes = ApiConfig.get().getIjkCodes();
        if (ijkCodes == null || ijkCodes.isEmpty()) {
            Toast.makeText(this, "无可用解码方式", Toast.LENGTH_SHORT).show();
            return;
        }

        int defaultPos = 0;
        String ijkSel = Hawk.get(HawkConfig.IJK_CODEC, "硬解码");
        String[] codecNames = new String[ijkCodes.size()];
        for (int j = 0; j < ijkCodes.size(); j++) {
            codecNames[j] = ijkCodes.get(j).getName();
            if (ijkSel.equals(ijkCodes.get(j).getName())) {
                defaultPos = j;
            }
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("请选择IJK解码方式")
                .setSingleChoiceItems(codecNames, defaultPos, (dialog, which) -> {
                    IJKCode selectedCodec = ijkCodes.get(which);
                    selectedCodec.selected(true);
                    updateSettingsItem("IJK解码方式", selectedCodec.getName());
                    dialog.dismiss();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showRenderDialog() {
        int defaultPos = Hawk.get(HawkConfig.PLAY_RENDER, 0);
        String[] renderNames = new String[]{
                PlayerHelper.getRenderName(0),
                PlayerHelper.getRenderName(1)
        };

        new MaterialAlertDialogBuilder(this)
                .setTitle("请选择默认渲染方式")
                .setSingleChoiceItems(renderNames, defaultPos, (dialog, which) -> {
                    Hawk.put(HawkConfig.PLAY_RENDER, which);
                    updateSettingsItem("渲染方式", PlayerHelper.getRenderName(which));
                    PlayerHelper.init();
                    dialog.dismiss();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showScaleDialog() {
        int defaultPos = Hawk.get(HawkConfig.PLAY_SCALE, 0);
        String[] scaleNames = new String[]{
                PlayerHelper.getScaleName(0),
                PlayerHelper.getScaleName(1),
                PlayerHelper.getScaleName(2),
                PlayerHelper.getScaleName(3),
                PlayerHelper.getScaleName(4),
                PlayerHelper.getScaleName(5)
        };

        new MaterialAlertDialogBuilder(this)
                .setTitle("请选择默认画面缩放")
                .setSingleChoiceItems(scaleNames, defaultPos, (dialog, which) -> {
                    Hawk.put(HawkConfig.PLAY_SCALE, which);
                    updateSettingsItem("画面缩放", PlayerHelper.getScaleName(which));
                    dialog.dismiss();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // 界面
    private void showHomeRecDialog() {
        int defaultPos = Hawk.get(HawkConfig.HOME_REC, 0);
        String[] recNames = new String[]{
                getHomeRecName(0),
                getHomeRecName(1),
                getHomeRecName(2)
        };

        new MaterialAlertDialogBuilder(this)
                .setTitle("请选择首页列表数据")
                .setSingleChoiceItems(recNames, defaultPos, (dialog, which) -> {
                    Hawk.put(HawkConfig.HOME_REC, which);
                    updateSettingsItem("首页推荐", getHomeRecName(which));
                    dialog.dismiss();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showDefaultLoadDialog() {
        boolean currentState = Hawk.get(HawkConfig.DEFAULT_LOAD_LIVE, false);
        int defaultPos = currentState ? 1 : 0;
        String[] options = new String[]{"点播", "直播"};

        new MaterialAlertDialogBuilder(this)
                .setTitle("设置启动后默认页面")
                .setSingleChoiceItems(options, defaultPos, (dialog, which) -> {
                    boolean newState = (which == 1);
                    Hawk.put(HawkConfig.DEFAULT_LOAD_LIVE, newState);
                    updateSettingsItem("启动方式", newState ? "直播" : "点播");
                    dialog.dismiss();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showHistoryNumDialog() {
        int defaultPos = Hawk.get(HawkConfig.HISTORY_NUM, 0);
        String[] historyOptions = new String[]{
                HistoryHelper.getHistoryNumName(0),
                HistoryHelper.getHistoryNumName(1),
                HistoryHelper.getHistoryNumName(2)
        };

        new MaterialAlertDialogBuilder(this)
                .setTitle("保留历史记录数量")
                .setSingleChoiceItems(historyOptions, defaultPos, (dialog, which) -> {
                    Hawk.put(HawkConfig.HISTORY_NUM, which);
                    updateSettingsItem("保留历史记录", HistoryHelper.getHistoryNumName(which));
                    dialog.dismiss();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // 高级
    private void requestStoragePermission() {
        if (checkStoragePermission()) {
            Toast.makeText(this, "已获得存储权限", Toast.LENGTH_SHORT).show();
            return;
        }

        // 根据Android版本使用不同的权限申请方式
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 及以上需要申请所有文件访问权限
            if (Environment.isExternalStorageManager()) {
                Toast.makeText(this, "已获得存储权限", Toast.LENGTH_SHORT).show();
            } else {
                new MaterialAlertDialogBuilder(this)
                        .setTitle("需要存储权限")
                        .setMessage("应用需要访问存储权限以进行备份和恢复操作。\n\n请在设置中授予\"允许访问所有文件\"权限。")
                        .setPositiveButton("去设置", (dialog, which) -> {
                            try {
                                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                                intent.setData(Uri.parse("package:" + getPackageName()));
                                settingsLauncher.launch(intent);
                            } catch (Exception e) {
                                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                                settingsLauncher.launch(intent);
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
            }
        } else {
            // Android 6.0 - 10 使用标准权限申请
            String[] permissions = {
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };

            // 检查是否需要显示权限说明
            boolean shouldShowRationale = false;
            for (String permission : permissions) {
                if (shouldShowRequestPermissionRationale(permission)) {
                    shouldShowRationale = true;
                    break;
                }
            }

            if (shouldShowRationale) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle("需要存储权限")
                        .setMessage("应用需要访问存储权限以进行备份和恢复操作。")
                        .setPositiveButton("授权", (dialog, which) -> storagePermissionLauncher.launch(permissions))
                        .setNegativeButton("取消", null)
                        .show();
            } else {
                storagePermissionLauncher.launch(permissions);
            }
        }
    }

    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 及以上检查所有文件访问权限
            return Environment.isExternalStorageManager();
        } else {
            int readPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
            int writePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return readPermission == PackageManager.PERMISSION_GRANTED && writePermission == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void showSearchRemoteTvDialog() {
        loadingSearchRemoteTvDialog = new SearchRemoteTvDialog(this);
        loadingSearchRemoteTvDialog.setTip("搜索附近TVBox");
        loadingSearchRemoteTvDialog.show();

        RemoteTVBox tv = new RemoteTVBox();
        remoteTvHostList = new ArrayList<>();
        foundRemoteTv = false;

        mHandler.postDelayed(() -> new Thread(() -> RemoteTVBox.searchAvailable(tv.new Callback() {
            @Override
            public void found(String viewHost, boolean end) {
                remoteTvHostList.add(viewHost);
                if (end) {
                    foundRemoteTv = true;
                    EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_SETTING_SEARCH_TV));
                }
            }

            @Override
            public void fail(boolean all, boolean end) {
                if (end) {
                    foundRemoteTv = !all;
                    EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_SETTING_SEARCH_TV));
                }
            }
        })).start(), 500);
    }

    private void clearCache() {
        String cachePath = FileUtils.getCachePath();
        File cacheDir = new File(cachePath);
        String cspCachePath = FileUtils.getFilePath() + "/csp/";
        File cspCacheDir = new File(cspCachePath);

        if (!cacheDir.exists() && !cspCacheDir.exists()) {
            Toast.makeText(this, "缓存已为空", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                if (cacheDir.exists()) {
                    FileUtils.cleanDirectory(cacheDir);
                }
                if (cspCacheDir.exists()) {
                    FileUtils.cleanDirectory(cspCacheDir);
                }
                runOnUiThread(() -> Toast.makeText(SettingsActivity.this, "播放和JAR缓存已清空", Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(SettingsActivity.this, "清空缓存失败", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // 其它
    private void showBackupDialog() {
        List<String> backups = getAllBackups();

        if (backups.isEmpty()) {
            // 如果没有备份，显示提示并提供备份按钮
            new MaterialAlertDialogBuilder(this)
                    .setTitle("备份与恢复")
                    .setMessage("暂无备份记录")
                    .setPositiveButton("立即备份", (dialog, which) -> performBackup())
                    .setNegativeButton("取消", null)
                    .show();
        } else {
            // 如果有备份，显示备份列表
            String[] items = backups.toArray(new String[0]);

            new MaterialAlertDialogBuilder(this)
                    .setTitle("备份与恢复")
                    .setItems(items, (dialog, which) -> {
                        // 选择了备份项，询问恢复或删除
                        String backupName = backups.get(which);
                        showBackupActionDialog(backupName);
                    })
                    .setPositiveButton("立即备份", (dialog, which) -> performBackup())
                    .setNegativeButton("取消", null)
                    .show();
        }
    }

    private void showBackupActionDialog(String backupName) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(backupName)
                .setMessage("请选择操作")
                .setPositiveButton("恢复", (dialog, which) -> performRestore(backupName))
                .setNegativeButton("删除", (dialog, which) -> performDelete(backupName))
                .setNeutralButton("取消", null)
                .show();
    }

    private List<String> getAllBackups() {
        ArrayList<String> result = new ArrayList<>();
        try {
            String root = Environment.getExternalStorageDirectory().getAbsolutePath();
            File file = new File(root + "/tvbox_backup/");
            if (file.exists()) {
                File[] list = file.listFiles();
                if (list != null) {
                    Arrays.sort(list, (o1, o2) -> {
                        if (o1.isDirectory() && o2.isFile()) return -1;
                        return o1.isFile() && o2.isDirectory() ? 1 : o2.getName().compareTo(o1.getName());
                    });
                    for (File f : list) {
                        if (result.size() > 10) {
                            FileUtils.recursiveDelete(f);
                            continue;
                        }
                        if (f.isDirectory()) {
                            result.add(f.getName());
                        }
                    }
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return result;
    }

    private void performBackup() {
        try {
            String root = Environment.getExternalStorageDirectory().getAbsolutePath();
            File file = new File(root + "/tvbox_backup/");
            if (!file.exists())
                file.mkdirs();
            Date now = new Date();
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd-HHmmss");
            File backup = new File(file, f.format(now));
            backup.mkdirs();
            File db = new File(backup, "sqlite");
            if (AppDataManager.backup(db)) {
                SharedPreferences sharedPreferences = getSharedPreferences("Hawk2", Context.MODE_PRIVATE);
                JSONObject jsonObject = new JSONObject();
                for (String key : sharedPreferences.getAll().keySet()) {
                    jsonObject.put(key, sharedPreferences.getString(key, ""));
                }
                SharedPreferences cryptoPrefs = getSharedPreferences("crypto.KEY_256", Context.MODE_PRIVATE);
                for (String key : cryptoPrefs.getAll().keySet()) {
                    jsonObject.put(key, cryptoPrefs.getString(key, ""));
                }
                if (!FileUtils.writeSimple(jsonObject.toString().getBytes(StandardCharsets.UTF_8), new File(backup, "hawk"))) {
                    backup.delete();
                    Toast.makeText(this, "备份失败", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "备份成功", Toast.LENGTH_SHORT).show();
                    // 刷新备份列表
                    showBackupDialog();
                }
            } else {
                Toast.makeText(this, "备份失败", Toast.LENGTH_SHORT).show();
                backup.delete();
            }
        } catch (Throwable e) {
            e.printStackTrace();
            Toast.makeText(this, "备份失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void performRestore(String dir) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("确认恢复")
                .setMessage("确定要恢复备份 " + dir + " 吗？\n恢复后应用将自动重启。")
                .setPositiveButton("确定", (dialog, which) -> {
                    try {
                        String root = Environment.getExternalStorageDirectory().getAbsolutePath();
                        File backup = new File(root + "/tvbox_backup/" + dir);
                        if (backup.exists()) {
                            File db = new File(backup, "sqlite");
                            if (AppDataManager.restore(db)) {
                                byte[] data = FileUtils.readSimple(new File(backup, "hawk"));
                                if (data != null) {
                                    String hawkJson = new String(data, StandardCharsets.UTF_8);
                                    JSONObject jsonObject = new JSONObject(hawkJson);
                                    Iterator<String> it = jsonObject.keys();
                                    SharedPreferences sharedPreferences = getSharedPreferences("Hawk2", Context.MODE_PRIVATE);
                                    while (it.hasNext()) {
                                        String key = it.next();
                                        String value = jsonObject.getString(key);
                                        if (key.equals("cipher_key")) {
                                            getSharedPreferences("crypto.KEY_256", Context.MODE_PRIVATE).edit().putString(key, value).commit();
                                        } else {
                                            sharedPreferences.edit().putString(key, value).commit();
                                        }
                                    }
                                    Toast.makeText(this, "恢复成功，即将自动重启应用", Toast.LENGTH_SHORT).show();
                                    new Handler().postDelayed(this::restartApp, 3000);
                                } else {
                                    Toast.makeText(this, "恢复失败", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(this, "恢复失败", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void performDelete(String dir) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("确认删除")
                .setMessage("确定要删除备份 " + dir + " 吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    try {
                        String root = Environment.getExternalStorageDirectory().getAbsolutePath();
                        File backup = new File(root + "/tvbox_backup/" + dir);
                        FileUtils.recursiveDelete(backup);
                        Toast.makeText(this, "删除成功", Toast.LENGTH_SHORT).show();
                        // 刷新备份列表
                        showBackupDialog();
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showAboutDialog() {
        String message = """
                本软件只提供聚合展示功能，所有资源来自网络, 软件不参与任何制作、上传、储存、下载等内容。软件仅供学习参考, 请于安装后24小时内删除。
                
                打包分发请保留出处：
                https://github.com/CatVodTVOfficial/TVBoxOSC
                https://github.com/q215613905/TVBoxOS
                https://github.com/LKY-Lockee/TVBoxOS""";

        new MaterialAlertDialogBuilder(this)
                .setTitle("关于")
                .setMessage(message)
                .setPositiveButton("确定", null)
                .create()
                .show();
    }

    // ========== 辅助方法 ==========

    private void updateSettingsItem(String title, String newValue) {
        for (int i = 0; i < settingItems.size(); i++) {
            SettingItem item = settingItems.get(i);
            if (item.getTitle().equals(title)) {
                item.setValue(newValue);
                adapter.updateItem(i);
                break;
            }
        }
    }

    private String getHomeRecName(int rec) {
        return switch (rec) {
            case 1 -> "站点推荐";
            case 2 -> "观看历史";
            default -> "豆瓣热播";
        };
    }

    private void restartApp() {
        Intent i = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (i != null) {
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
            System.exit(0);
        }
    }

    public interface DevModeCallback {
        void onChange();
    }
}