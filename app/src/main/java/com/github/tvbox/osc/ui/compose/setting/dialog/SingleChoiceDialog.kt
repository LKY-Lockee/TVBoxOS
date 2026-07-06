package com.github.tvbox.osc.ui.compose.setting.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
fun SingleChoiceDialog(
	title: String,
	options: List<String>,
	selected: Int,
	onConfirm: (Int) -> Unit,
	onDismiss: () -> Unit,
	confirmText: String = "确定",
	dismissText: String = "取消",
	onClear: (() -> Unit)? = null,
	clearText: String = "清空历史"
) {
	var current by remember { mutableIntStateOf(selected) }

	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text(title) },
		text = {
			Column(Modifier.selectableGroup()) {
				options.forEachIndexed { index, label ->
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
							text = label,
							style = MaterialTheme.typography.bodyLarge,
							modifier = Modifier.padding(start = 16.dp)
						)
					}
				}
			}
		},
		confirmButton = {
			TextButton(onClick = { onConfirm(current) }) {
				Text(confirmText)
			}
		},
		dismissButton = {
			if (onClear != null) {
				TextButton(onClick = onClear) { Text(clearText) }
			} else {
				TextButton(onClick = onDismiss) { Text(dismissText) }
			}
		}
	)
}

@Composable
fun ConfirmDialog(
	title: String,
	message: String,
	onConfirm: () -> Unit,
	onDismiss: () -> Unit,
	confirmText: String = "确定",
	dismissText: String = "取消"
) {
	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text(title) },
		text = { Text(message) },
		confirmButton = {
			TextButton(onClick = {
				onConfirm()
				onDismiss()
			}) { Text(confirmText) }
		},
		dismissButton = {
			TextButton(onClick = onDismiss) { Text(dismissText) }
		}
	)
}
