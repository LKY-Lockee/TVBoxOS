package com.github.tvbox.osc.ui.activity

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.tvbox.osc.R
import com.github.tvbox.osc.api.ApiConfig
import com.github.tvbox.osc.base.BaseActivity
import com.github.tvbox.osc.bean.SettingItem
import com.github.tvbox.osc.bean.SettingItem.Companion.createCategory
import com.github.tvbox.osc.bean.SettingItem.Companion.createPreference
import com.github.tvbox.osc.bean.SettingItem.Companion.createSwitch
import com.github.tvbox.osc.bean.SourceBean
import com.github.tvbox.osc.data.AppDataManager.backup
import com.github.tvbox.osc.data.AppDataManager.restore
import com.github.tvbox.osc.data.ConfigKey
import com.github.tvbox.osc.data.PreferenceStore
import com.github.tvbox.osc.event.RefreshEvent
import com.github.tvbox.osc.player.thirdparty.RemoteTVBox
import com.github.tvbox.osc.ui.adapter.SettingM3Adapter
import com.github.tvbox.osc.ui.dialog.ApiDialog
import com.github.tvbox.osc.ui.dialog.SearchRemoteTvDialog
import com.github.tvbox.osc.util.AppManager
import com.github.tvbox.osc.util.FileUtils.cachePath
import com.github.tvbox.osc.util.FileUtils.cleanDirectory
import com.github.tvbox.osc.util.FileUtils.filePath
import com.github.tvbox.osc.util.FileUtils.recursiveDelete
import com.github.tvbox.osc.util.HistoryHelper.getHistoryNumName
import com.github.tvbox.osc.util.HistoryHelper.setLiveApiHistory
import com.github.tvbox.osc.util.OkGoHelper.dnsHttpsList
import com.github.tvbox.osc.util.PlayerHelper
import com.github.tvbox.osc.util.PlayerHelper.existPlayerTypes
import com.github.tvbox.osc.util.PlayerHelper.getPlayerName
import com.github.tvbox.osc.util.PlayerHelper.getRenderName
import com.github.tvbox.osc.util.PlayerHelper.getScaleName
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

/**
 * @author pj567
 * @date :2020/12/23
 * @description:
 */
class SettingsActivity : BaseActivity() {
	private val mHandler = Handler(Looper.getMainLooper())
	var devMode: String = ""
	private val mDevModeRun = Runnable { devMode = "" }
	private var adapter: SettingM3Adapter? = null
	private val settingItems: MutableList<SettingItem> = ArrayList()
	private var homeSourceKey: String? = null
	private var currentApi: String? = null
	private var homeRec = 0
	private var dnsOpt = 0
	private var currentLiveApi: String? = null
	private var storagePermissionLauncher: ActivityResultLauncher<Array<String>>? = null
	private var settingsLauncher: ActivityResultLauncher<Intent>? = null

	override val layoutResID: Int
		get() = R.layout.activity_settings

	override fun init() {
		onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
			override fun handleOnBackPressed() {
				handleBackPressed()
			}
		})
		initPermissionLaunchers()
		initView()
		initData()
	}

	private fun initPermissionLaunchers() {
		// 存储权限申请器
		storagePermissionLauncher = registerForActivityResult(RequestMultiplePermissions()) { result ->
			val allGranted = result.values.all { it }
			if (allGranted) {
				Toast.makeText(this, "已获得存储权限", Toast.LENGTH_SHORT).show()
			} else {
				Toast.makeText(this, "存储权限被拒绝", Toast.LENGTH_SHORT).show()
			}
		}

		// 设置页面返回监听器
		settingsLauncher = registerForActivityResult(StartActivityForResult()) {
			// 从设置页面返回，检查权限状态
			if (checkStoragePermission()) {
				Toast.makeText(this, "已获得存储权限", Toast.LENGTH_SHORT).show()
			}
		}
	}

	private fun initView() {
		val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
		toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
		toolbar.setNavigationOnClickListener { handleBackPressed() }

		val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
		recyclerView.setLayoutManager(LinearLayoutManager(this))
		adapter = SettingM3Adapter()
		recyclerView.setAdapter(adapter)
	}

	private fun initData() {
		currentApi = PreferenceStore.get(ConfigKey.API_URL, "")
		homeSourceKey = ApiConfig.instance.homeSourceBean.key
		homeRec = PreferenceStore.get(ConfigKey.HOME_REC, 0)
		dnsOpt = PreferenceStore.get(ConfigKey.DOH_URL, 0)
		currentLiveApi = PreferenceStore.get(ConfigKey.LIVE_API_URL, "")

		buildSettingItems()
		adapter?.setItems(settingItems)
	}

	private fun buildSettingItems() {
		settingItems.clear()

		// 数据源设置
		settingItems.add(createCategory("数据源"))

		settingItems.add(createPreference("配置地址", PreferenceStore.get(ConfigKey.API_URL, "")) {
			showApiDialog()
		}.apply { summary = "设置应用数据源配置地址" })

		settingItems.add(createPreference("点播历史配置", "") {
			showApiHistoryDialog()
		}.apply { summary = "查看和选择点播历史配置" })

		settingItems.add(createPreference("直播历史配置", "") {
			showLiveHistoryDialog()
		}.apply { summary = "查看和选择直播历史配置" })

		settingItems.add(createPreference("首页站源", ApiConfig.instance.homeSourceBean.name) {
			showHomeSourceDialog()
		}.apply { summary = "选择默认首页数据源" })

		settingItems.add(createPreference("安全DNS", dnsHttpsList[PreferenceStore.get(ConfigKey.DOH_URL, 0)]) {
			showDnsDialog()
		}.apply { summary = "选择DNS解析服务" })

		// 播放设置
		settingItems.add(createCategory("播放"))

		settingItems.add(createPreference("默认播放器", getPlayerName(PreferenceStore.get(ConfigKey.PLAY_TYPE, 0))) {
			showPlayerDialog()
		}.apply { summary = "选择视频播放器" })

		settingItems.add(createPreference("IJK解码方式", PreferenceStore.get(ConfigKey.IJK_CODEC, "硬解码")) {
			showCodecDialog()
		}.apply { summary = "IJK播放器解码方式" })

		settingItems.add(createPreference("渲染方式", getRenderName(PreferenceStore.get(ConfigKey.PLAY_RENDER, 0))) {
			showRenderDialog()
		}.apply { summary = "视频画面渲染方式" })

		settingItems.add(createPreference("画面缩放", getScaleName(PreferenceStore.get(ConfigKey.PLAY_SCALE, 0))) {
			showScaleDialog()
		}.apply { summary = "默认画面缩放比例" })

		settingItems.add(createSwitch("IJK缓存播放", PreferenceStore.get(ConfigKey.IJK_CACHE_PLAY, false)) { item ->
			PreferenceStore.put(ConfigKey.IJK_CACHE_PLAY, item.switchState)
		}.apply { summary = "开启IJK缓存" })

		settingItems.add(createSwitch("去广告", PreferenceStore.get(ConfigKey.M3U8_PURIFY, false)) { item ->
			PreferenceStore.put(ConfigKey.M3U8_PURIFY, item.switchState)
		}.apply { summary = "过滤M3U8视频广告" })

		// 界面设置
		settingItems.add(createCategory("界面"))

		settingItems.add(createPreference("首页推荐", getHomeRecName(PreferenceStore.get(ConfigKey.HOME_REC, 0))) {
			showHomeRecDialog()
		}.apply { summary = "设置首页推荐内容" })

		settingItems.add(createPreference("启动方式", if (PreferenceStore.get(ConfigKey.DEFAULT_LOAD_LIVE, false)) "直播" else "点播") {
			showDefaultLoadDialog()
		}.apply { summary = "设置启动后默认页面" })

		settingItems.add(createPreference("保留历史记录", getHistoryNumName(PreferenceStore.get(ConfigKey.HISTORY_NUM, 0))) {
			showHistoryNumDialog()
		}.apply { summary = "保留历史记录的数量" })

		// 高级设置
		settingItems.add(createCategory("高级"))

		settingItems.add(createPreference("存储权限", "") {
			requestStoragePermission()
		}.apply { summary = "请求存储权限用于备份恢复" })

		settingItems.add(createPreference("搜索附近TVBox", "") {
			showSearchRemoteTvDialog()
		}.apply { summary = "搜索局域网内的其他TVBox设备" })

		settingItems.add(createPreference("清空缓存", "") {
			clearCache()
		}.apply { summary = "清空播放缓存和JAR缓存" })

		settingItems.add(createSwitch("调试模式", PreferenceStore.get(ConfigKey.DEBUG_OPEN, false)) { item ->
			PreferenceStore.put(ConfigKey.DEBUG_OPEN, item.switchState)
		}.apply { summary = "开启应用调试信息" })

		// 其他
		settingItems.add(createCategory("其他"))

		settingItems.add(createPreference("备份与恢复", "") {
			showBackupDialog()
		}.apply { summary = "备份或恢复应用数据" })

		settingItems.add(createPreference("关于", "") {
			showAboutDialog()
		}.apply { summary = "应用版本和信息" })
	}

	override fun dispatchKeyEvent(event: KeyEvent): Boolean {
		if (event.action == KeyEvent.ACTION_DOWN) {
			val keyCode = event.keyCode
			if (keyCode == KeyEvent.KEYCODE_0) {
				mHandler.removeCallbacks(mDevModeRun)
				devMode += "0"
				mHandler.postDelayed(mDevModeRun, 200)
				if (devMode.length >= 4) {
					if (callback != null) {
						callback?.onChange()
					}
				}
			}
		}
		return super.dispatchKeyEvent(event)
	}

	private fun handleBackPressed() {
		if (currentApi == PreferenceStore.get(ConfigKey.API_URL, "")) {
			if (dnsOpt != PreferenceStore.get(ConfigKey.DOH_URL, 0)) {
				AppManager.instance.finishAllActivity()
				jumpActivity(HomeActivity::class.java)
			} else if ((homeSourceKey != null && homeSourceKey != PreferenceStore.get(ConfigKey.HOME_API, "")) || homeRec != PreferenceStore.get(ConfigKey.HOME_REC, 0)) {
				jumpActivity(HomeActivity::class.java, createBundle())
			} else if (currentLiveApi != PreferenceStore.get(ConfigKey.LIVE_API_URL, "")) {
				jumpActivity(HomeActivity::class.java)
			}
		} else {
			AppManager.instance.finishAllActivity()
			jumpActivity(HomeActivity::class.java)
		}
		finish()
	}

	private fun createBundle(): Bundle {
		val bundle = Bundle()
		bundle.putBoolean("useCache", true)
		return bundle
	}

	// ========== 设置项功能 ==========
	// 数据源
	private fun showApiDialog() {
		val dialog = ApiDialog(this)
		dialog.setOnListener { api: String? ->
			PreferenceStore.put(ConfigKey.API_URL, api)
			updateSettingsItem("配置地址", api)
		}
		dialog.show()
	}

	private fun showApiHistoryDialog() {
		val history = PreferenceStore.getObj(ConfigKey.API_HISTORY, arrayListOf<String>())
		if (history.isEmpty()) {
			Toast.makeText(this, "暂无点播配置历史", Toast.LENGTH_SHORT).show()
			return
		}
		val current = PreferenceStore.get(ConfigKey.API_URL, "")
		val idx = if (history.contains(current)) history.indexOf(current) else 0

		val historyArray = history.toTypedArray()

		MaterialAlertDialogBuilder(this)
			.setTitle("点播配置历史")
			.setSingleChoiceItems(historyArray, idx) { dialog: DialogInterface?, which: Int ->
				val selectedUrl = historyArray[which]
				PreferenceStore.put(ConfigKey.API_URL, selectedUrl)
				PreferenceStore.put(ConfigKey.LIVE_API_URL, selectedUrl)
				setLiveApiHistory(selectedUrl)
				updateSettingsItem("配置地址", selectedUrl)
				dialog?.dismiss()
			}
			.setNeutralButton("清空历史") { dialog: DialogInterface?, which: Int ->
				MaterialAlertDialogBuilder(this)
					.setTitle("确认清空")
					.setMessage("确定要清空所有点播配置历史吗？")
					.setPositiveButton("确定") { d: DialogInterface?, w: Int ->
						PreferenceStore.putObj(ConfigKey.API_HISTORY, arrayListOf<String>())
						Toast.makeText(this, "已清点播空历史配置", Toast.LENGTH_SHORT).show()
					}
					.setNegativeButton("取消", null)
					.show()
			}
			.setNegativeButton("取消", null)
			.show()
	}

	private fun showLiveHistoryDialog() {
		val history = PreferenceStore.getObj(ConfigKey.LIVE_API_HISTORY, arrayListOf<String>())
		if (history.isEmpty()) {
			Toast.makeText(this, "暂无直播历史配置", Toast.LENGTH_SHORT).show()
			return
		}
		val current = PreferenceStore.get(ConfigKey.LIVE_API_URL, "")
		val idx = if (history.contains(current)) history.indexOf(current) else 0

		val historyArray: Array<String> = history.toTypedArray()

		MaterialAlertDialogBuilder(this)
			.setTitle("直播历史配置")
			.setSingleChoiceItems(historyArray, idx) { dialog: DialogInterface?, which: Int ->
				val selectedUrl: String = historyArray[which]
				PreferenceStore.put(ConfigKey.LIVE_API_URL, selectedUrl)
				Toast.makeText(this, "已选择直播配置", Toast.LENGTH_SHORT).show()
				dialog?.dismiss()
			}
			.setNeutralButton("清空历史") { dialog: DialogInterface?, which: Int ->
				MaterialAlertDialogBuilder(this)
					.setTitle("确认清空")
					.setMessage("确定要清空所有直播历史配置吗？")
					.setPositiveButton("确定") { d: DialogInterface?, w: Int ->
						PreferenceStore.putObj(ConfigKey.LIVE_API_HISTORY, arrayListOf<String>())
						Toast.makeText(this, "已清空直播历史配置", Toast.LENGTH_SHORT).show()
					}
					.setNegativeButton("取消", null)
					.show()
			}
			.setNegativeButton("取消", null)
			.show()
	}

	private fun showHomeSourceDialog() {
		val sites: List<SourceBean> = ApiConfig.instance.switchSourceBeanList
		if (sites.isEmpty()) {
			Toast.makeText(this, "无可用数据源", Toast.LENGTH_SHORT).show()
			return
		}

		var select = sites.indexOf(ApiConfig.instance.homeSourceBean)
		if (select < 0 || select >= sites.size) select = 0

		val siteNames = Array(sites.size) { i -> sites[i].name ?: "" }

		MaterialAlertDialogBuilder(this)
			.setTitle("请选择首页数据源")
			.setSingleChoiceItems(siteNames, select) { dialog: DialogInterface?, which: Int ->
				val selectedSite = sites[which]
				ApiConfig.instance.setSourceBean(selectedSite)
				updateSettingsItem("首页站源", selectedSite.name)
				dialog?.dismiss()

				val intent = Intent(this@SettingsActivity, HomeActivity::class.java)
				intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
				val bundle = Bundle()
				bundle.putBoolean("useCache", true)
				intent.putExtras(bundle)
				startActivity(intent)
			}
			.setNegativeButton("取消", null)
			.show()
	}

	private fun showDnsDialog() {
		val dohUrl = PreferenceStore.get(ConfigKey.DOH_URL, 0)
		val dnsOptions: Array<String> = dnsHttpsList.toTypedArray()

		MaterialAlertDialogBuilder(this)
			.setTitle("请选择安全DNS")
			.setSingleChoiceItems(dnsOptions, dohUrl) { dialog: DialogInterface?, which: Int ->
				PreferenceStore.put(ConfigKey.DOH_URL, which)
				updateSettingsItem("安全DNS", dnsHttpsList[which])
				dialog?.dismiss()
			}
			.setNegativeButton("取消", null)
			.show()
	}

	// 播放
	private fun showPlayerDialog() {
		val playerType = PreferenceStore.get(ConfigKey.PLAY_TYPE, 0)
		var defaultPos = 0
		val players: List<Int> = existPlayerTypes

		val playerNames = Array(players.size) { p -> getPlayerName(players[p]) }
		for (p in players.indices) {
			if (players[p] == playerType) {
				defaultPos = p
			}
		}

		MaterialAlertDialogBuilder(this)
			.setTitle("请选择默认播放器")
			.setSingleChoiceItems(playerNames, defaultPos) { dialog: DialogInterface?, which: Int ->
				val thisPlayerType = players[which]
				PreferenceStore.put(ConfigKey.PLAY_TYPE, thisPlayerType)
				updateSettingsItem("默认播放器", getPlayerName(thisPlayerType))
				PlayerHelper.init()
				dialog?.dismiss()
			}
			.setNegativeButton("取消", null)
			.show()
	}

	private fun showCodecDialog() {
		val ijkCodes = ApiConfig.instance.ijkCodes
		if (ijkCodes.isEmpty()) {
			Toast.makeText(this, "无可用解码方式", Toast.LENGTH_SHORT).show()
			return
		}

		var defaultPos = 0
		val ijkSel = PreferenceStore.get(ConfigKey.IJK_CODEC, "硬解码")
		val codecNames = Array(ijkCodes.size) { j -> ijkCodes[j].name ?: "" }
		for (j in ijkCodes.indices) {
			if (ijkSel == ijkCodes[j].name) {
				defaultPos = j
			}
		}

		MaterialAlertDialogBuilder(this)
			.setTitle("请选择IJK解码方式")
			.setSingleChoiceItems(codecNames, defaultPos) { dialog: DialogInterface?, which: Int ->
				val selectedCodec = ijkCodes[which]
				selectedCodec.selected(true)
				updateSettingsItem("IJK解码方式", selectedCodec.name)
				dialog?.dismiss()
			}
			.setNegativeButton("取消", null)
			.show()
	}

	private fun showRenderDialog() {
		val defaultPos = PreferenceStore.get(ConfigKey.PLAY_RENDER, 0)
		val renderNames = arrayOf(
			getRenderName(0),
			getRenderName(1)
		)

		MaterialAlertDialogBuilder(this)
			.setTitle("请选择默认渲染方式")
			.setSingleChoiceItems(renderNames, defaultPos) { dialog: DialogInterface?, which: Int ->
				PreferenceStore.put(ConfigKey.PLAY_RENDER, which)
				updateSettingsItem("渲染方式", getRenderName(which))
				PlayerHelper.init()
				dialog?.dismiss()
			}
			.setNegativeButton("取消", null)
			.show()
	}

	private fun showScaleDialog() {
		val defaultPos = PreferenceStore.get(ConfigKey.PLAY_SCALE, 0)
		val scaleNames = arrayOf(
			getScaleName(0),
			getScaleName(1),
			getScaleName(2),
			getScaleName(3),
			getScaleName(4),
			getScaleName(5)
		)

		MaterialAlertDialogBuilder(this)
			.setTitle("请选择默认画面缩放")
			.setSingleChoiceItems(scaleNames, defaultPos) { dialog: DialogInterface?, which: Int ->
				PreferenceStore.put(ConfigKey.PLAY_SCALE, which)
				updateSettingsItem("画面缩放", getScaleName(which))
				dialog?.dismiss()
			}
			.setNegativeButton("取消", null)
			.show()
	}

	// 界面
	private fun showHomeRecDialog() {
		val defaultPos = PreferenceStore.get(ConfigKey.HOME_REC, 0)
		val recNames = arrayOf(
			getHomeRecName(0),
			getHomeRecName(1),
			getHomeRecName(2)
		)

		MaterialAlertDialogBuilder(this)
			.setTitle("请选择首页列表数据")
			.setSingleChoiceItems(recNames, defaultPos) { dialog: DialogInterface?, which: Int ->
				PreferenceStore.put(ConfigKey.HOME_REC, which)
				updateSettingsItem("首页推荐", getHomeRecName(which))
				dialog?.dismiss()
			}
			.setNegativeButton("取消", null)
			.show()
	}

	private fun showDefaultLoadDialog() {
		val currentState = PreferenceStore.get(ConfigKey.DEFAULT_LOAD_LIVE, false)
		val defaultPos = if (currentState) 1 else 0
		val options = arrayOf("点播", "直播")

		MaterialAlertDialogBuilder(this)
			.setTitle("设置启动后默认页面")
			.setSingleChoiceItems(options, defaultPos) { dialog: DialogInterface?, which: Int ->
				val newState = (which == 1)
				PreferenceStore.put(ConfigKey.DEFAULT_LOAD_LIVE, newState)
				updateSettingsItem("启动方式", if (newState) "直播" else "点播")
				dialog?.dismiss()
			}
			.setNegativeButton("取消", null)
			.show()
	}

	private fun showHistoryNumDialog() {
		val defaultPos = PreferenceStore.get(ConfigKey.HISTORY_NUM, 0)
		val historyOptions = arrayOf(
			getHistoryNumName(0),
			getHistoryNumName(1),
			getHistoryNumName(2)
		)

		MaterialAlertDialogBuilder(this)
			.setTitle("保留历史记录数量")
			.setSingleChoiceItems(historyOptions, defaultPos) { dialog: DialogInterface?, which: Int ->
				PreferenceStore.put(ConfigKey.HISTORY_NUM, which)
				updateSettingsItem("保留历史记录", getHistoryNumName(which))
				dialog?.dismiss()
			}
			.setNegativeButton("取消", null)
			.show()
	}

	// 高级
	private fun requestStoragePermission() {
		if (checkStoragePermission()) {
			Toast.makeText(this, "已获得存储权限", Toast.LENGTH_SHORT).show()
			return
		}

		// 根据Android版本使用不同的权限申请方式
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			// Android 11 及以上需要申请所有文件访问权限
			if (Environment.isExternalStorageManager()) {
				Toast.makeText(this, "已获得存储权限", Toast.LENGTH_SHORT).show()
			} else {
				MaterialAlertDialogBuilder(this)
					.setTitle("需要存储权限")
					.setMessage("应用需要访问存储权限以进行备份和恢复操作。\n\n请在设置中授予\"允许访问所有文件\"权限。")
					.setPositiveButton("去设置") { dialog: DialogInterface?, which: Int ->
						try {
							val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
							intent.data = "package:$packageName".toUri()
							settingsLauncher?.launch(intent)
						} catch (e: Exception) {
							val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
							settingsLauncher?.launch(intent)
						}
					}
					.setNegativeButton("取消", null)
					.show()
			}
		} else {
			// Android 6.0 - 10 使用标准权限申请
			val permissions = arrayOf(
				Manifest.permission.READ_EXTERNAL_STORAGE,
				Manifest.permission.WRITE_EXTERNAL_STORAGE
			)

			// 检查是否需要显示权限说明
			var shouldShowRationale = false
			for (permission in permissions) {
				if (shouldShowRequestPermissionRationale(permission)) {
					shouldShowRationale = true
					break
				}
			}

			if (shouldShowRationale) {
				MaterialAlertDialogBuilder(this)
					.setTitle("需要存储权限")
					.setMessage("应用需要访问存储权限以进行备份和恢复操作。")
					.setPositiveButton("授权") { dialog: DialogInterface?, which: Int -> storagePermissionLauncher?.launch(permissions) }
					.setNegativeButton("取消", null)
					.show()
			} else {
				storagePermissionLauncher?.launch(permissions)
			}
		}
	}

	private fun checkStoragePermission(): Boolean {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			// Android 11 及以上检查所有文件访问权限
			return Environment.isExternalStorageManager()
		} else {
			val readPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
			val writePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
			return readPermission == PackageManager.PERMISSION_GRANTED && writePermission == PackageManager.PERMISSION_GRANTED
		}
	}

	private fun showSearchRemoteTvDialog() {
		loadingSearchRemoteTvDialog = SearchRemoteTvDialog(this)
		val dialog = loadingSearchRemoteTvDialog
		dialog?.setTip("搜索附近TVBox")
		dialog?.show()

		remoteTvHostList = mutableListOf()
		foundRemoteTv = false

		mHandler.postDelayed({
			Thread {
				RemoteTVBox.searchAvailable(object : RemoteTVBox.Callback() {
					override fun found(viewHost: String?, end: Boolean) {
						if (viewHost != null) {
							remoteTvHostList.add(viewHost)
						}
						if (end) {
							foundRemoteTv = true
							EventBus.getDefault().post(RefreshEvent(RefreshEvent.TYPE_SETTING_SEARCH_TV))
						}
					}

					override fun fail(all: Boolean, end: Boolean) {
						if (end) {
							foundRemoteTv = !all
							EventBus.getDefault().post(RefreshEvent(RefreshEvent.TYPE_SETTING_SEARCH_TV))
						}
					}
				})
			}.start()
		}, 500)
	}

	private fun clearCache() {
		val cachePath = cachePath
		val cacheDir = File(cachePath)
		val cspCachePath = "$filePath/csp/"
		val cspCacheDir = File(cspCachePath)

		if (!cacheDir.exists() && !cspCacheDir.exists()) {
			Toast.makeText(this, "缓存已为空", Toast.LENGTH_SHORT).show()
			return
		}

		Thread {
			try {
				if (cacheDir.exists()) {
					cleanDirectory(cacheDir)
				}
				if (cspCacheDir.exists()) {
					cleanDirectory(cspCacheDir)
				}
				runOnUiThread { Toast.makeText(this@SettingsActivity, "播放和JAR缓存已清空", Toast.LENGTH_LONG).show() }
			} catch (e: Exception) {
				e.printStackTrace()
				runOnUiThread { Toast.makeText(this@SettingsActivity, "清空缓存失败", Toast.LENGTH_SHORT).show() }
			}
		}.start()
	}

	// 其它
	private fun showBackupDialog() {
		val backups = this.allBackups

		if (backups.isEmpty()) {
			// 如果没有备份，显示提示并提供备份按钮
			MaterialAlertDialogBuilder(this)
				.setTitle("备份与恢复")
				.setMessage("暂无备份记录")
				.setPositiveButton("立即备份") { dialog: DialogInterface?, which: Int -> performBackup() }
				.setNegativeButton("取消", null)
				.show()
		} else {
			// 如果有备份，显示备份列表
			val items = backups.toTypedArray()

			MaterialAlertDialogBuilder(this)
				.setTitle("备份与恢复")
				.setItems(items) { dialog: DialogInterface?, which: Int ->
					// 选择了备份项，询问恢复或删除
					val backupName = backups[which]
					showBackupActionDialog(backupName)
				}
				.setPositiveButton("立即备份") { dialog: DialogInterface?, which: Int -> performBackup() }
				.setNegativeButton("取消", null)
				.show()
		}
	}

	private fun showBackupActionDialog(backupName: String?) {
		MaterialAlertDialogBuilder(this)
			.setTitle(backupName)
			.setMessage("请选择操作")
			.setPositiveButton("恢复") { dialog: DialogInterface?, which: Int -> performRestore(backupName) }
			.setNegativeButton("删除") { dialog: DialogInterface?, which: Int -> performDelete(backupName) }
			.setNeutralButton("取消", null)
			.show()
	}

	private val allBackups: List<String>
		get() {
			val result = mutableListOf<String>()
			try {
				val root = Environment.getExternalStorageDirectory().absolutePath
				val file = File("$root/tvbox_backup/")
				if (file.exists()) {
					val list = file.listFiles()
					if (list != null) {
						Arrays.sort(list) { o1, o2 ->
							if (o1.isDirectory && o2.isFile) return@sort -1
							if (o1.isFile && o2.isDirectory) 1 else o2.name.compareTo(o1.name)
						}
						for (f in list) {
							if (result.size > 10) {
								recursiveDelete(f)
								continue
							}
							if (f.isDirectory) {
								result.add(f.name)
							}
						}
					}
				}
			} catch (e: Throwable) {
				e.printStackTrace()
			}
			return result
		}

	private fun performBackup() {
		try {
			val root = Environment.getExternalStorageDirectory().absolutePath
			val file = File("$root/tvbox_backup/")
			if (!file.exists()) file.mkdirs()
			val now = Date()
			val f = SimpleDateFormat("yyyy-MM-dd-HHmmss", Locale.getDefault())
			val backup = File(file, f.format(now))
			backup.mkdirs()
			val db = File(backup, "sqlite")
			if (backup(db)) {
				PreferenceStore.exportFile(File(backup, "preferences"))
				Toast.makeText(this, "备份成功", Toast.LENGTH_SHORT).show()
				// 刷新备份列表
				showBackupDialog()
			} else {
				Toast.makeText(this, "备份失败", Toast.LENGTH_SHORT).show()
				backup.delete()
			}
		} catch (e: Throwable) {
			e.printStackTrace()
			Toast.makeText(this, "备份失败", Toast.LENGTH_SHORT).show()
		}
	}

	private fun performRestore(dir: String?) {
		MaterialAlertDialogBuilder(this)
			.setTitle("确认恢复")
			.setMessage("确定要恢复备份 $dir 吗？\n恢复后应用将自动重启。")
			.setPositiveButton("确定") { dialog: DialogInterface?, which: Int ->
				try {
					val root = Environment.getExternalStorageDirectory().absolutePath
					val backup = File("$root/tvbox_backup/$dir")
					if (backup.exists()) {
						val db = File(backup, "sqlite")
						if (restore(db)) {
							PreferenceStore.importFile(File(backup, "preferences"))
							Toast.makeText(this, "恢复成功，即将自动重启应用", Toast.LENGTH_SHORT).show()
							Handler(Looper.getMainLooper()).postDelayed({ this.restartApp() }, 3000)
						} else {
							Toast.makeText(this, "恢复失败", Toast.LENGTH_SHORT).show()
						}
					}
				} catch (e: Throwable) {
					e.printStackTrace()
				}
			}
			.setNegativeButton("取消", null)
			.show()
	}

	private fun performDelete(dir: String?) {
		MaterialAlertDialogBuilder(this)
			.setTitle("确认删除")
			.setMessage("确定要删除备份 $dir 吗？")
			.setPositiveButton("删除") { dialog: DialogInterface?, which: Int ->
				try {
					val root = Environment.getExternalStorageDirectory().absolutePath
					val backup = File("$root/tvbox_backup/$dir")
					recursiveDelete(backup)
					Toast.makeText(this, "删除成功", Toast.LENGTH_SHORT).show()
					// 刷新备份列表
					showBackupDialog()
				} catch (e: Throwable) {
					e.printStackTrace()
				}
			}
			.setNegativeButton("取消", null)
			.show()
	}

	private fun showAboutDialog() {
		val message = """
                本软件只提供聚合展示功能，所有资源来自网络, 软件不参与任何制作、上传、储存、下载等内容。软件仅供学习参考, 请于安装后24小时内删除。
                
                打包分发请保留出处：
                https://github.com/CatVodTVOfficial/TVBoxOSC
                https://github.com/q215613905/TVBoxOS
                https://github.com/LKY-Lockee/TVBoxOS
                """.trimIndent()

		MaterialAlertDialogBuilder(this)
			.setTitle("关于")
			.setMessage(message)
			.setPositiveButton("确定", null)
			.create()
			.show()
	}

	// ========== 辅助方法 ==========
	private fun updateSettingsItem(title: String?, newValue: String?) {
		for (i in settingItems.indices) {
			val item = settingItems[i]
			if (item.title == title) {
				item.value = newValue
				adapter?.updateItem(i)
				break
			}
		}
	}

	private fun getHomeRecName(rec: Int): String {
		return when (rec) {
			1 -> "站点推荐"
			2 -> "观看历史"
			else -> "豆瓣热播"
		}
	}

	private fun restartApp() {
		val i = packageManager.getLaunchIntentForPackage(packageName)
		if (i != null) {
			i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
			startActivity(i)
			exitProcess(0)
		}
	}

	interface DevModeCallback {
		fun onChange()
	}

	companion object {
		var callback: DevModeCallback? = null
		var loadingSearchRemoteTvDialog: SearchRemoteTvDialog? = null
		var remoteTvHostList: MutableList<String> = mutableListOf()
		var foundRemoteTv: Boolean = false
	}
}
