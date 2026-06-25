package com.github.catvod.crawler

import android.util.*

object SpiderDebug {
	fun log(th: Throwable) {
		Log.d("SpiderLog", th.message, th)
	}

	fun log(msg: String) {
		Log.d("SpiderLog", msg)
	}

	fun ec(i: Int): String {
		return ""
	}
}
