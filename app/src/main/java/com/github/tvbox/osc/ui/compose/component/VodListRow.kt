package com.github.tvbox.osc.ui.compose.component

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.tvbox.osc.ui.compose.theme.Ts20
import com.github.tvbox.osc.ui.compose.util.focusVisuals

/**
 * 列表行（对应 item_list.xml，文件夹模式）。
 */
@Composable
fun VodListRow(
	name: String?,
	pic: String?,
	note: String?,
	onClick: () -> Unit,
	modifier: Modifier = Modifier
) {
	Row(
		modifier
			.fillMaxWidth()
			.height(50.dp)
			.clickable(onClick = onClick)
			.focusVisuals(focusedScale = 1f),
		verticalAlignment = Alignment.CenterVertically
	) {
		VodThumb(
			pic, name, Modifier
				.size(50.dp)
				.fillMaxHeight()
				.padding(1.dp)
		)
		Text(
			text = name.orEmpty(),
			modifier = Modifier
				.weight(1f)
				.padding(horizontal = 10.dp)
				.basicMarquee(),
			color = MaterialTheme.colorScheme.onSurface,
			fontSize = Ts20,
			maxLines = 1,
			overflow = TextOverflow.Ellipsis
		)
		Text(
			text = note.orEmpty(),
			modifier = Modifier
				.weight(4f)
				.padding(horizontal = 10.dp)
				.basicMarquee(),
			color = MaterialTheme.colorScheme.onSurface,
			fontSize = Ts20,
			maxLines = 1,
			overflow = TextOverflow.Ellipsis,
			textAlign = TextAlign.End
		)
	}
}
