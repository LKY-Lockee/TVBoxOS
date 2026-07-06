package com.github.tvbox.osc.ui.compose.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RefreshContentBox(
	isRefreshing: Boolean,
	isEmpty: Boolean,
	onRefresh: () -> Unit,
	modifier: Modifier = Modifier,
	emptyMessage: String = "没有数据",
	content: @Composable () -> Unit
) {
	val state = rememberPullToRefreshState()
	PullToRefreshBox(
		state = state,
		isRefreshing = isRefreshing,
		onRefresh = onRefresh,
		indicator = {
			PullToRefreshDefaults.LoadingIndicator(
				state = state,
				isRefreshing = isRefreshing,
				modifier = Modifier.align(Alignment.TopCenter),
			)
		},
		modifier = modifier.fillMaxSize()
	) {
		if (isEmpty && !isRefreshing) {
			Box(
				Modifier
					.fillMaxSize()
					.verticalScroll(rememberScrollState()),
				contentAlignment = Alignment.Center
			) {
				Text(emptyMessage)
			}
		} else {
			Box(Modifier.fillMaxSize()) { content() }
		}
	}
}
