package com.github.tvbox.osc.ui.home

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.github.tvbox.osc.R
import com.github.tvbox.osc.api.ApiConfig
import com.github.tvbox.osc.bean.VodInfo
import com.github.tvbox.osc.cache.RoomDataManger
import com.github.tvbox.osc.event.RefreshEvent
import com.github.tvbox.osc.ui.activity.DetailActivity
import com.github.tvbox.osc.ui.compose.component.AdaptiveVodGrid
import com.github.tvbox.osc.ui.compose.component.RefreshContentBox
import com.github.tvbox.osc.ui.compose.util.rememberEventBusCallback

@Composable
fun HistoryScreen(
	isCurrent: Boolean,
	toolbarState: HomeToolbarState,
	onSwitchToSearchAndSearch: (String?) -> Unit,
	modifier: Modifier = Modifier
) {
	val context = LocalContext.current
	var list by remember { mutableStateOf<List<VodInfo>>(emptyList()) }
	var refreshing by remember { mutableStateOf(true) }
	var showClearDialog by remember { mutableStateOf(false) }
	var deleteTarget by remember { mutableStateOf<Pair<VodInfo, Int>?>(null) }

	fun loadData() {
		val all = RoomDataManger.getAllVodRecord(100).map { info ->
			if (info.playNote.isNotEmpty()) info.note = "上次看到" + info.playNote
			info
		}
		list = all
		refreshing = false
	}

	LaunchedEffect(isCurrent) {
		if (isCurrent) {
			toolbarState.title = "历史记录"
			toolbarState.actions = listOf(
				ToolbarAction(R.drawable.icon_clear, "清空历史记录") { showClearDialog = true }
			)
		}
	}

	LaunchedEffect(Unit) { loadData() }
	rememberEventBusCallback<RefreshEvent> { e ->
		if (e.type == RefreshEvent.TYPE_HISTORY_REFRESH) loadData()
	}

	fun openVod(info: VodInfo) {
		if (ApiConfig.instance.getSource(info.sourceKey) != null) {
			context.startActivity(
				Intent(context, DetailActivity::class.java).apply {
					putExtra("id", info.id)
					putExtra("sourceKey", info.sourceKey)
					putExtra("picture", info.pic)
				}
			)
		} else {
			onSwitchToSearchAndSearch(info.name)
		}
	}

	RefreshContentBox(
		isRefreshing = refreshing,
		isEmpty = list.isEmpty(),
		onRefresh = { refreshing = true; loadData() },
		modifier = modifier.fillMaxSize()
	) {
		AdaptiveVodGrid(
			items = list,
			name = { it.name },
			pic = { it.pic },
			year = { it.year },
			note = { it.note },
			onClick = ::openVod,
			onLongClick = { info -> deleteTarget = info to list.indexOf(info) },
			modifier = Modifier.fillMaxSize()
		)
	}

	if (showClearDialog) {
		AlertDialog(
			onDismissRequest = { showClearDialog = false },
			title = { Text("清空历史记录") },
			text = { Text("确定要清空所有观看记录吗？") },
			confirmButton = {
				TextButton(onClick = {
					showClearDialog = false
					RoomDataManger.deleteVodRecordAll()
					list = emptyList()
					Toast.makeText(context, "已清空历史记录", Toast.LENGTH_SHORT).show()
				}) { Text("清空") }
			},
			dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("取消") } }
		)
	}

	deleteTarget?.let { (info, _) ->
		AlertDialog(
			onDismissRequest = { deleteTarget = null },
			title = { Text("删除历史记录") },
			text = { Text("确定要删除「${info.name}」的观看记录吗？") },
			confirmButton = {
				TextButton(onClick = {
					deleteTarget = null
					RoomDataManger.deleteVodRecord(info.sourceKey, info)
					list = list.filterNot { it.id == info.id && it.sourceKey == info.sourceKey }
					Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show()
				}) { Text("删除") }
			},
			dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("取消") } }
		)
	}
}
