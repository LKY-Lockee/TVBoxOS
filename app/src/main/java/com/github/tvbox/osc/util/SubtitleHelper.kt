package com.github.tvbox.osc.util

import android.app.Activity
import com.github.tvbox.osc.util.ScreenUtils.getSqrt
import com.orhanobut.hawk.Hawk

object SubtitleHelper {
	fun getSubtitleTextAutoSize(activity: Activity): Int {
		val screenSqrt = getSqrt(activity)
		return when {
			screenSqrt > 50.0 -> 46
			screenSqrt > 13.0 -> 36
			screenSqrt > 7.0 -> 24
			else -> 16
		}
	}

	fun getTextSize(activity: Activity): Int {
		val autoSize = getSubtitleTextAutoSize(activity)
		return Hawk.get(HawkConfig.SUBTITLE_TEXT_SIZE, autoSize)
	}

	fun setTextSize(size: Int) {
		Hawk.put(HawkConfig.SUBTITLE_TEXT_SIZE, size)
	}

	var timeDelay: Int
		get() = Hawk.get(HawkConfig.SUBTITLE_TIME_DELAY, 0)
		set(delay) {
			Hawk.put(HawkConfig.SUBTITLE_TIME_DELAY, delay)
		}
}
