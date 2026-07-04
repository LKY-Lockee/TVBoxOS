package com.github.tvbox.osc.ui.compose.util

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 为可聚焦元素添加 TV/键盘聚焦视觉：聚焦时缩放 + 边框高亮。
 * 应在使元素聚焦的修饰符（clickable/focusable）之后链式调用。
 */
@Composable
fun Modifier.focusVisuals(
	focusedScale: Float = 1.05f,
	borderWidth: Dp = 2.dp,
	cornerRadius: Dp = 8.dp
): Modifier {
	var isFocused by remember { mutableStateOf(false) }
	val scale by animateFloatAsState(if (isFocused) focusedScale else 1f, label = "focusScale")
	val borderColor by animateColorAsState(
		if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
		label = "focusBorder"
	)
	return this
		.onFocusChanged { isFocused = it.isFocused }
		.graphicsLayer { scaleX = scale; scaleY = scale }
		.border(borderWidth, borderColor, RoundedCornerShape(cornerRadius))
}
