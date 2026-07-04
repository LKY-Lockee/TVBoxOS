package com.github.tvbox.osc.ui.compose.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.github.tvbox.osc.R
import com.github.tvbox.osc.ui.compose.theme.Ts24

/** 内容加载状态。 */
sealed interface ContentState {
	data object Loading : ContentState
	data object Empty : ContentState
	data class Content(val key: Any? = null) : ContentState
}

/**
 * 根据 [state] 显示加载/空/内容。内容态调用 [content]。
 */
@Composable
fun StateBox(
	state: ContentState,
	modifier: Modifier = Modifier,
	content: @Composable () -> Unit
) {
	when (state) {
		ContentState.Loading -> LoadingState(modifier.fillMaxSize())
		ContentState.Empty -> EmptyState(modifier.fillMaxSize())
		is ContentState.Content -> Box(modifier) { content() }
	}
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
	Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
		CircularProgressIndicator()
	}
}

@Composable
fun EmptyState(modifier: Modifier = Modifier, message: String = "没有数据") {
	Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
		Column(horizontalAlignment = Alignment.CenterHorizontally) {
			Icon(
				painter = painterResource(R.drawable.icon_empty),
				contentDescription = null,
				modifier = Modifier.size(120.dp),
				tint = MaterialTheme.colorScheme.onSurfaceVariant
			)
			Text(
				text = message,
				modifier = Modifier.padding(top = 20.dp),
				color = MaterialTheme.colorScheme.onSurfaceVariant,
				fontSize = Ts24
			)
		}
	}
}
