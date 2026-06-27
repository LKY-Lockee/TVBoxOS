package com.github.tvbox.osc.event

/**
 * @author pj567
 * @date 2021/1/6
 */
class RefreshEvent(
	val type: Int,
	var obj: Any? = null
) {
	companion object {
		const val TYPE_REFRESH = 0
		const val TYPE_HISTORY_REFRESH = 1
		const val TYPE_QUICK_SEARCH = 2
		const val TYPE_QUICK_SEARCH_SELECT = 3
		const val TYPE_QUICK_SEARCH_WORD = 4
		const val TYPE_QUICK_SEARCH_WORD_CHANGE = 5
		const val TYPE_SEARCH_RESULT = 6
		const val TYPE_QUICK_SEARCH_RESULT = 7
		const val TYPE_API_URL_CHANGE = 8
		const val TYPE_PUSH_URL = 9
		const val TYPE_EPG_URL_CHANGE = 10
		const val TYPE_SETTING_SEARCH_TV = 11
		const val TYPE_SUBTITLE_SIZE_CHANGE = 12
		const val TYPE_FILTER_CHANGE = 13
	}
}
