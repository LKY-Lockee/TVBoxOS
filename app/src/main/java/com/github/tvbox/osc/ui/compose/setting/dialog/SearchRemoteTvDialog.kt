package com.github.tvbox.osc.ui.compose.setting.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.github.tvbox.osc.player.thirdparty.RemoteTVBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

private enum class SearchState { Loading, Empty, Found, Failed }

@Composable
fun SearchRemoteTvDialog(
	onDismiss: () -> Unit,
	onSelected: (String) -> Unit
) {
	val hosts = remember { mutableStateListOf<String>() }
	var state by remember { mutableStateOf(SearchState.Loading) }
	var finished by remember { mutableStateOf(false) }

	val configuration = LocalConfiguration.current
	val density = LocalDensity.current
	val dialogWidthDp = with(density) {
		minOf(
			(configuration.screenWidthDp.dp - 48.dp).toPx(),
			560.dp.toPx()
		).toDp()
	}

	LaunchedEffect(Unit) {
		val scope = this
		delay(500.milliseconds)
		scope.launch(Dispatchers.IO) {
			RemoteTVBox.searchAvailable(object : RemoteTVBox.Callback() {
				override fun found(viewHost: String?, end: Boolean) {
					if (viewHost != null) {
						hosts.add(viewHost)
						state = SearchState.Found
					}
					if (end) finished = true
				}

				override fun fail(all: Boolean, end: Boolean) {
					if (end) {
						finished = true
						if (all && hosts.isEmpty()) state = SearchState.Failed
					}
				}
			})
		}
	}

	LaunchedEffect(finished) {
		if (finished && hosts.isEmpty()) {
			state = SearchState.Empty
		}
	}

	Dialog(onDismissRequest = onDismiss) {
		Surface(
			shape = RoundedCornerShape(28.dp),
			color = MaterialTheme.colorScheme.surfaceContainerHigh,
			contentColor = MaterialTheme.colorScheme.onSurface,
			tonalElevation = 6.dp,
			modifier = Modifier
				.fillMaxWidth()
				.widthIn(max = dialogWidthDp)
		) {
			Column(modifier = Modifier.padding(24.dp)) {
				Text(
					text = "搜索附近TVBox",
					style = MaterialTheme.typography.headlineSmall
				)
				Box(
					modifier = Modifier
						.fillMaxWidth()
						.heightIn(min = 120.dp, max = 320.dp)
						.padding(top = 16.dp),
					contentAlignment = Alignment.Center
				) {
					when (state) {
						SearchState.Loading -> CircularProgressIndicator()
						SearchState.Empty, SearchState.Failed -> Text(
							text = "未找到附近TVBox",
							style = MaterialTheme.typography.bodyMedium,
							color = MaterialTheme.colorScheme.onSurfaceVariant
						)

						SearchState.Found -> LazyColumn(
							modifier = Modifier.fillMaxWidth(),
							verticalArrangement = Arrangement.spacedBy(4.dp)
						) {
							items(hosts) { host ->
								Text(
									text = host,
									style = MaterialTheme.typography.bodyLarge,
									modifier = Modifier
										.fillMaxWidth()
										.clickable {
											onSelected(host)
										}
										.padding(vertical = 12.dp)
								)
							}
						}
					}
				}
				TextButton(
					onClick = onDismiss,
					modifier = Modifier.align(Alignment.End)
				) { Text("取消") }
			}
		}
	}
}
