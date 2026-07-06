package com.github.tvbox.osc.ui.compose.setting.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * 地址输入对话框：点击"历史"会叠加弹出历史选择对话框，选中并确定后将历史地址填入输入框。
 *
 * 三按钮布局（历史 / 取消 / 确定）需要自定义 Dialog + Surface，因为 M3 AlertDialog 仅两个按钮槽位。
 */
@Composable
fun AddressInputDialog(
	title: String,
	initialValue: String,
	history: List<String>,
	onConfirm: (String) -> Unit,
	onDismiss: () -> Unit
) {
	var input by remember { mutableStateOf(initialValue) }
	var showHistory by remember { mutableStateOf(false) }
	var selectedHistoryIndex by remember { mutableIntStateOf(-1) }

	val configuration = LocalConfiguration.current
	val density = LocalDensity.current
	val dialogWidthDp = with(density) {
		minOf(
			(configuration.screenWidthDp.dp - 48.dp).toPx(),
			560.dp.toPx()
		).toDp()
	}

	Dialog(onDismissRequest = onDismiss) {
		Surface(
			shape = RoundedCornerShape(28.dp),
			color = MaterialTheme.colorScheme.surfaceContainerHigh,
			contentColor = MaterialTheme.colorScheme.onSurface,
			tonalElevation = 6.dp,
			modifier = Modifier.widthIn(max = dialogWidthDp)
		) {
			Column(modifier = Modifier.padding(24.dp)) {
				Text(
					text = title,
					style = MaterialTheme.typography.headlineSmall
				)
				Spacer(Modifier.height(16.dp))
				OutlinedTextField(
					value = input,
					onValueChange = { input = it },
					label = { Text("配置地址") },
					singleLine = true,
					keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
					modifier = Modifier.fillMaxWidth()
				)
				Spacer(Modifier.height(24.dp))
				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.SpaceBetween,
					verticalAlignment = Alignment.CenterVertically
				) {
					TextButton(
						onClick = {
							selectedHistoryIndex = if (history.contains(input)) history.indexOf(input) else 0
							showHistory = true
						},
						enabled = history.isNotEmpty()
					) { Text("历史") }
					Row(horizontalArrangement = Arrangement.End) {
						TextButton(onClick = onDismiss) { Text("取消") }
						Spacer(Modifier.size(8.dp))
						Button(
							onClick = {
								onConfirm(input.trim())
								onDismiss()
							},
							enabled = input.isNotBlank()
						) { Text("确定") }
					}
				}
			}
		}
	}

	if (showHistory) {
		HistorySelectionDialog(
			title = "历史配置",
			history = history,
			selected = selectedHistoryIndex,
			onConfirm = { index ->
				showHistory = false
				if (index in history.indices) {
					input = history[index]
				}
			},
			onDismiss = { showHistory = false },
			onClear = null
		)
	}
}

@Composable
fun HistorySelectionDialog(
	title: String,
	history: List<String>,
	selected: Int,
	onConfirm: (Int) -> Unit,
	onDismiss: () -> Unit,
	onClear: (() -> Unit)?
) {
	var current by remember { mutableIntStateOf(selected) }

	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text(title) },
		text = {
			Column(Modifier.selectableGroup()) {
				history.forEachIndexed { index, url ->
					Row(
						Modifier
							.fillMaxWidth()
							.height(56.dp)
							.selectable(
								selected = index == current,
								onClick = { current = index },
								role = Role.RadioButton
							)
							.padding(horizontal = 16.dp),
						verticalAlignment = Alignment.CenterVertically
					) {
						RadioButton(
							selected = index == current,
							onClick = null
						)
						Text(
							text = url,
							style = MaterialTheme.typography.bodyLarge,
							modifier = Modifier.padding(start = 16.dp)
						)
					}
				}
			}
		},
		confirmButton = {
			TextButton(onClick = { onConfirm(current) }) { Text("确定") }
		},
		dismissButton = {
			Row {
				if (onClear != null) {
					TextButton(onClick = onClear) { Text("清空历史") }
					Spacer(Modifier.size(4.dp))
				}
				TextButton(onClick = onDismiss) { Text("取消") }
			}
		}
	)
}
