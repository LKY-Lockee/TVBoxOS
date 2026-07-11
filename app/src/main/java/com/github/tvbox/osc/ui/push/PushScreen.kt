package com.github.tvbox.osc.ui.push

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import com.github.tvbox.osc.ui.activity.DetailActivity
import com.github.tvbox.osc.ui.tv.QRCodeGen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PushScreen(onBack: () -> Unit) {
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
				.padding(padding),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.Center
		) {
			qrBitmap?.let { bmp ->
				Image(
					bitmap = bmp.asImageBitmap(),
					contentDescription = null,
					modifier = Modifier.size(220.dp)
				)
			}
			Spacer(Modifier.height(16.dp))
			Text(
				text = "扫描上方二维码或访问地址\n$address",
				style = MaterialTheme.typography.bodyMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
				textAlign = TextAlign.Center,
				modifier = Modifier.fillMaxWidth()
			)
			Spacer(Modifier.height(24.dp))
			Button(onClick = { pushClipboard(context) }) {
				Text("推送剪贴板内容")
			}
		}
	}
}

private fun pushClipboard(context: Context) {
	try {
		val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
		val clip = manager?.primaryClip
		if (clip != null && manager.hasPrimaryClip() && clip.itemCount > 0) {
			val clipText = clip.getItemAt(0).text?.toString()?.trim() ?: ""
			if (clipText.isNotEmpty()) {
				val intent = Intent(context, DetailActivity::class.java).apply {
					putExtra("id", clipText)
					putExtra("sourceKey", "push_agent")
					flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
				}
				context.startActivity(intent)
				return
			}
		}
		Toast.makeText(context, "剪贴板为空", Toast.LENGTH_SHORT).show()
	} catch (_: Throwable) {
		Toast.makeText(context, "推送失败", Toast.LENGTH_SHORT).show()
	}
}