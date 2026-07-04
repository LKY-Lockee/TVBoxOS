package com.github.tvbox.osc.ui.activity

import android.content.Intent
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.github.tvbox.osc.base.BaseActivity
import com.github.tvbox.osc.server.ControlManager
import com.github.tvbox.osc.ui.compose.theme.TVBoxTheme
import com.github.tvbox.osc.ui.home.HomeScreen
import com.github.tvbox.osc.ui.home.HomeViewModel
import com.github.tvbox.osc.util.AppManager

class HomeActivity : BaseActivity() {
	private val homeViewModel: HomeViewModel by viewModels()

	override val layoutResID: Int = 0

	override fun init() {
		ControlManager.instance.startServer()

		setContent {
			TVBoxTheme {
				HomeScreen(
					homeViewModel = homeViewModel,
					onLaunchLive = { jumpActivity(LivePlayActivity::class.java) },
					onOpenSettings = { jumpActivity(SettingsActivity::class.java) },
					onExit = { exitApp() }
				)
			}
		}

		homeViewModel.initData(false)
		handleIntent(intent)
	}

	override fun onNewIntent(intent: Intent) {
		super.onNewIntent(intent)
		setIntent(intent)
		handleIntent(intent)
	}

	private fun handleIntent(intent: Intent?) {
		val bundle = intent?.extras ?: return
		if (!bundle.getBoolean("openSearch", false)) return
		homeViewModel.requestSearch(bundle.getString("searchTitle"))
	}

	override fun onDestroy() {
		super.onDestroy()
		ControlManager.instance.stopServer()
	}

	private fun exitApp() {
		AppManager.instance.finishAllActivity()
		ControlManager.instance.stopServer()
		AppManager.instance.appExit(0)
	}
}
