package com.github.tvbox.osc.ui.compose.setting.dialog

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.tvbox.osc.server.ControlManager
import com.github.tvbox.osc.ui.tv.QRCodeGen

@Composable
fun QrCodeDialog(onDismiss: () -> Unit) {
	val address = remember { ControlManager.instance.getAddress(false) ?: "" }
	val qrBitmap = remember(address) {
		if (address.isNotEmpty()) {
			QRCodeGen.generateBitmap(address + "api.html", 600, 600)
		} else null
	}

	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text("扫码配置") },
		text = {
			Column(
				modifier = Modifier.fillMaxWidth(),
				horizontalAlignment = Alignment.CenterHorizontally
			) {
				qrBitmap?.let { bmp ->
					Image(
						bitmap = bmp.asImageBitmap(),
						contentDescription = null,
						modifier = Modifier.size(180.dp)
					)
				}
				Spacer(Modifier.height(12.dp))
				Text(
					text = "扫描上方二维码或访问地址\n$address",
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
					textAlign = TextAlign.Center
				)
			}
		},
		confirmButton = {
			TextButton(onClick = onDismiss) { Text("确定") }
		}
	)
}
