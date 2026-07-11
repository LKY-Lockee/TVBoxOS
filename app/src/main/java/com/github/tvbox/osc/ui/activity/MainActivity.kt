package com.github.tvbox.osc.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.github.tvbox.osc.server.ControlManager
import com.github.tvbox.osc.ui.compose.theme.TVBoxTheme
import com.github.tvbox.osc.ui.home.HomeScreen
import com.github.tvbox.osc.ui.home.HomeViewModel
import com.github.tvbox.osc.ui.setting.SettingsScreen
import com.github.tvbox.osc.ui.setting.SettingsViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
	private val pendingSearch = MutableStateFlow<String?>(null)

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		ControlManager.instance.startServer()

		setContent {
			TVBoxTheme {
				val navController = rememberNavController()
				val searchRequest by pendingSearch.collectAsState()

				NavHost(navController = navController, startDestination = "home") {
					composable("home") {
						val homeViewModel: HomeViewModel = viewModel()
						HomeScreen(
							homeViewModel = homeViewModel,
							searchRequest = searchRequest,
							onSearchRequestConsumed = { pendingSearch.value = null },
							onLaunchLive = { startActivity(Intent(this@MainActivity, LivePlayActivity::class.java)) },
							onOpenSettings = { navController.navigate("settings") }
						)
					}
					composable("settings") {
						val settingsViewModel: SettingsViewModel = viewModel()
						SettingsScreen(
							viewModel = settingsViewModel,
							onBack = { navController.popBackStack() }
						)
					}
				}
			}
		}

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
		pendingSearch.value = bundle.getString("searchTitle")
	}

	override fun onDestroy() {
		super.onDestroy()
		ControlManager.instance.stopServer()
	}
}
