package com.github.tvbox.osc.event

/**
 * @author pj567
 * @date 2021/1/5
 */
class ServerEvent(
	val type: Int,
	var obj: Any? = null
) {
	companion object {
		const val SERVER_SUCCESS = 0
		const val SERVER_CONNECTION = 1
		const val SERVER_SEARCH = 2
	}
}
