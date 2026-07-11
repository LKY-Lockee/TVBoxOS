package com.github.tvbox.osc.ui.home

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.tvbox.osc.R
import com.github.tvbox.osc.ui.activity.DetailActivity
import com.github.tvbox.osc.ui.compose.component.AdaptiveVodGrid
import com.github.tvbox.osc.ui.compose.component.RefreshContentBox
import com.github.tvbox.osc.ui.compose.component.SearchWordRow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
	toolbarState: HomeToolbarState,
	pendingSearch: String?,
	onConsumePendingSearch: () -> Unit,
	modifier: Modifier = Modifier
) {
	val context = LocalContext.current
	val searchVm: SearchViewModel = viewModel()

	val isSearching by searchVm.isSearching.collectAsState()
	val progress by searchVm.progress.collectAsState()
	val tabs by searchVm.tabs.collectAsState()
	val currentFilter by searchVm.currentFilter.collectAsState()
	val results by searchVm.results.collectAsState()
	val suggestions by searchVm.suggestions.collectAsState()
	val toast by searchVm.toast.collectAsState()

	val textFieldState = rememberTextFieldState()
	val searchBarState = rememberSearchBarState()
	val scope = rememberCoroutineScope()
	var showClearDialog by remember { mutableStateOf(false) }
	var deleteWord by remember { mutableStateOf<String?>(null) }

	val pageActions = remember { listOf(ToolbarAction(R.drawable.icon_delete, "清空搜索记录") { showClearDialog = true }) }
	SideEffect {
		toolbarState.title = "搜索"
		toolbarState.actions = pageActions
	}

	LaunchedEffect(toast) {
		toast?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show(); searchVm.consumeToast() }
	}

	LaunchedEffect(textFieldState) {
		snapshotFlow { textFieldState.text.toString() }
			.distinctUntilChanged()
			.collect { value ->
				val keyword = value.trim()
				if (keyword.isNotEmpty()) searchVm.loadSearchSuggestions(keyword)
				else searchVm.loadHistoryAndHotWords()
			}
	}

	LaunchedEffect(pendingSearch) {
		pendingSearch?.let {
			if (it.isNotEmpty()) {
				textFieldState.setTextAndPlaceCursorAtEnd(it)
				searchBarState.animateToCollapsed()
				searchVm.search(it)
			}
			onConsumePendingSearch()
		}
	}

	val keyboard = LocalSoftwareKeyboardController.current

	fun submitSearch() {
		val keyword = textFieldState.text.toString().trim()
		keyboard?.hide()
		scope.launch { searchBarState.animateToCollapsed() }
		searchVm.search(keyword)
	}

	fun refreshSearch() {
		val keyword = textFieldState.text.toString().trim()
		if (keyword.isNotEmpty()) searchVm.search(keyword)
		else searchVm.loadHistoryAndHotWords()
	}

	val inputField: @Composable () -> Unit = {
		SearchBarDefaults.InputField(
			textFieldState = textFieldState,
			searchBarState = searchBarState,
			onSearch = { submitSearch() },
			placeholder = { Text("搜索...") },
			leadingIcon = {
				Icon(Icons.Filled.Search, contentDescription = "搜索")
			},
			trailingIcon = {
				if (isSearching) {
					IconButton(onClick = { searchVm.cancel() }) {
						Icon(Icons.Default.Close, contentDescription = "停止")
					}
				}
			}
		)
	}

	Box(modifier.fillMaxSize()) {
		Column(Modifier.fillMaxSize()) {
			if (isSearching && progress.second > 0) {
				LinearWavyProgressIndicator(
					progress = { progress.first.toFloat() / progress.second },
					modifier = Modifier.fillMaxWidth()
				)
			}
			if (tabs.isNotEmpty()) {
				PrimaryScrollableTabRow(selectedTabIndex = tabs.indexOfFirst { it.key == currentFilter }.coerceAtLeast(0)) {
					tabs.forEach { tab ->
						Tab(
							selected = tab.key == currentFilter,
							onClick = { searchVm.selectFilter(tab.key) },
							text = { Text("${tab.name} (${tab.count})") }
						)
					}
				}
			}
			RefreshContentBox(
				isRefreshing = isSearching,
				isEmpty = results.isEmpty(),
				onRefresh = ::refreshSearch,
				modifier = Modifier.fillMaxSize()
			) {
				AdaptiveVodGrid(
					items = results.filterNotNull(),
					name = { it.name },
					pic = { it.pic },
					year = { it.year },
					note = { it.note },
					onClick = { video ->
						context.startActivity(
							Intent(context, DetailActivity::class.java).apply {
								putExtra("id", video.id)
								putExtra("sourceKey", video.sourceKey)
							}
						)
					},
					modifier = Modifier.fillMaxSize(),
					contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 96.dp),
					key = { video -> video.sourceKey + "_" + video.id }
				)
			}
		}

		SearchBar(
			state = searchBarState,
			inputField = inputField,
			modifier = Modifier
				.align(Alignment.BottomCenter)
				.fillMaxWidth()
				.imePadding()
				.padding(horizontal = 12.dp, vertical = 12.dp)
		)

		ExpandedFullScreenSearchBar(
			state = searchBarState,
			windowInsets = { SearchBarDefaults.windowInsets },
			inputField = inputField
		) {
			LazyColumn(Modifier.fillMaxSize()) {
				items(suggestions) { item ->
					SearchWordRow(
						item = item,
						onClick = {
							textFieldState.setTextAndPlaceCursorAtEnd(item.title)
							submitSearch()
						},
						onLongClick = if (item.type == 0) ({ deleteWord = item.title }) else null
					)
				}

				item { Spacer(Modifier.height(96.dp)) }
			}
		}
	}

	if (showClearDialog) {
		AlertDialog(
			onDismissRequest = { showClearDialog = false },
			title = { Text("清空搜索记录") },
			text = { Text("确定要清空所有搜索记录吗？") },
			confirmButton = {
				TextButton(onClick = {
					showClearDialog = false
					searchVm.clearSearchHistory()
					Toast.makeText(context, "已清空搜索记录", Toast.LENGTH_SHORT).show()
				}) { Text("清空") }
			},
			dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("取消") } }
		)
	}

	deleteWord?.let { word ->
		AlertDialog(
			onDismissRequest = { deleteWord = null },
			title = { Text("删除搜索记录") },
			text = { Text("确定要删除「$word」吗？") },
			confirmButton = {
				TextButton(onClick = { deleteWord = null; searchVm.deleteSearchWord(word) }) { Text("删除") }
			},
			dismissButton = { TextButton(onClick = { deleteWord = null }) { Text("取消") } }
		)
	}
}
