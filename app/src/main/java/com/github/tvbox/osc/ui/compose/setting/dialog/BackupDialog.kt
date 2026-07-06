package com.github.tvbox.osc.ui.compose.setting.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BackupDialog(
	backups: List<String>,
	onBackup: () -> Unit,
	onSelectBackup: (String) -> Unit,
	onDismiss: () -> Unit
) {
	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text("备份与恢复") },
		text = {
			if (backups.isEmpty()) {
				Text("暂无备份记录")
			} else {
				LazyColumn(modifier = Modifier.fillMaxWidth()) {
					items(backups) { name ->
						Text(
							text = name,
							style = MaterialTheme.typography.bodyLarge,
							modifier = Modifier
								.fillMaxWidth()
								.clickable { onSelectBackup(name) }
								.padding(vertical = 12.dp)
						)
					}
				}
			}
		},
		confirmButton = {
			TextButton(onClick = {
				onBackup()
				onDismiss()
			}) { Text("立即备份") }
		},
		dismissButton = {
			TextButton(onClick = onDismiss) { Text("取消") }
		}
	)
}

@Composable
fun BackupActionDialog(
	backupName: String,
	onRestore: () -> Unit,
	onDelete: () -> Unit,
	onDismiss: () -> Unit
) {
	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text(backupName) },
		text = { Text("请选择操作") },
		confirmButton = {
			TextButton(onClick = {
				onRestore()
				onDismiss()
			}) { Text("恢复") }
		},
		dismissButton = {
			TextButton(onClick = {
				onDelete()
				onDismiss()
			}) { Text("删除") }
		},
	)
}
