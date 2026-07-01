package com.github.tvbox.osc.bean

import com.github.tvbox.osc.data.ConfigKey
import com.github.tvbox.osc.data.PreferenceStore

/**
 * @author pj567
 * @date 2021/3/8
 */
class IJKCode {
	var name: String? = null
	var option: LinkedHashMap<String, String>? = null
	var isSelected: Boolean = false
		private set

	fun selected(selected: Boolean) {
		isSelected = selected
		if (selected) {
			PreferenceStore.put(ConfigKey.IJK_CODEC, name)
		}
	}
}
