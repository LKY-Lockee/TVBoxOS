package com.github.tvbox.osc.ui.home

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import com.github.tvbox.osc.R

/** 顶部工具栏动作。 */
data class ToolbarAction(val icon: ImageVector, val contentDescription: String, val onClick: () -> Unit)

/** 由当前页面持有的工具栏状态（标题 + 动作），供 HomeScreen 顶部栏渲染。 */
class HomeToolbarState {
	var title: String by mutableStateOf("")
	var actions: List<ToolbarAction> by mutableStateOf(emptyList())
}

private data class NavItem(
	val tab: HomeTab?,
	val label: String,
	val unselectedIcon: ImageVector,
	val selectedIcon: ImageVector
)

private enum class HomeNavigationType {
	BottomBar,
	NavigationRail
}

private data class HomeAdaptiveInfo(
	val navigationType: HomeNavigationType
) {
	val showBottomBar: Boolean
		get() = navigationType == HomeNavigationType.BottomBar
}

private val NAV_ITEMS = listOf(
	NavItem(HomeTab.Home, "主页", Icons.Outlined.Home, Icons.Filled.Home),
	NavItem(HomeTab.History, "历史", Icons.Outlined.History, Icons.Filled.History),
	NavItem(HomeTab.Search, "搜索", Icons.Outlined.Search, Icons.Filled.Search),
	NavItem(HomeTab.Collect, "收藏", Icons.Outlined.FavoriteBorder, Icons.Filled.Favorite),
	NavItem(null, "直播", Icons.Outlined.LiveTv, Icons.Filled.LiveTv)
)

private val HomeTab.route: String
	get() = when (this) {
		HomeTab.Home -> "home"
		HomeTab.History -> "history"
		HomeTab.Search -> "search"
		HomeTab.Collect -> "collect"
	}

private fun routeToHomeTab(route: String?): HomeTab = HomeTab.entries.firstOrNull { it.route == route } ?: HomeTab.Home

@Composable
private fun rememberHomeAdaptiveInfo(): HomeAdaptiveInfo {
	val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
	val useNavigationRail = windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND)

	return HomeAdaptiveInfo(
		navigationType = if (useNavigationRail) {
			HomeNavigationType.NavigationRail
		} else {
			HomeNavigationType.BottomBar
		}
	)
}

private fun NavHostController.navigateToHomeTab(tab: HomeTab) {
	val route = tab.route
	if (currentBackStackEntry?.destination?.route == route) return
	navigate(route) {
		popUpTo(graph.findStartDestination().id) { saveState = true }
		launchSingleTop = true
		restoreState = true
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
	homeViewModel: HomeViewModel,
	searchRequest: String?,
	onSearchRequestConsumed: () -> Unit,
	onLaunchLive: () -> Unit,
	onOpenSettings: () -> Unit,
	onOpenPush: () -> Unit,
	onNavigateToDetail: (String, String, String?) -> Unit,
	onSwitchToSearchAndSearch: (String?) -> Unit = { homeViewModel.requestSearch(it) }
) {
	val context = LocalContext.current
	val uiState by homeViewModel.uiState.collectAsState()
	val requestedPage by homeViewModel.requestedPage.collectAsState()
	val pendingSearch by homeViewModel.pendingSearch.collectAsState()
	val navController = rememberNavController()
	val backStackEntry by navController.currentBackStackEntryAsState()
	val currentTab = routeToHomeTab(backStackEntry?.destination?.route)
	val toolbarState = remember { HomeToolbarState() }

	LaunchedEffect(Unit) {
		homeViewModel.toast.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
	}
	LaunchedEffect(Unit) {
		homeViewModel.launchLive.collect { onLaunchLive() }
	}
	LaunchedEffect(requestedPage) {
		requestedPage?.let { page ->
			navController.navigateToHomeTab(page)
			homeViewModel.consumeRequestedPage()
		}
	}
	LaunchedEffect(searchRequest) {
		searchRequest?.let {
			onSearchRequestConsumed()
			homeViewModel.requestSearch(it)
		}
	}

	fun selectTab(tab: HomeTab?) {
		if (tab == null) onLaunchLive()
		else navController.navigateToHomeTab(tab)
	}

	when (val state = uiState) {
		is HomeUiState.Loading -> {
			Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
				CircularWavyProgressIndicator()
			}
			return
		}

		is HomeUiState.Error -> {
			AlertDialog(
				onDismissRequest = { homeViewModel.dismissErrorAndContinue() },
				title = { Text("提示") },
				text = { Text(state.message) },
				confirmButton = {
					TextButton(onClick = { homeViewModel.dismissErrorAndContinue() }) { Text("重试") }
				},
				dismissButton = {
					TextButton(onClick = { homeViewModel.dismissErrorAndContinue() }) { Text("取消") }
				}
			)
		}

		HomeUiState.Ready -> {}
	}

	val adaptiveInfo = rememberHomeAdaptiveInfo()
	val showBottomBar = adaptiveInfo.showBottomBar

	Scaffold(
		modifier = Modifier.fillMaxSize(),
		contentWindowInsets = WindowInsets(),
		topBar = {
			TopAppBar(
				title = { Text(toolbarState.title.ifEmpty { stringResource(R.string.app_name) }) },
				actions = {
					HomeOverflowMenu(
						actions = toolbarState.actions,
						onOpenSettings = onOpenSettings,
						onOpenPush = onOpenPush
					)
				},
			)
		},
		bottomBar = {
			if (showBottomBar) {
				HomeNavigationBar(
					currentTab = currentTab,
					onSelectTab = ::selectTab
				)
			}
		}
	) { padding ->
		if (showBottomBar) {
			HomeNavHost(
				navController = navController,
				navHostState = HomeNavHostState(
					toolbarState = toolbarState,
					onSwitchToSearchAndSearch = onSwitchToSearchAndSearch,
					onNavigateToDetail = onNavigateToDetail,
					pendingSearch = pendingSearch,
					onConsumePendingSearch = { homeViewModel.consumePendingSearch() }
				),
				modifier = Modifier
					.fillMaxSize()
					.padding(padding)
			)
		} else {
			Row(
				Modifier
					.fillMaxSize()
					.padding(top = padding.calculateTopPadding())
			) {
				HomeNavigationRail(
					currentTab = currentTab,
					onSelectTab = ::selectTab
				)
				HomeNavHost(
					navController = navController,
					navHostState = HomeNavHostState(
						toolbarState = toolbarState,
						onSwitchToSearchAndSearch = onSwitchToSearchAndSearch,
						onNavigateToDetail = onNavigateToDetail,
						pendingSearch = pendingSearch,
						onConsumePendingSearch = { homeViewModel.consumePendingSearch() }
					),
					modifier = Modifier
						.weight(1f)
						.fillMaxSize()
				)
			}
		}
	}
}

private data class HomeNavHostState(
	val toolbarState: HomeToolbarState,
	val onSwitchToSearchAndSearch: (String?) -> Unit,
	val onNavigateToDetail: (String, String, String?) -> Unit,
	val pendingSearch: String?,
	val onConsumePendingSearch: () -> Unit
)

@Composable
private fun HomeOverflowMenu(
	actions: List<ToolbarAction>,
	onOpenSettings: () -> Unit,
	onOpenPush: () -> Unit
) {
	var expanded by remember { mutableStateOf(false) }

	IconButton(onClick = { expanded = true }) {
		Icon(Icons.Filled.MoreVert, contentDescription = "更多选项")
	}
	DropdownMenu(
		expanded = expanded,
		onDismissRequest = { expanded = false }
	) {
		actions.forEach { action ->
			DropdownMenuItem(
				text = { Text(action.contentDescription) },
				leadingIcon = {
					Icon(action.icon, contentDescription = null)
				},
				onClick = {
					expanded = false
					action.onClick()
				}
			)
		}
		if (actions.isNotEmpty()) {
			HorizontalDivider()
		}
		DropdownMenuItem(
			text = { Text("推送") },
			leadingIcon = {
				Icon(Icons.Outlined.CloudUpload, contentDescription = null)
			},
			onClick = {
				expanded = false
				onOpenPush()
			}
		)
		DropdownMenuItem(
			text = { Text(stringResource(R.string.action_settings)) },
			leadingIcon = {
				Icon(Icons.Outlined.Settings, contentDescription = null)
			},
			onClick = {
				expanded = false
				onOpenSettings()
			}
		)
	}
}

@Composable
private fun HomeNavHost(
	navController: NavHostController,
	navHostState: HomeNavHostState,
	modifier: Modifier = Modifier
) {
	NavHost(
		navController = navController,
		startDestination = HomeTab.Home.route,
		modifier = modifier
	) {
		composable(HomeTab.Home.route) {
			HomePagerPage(tab = HomeTab.Home, state = navHostState)
		}
		composable(HomeTab.History.route) {
			HomePagerPage(tab = HomeTab.History, state = navHostState)
		}
		composable(HomeTab.Search.route) {
			HomePagerPage(tab = HomeTab.Search, state = navHostState)
		}
		composable(HomeTab.Collect.route) {
			HomePagerPage(tab = HomeTab.Collect, state = navHostState)
		}
	}
}

@Composable
private fun HomeNavigationBar(
	currentTab: HomeTab,
	onSelectTab: (HomeTab?) -> Unit
) {
	NavigationBar {
		NAV_ITEMS.forEach { item ->
			NavigationBarItem(
				icon = { HomeNavigationIcon(item = item, currentTab = currentTab) },
				label = { Text(item.label) },
				onClick = { onSelectTab(item.tab) },
				selected = item.tab != null && item.tab == currentTab
			)
		}
	}
}

@Composable
private fun HomeNavigationRail(
	currentTab: HomeTab,
	onSelectTab: (HomeTab?) -> Unit
) {
	NavigationRail {
		NAV_ITEMS.forEach { item ->
			NavigationRailItem(
				icon = { HomeNavigationIcon(item = item, currentTab = currentTab) },
				label = { Text(item.label) },
				onClick = { onSelectTab(item.tab) },
				selected = item.tab != null && item.tab == currentTab
			)
		}
	}
}

@Composable
private fun HomeNavigationIcon(
	item: NavItem,
	currentTab: HomeTab
) {
	Icon(
		imageVector = if (item.tab != null && item.tab == currentTab) item.selectedIcon else item.unselectedIcon,
		contentDescription = item.label
	)
}

/** 各页内容。 */
@Composable
private fun HomePagerPage(
	tab: HomeTab,
	state: HomeNavHostState
) {
	when (tab) {
		HomeTab.Home -> HomeTabScreen(
			toolbarState = state.toolbarState,
			onSwitchToSearchAndSearch = state.onSwitchToSearchAndSearch,
			onNavigateToDetail = state.onNavigateToDetail
		)

		HomeTab.History -> HistoryScreen(
			toolbarState = state.toolbarState,
			onSwitchToSearchAndSearch = state.onSwitchToSearchAndSearch,
			onNavigateToDetail = state.onNavigateToDetail
		)

		HomeTab.Search -> SearchScreen(
			toolbarState = state.toolbarState,
			pendingSearch = state.pendingSearch,
			onConsumePendingSearch = state.onConsumePendingSearch,
			onNavigateToDetail = state.onNavigateToDetail
		)

		HomeTab.Collect -> CollectScreen(
			toolbarState = state.toolbarState,
			onSwitchToSearchAndSearch = state.onSwitchToSearchAndSearch,
			onNavigateToDetail = state.onNavigateToDetail
		)
	}
}
