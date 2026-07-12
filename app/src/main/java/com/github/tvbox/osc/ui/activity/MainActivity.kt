package com.github.tvbox.osc.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.github.tvbox.osc.server.ControlManager
import com.github.tvbox.osc.ui.compose.theme.TVBoxTheme
import com.github.tvbox.osc.ui.home.DetailScreen
import com.github.tvbox.osc.ui.home.HomeScreen
import com.github.tvbox.osc.ui.home.HomeViewModel
import com.github.tvbox.osc.ui.push.PushScreen
import com.github.tvbox.osc.ui.setting.SettingsScreen
import com.github.tvbox.osc.ui.setting.SettingsViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
	private val pendingSearch = MutableStateFlow<String?>(null)

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
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
							onOpenSettings = { navController.navigate("settings") },
							onOpenPush = { navController.navigate("push") },
							onNavigateToDetail = { sourceKey, id, picture ->
								val encodedKey = Uri.encode(sourceKey)
								val encodedId = Uri.encode(id)
								val encodedPic = Uri.encode(picture ?: "")
								navController.navigate("detail/$encodedKey/$encodedId?picture=$encodedPic")
							}
						)
					}
					composable("settings") {
						val settingsViewModel: SettingsViewModel = viewModel()
						SettingsScreen(
							viewModel = settingsViewModel,
							onBack = { navController.popBackStack() }
						)
					}
					composable("push") {
						PushScreen(
							onBack = { navController.popBackStack() },
							onNavigateToDetail = { sourceKey, id, picture ->
								val encodedKey = Uri.encode(sourceKey)
								val encodedId = Uri.encode(id)
								val encodedPic = Uri.encode(picture ?: "")
								navController.navigate("detail/$encodedKey/$encodedId?picture=$encodedPic")
							}
						)
					}
					composable(
						route = "detail/{sourceKey}/{id}?picture={picture}",
						arguments = listOf(
							navArgument("sourceKey") { type = NavType.StringType },
							navArgument("id") { type = NavType.StringType },
							navArgument("picture") {
								type = NavType.StringType
								defaultValue = ""
							}
						)
					) { backStackEntry ->
						val args = backStackEntry.arguments!!
						val detailViewModel: com.github.tvbox.osc.ui.home.DetailViewModel = viewModel()
						DetailScreen(
							sourceKey = args.getString("sourceKey")!!,
							id = args.getString("id")!!,
							picture = args.getString("picture") ?: "",
							onBack = { navController.popBackStack() },
							onNavigateToDetail = { sourceKey, id, picture ->
								val encodedKey = Uri.encode(sourceKey)
								val encodedId = Uri.encode(id)
								val encodedPic = Uri.encode(picture ?: "")
								navController.navigate("detail/$encodedKey/$encodedId?picture=$encodedPic")
							},
							viewModel = detailViewModel
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
