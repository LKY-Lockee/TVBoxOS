package com.github.tvbox.osc.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Pair
import androidx.core.content.edit

/**
 * 音轨记忆
 */
class AudioTrackMemory private constructor(context: Context) {
	private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

	fun save(playKey: String, groupIndex: Int, trackIndex: Int) {
		TVBoxRuntimeLog.i("echo-AudioTrackMemory save playKey:$playKey")
		val key = "${playKey}_exo"
		prefs.edit { putInt(key + KEY_GROUP_SUFFIX, groupIndex).putInt(key + KEY_TRACK_SUFFIX, trackIndex) }
	}

	fun save(playKey: String, trackIndex: Int) {
		TVBoxRuntimeLog.i("echo-AudioTrackMemory save playKey:$playKey")
		prefs.edit { putInt("${playKey}_ijk$KEY_TRACK_SUFFIX", trackIndex) }
	}

	fun exoLoad(playKey: String): Pair<Int, Int>? {
		val key = "${playKey}_exo"
		val group = prefs.getInt(key + KEY_GROUP_SUFFIX, -1)
		val track = prefs.getInt(key + KEY_TRACK_SUFFIX, -1)
		return if (group >= 0 && track >= 0) Pair.create(group, track) else null
	}

	fun ijkLoad(playKey: String): Int {
		return prefs.getInt("${playKey}_ijk$KEY_TRACK_SUFFIX", -1)
	}

	companion object {
		private const val PREFS_NAME = "audio_track_prefs"
		private const val KEY_GROUP_SUFFIX = "_group"
		private const val KEY_TRACK_SUFFIX = "_track"
		private var instance: AudioTrackMemory? = null

		fun getInstance(context: Context): AudioTrackMemory {
			return instance ?: synchronized(this) {
				instance ?: AudioTrackMemory(context).also { instance = it }
			}
		}
	}
}
