package com.github.tvbox.osc.ui.compose.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.github.tvbox.osc.ui.compose.theme.BadgeNote
import com.github.tvbox.osc.ui.compose.theme.BadgeYear
import com.github.tvbox.osc.ui.compose.theme.PlaceholderText
import com.github.tvbox.osc.ui.compose.theme.ScrimBottomName
import com.github.tvbox.osc.ui.compose.theme.Ts16
import com.github.tvbox.osc.ui.compose.theme.Ts22
import com.github.tvbox.osc.ui.compose.util.focusVisuals
import com.github.tvbox.osc.util.ImgUtil

private val CardPalette = listOf(
	0xFFE57373, 0xFFBA68C8, 0xFF64B5F6, 0xFF4DB6AC, 0xFFFFB74D, 0xFFA1887F, 0xFF90A4AE
)

private fun colorForChar(c: Char): Color = Color(CardPalette[c.code.mod(CardPalette.size)])

/** 无图时显示的首字符占位（对应 ImgUtil.createTextDrawable）。 */
@Composable
fun TextPlaceholder(name: String?, modifier: Modifier = Modifier) {
	val first = name?.firstOrNull()?.takeIf { it.isLetterOrDigit() } ?: 'T'
	Box(
		modifier.background(colorForChar(first), RoundedCornerShape(8.dp)),
		contentAlignment = Alignment.Center
	) {
		Text(text = first.toString(), color = PlaceholderText, fontSize = 40.sp)
	}
}

/**
 * 视频封面图：支持 base64、@ 头后缀 URL、无图占位与加载/错误态。
 */
@Composable
fun VodThumb(
	pic: String?,
	name: String?,
	modifier: Modifier = Modifier
) {
	when {
		pic.isNullOrEmpty() -> TextPlaceholder(name, modifier)
		ImgUtil.isBase64Image(pic) -> {
			val bitmap = remember(pic) { ImgUtil.decodeBase64ToBitmap(pic) }
			if (bitmap != null) {
				androidx.compose.foundation.Image(
					bitmap = bitmap.asImageBitmap(),
					contentDescription = name,
					modifier = modifier,
					contentScale = ContentScale.Crop
				)
			} else {
				TextPlaceholder(name, modifier)
			}
		}

		else -> SubcomposeAsyncImage(
			model = pic,
			contentDescription = name,
			modifier = modifier,
			contentScale = ContentScale.Crop
		) {
			when (painter.state) {
				is AsyncImagePainter.State.Loading ->
					Box(
						Modifier
							.fillMaxSize()
							.background(MaterialTheme.colorScheme.surfaceVariant)
					)

				is AsyncImagePainter.State.Error ->
					TextPlaceholder(name, Modifier.fillMaxSize())

				else -> SubcomposeAsyncImageContent()
			}
		}
	}
}

/** 顶部圆角小标签（年份/备注等）。 */
@Composable
private fun Badge(text: String, color: Color) {
	Box(
		Modifier
			.background(color, RoundedCornerShape(5.dp))
			.padding(horizontal = 5.dp, vertical = 1.dp)
	) {
		Text(text = text, color = Color.White, fontSize = Ts16, maxLines = 1)
	}
}

/**
 * 影片卡片（对应 item_grid.xml + Grid/History/Collect/SearchAdapter 的渲染逻辑）。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VodCard(
	name: String?,
	pic: String?,
	year: Int,
	note: String?,
	modifier: Modifier = Modifier,
	onLongClick: (() -> Unit)? = null,
	onClick: () -> Unit
) {
	val clickModifier = if (onLongClick != null) {
		Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
	} else {
		Modifier.clickable(onClick = onClick)
	}
	Card(
		modifier = modifier
			.padding(4.dp)
			.fillMaxWidth()
			.then(clickModifier)
			.focusVisuals(),
		shape = RoundedCornerShape(8.dp),
		colors = CardDefaults.cardColors(),
		elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
		border = null
	) {
		Box(Modifier.aspectRatio(214f / 280f)) {
			VodThumb(pic, name, Modifier.fillMaxSize())

			if (year > 0) {
				Row(
					Modifier
						.align(Alignment.TopStart)
						.padding(5.dp),
					horizontalArrangement = Arrangement.spacedBy(5.dp)
				) {
					Badge(year.toString(), BadgeYear)
				}
			}

			Column(
				Modifier
					.align(Alignment.BottomCenter)
					.fillMaxWidth()
					.background(Brush.verticalGradient(listOf(Color.Transparent, ScrimBottomName)))
					.clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
			) {
				if (!note.isNullOrEmpty()) {
					Box(
						Modifier
							.align(Alignment.Start)
							.padding(start = 5.dp, bottom = 5.dp)
					) {
						Badge(note, BadgeNote)
					}
				}
				Text(
					text = name.orEmpty(),
					modifier = Modifier
						.fillMaxWidth()
						.basicMarquee()
						.padding(5.dp),
					color = Color.White,
					fontSize = Ts22,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis,
					textAlign = TextAlign.Center
				)
			}
		}
	}
}
