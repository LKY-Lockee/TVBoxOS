package com.github.tvbox.osc.ui.push

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.tvbox.osc.server.ControlManager
import com.github.tvbox.osc.ui.tv.QRCodeGen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PushScreen(
	onBack: () -> Unit,
	onNavigateToDetail: (String, String, String?) -> Unit
) {
	val context = LocalContext.current
	val address = remember { ControlManager.instance.getAddress(false) ?: "" }
	val qrBitmap = remember(address) {
		if (address.isNotEmpty()) {
			QRCodeGen.generateBitmap(address + "push.html", 600, 600)
		} else null
	}

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text("推送") },
				navigationIcon = {
					IconButton(onClick = onBack) {
						Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
					}
				}
			)
		}
	) { padding ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(padding)
				.padding(24.dp),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.Center
		) {
			if (qrBitmap != null) {
				Image(
					bitmap = qrBitmap.asImageBitmap(),
					contentDescription = "推送二维码",
					modifier = Modifier.size(280.dp)
				)
				Spacer(Modifier.height(16.dp))
				Text(
					text = address,
					style = MaterialTheme.typography.bodySmall,
					textAlign = TextAlign.Center
				)
			} else {
				Text("推送服务未启动")
			}

			Spacer(Modifier.height(24.dp))

			Button(
				onClick = { pushClipboard(context, onNavigateToDetail) },
				modifier = Modifier.fillMaxWidth()
			) {
				Text("推送剪贴板内容")
			}
		}
	}
}

private fun pushClipboard(
	context: Context,
	onNavigateToDetail: (String, String, String?) -> Unit
) {
	try {
		val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
		val clip = manager?.primaryClip
		if (clip != null && manager.hasPrimaryClip() && clip.itemCount > 0) {
			val clipText = clip.getItemAt(0).text?.toString()?.trim() ?: ""
			if (clipText.isNotEmpty()) {
				onNavigateToDetail("push_agent", clipText, null)
				return
			}
		}
		Toast.makeText(context, "剪贴板为空", Toast.LENGTH_SHORT).show()
	} catch (_: Throwable) {
		Toast.makeText(context, "推送失败", Toast.LENGTH_SHORT).show()
	}
}
