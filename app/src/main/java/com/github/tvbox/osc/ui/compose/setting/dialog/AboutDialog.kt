package com.github.tvbox.osc.ui.compose.setting.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
	val message = """
		本软件只提供聚合展示功能，所有资源来自网络, 软件不参与任何制作、上传、储存、下载等内容。软件仅供学习参考, 请于安装后24小时内删除。

		打包分发请保留出处：
		https://github.com/CatVodTVOfficial/TVBoxOSC
		https://github.com/q215613905/TVBoxOS
		https://github.com/LKY-Lockee/TVBoxOS
	""".trimIndent()

	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text("关于") },
		text = { Text(message) },
		confirmButton = {
			TextButton(onClick = onDismiss) { Text("确定") }
		}
	)
}
