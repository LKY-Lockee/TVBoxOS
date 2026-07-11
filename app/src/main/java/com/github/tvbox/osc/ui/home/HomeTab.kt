package com.github.tvbox.osc.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.tvbox.osc.R
import com.github.tvbox.osc.api.ApiConfig
import com.github.tvbox.osc.bean.MovieSort.SortData
import com.github.tvbox.osc.ui.compose.theme.Ts20
import com.github.tvbox.osc.util.DefaultConfig
import com.github.tvbox.osc.viewmodel.SourceViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeTabScreen(
	toolbarState: HomeToolbarState,
	onSwitchToSearchAndSearch: (String?) -> Unit
) {
	val sortVm: SourceViewModel = viewModel()
	val sortResult by sortVm.sortResult.observeAsState()
	var showSwitchDialog by remember { mutableStateOf(false) }

	val pageActions = remember { listOf(ToolbarAction(Icons.Outlined.SwapHoriz, "切换首页源") { showSwitchDialog = true }) }
	SideEffect {
		toolbarState.title = ""
		toolbarState.actions = pageActions
	}

	val sortDataList: List<SortData> = remember(sortResult) {
		val list = sortResult?.classes?.sortList ?: ArrayList()
		DefaultConfig.adjustSort(ApiConfig.instance.homeSourceBean.key, list, true)
	}

	val pagerState = rememberPagerState(pageCount = { sortDataList.size })
	val scope = rememberCoroutineScope()

	LaunchedEffect(Unit) { sortVm.getSort(ApiConfig.instance.homeSourceBean.key) }

	BackHandler(enabled = pagerState.currentPage != 0) {
		scope.launch { pagerState.animateScrollToPage(0) }
	}

	Column(Modifier.fillMaxSize()) {
		if (sortDataList.isNotEmpty()) {
			PrimaryScrollableTabRow(selectedTabIndex = pagerState.currentPage) {
				sortDataList.forEachIndexed { i, data ->
					Tab(
						selected = pagerState.currentPage == i,
						onClick = { scope.launch { pagerState.animateScrollToPage(i) } },
						text = { Text(data.name) }
					)
				}
			}
		}
		HorizontalPager(
			state = pagerState,
			modifier = Modifier
				.weight(1f)
				.fillMaxWidth()
		) { pageIndex ->
			val data = sortDataList[pageIndex]
			if (data.id == "my0") {
				UserScreen(data, onSwitchToSearchAndSearch)
			} else {
				CategoryGridScreen(data, onSwitchToSearchAndSearch)
			}
		}
	}

	if (showSwitchDialog) {
		SwitchSiteDialog(
			onDismiss = { showSwitchDialog = false },
			onSelect = { site ->
				ApiConfig.instance.setSourceBean(site)
				sortVm.getSort(site.key)
				showSwitchDialog = false
			}
		)
	}
}

@Composable
private fun SwitchSiteDialog(onDismiss: () -> Unit, onSelect: (com.github.tvbox.osc.bean.SourceBean) -> Unit) {
	val sites = ApiConfig.instance.switchSourceBeanList
	var selected by remember {
		mutableIntStateOf(sites.indexOf(ApiConfig.instance.homeSourceBean).coerceAtLeast(0))
	}
	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text("请选择首页数据源") },
		text = {
			Column {
				sites.forEachIndexed { i, site ->
					Row(
						modifier = Modifier
							.fillMaxWidth()
							.clickable {
								selected = i
								onSelect(site)
							}
							.padding(vertical = 8.dp),
						verticalAlignment = Alignment.CenterVertically
					) {
						RadioButton(selected = selected == i, onClick = { selected = i; onSelect(site) })
						Text(site.name, fontSize = Ts20)
					}
				}
			}
		},
		confirmButton = {},
		dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
	)
}
