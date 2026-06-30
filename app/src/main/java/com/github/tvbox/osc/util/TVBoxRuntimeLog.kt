package com.github.tvbox.osc.util

import android.util.Log

/**
 * @author pj567
 * @date 2020/12/18
 */
object TVBoxRuntimeLog {
	private const val TAG = "TVBox-runtime"

	fun e(msg: String) {
		Log.e(TAG, msg)
	}

	fun i(msg: String) {
		Log.i(TAG, msg)
	}
}
