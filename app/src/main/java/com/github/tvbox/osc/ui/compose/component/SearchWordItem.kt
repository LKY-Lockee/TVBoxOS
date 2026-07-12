package com.github.tvbox.osc.ui.compose.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.tvbox.osc.R
import com.github.tvbox.osc.ui.compose.theme.Ts16
import com.github.tvbox.osc.ui.compose.util.focusVisuals

/** 搜索词项类型：0 历史 / 1 热搜 / 2 建议。 */
data class SearchWordItem(val title: String, val type: Int)

private fun iconRes(type: Int) = when (type) {
	0 -> R.drawable.icon_history
	1 -> R.drawable.icon_hot
	else -> R.drawable.icon_search
}

/**
 * 搜索词项。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchWordRow(
	item: SearchWordItem,
	onClick: () -> Unit,
	onLongClick: (() -> Unit)? = null,
	modifier: Modifier = Modifier
) {
	val clickModifier = if (onLongClick != null) {
		Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
	} else {
		Modifier.clickable(onClick = onClick)
	}
	Row(
		modifier
			.fillMaxWidth()
			.then(clickModifier)
			.focusVisuals(focusedScale = 1f)
			.padding(horizontal = 16.dp, vertical = 12.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		Icon(
			painter = painterResource(iconRes(item.type)),
			contentDescription = null,
			modifier = Modifier.size(24.dp),
			tint = MaterialTheme.colorScheme.onSurfaceVariant
		)
		Spacer(Modifier.width(16.dp))
		Text(
			text = item.title,
			modifier = Modifier.weight(1f),
			color = MaterialTheme.colorScheme.onSurface,
			fontSize = Ts16,
			maxLines = 1,
			overflow = TextOverflow.Ellipsis
		)
	}
}
