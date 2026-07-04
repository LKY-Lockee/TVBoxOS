package com.github.tvbox.osc.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun <T> AdaptiveVodGrid(
	items: List<T>,
	name: (T) -> String?,
	pic: (T) -> String?,
	year: (T) -> Int,
	note: (T) -> String?,
	onClick: (T) -> Unit,
	modifier: Modifier = Modifier,
	state: LazyGridState = rememberLazyGridState(),
	contentPadding: PaddingValues = PaddingValues(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 120.dp),
	onLongClick: ((T) -> Unit)? = null
) {
	BoxWithConstraints(modifier) {
		val columns = if (maxWidth < maxHeight && maxWidth < 600.dp) {
			GridCells.Fixed(3)
		} else {
			GridCells.Adaptive(150.dp)
		}

		LazyVerticalGrid(
			columns = columns,
			state = state,
			modifier = Modifier.fillMaxSize(),
			contentPadding = contentPadding,
			horizontalArrangement = Arrangement.spacedBy(4.dp)
		) {
			items(items) { item ->
				VodCard(
					name = name(item),
					pic = pic(item),
					year = year(item),
					note = note(item),
					onLongClick = if (onLongClick != null) ({ onLongClick(item) }) else null
				) { onClick(item) }
			}
		}
	}
}
