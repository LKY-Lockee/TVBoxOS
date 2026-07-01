package com.github.catvod.crawler

import android.util.Log

object SpiderDebug {
	@JvmStatic
	fun log(th: Throwable) {
		Log.d("SpiderLog", th.message, th)
	}

	@JvmStatic
	fun log(msg: String) {
		Log.d("SpiderLog", msg)
	}

	fun ec(i: Int): String {
		return ""
	}
}
