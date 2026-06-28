package com.github.tvbox.osc.server

import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.IHTTPSession

/**
 * @author pj567
 * @date 2021/1/5
 */
interface RequestProcess {
	fun isRequest(session: IHTTPSession, fileName: String): Boolean

	fun doResponse(session: IHTTPSession, fileName: String, params: Map<String, List<String>>, files: Map<String, String>?): NanoHTTPD.Response

	companion object {
		const val KEY_ACTION_PRESSED: Int = 0
		const val KEY_ACTION_DOWN: Int = 1
		const val KEY_ACTION_UP: Int = 2
	}
}
