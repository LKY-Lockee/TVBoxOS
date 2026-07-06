package com.github.tvbox.osc.ui.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import com.github.tvbox.osc.base.BaseActivity
import com.github.tvbox.osc.ui.compose.theme.TVBoxTheme
import com.github.tvbox.osc.ui.setting.SettingsExitAction
import com.github.tvbox.osc.ui.setting.SettingsScreen
import com.github.tvbox.osc.ui.setting.SettingsViewModel
import com.github.tvbox.osc.util.AppManager

class SettingsActivity : BaseActivity() {

	private val settingsViewModel: SettingsViewModel by viewModels()

	private var storagePermissionLauncher: ActivityResultLauncher<Array<String>>? = null
	private var settingsLauncher: ActivityResultLauncher<Intent>? = null

	private val handler = Handler(Looper.getMainLooper())
	private var devMode: String = ""
	private val devModeRun = Runnable { devMode = "" }

	override val layoutResID: Int = 0

	override fun init() {
		initPermissionLaunchers()

		setContent {
			TVBoxTheme {
				LaunchedEffect(Unit) {
					settingsViewModel.exitAction.collect { action ->
						when (action) {
							SettingsExitAction.ReloadFull -> {
								AppManager.instance.finishAllActivity()
								jumpActivity(HomeActivity::class.java)
							}

							SettingsExitAction.ReloadCache -> {
								jumpActivity(HomeActivity::class.java, createCacheBundle())
							}

							SettingsExitAction.ReloadLive -> {
								jumpActivity(HomeActivity::class.java)
							}

							SettingsExitAction.JustFinish -> {
								finish()
							}
						}
					}
				}
				SettingsScreen(
					viewModel = settingsViewModel,
					onBack = { settingsViewModel.onBack() },
					onRequestStoragePermission = { requestLegacyStoragePermission() },
					onOpenSystemStorageSettings = { intent -> settingsLauncher?.launch(intent) }
				)
			}
		}
	}

	private fun createCacheBundle(): Bundle {
		val bundle = Bundle()
		bundle.putBoolean("useCache", true)
		return bundle
	}

	private fun initPermissionLaunchers() {
		storagePermissionLauncher = registerForActivityResult(RequestMultiplePermissions()) { result ->
			val allGranted = result.values.all { it }
			if (allGranted) {
				Toast.makeText(this, "已获得存储权限", Toast.LENGTH_SHORT).show()
			} else {
				Toast.makeText(this, "存储权限被拒绝", Toast.LENGTH_SHORT).show()
			}
		}

		settingsLauncher = registerForActivityResult(StartActivityForResult()) {
			if (checkStoragePermission()) {
				Toast.makeText(this, "已获得存储权限", Toast.LENGTH_SHORT).show()
			}
		}
	}

	private fun requestLegacyStoragePermission() {
		val permissions = arrayOf(
			Manifest.permission.READ_EXTERNAL_STORAGE,
			Manifest.permission.WRITE_EXTERNAL_STORAGE
		)
		storagePermissionLauncher?.launch(permissions)
	}

	private fun checkStoragePermission(): Boolean {
		return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
			android.os.Environment.isExternalStorageManager()
		} else {
			val readPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
			val writePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
			readPermission == PackageManager.PERMISSION_GRANTED && writePermission == PackageManager.PERMISSION_GRANTED
		}
	}

	override fun dispatchKeyEvent(event: KeyEvent): Boolean {
		if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_0) {
			handler.removeCallbacks(devModeRun)
			devMode += "0"
			handler.postDelayed(devModeRun, 200)
			if (devMode.length >= 4) {
				Toast.makeText(this, "开发者模式已触发", Toast.LENGTH_SHORT).show()
			}
		}
		return super.dispatchKeyEvent(event)
	}
}
