package com.github.tvbox.osc.player

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import com.github.tvbox.osc.util.AudioTrackMemory
import com.github.tvbox.osc.util.TVBoxRuntimeLog
import xyz.doikki.videoplayer.exo.ExoMediaPlayer
import java.util.Locale

@UnstableApi
class ExoPlayer(context: Context) : ExoMediaPlayer(context) {
	init {
		memory = AudioTrackMemory.getInstance(context)
	}

	val trackInfo: TrackInfo
		// 3. 获取所有轨道信息
		get() {
			val data = TrackInfo()
			val player = mInternalPlayer ?: return data
			val currentTracks = player.currentTracks

			var audioGroupIndex = 0
			var subtitleGroupIndex = 0

			for (trackGroup in currentTracks.groups) {
				val trackType = trackGroup.type
				if (trackType != C.TRACK_TYPE_AUDIO && trackType != C.TRACK_TYPE_TEXT) continue

				for (i in 0..<trackGroup.length) {
					if (!trackGroup.isTrackSupported(i)) continue

					val fmt = trackGroup.getTrackFormat(i)
					val bean = TrackInfoBean()
					bean.language = getLanguage(fmt)
					bean.name = getName(fmt)
					bean.index = i
					bean.selected = trackGroup.isTrackSelected(i)

					if (trackType == C.TRACK_TYPE_AUDIO) {
						bean.groupIndex = audioGroupIndex
						data.addAudio(bean)
					} else {
						bean.groupIndex = subtitleGroupIndex
						data.addSubtitle(bean)
					}
				}

				if (trackType == C.TRACK_TYPE_AUDIO) {
					audioGroupIndex++
				} else {
					subtitleGroupIndex++
				}
			}
			return data
		}

	/**
	 * 设置当前播放的音轨
	 * 
	 * @param groupIndex 音轨组的索引
	 * @param trackIndex 音轨在组内的索引
	 */
	fun setTrack(groupIndex: Int, trackIndex: Int, playKey: String) {
		try {
			val player = mInternalPlayer ?: run {
				TVBoxRuntimeLog.i("echo-setTrack: Player is null")
				return
			}

			val currentTracks = player.currentTracks
			var audioGroupCount = 0
			var targetGroup: Tracks.Group? = null

			// Find the target audio track group
			for (trackGroup in currentTracks.groups) {
				if (trackGroup.type == C.TRACK_TYPE_AUDIO) {
					if (audioGroupCount == groupIndex) {
						targetGroup = trackGroup
						break
					}
					audioGroupCount++
				}
			}

			if (targetGroup == null || trackIndex >= targetGroup.length) {
				TVBoxRuntimeLog.i("echo-setTrack: Invalid track index - group:$groupIndex, track:$trackIndex")
				return
			}

			// In Media3, we need to use preferred audio language or manual track selection
			// For now, using the TrackSelectionParameters to prefer specific tracks
			val targetFormat = targetGroup.getTrackFormat(trackIndex)

			// Set parameters to prefer this specific audio track
			// mTrackSelector is private in parent; use Player's public API instead
			player.trackSelectionParameters = player.trackSelectionParameters
				.buildUpon()
				.setPreferredAudioLanguage(targetFormat.language.orEmpty())
				.build()

			// 缓存到 map：下次同一路径播放时使用
			if (playKey.isNotEmpty()) {
				memory.save(playKey, groupIndex, trackIndex)
			}
		} catch (e: Exception) {
			TVBoxRuntimeLog.i("echo-setTrack error: " + e.message)
		}
	}

	//加载上一次选中的音轨
	fun loadDefaultTrack(playKey: String) {
		val pair = memory.exoLoad(playKey) ?: return

		val groupIndex: Int = pair.first ?: return
		val trackIndex: Int = pair.second ?: return

		setTrack(groupIndex, trackIndex, "")
	}

	private fun getLanguage(fmt: Format): String {
		val lang = fmt.language
		if (lang.isNullOrEmpty() || "und".equals(lang, ignoreCase = true)) {
			return "未知"
		}
		return LANG_MAP[lang.lowercase(Locale.getDefault())] ?: lang
	}

	private fun getName(fmt: Format): String {
		val channelLabel = when {
			fmt.channelCount <= 0 -> ""
			fmt.channelCount == 1 -> "单声道"
			fmt.channelCount == 2 -> "立体声"
			else -> "${fmt.channelCount} 声道"
		}
		var codec = ""
		val mimeType = fmt.sampleMimeType
		if (mimeType != null) {
			codec = mimeType.substring(mimeType.indexOf('/') + 1).uppercase(Locale.getDefault())
		}
		return "$channelLabel, $codec"
	}

	companion object {
		private val LANG_MAP: Map<String, String> = mapOf(
			"zh" to "中文",
			"zh-cn" to "中文",
			"en" to "英语",
			"en-us" to "英语"
		)
		private lateinit var memory: AudioTrackMemory
	}
}
