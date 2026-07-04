package com.github.tvbox.osc.ui.compose.component

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.tvbox.osc.bean.MovieSort.SortFilter
import com.github.tvbox.osc.ui.compose.theme.Ts20

/**
 * 筛选条（替代 item_filter_split_button + bottom_sheet_filter_options）。
 * 横向滚动的 FilterChip 列表，点击任一项弹出底部选择面板。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterChipRow(
	filters: List<SortFilter>,
	filterSelect: Map<String, String>,
	onSelect: (filterKey: String, value: String?) -> Unit,
	modifier: Modifier = Modifier
) {
	var openedFilter by remember { mutableStateOf<SortFilter?>(null) }

	Row(
		modifier
			.fillMaxWidth()
			.horizontalScroll(rememberScrollState())
			.padding(horizontal = 10.dp, vertical = 6.dp),
		horizontalArrangement = Arrangement.spacedBy(8.dp)
	) {
		filters.forEach { filter ->
			val key = filter.key
			val selectedValue = key?.let { filterSelect[it] }
			val displayText = if (selectedValue != null) {
				val displayKey = filter.values?.entries?.firstOrNull { it.value == selectedValue }?.key
				"${filter.name}: $displayKey"
			} else {
				filter.name.orEmpty()
			}
			FilterChip(
				selected = selectedValue != null,
				onClick = { openedFilter = filter },
				label = { Text(displayText, fontSize = Ts20) }
			)
		}
	}

	openedFilter?.let { filter ->
		val sheetState = rememberModalBottomSheetState()
		ModalBottomSheet(
			onDismissRequest = { openedFilter = null },
			sheetState = sheetState
		) {
			Text(
				text = filter.name.orEmpty(),
				modifier = Modifier
					.fillMaxWidth()
					.padding(16.dp),
				fontSize = Ts20
			)
			FlowRow(
				modifier = Modifier
					.fillMaxWidth()
					.padding(16.dp),
				horizontalArrangement = Arrangement.spacedBy(8.dp),
				verticalArrangement = Arrangement.spacedBy(8.dp)
			) {
				filter.values?.forEach { (displayKey, actualValue) ->
					val key = filter.key ?: return@forEach
					FilterChip(
						selected = filterSelect[key] == actualValue,
						onClick = {
							val newValue = if (filterSelect[key] == actualValue) null else actualValue
							onSelect(key, newValue)
							openedFilter = null
						},
						label = { Text(displayKey, fontSize = Ts20) }
					)
				}
			}
		}
	}
}
