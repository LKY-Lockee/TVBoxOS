package com.github.tvbox.osc.ui.home

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ClearAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.github.tvbox.osc.R
import com.github.tvbox.osc.api.ApiConfig
import com.github.tvbox.osc.cache.RoomDataManger
import com.github.tvbox.osc.cache.VodCollect
import com.github.tvbox.osc.event.RefreshEvent
import com.github.tvbox.osc.ui.activity.DetailActivity
import com.github.tvbox.osc.ui.compose.component.AdaptiveVodGrid
import com.github.tvbox.osc.ui.compose.component.RefreshContentBox
import com.github.tvbox.osc.ui.compose.util.rememberEventBusCallback

@Composable
fun CollectScreen(
	toolbarState: HomeToolbarState,
	onSwitchToSearchAndSearch: (String?) -> Unit,
	modifier: Modifier = Modifier
) {
	val context = LocalContext.current
	var list by remember { mutableStateOf<List<VodCollect>>(emptyList()) }
	var refreshing by remember { mutableStateOf(false) }
	var loaded by remember { mutableStateOf(false) }
	var showClearDialog by remember { mutableStateOf(false) }
	var deleteTarget by remember { mutableStateOf<VodCollect?>(null) }

	fun loadData() {
		list = RoomDataManger.getAllVodCollect()
		loaded = true
		refreshing = false
	}

	val pageActions = remember { listOf(ToolbarAction(Icons.Outlined.ClearAll, "清空收藏") { showClearDialog = true }) }
	SideEffect {
		toolbarState.title = "收藏"
		toolbarState.actions = pageActions
	}

	LaunchedEffect(Unit) { loadData() }
	rememberEventBusCallback<RefreshEvent> { e ->
		if (e.type == RefreshEvent.TYPE_HISTORY_REFRESH) loadData()
	}

	fun openVod(item: VodCollect) {
		if (ApiConfig.instance.getSource(item.sourceKey) != null) {
			context.startActivity(
				Intent(context, DetailActivity::class.java).apply {
					putExtra("id", item.vodId)
					putExtra("sourceKey", item.sourceKey)
					putExtra("picture", item.pic)
				}
			)
		} else {
			onSwitchToSearchAndSearch(item.name)
		}
	}

	RefreshContentBox(
		isRefreshing = refreshing,
		isEmpty = loaded && list.isEmpty(),
		onRefresh = { refreshing = true; loadData() },
		modifier = modifier.fillMaxSize()
	) {
		AdaptiveVodGrid(
			items = list,
			name = { it.name },
			pic = { it.pic },
			year = { 0 },
			note = { null },
			onClick = ::openVod,
			onLongClick = { item -> deleteTarget = item },
			modifier = Modifier.fillMaxSize()
		)
	}

	if (showClearDialog) {
		AlertDialog(
			onDismissRequest = { showClearDialog = false },
			title = { Text("清空收藏") },
			text = { Text("确定要清空所有收藏吗？") },
			confirmButton = {
				TextButton(onClick = {
					showClearDialog = false
					RoomDataManger.deleteVodCollectAll()
					list = emptyList()
					Toast.makeText(context, "已清空收藏", Toast.LENGTH_SHORT).show()
				}) { Text("清空") }
			},
			dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("取消") } }
		)
	}

	deleteTarget?.let { item ->
		AlertDialog(
			onDismissRequest = { deleteTarget = null },
			title = { Text("取消收藏") },
			text = { Text("确定要取消收藏「${item.name}」吗？") },
			confirmButton = {
				TextButton(onClick = {
					deleteTarget = null
					RoomDataManger.deleteVodCollect(item.id)
					list = list.filterNot { it.id == item.id }
					Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show()
				}) { Text("删除") }
			},
			dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("取消") } }
		)
	}
}
