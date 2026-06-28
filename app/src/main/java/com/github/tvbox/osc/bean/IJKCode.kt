package com.github.tvbox.osc.bean

import com.github.tvbox.osc.util.HawkConfig
import com.orhanobut.hawk.Hawk

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
			Hawk.put(HawkConfig.IJK_CODEC, name)
		}
	}
}
