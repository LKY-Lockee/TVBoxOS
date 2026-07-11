package com.github.tvbox.osc.ui.setting

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.VideoSettings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.github.tvbox.osc.api.ApiConfig
import com.github.tvbox.osc.data.AppDataManager
import com.github.tvbox.osc.data.PreferenceStore
import com.github.tvbox.osc.player.thirdparty.RemoteTVBox
import com.github.tvbox.osc.ui.compose.setting.BaseWidget
import com.github.tvbox.osc.ui.compose.setting.SegmentedColumn
import com.github.tvbox.osc.ui.compose.setting.SwitchWidget
import com.github.tvbox.osc.ui.compose.setting.dialog.AboutDialog
import com.github.tvbox.osc.ui.compose.setting.dialog.AddressInputDialog
import com.github.tvbox.osc.ui.compose.setting.dialog.BackupActionDialog
import com.github.tvbox.osc.ui.compose.setting.dialog.BackupDialog
import com.github.tvbox.osc.ui.compose.setting.dialog.ConfirmDialog
import com.github.tvbox.osc.ui.compose.setting.dialog.QrCodeDialog
import com.github.tvbox.osc.ui.compose.setting.dialog.SearchRemoteTvDialog
import com.github.tvbox.osc.ui.compose.setting.dialog.SingleChoiceDialog
import com.github.tvbox.osc.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
	viewModel: SettingsViewModel,
	onBack: () -> Unit
) {
	val context = LocalContext.current
	val uiState by viewModel.uiState.collectAsState()

	LaunchedEffect(Unit) {
		viewModel.toast.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
	}

	val storagePermissionLauncher = rememberLauncherForActivityResult(
		ActivityResultContracts.RequestMultiplePermissions()
	) { result ->
		val allGranted = result.values.all { it }
		Toast.makeText(context, if (allGranted) "已获得存储权限" else "存储权限被拒绝", Toast.LENGTH_SHORT).show()
	}

	val settingsLauncher = rememberLauncherForActivityResult(
		ActivityResultContracts.StartActivityForResult()
	) {
		if (checkStoragePermission(context)) {
			Toast.makeText(context, "已获得存储权限", Toast.LENGTH_SHORT).show()
		}
	}

	// 对话框状态
	var showVodInputDialog by remember { mutableStateOf(false) }
	var showLiveInputDialog by remember { mutableStateOf(false) }
	var showQrCodeDialog by remember { mutableStateOf(false) }
	var showHomeSourceDialog by remember { mutableStateOf(false) }
	var showDnsDialog by remember { mutableStateOf(false) }
	var showPlayerDialog by remember { mutableStateOf(false) }
	var showCodecDialog by remember { mutableStateOf(false) }
	var showRenderDialog by remember { mutableStateOf(false) }
	var showScaleDialog by remember { mutableStateOf(false) }
	var showHomeRecDialog by remember { mutableStateOf(false) }
	var showDefaultLoadDialog by remember { mutableStateOf(false) }
	var showHistoryNumDialog by remember { mutableStateOf(false) }
	var showSearchRemoteTvDialog by remember { mutableStateOf(false) }
	var showBackupDialog by remember { mutableStateOf(false) }
	var showAboutDialog by remember { mutableStateOf(false) }
	var showStoragePermissionRationale by remember { mutableStateOf(false) }
	var showRestartDialog by remember { mutableStateOf(false) }

	// 备份子对话框
	var backupList by remember { mutableStateOf(emptyList<String>()) }
	var selectedBackup by remember { mutableStateOf<String?>(null) }
	var restoreTarget by remember { mutableStateOf<String?>(null) }
	var deleteTarget by remember { mutableStateOf<String?>(null) }

	val needRestart = viewModel.needsRestart()

	BackHandler(enabled = needRestart) { showRestartDialog = true }

	val handleBack = {
		if (needRestart) showRestartDialog = true
		else onBack()
	}

	Scaffold(
		modifier = Modifier.fillMaxSize(),
		contentWindowInsets = WindowInsets(),
		containerColor = MaterialTheme.colorScheme.surfaceContainer,
		topBar = {
			TopAppBar(
				title = { Text("设置") },
				navigationIcon = {
					IconButton(onClick = { handleBack() }) {
						Icon(
							imageVector = Icons.AutoMirrored.Filled.ArrowBack,
							contentDescription = "返回"
						)
					}
				}
			)
		}
	) { padding ->
		LazyColumn(
			modifier = Modifier
				.fillMaxSize()
				.padding(padding),
			contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
		) {
			// 数据源
			item {
				SegmentedColumn(title = "数据源") {
					item {
						BaseWidget(
							icon = Icons.Filled.VideoLibrary,
							title = "配置点播地址",
							description = uiState.apiUrl.ifBlank { "设置应用点播数据源配置地址" },
							onClick = { showVodInputDialog = true }
						)
					}
					item {
						BaseWidget(
							icon = Icons.Filled.LiveTv,
							title = "配置直播地址",
							description = uiState.liveApiUrl.ifBlank { "设置应用直播数据源配置地址" },
							onClick = { showLiveInputDialog = true }
						)
					}
					item {
						BaseWidget(
							icon = Icons.Filled.QrCodeScanner,
							title = "扫码配置",
							description = "扫描二维码访问本机 Web 配置页",
							onClick = { showQrCodeDialog = true }
						)
					}
					item {
						BaseWidget(
							icon = Icons.Filled.VideoSettings,
							title = "首页站源",
							description = "选择默认首页数据源",
							onClick = { showHomeSourceDialog = true }
						)
					}
					item {
						BaseWidget(
							icon = Icons.Filled.Dns,
							title = "安全DNS",
							description = "选择DNS解析服务",
							onClick = { showDnsDialog = true }
						)
					}
				}
			}

			// 播放
			item {
				SegmentedColumn(title = "播放") {
					item {
						BaseWidget(
							title = "默认播放器",
							description = "选择视频播放器",
							onClick = { showPlayerDialog = true }
						)
					}
					item {
						BaseWidget(
							title = "IJK解码方式",
							description = "IJK播放器解码方式",
							onClick = { showCodecDialog = true }
						)
					}
					item {
						BaseWidget(
							title = "渲染方式",
							description = "视频画面渲染方式",
							onClick = { showRenderDialog = true }
						)
					}
					item {
						BaseWidget(
							title = "画面缩放",
							description = "默认画面缩放比例",
							onClick = { showScaleDialog = true }
						)
					}
					item {
						SwitchWidget(
							title = "IJK缓存播放",
							description = "开启IJK缓存",
							checked = uiState.ijkCachePlay,
							onCheckedChange = { viewModel.setIjkCachePlay(it) }
						)
					}
					item {
						SwitchWidget(
							title = "去广告",
							description = "过滤M3U8视频广告",
							checked = uiState.m3u8Purify,
							onCheckedChange = { viewModel.setM3u8Purify(it) }
						)
					}
				}
			}

			// 界面
			item {
				SegmentedColumn(title = "界面") {
					item {
						BaseWidget(
							title = "首页推荐",
							description = "设置首页推荐内容",
							onClick = { showHomeRecDialog = true }
						)
					}
					item {
						BaseWidget(
							title = "启动方式",
							description = "设置启动后默认页面",
							onClick = { showDefaultLoadDialog = true }
						)
					}
					item {
						BaseWidget(
							title = "保留历史记录",
							description = "保留历史记录的数量",
							onClick = { showHistoryNumDialog = true }
						)
					}
				}
			}

			// 高级
			item {
				SegmentedColumn(title = "高级") {
					item {
						BaseWidget(
							icon = Icons.Filled.Storage,
							title = "存储权限",
							description = "请求存储权限用于备份恢复",
							onClick = {
								if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
									if (Environment.isExternalStorageManager()) {
										Toast.makeText(context, "已获得存储权限", Toast.LENGTH_SHORT).show()
									} else {
										showStoragePermissionRationale = true
									}
								} else {
									storagePermissionLauncher.launch(
										arrayOf(
											Manifest.permission.READ_EXTERNAL_STORAGE,
											Manifest.permission.WRITE_EXTERNAL_STORAGE
										)
									)
								}
							}
						)
					}
					item {
						BaseWidget(
							icon = Icons.Filled.SettingsInputAntenna,
							title = "搜索附近TVBox",
							description = "搜索局域网内的其他TVBox设备",
							onClick = { showSearchRemoteTvDialog = true }
						)
					}
					item {
						BaseWidget(
							icon = Icons.Filled.CleaningServices,
							title = "清空缓存",
							description = "清空播放缓存和JAR缓存",
							onClick = { clearCache(viewModel) }
						)
					}
					item {
						SwitchWidget(
							icon = Icons.Filled.BugReport,
							title = "调试模式",
							description = "开启应用调试信息",
							checked = uiState.debugOpen,
							onCheckedChange = { viewModel.setDebugOpen(it) }
						)
					}
				}
			}

			// 其他
			item {
				SegmentedColumn(title = "其他") {
					item {
						BaseWidget(
							icon = Icons.Filled.Backup,
							title = "备份与恢复",
							description = "备份或恢复应用数据",
							onClick = {
								backupList = allBackups()
								showBackupDialog = true
							}
						)
					}
					item {
						BaseWidget(
							icon = Icons.Filled.Info,
							title = "关于",
							description = "应用版本和信息",
							onClick = { showAboutDialog = true }
						)
					}
				}
			}

			item { Spacer(Modifier.height(96.dp)) }
		}
	}

	// ========== 对话框 ==========

	if (showVodInputDialog) {
		AddressInputDialog(
			title = "配置点播地址",
			initialValue = uiState.apiUrl,
			history = viewModel.getApiHistory(),
			onConfirm = { url -> viewModel.setVodApi(url) },
			onDismiss = { showVodInputDialog = false }
		)
	}

	if (showLiveInputDialog) {
		AddressInputDialog(
			title = "配置直播地址",
			initialValue = uiState.liveApiUrl,
			history = viewModel.getLiveApiHistory(),
			onConfirm = { url -> viewModel.setLiveApi(url) },
			onDismiss = { showLiveInputDialog = false }
		)
	}

	if (showQrCodeDialog) {
		QrCodeDialog(onDismiss = { showQrCodeDialog = false })
	}

	if (showHomeSourceDialog) {
		val sites = ApiConfig.instance.switchSourceBeanList
		if (sites.isNotEmpty()) {
			val names = sites.map { it.name }
			val selectedIdx = sites.indexOf(ApiConfig.instance.homeSourceBean).coerceAtLeast(0)
			SingleChoiceDialog(
				title = "请选择首页数据源",
				options = names,
				selected = selectedIdx,
				onConfirm = { idx ->
					viewModel.setHomeSource(sites[idx])
					showHomeSourceDialog = false
				},
				onDismiss = { showHomeSourceDialog = false }
			)
		} else {
			LaunchedEffect(Unit) {
				viewModel.showToast("无可用数据源")
				showHomeSourceDialog = false
			}
		}
	}

	if (showDnsDialog) {
		val dnsList = SettingsViewModel.dnsList
		SingleChoiceDialog(
			title = "请选择安全DNS",
			options = dnsList,
			selected = uiState.dnsOpt,
			onConfirm = { idx ->
				viewModel.setDns(idx)
				showDnsDialog = false
			},
			onDismiss = { showDnsDialog = false }
		)
	}

	if (showPlayerDialog) {
		val players = com.github.tvbox.osc.util.PlayerHelper.existPlayerTypes
		val names = players.map { com.github.tvbox.osc.util.PlayerHelper.getPlayerName(it) }
		val selectedIdx = players.indexOf(uiState.playType).coerceAtLeast(0)
		SingleChoiceDialog(
			title = "请选择默认播放器",
			options = names,
			selected = selectedIdx,
			onConfirm = { idx ->
				viewModel.setPlayType(players[idx])
				showPlayerDialog = false
			},
			onDismiss = { showPlayerDialog = false }
		)
	}

	if (showCodecDialog) {
		val codecs = ApiConfig.instance.ijkCodes
		if (codecs.isNotEmpty()) {
			val names = codecs.map { it.name ?: "" }
			val selectedIdx = codecs.indexOfFirst { it.name == uiState.ijkCodec }.coerceAtLeast(0)
			SingleChoiceDialog(
				title = "请选择IJK解码方式",
				options = names,
				selected = selectedIdx,
				onConfirm = { idx ->
					viewModel.setCodec(codecs[idx])
					showCodecDialog = false
				},
				onDismiss = { showCodecDialog = false }
			)
		} else {
			LaunchedEffect(Unit) {
				viewModel.showToast("无可用解码方式")
				showCodecDialog = false
			}
		}
	}

	if (showRenderDialog) {
		val names = listOf(
			com.github.tvbox.osc.util.PlayerHelper.getRenderName(0),
			com.github.tvbox.osc.util.PlayerHelper.getRenderName(1)
		)
		SingleChoiceDialog(
			title = "请选择默认渲染方式",
			options = names,
			selected = uiState.playRender,
			onConfirm = { idx ->
				viewModel.setRender(idx)
				showRenderDialog = false
			},
			onDismiss = { showRenderDialog = false }
		)
	}

	if (showScaleDialog) {
		val names = (0..5).map { com.github.tvbox.osc.util.PlayerHelper.getScaleName(it) }
		SingleChoiceDialog(
			title = "请选择默认画面缩放",
			options = names,
			selected = uiState.playScale,
			onConfirm = { idx ->
				viewModel.setScale(idx)
				showScaleDialog = false
			},
			onDismiss = { showScaleDialog = false }
		)
	}

	if (showHomeRecDialog) {
		val names = (0..2).map { SettingsViewModel.getHomeRecName(it) }
		SingleChoiceDialog(
			title = "请选择首页列表数据",
			options = names,
			selected = uiState.homeRec,
			onConfirm = { idx ->
				viewModel.setHomeRec(idx)
				showHomeRecDialog = false
			},
			onDismiss = { showHomeRecDialog = false }
		)
	}

	if (showDefaultLoadDialog) {
		val names = listOf("点播", "直播")
		val selectedIdx = if (uiState.defaultLoadLive) 1 else 0
		SingleChoiceDialog(
			title = "设置启动后默认页面",
			options = names,
			selected = selectedIdx,
			onConfirm = { idx ->
				viewModel.setDefaultLoadLive(idx == 1)
				showDefaultLoadDialog = false
			},
			onDismiss = { showDefaultLoadDialog = false }
		)
	}

	if (showHistoryNumDialog) {
		val names = (0..2).map {
			com.github.tvbox.osc.util.HistoryHelper.getHistoryNumName(it)
		}
		SingleChoiceDialog(
			title = "保留历史记录数量",
			options = names,
			selected = uiState.historyNum,
			onConfirm = { idx ->
				viewModel.setHistoryNum(idx)
				showHistoryNumDialog = false
			},
			onDismiss = { showHistoryNumDialog = false }
		)
	}

	if (showSearchRemoteTvDialog) {
		SearchRemoteTvDialog(
			onDismiss = { showSearchRemoteTvDialog = false },
			onSelected = { host ->
				RemoteTVBox.available = host
				viewModel.showToast("设置成功")
				showSearchRemoteTvDialog = false
			}
		)
	}

	if (showBackupDialog) {
		BackupDialog(
			backups = backupList,
			onBackup = {
				performBackup(viewModel) {
					backupList = allBackups()
					showBackupDialog = true
				}
			},
			onSelectBackup = { name ->
				showBackupDialog = false
				selectedBackup = name
			},
			onDismiss = { showBackupDialog = false }
		)
	}

	selectedBackup?.let { name ->
		BackupActionDialog(
			backupName = name,
			onRestore = {
				restoreTarget = name
				selectedBackup = null
			},
			onDelete = {
				deleteTarget = name
				selectedBackup = null
			},
			onDismiss = { selectedBackup = null }
		)
	}

	restoreTarget?.let { name ->
		ConfirmDialog(
			title = "确认恢复",
			message = "确定要恢复备份 $name 吗？\n恢复后应用将自动重启。",
			confirmText = "确定",
			onConfirm = {
				performRestore(context, name, viewModel) {
					restoreTarget = null
				}
			},
			onDismiss = { restoreTarget = null }
		)
	}

	deleteTarget?.let { name ->
		ConfirmDialog(
			title = "确认删除",
			message = "确定要删除备份 $name 吗？",
			confirmText = "删除",
			onConfirm = {
				val root = Environment.getExternalStorageDirectory().absolutePath
				FileUtils.recursiveDelete(File("$root/tvbox_backup/$name"))
				viewModel.showToast("删除成功")
				deleteTarget = null
				backupList = allBackups()
				showBackupDialog = true
			},
			onDismiss = { deleteTarget = null }
		)
	}

	if (showStoragePermissionRationale) {
		ConfirmDialog(
			title = "需要存储权限",
			message = "应用需要访问存储权限以进行备份和恢复操作。\n\n请在设置中授予\"允许访问所有文件\"权限。",
			confirmText = "去设置",
			onConfirm = {
				try {
					val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
					intent.data = "package:${context.packageName}".toUri()
					settingsLauncher.launch(intent)
				} catch (_: Exception) {
					settingsLauncher.launch(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
				}
			},
			onDismiss = { showStoragePermissionRationale = false }
		)
	}

	if (showRestartDialog) {
		ConfirmDialog(
			title = "需要重启",
			message = "已修改的设置项需要重启应用后生效，是否立即重启？",
			confirmText = "重启",
			onConfirm = { restartApp(context) },
			onDismiss = { showRestartDialog = false }
		)
	}

	if (showAboutDialog) {
		AboutDialog(onDismiss = { showAboutDialog = false })
	}
}

private fun clearCache(viewModel: SettingsViewModel) {
	val cachePath = FileUtils.cachePath
	val cacheDir = File(cachePath)
	val cspCachePath = "${FileUtils.filePath}/csp/"
	val cspCacheDir = File(cspCachePath)

	if (!cacheDir.exists() && !cspCacheDir.exists()) {
		viewModel.showToast("缓存已为空")
		return
	}

	Thread {
		try {
			if (cacheDir.exists()) FileUtils.cleanDirectory(cacheDir)
			if (cspCacheDir.exists()) FileUtils.cleanDirectory(cspCacheDir)
			Handler(Looper.getMainLooper()).post {
				viewModel.showToast("播放和JAR缓存已清空")
			}
		} catch (e: Exception) {
			e.printStackTrace()
			Handler(Looper.getMainLooper()).post {
				viewModel.showToast("清空缓存失败")
			}
		}
	}.start()
}

private fun allBackups(): List<String> {
	val result = mutableListOf<String>()
	try {
		val root = Environment.getExternalStorageDirectory().absolutePath
		val file = File("$root/tvbox_backup/")
		if (file.exists()) {
			val list = file.listFiles() ?: return result
			list.sortedByDescending { it.name }
			for (f in list) {
				if (result.size > 10) {
					FileUtils.recursiveDelete(f)
					continue
				}
				if (f.isDirectory) {
					result.add(f.name)
				}
			}
		}
	} catch (e: Throwable) {
		e.printStackTrace()
	}
	return result
}

private fun performBackup(
	viewModel: SettingsViewModel,
	onDone: () -> Unit
) {
	val scope = kotlinx.coroutines.CoroutineScope(Dispatchers.Main)
	scope.launch {
		withContext(Dispatchers.IO) {
			try {
				val root = Environment.getExternalStorageDirectory().absolutePath
				val file = File("$root/tvbox_backup/")
				if (!file.exists()) file.mkdirs()
				val now = Date()
				val f = SimpleDateFormat("yyyy-MM-dd-HHmmss", Locale.getDefault())
				val backup = File(file, f.format(now))
				backup.mkdirs()
				val db = File(backup, "sqlite")
				if (AppDataManager.backup(db)) {
					PreferenceStore.exportFile(File(backup, "preferences"))
					withContext(Dispatchers.Main) {
						viewModel.showToast("备份成功")
						onDone()
					}
				} else {
					backup.delete()
					withContext(Dispatchers.Main) { viewModel.showToast("备份失败") }
				}
			} catch (e: Throwable) {
				e.printStackTrace()
				withContext(Dispatchers.Main) { viewModel.showToast("备份失败") }
			}
		}
	}
}

private fun performRestore(
	context: android.content.Context,
	dir: String,
	viewModel: SettingsViewModel,
	onDone: () -> Unit
) {
	try {
		val root = Environment.getExternalStorageDirectory().absolutePath
		val backup = File("$root/tvbox_backup/$dir")
		if (backup.exists()) {
			val db = File(backup, "sqlite")
			if (AppDataManager.restore(db)) {
				PreferenceStore.importFile(File(backup, "preferences"))
				viewModel.showToast("恢复成功，即将自动重启应用")
				Handler(Looper.getMainLooper()).postDelayed({
					restartApp(context)
				}, 3000)
			} else {
				viewModel.showToast("恢复失败")
			}
		}
	} catch (e: Throwable) {
		e.printStackTrace()
	} finally {
		onDone()
	}
}

private fun restartApp(context: android.content.Context) {
	val i = context.packageManager.getLaunchIntentForPackage(context.packageName)
	if (i != null) {
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
		context.startActivity(i)
		kotlin.system.exitProcess(0)
	}
}

private fun checkStoragePermission(context: android.content.Context): Boolean {
	return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
		Environment.isExternalStorageManager()
	} else {
		val readPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
		val writePermission = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
		readPermission == PackageManager.PERMISSION_GRANTED && writePermission == PackageManager.PERMISSION_GRANTED
	}
}
