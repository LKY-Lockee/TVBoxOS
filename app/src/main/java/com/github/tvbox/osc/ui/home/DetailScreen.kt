package com.github.tvbox.osc.ui.home

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.tvbox.osc.R
import com.github.tvbox.osc.api.ApiConfig
import com.github.tvbox.osc.bean.Movie
import com.github.tvbox.osc.bean.VodInfo
import com.github.tvbox.osc.ui.compose.component.VodThumb
import com.github.tvbox.osc.ui.compose.theme.Ts16
import com.github.tvbox.osc.ui.compose.theme.Ts24
import com.github.tvbox.osc.ui.fragment.PlayView
import com.github.tvbox.osc.util.DefaultConfig

private fun Context.findActivity(): Activity? {
	var ctx = this
	while (ctx is ContextWrapper) {
		if (ctx is Activity) return ctx
		ctx = ctx.baseContext
	}
	return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
	sourceKey: String,
	id: String,
	picture: String,
	onBack: () -> Unit,
	onNavigateToDetail: (String, String, String?) -> Unit,
	modifier: Modifier = Modifier,
	viewModel: DetailViewModel = viewModel()
) {
	val context = LocalContext.current
	val activity = remember { context.findActivity() }
	val window = activity?.window
	val windowInsetsController = window?.let {
		WindowCompat.getInsetsController(it, it.decorView)
	}

	val uiState by viewModel.uiState.collectAsState()
	val isFullscreen by viewModel.isFullscreen.collectAsState()
	val isCollected by viewModel.isCollected.collectAsState()
	val playUrl by viewModel.playUrl.collectAsState()
	val refreshTick by viewModel.refreshTick.collectAsState()

	LaunchedEffect(sourceKey, id, picture) {
		viewModel.loadDetail(sourceKey, id, picture)
	}

	LaunchedEffect(Unit) {
		viewModel.toast.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
	}

	DisposableEffect(isFullscreen) {
		windowInsetsController?.apply {
			if (isFullscreen) {
				hide(WindowInsetsCompat.Type.systemBars())
				systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
			} else {
				show(WindowInsetsCompat.Type.systemBars())
			}
		}
		onDispose { }
	}

	DisposableEffect(Unit) {
		onDispose {
			activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
		}
	}

	when (val state = uiState) {
		DetailUiState.Loading -> {
			Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
				CircularWavyProgressIndicator()
			}
		}

		DetailUiState.Empty -> {
			Scaffold(
				topBar = {
					TopAppBar(
						title = {},
						navigationIcon = {
							IconButton(onClick = onBack) {
								Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
							}
						}
					)
				}
			) { padding ->
				Box(
					Modifier
						.fillMaxSize()
						.padding(padding),
					contentAlignment = Alignment.Center
				) {
					Text("暂无数据")
				}
			}
		}

		is DetailUiState.Success -> {
			val vodInfo = state.vodInfo
			val video = state.video
			refreshTick // observe tick to trigger recomposition

			if (isFullscreen) {
				FullscreenPlayer(
					viewModel = viewModel,
					modifier = modifier.fillMaxSize()
				)
			} else {
				DetailContent(
					video = video,
					vodInfo = vodInfo,
					isCollected = isCollected,
					playUrl = playUrl,
					viewModel = viewModel,
					onBack = onBack,
					onNavigateToDetail = onNavigateToDetail,
					modifier = modifier
				)
			}
		}
	}
}

@Composable
private fun FullscreenPlayer(
	viewModel: DetailViewModel,
	modifier: Modifier = Modifier
) {
	PlayerContainer(
		viewModel = viewModel,
		modifier = modifier.background(Color.Black)
	)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailContent(
	video: Movie.Video,
	vodInfo: VodInfo,
	isCollected: Boolean,
	playUrl: String,
	viewModel: DetailViewModel,
	onBack: () -> Unit,
	onNavigateToDetail: (String, String, String?) -> Unit,
	modifier: Modifier = Modifier
) {
	var selectedTab by remember { mutableIntStateOf(0) }
	val hasPlaylist = vodInfo.seriesMap?.isNotEmpty() == true

	val tabMapping = remember(hasPlaylist) {
		if (hasPlaylist) listOf("详情", "选集", "搜索")
		else listOf("详情", "搜索")
	}

	Scaffold(
		modifier = modifier.fillMaxSize(),
		contentWindowInsets = WindowInsets(),
		topBar = {
			TopAppBar(
				title = { Text(video.name ?: "TVBox") },
				navigationIcon = {
					IconButton(onClick = onBack) {
						Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
					}
				}
			)
		}
	) { padding ->
		Column(
			Modifier
				.fillMaxSize()
				.padding(padding)
		) {
			if (hasPlaylist) {
				PlayerContainer(
					viewModel = viewModel,
					modifier = Modifier
						.fillMaxWidth()
						.aspectRatio(16f / 9f)
						.background(Color.Black)
				)
			}

			PrimaryScrollableTabRow(
				selectedTabIndex = selectedTab,
				edgePadding = 0.dp
			) {
				tabMapping.forEachIndexed { index, title ->
					Tab(
						selected = selectedTab == index,
						onClick = { selectedTab = index },
						text = { Text(title) }
					)
				}
			}

			Box(Modifier.fillMaxSize()) {
				val tabTitle = tabMapping[selectedTab]
				when (tabTitle) {
					"详情" -> DetailInfoTab(
						video = video,
						isCollected = isCollected,
						playUrl = playUrl,
						sourceKey = viewModel.sourceKey,
						firstSourceKey = viewModel.firstSourceKey,
						onToggleCollect = { viewModel.toggleCollect() }
					)

					"选集" -> DetailPlaylistTab(
						vodInfo = vodInfo,
						viewModel = viewModel
					)

					"搜索" -> DetailSearchTab(
						videoName = video.name.orEmpty(),
						onNavigateToDetail = onNavigateToDetail
					)
				}
			}
		}
	}
}

/**
 * 播放器容器：用 AndroidView 嵌入 PlayView。
 */
@Composable
private fun PlayerContainer(
	viewModel: DetailViewModel,
	modifier: Modifier = Modifier
) {
	val context = LocalContext.current
	val playView = remember { PlayView(context) }

	val playSignal by viewModel.playSignal.collectAsState()

	LaunchedEffect(playSignal) {
		if (playSignal > 0) {
			val bundle = Bundle().apply { putString("sourceKey", viewModel.sourceKey) }
			playView.setData(bundle)
		}
	}

	DisposableEffect(Unit) {
		onDispose {
			try {
				playView.setPlayTitle(false)
			} catch (_: Exception) {
			}
		}
	}

	AndroidView(
		factory = { playView },
		modifier = modifier
	)
}

@Composable
private fun DetailInfoTab(
	video: Movie.Video,
	isCollected: Boolean,
	playUrl: String,
	sourceKey: String,
	firstSourceKey: String,
	onToggleCollect: () -> Unit
) {
	val context = LocalContext.current

	Column(
		Modifier
			.fillMaxSize()
			.verticalScroll(rememberScrollState())
			.padding(16.dp)
	) {
		// Title + Collect button
		Row(
			verticalAlignment = Alignment.CenterVertically,
			modifier = Modifier.fillMaxWidth()
		) {
			Text(
				text = video.name ?: "TVBox",
				fontSize = Ts24,
				fontWeight = FontWeight.Bold,
				modifier = Modifier.weight(1f),
				color = MaterialTheme.colorScheme.onSurface
			)
			IconButton(onClick = onToggleCollect) {
				Icon(
					painter = androidx.compose.ui.res.painterResource(
						if (isCollected) R.drawable.icon_collect_filled else R.drawable.icon_collect
					),
					contentDescription = "收藏",
					tint = if (isCollected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
				)
			}
		}

		Row(
			Modifier
				.fillMaxWidth()
				.padding(top = 8.dp),
			horizontalArrangement = Arrangement.spacedBy(12.dp)
		) {
			// Thumbnail
			VodThumb(
				pic = DefaultConfig.checkReplaceProxy(video.pic.orEmpty()),
				name = video.name,
				modifier = Modifier
					.width(80.dp)
					.height(108.dp)
					.clip(RoundedCornerShape(8.dp))
			)

			// Info fields
			Column(
				Modifier.weight(1f),
				verticalArrangement = Arrangement.spacedBy(2.dp)
			) {
				InfoLine("来源", ApiConfig.instance.getSource(firstSourceKey)?.name.orEmpty())
				if (video.year != 0) InfoLine("年份", video.year.toString())
				InfoLine("地区", video.area.orEmpty())
				InfoLine("语言", video.lang.orEmpty())
				if (firstSourceKey != sourceKey) {
					InfoLine("类型", "[${ApiConfig.instance.getSource(sourceKey)?.name.orEmpty()}] 解析")
				} else {
					InfoLine("类型", video.type.orEmpty())
				}
				InfoLine("演员", video.actor.orEmpty())
				InfoLine("导演", video.director.orEmpty())
				if (playUrl.isNotEmpty()) {
					Row(verticalAlignment = Alignment.CenterVertically) {
						Text(
							text = "地址：$playUrl",
							fontSize = Ts16,
							color = MaterialTheme.colorScheme.onSurfaceVariant,
							modifier = Modifier.weight(1f)
						)
						IconButton(onClick = {
							val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
							cm.setPrimaryClip(ClipData.newPlainText(null, playUrl))
							Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
						}) {
							Icon(Icons.Filled.ContentCopy, contentDescription = "复制地址", modifier = Modifier.width(20.dp))
						}
					}
				}
			}
		}

		// Description
		val desc = video.des.replace("<.*?>".toRegex(), "").replace("\\s".toRegex(), "")
		if (desc.isNotEmpty()) {
			Text(
				text = "简介：$desc",
				fontSize = Ts16,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
				modifier = Modifier.padding(top = 8.dp)
			)
		}
	}
}

@Composable
private fun InfoLine(label: String, value: String) {
	if (value.isBlank()) return
	Text(
		text = "$label：$value",
		fontSize = Ts16,
		color = MaterialTheme.colorScheme.onSurfaceVariant
	)
}

@Composable
private fun DetailPlaylistTab(
	vodInfo: VodInfo,
	viewModel: DetailViewModel
) {
	val flags = vodInfo.seriesFlags ?: emptyList()
	val seriesMap = vodInfo.seriesMap ?: emptyMap()
	val currentFlag = vodInfo.playFlag
	val seriesList = seriesMap[currentFlag] ?: emptyList()

	var selectedFlagIndex by remember(currentFlag) {
		mutableIntStateOf(flags.indexOfFirst { it.name == currentFlag }.coerceAtLeast(0))
	}

	val gridState = rememberLazyGridState()

	LaunchedEffect(vodInfo.playIndex) {
		if (seriesList.isNotEmpty() && vodInfo.playIndex < seriesList.size) {
			gridState.scrollToItem(vodInfo.playIndex)
		}
	}

	Column(Modifier.fillMaxSize()) {
		// Flag tabs
		if (flags.size > 1) {
			PrimaryScrollableTabRow(
				selectedTabIndex = selectedFlagIndex,
				modifier = Modifier.fillMaxWidth()
			) {
				flags.forEachIndexed { index, flag ->
					Tab(
						selected = selectedFlagIndex == index,
						onClick = {
							selectedFlagIndex = index
							viewModel.selectFlag(index)
						},
						text = { Text(flag.name) }
					)
				}
			}
		}

		// Sort button
		Row(
			Modifier
				.fillMaxWidth()
				.padding(horizontal = 10.dp, vertical = 4.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			FilterChip(
				selected = vodInfo.reverseSort,
				onClick = { viewModel.toggleReverse() },
				label = { Text(if (vodInfo.reverseSort) "倒序" else "正序", fontSize = Ts16) },
				leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Sort, contentDescription = null, modifier = Modifier.width(18.dp)) }
			)
		}

		// Episode grid
		LazyVerticalGrid(
			columns = GridCells.Adaptive(120.dp),
			state = gridState,
			modifier = Modifier
				.fillMaxSize()
				.padding(horizontal = 10.dp),
			horizontalArrangement = Arrangement.spacedBy(6.dp),
			verticalArrangement = Arrangement.spacedBy(6.dp)
		) {
			items(
				items = seriesList,
				key = { it.url }
			) { series ->
				val isSelected = seriesList.indexOf(series) == vodInfo.playIndex
				FilterChip(
					selected = isSelected,
					onClick = {
						val pos = seriesList.indexOf(series)
						viewModel.selectSeries(pos)
					},
					label = { Text(series.name, fontSize = Ts16, maxLines = 1) }
				)
			}
		}
	}
}

@Composable
private fun DetailSearchTab(
	videoName: String,
	onNavigateToDetail: (String, String, String?) -> Unit
) {
	val searchVm: SearchViewModel = viewModel(key = "detail_search")
	var hasSearched by remember { mutableStateOf(false) }

	LaunchedEffect(videoName) {
		if (videoName.isNotEmpty() && !hasSearched) {
			searchVm.search(videoName)
			hasSearched = true
		}
	}

	SearchContent(
		vm = searchVm,
		onNavigateToDetail = { video ->
			onNavigateToDetail(video.sourceKey, video.id, video.pic)
		},
		modifier = Modifier.fillMaxSize()
	)
}
