package com.github.tvbox.osc.server

/**
 * @author pj567
 * @date 2021/1/5
 */
interface DataReceiver {
	fun onTextReceived(text: String?)

	fun onApiReceived(url: String?)

	fun onPushReceived(url: String?)
}
