package com.github.tvbox.osc.util

import android.app.Activity
import com.github.tvbox.osc.data.ConfigKey
import com.github.tvbox.osc.data.PreferenceStore
import com.github.tvbox.osc.util.ScreenUtils.getSqrt

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
		return PreferenceStore.get(ConfigKey.SUBTITLE_TEXT_SIZE, autoSize)
	}

	fun setTextSize(size: Int) {
		PreferenceStore.put(ConfigKey.SUBTITLE_TEXT_SIZE, size)
	}

	var timeDelay: Int
		get() = PreferenceStore.get(ConfigKey.SUBTITLE_TIME_DELAY, 0)
		set(delay) {
			PreferenceStore.put(ConfigKey.SUBTITLE_TIME_DELAY, delay)
		}
}
