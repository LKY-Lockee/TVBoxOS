package com.github.tvbox.osc.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.tvbox.osc.bean.Movie
import com.github.tvbox.osc.bean.MovieSort.SortData
import com.github.tvbox.osc.ui.compose.component.AdaptiveVodGrid
import com.github.tvbox.osc.ui.compose.component.FilterChipRow
import com.github.tvbox.osc.ui.compose.component.RefreshContentBox
import com.github.tvbox.osc.ui.compose.component.VodListRow
import com.github.tvbox.osc.viewmodel.SourceViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/** 文件夹视图栈中的一帧（用于返回时还原数据）。 */
private class GridFrame(
	val id: String,
	val flag: String,
	val videos: List<Movie.Video> = emptyList(),
	val page: Int = 1,
	val maxPage: Int = 1,
	val loaded: Boolean = false
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategoryGridScreen(
	sortData: SortData,
	onSwitchToSearchAndSearch: (String?) -> Unit,
	onNavigateToDetail: (String, String, String?) -> Unit,
	modifier: Modifier = Modifier
) {
	val gridVm: SourceViewModel = viewModel(key = "grid_${sortData.id}")
	val listResult by gridVm.listResult.observeAsState()

	var frames by remember(sortData.id) {
		mutableStateOf(listOf(GridFrame(sortData.id, sortData.flag)))
	}
	var refreshing by remember { mutableStateOf(false) }

	val current = frames.last()
	val isFolderMode = current.flag.firstOrNull() == '1'

	fun reload(page: Int) {
		sortData.id = current.id
		sortData.flag = current.flag
		gridVm.getList(sortData, page)
	}

	LaunchedEffect(current.id, current.flag) {
		if (current.videos.isEmpty() && !current.loaded) {
			refreshing = true
			reload(1)
		}
	}

	LaunchedEffect(listResult) {
		val xml = listResult ?: return@LaunchedEffect
		refreshing = false
		val videos = xml.movie?.videoList.orEmpty()
		val pageCount = xml.movie?.pageCount ?: 1
		frames = frames.dropLast(1) + GridFrame(
			id = current.id,
			flag = current.flag,
			videos = if (current.page == 1) videos else current.videos + videos,
			page = current.page + 1,
			maxPage = pageCount,
			loaded = true
		)
	}

	val gridState = rememberLazyGridState()
	LaunchedEffect(current.videos.size) {
		snapshotFlow {
			val last = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
			current.videos.isNotEmpty() && last >= current.videos.size - 4
		}.distinctUntilChanged().filter { it }.collect {
			if (current.page <= current.maxPage) reload(current.page)
		}
	}

	BackHandler(enabled = frames.size > 1) {
		frames = frames.dropLast(1)
	}

	fun openVideo(video: Movie.Video) {
		val tag = video.tag
		if (tag == "folder" || tag == "cover") {
			frames = frames + GridFrame(video.id, if (tag == "folder") "1" else "2")
			return
		}
		val id = video.id
		if (id.isEmpty() || id.startsWith("msearch:")) {
			onSwitchToSearchAndSearch(video.name)
		} else {
			onNavigateToDetail(video.sourceKey, id, video.pic)
		}
	}

	Column(modifier.fillMaxSize()) {
		if (sortData.filters.isNotEmpty()) {
			FilterChipRow(
				filters = sortData.filters,
				filterSelect = sortData.filterSelect,
				onSelect = { key, value ->
					if (value == null) sortData.filterSelect.remove(key)
					else sortData.filterSelect[key] = value
					refreshing = true
					frames = frames.dropLast(1) + GridFrame(id = current.id, flag = current.flag, page = 1)
					reload(1)
				}
			)
		}
		Box(
			Modifier
				.fillMaxWidth()
				.weight(1f)
		) {
			RefreshContentBox(
				isRefreshing = refreshing,
				isEmpty = current.loaded && current.videos.isEmpty(),
				onRefresh = {
					refreshing = true
					frames = frames.dropLast(1) + GridFrame(id = current.id, flag = current.flag, page = 1)
					reload(1)
				},
				modifier = Modifier.fillMaxSize()
			) {
				if (isFolderMode) {
					LazyColumn(
						modifier = Modifier.fillMaxSize(),
						contentPadding = PaddingValues(4.dp)
					) {
						items(current.videos) { video ->
							VodListRow(
								name = video.name,
								pic = video.pic,
								note = video.note,
								onClick = { openVideo(video) }
							)
						}
					}
				} else {
					AdaptiveVodGrid(
						items = current.videos,
						name = { it.name },
						pic = { it.pic },
						year = { it.year },
						note = { it.note },
						onClick = ::openVideo,
						state = gridState,
						modifier = Modifier.fillMaxSize()
					)
				}
			}
		}
	}
}
