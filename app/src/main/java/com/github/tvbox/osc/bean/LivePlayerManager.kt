package com.github.tvbox.osc.bean

import com.github.tvbox.osc.data.ConfigKey
import com.github.tvbox.osc.data.PreferenceStore
import com.github.tvbox.osc.util.PlayerHelper
import org.json.JSONException
import org.json.JSONObject
import xyz.doikki.videoplayer.player.VideoView

class LivePlayerManager {
	val defaultPlayerConfig: JSONObject = JSONObject()
	var currentPlayerConfig: JSONObject? = null
	private var currentApi: String? = ""

	val livePlayerType: Int
		get() {
			val config = currentPlayerConfig ?: return 0
			return try {
				when (config.getInt("pl")) {
					0 -> 0
					1 -> if (config.getString("ijk") == "硬解码") 1 else 2
					2 -> 3
					else -> 0
				}
			} catch (e: JSONException) {
				e.printStackTrace()
				0
			}
		}

	val livePlayerScale: Int
		get() {
			val config = currentPlayerConfig ?: return 0
			return try {
				config.getInt("sc")
			} catch (e: JSONException) {
				e.printStackTrace()
				0
			}
		}

	fun init(videoView: VideoView?) {
		try {
			currentApi = PreferenceStore.get(ConfigKey.LIVE_API_URL, "")
			defaultPlayerConfig.put("pl", PreferenceStore.get(ConfigKey.LIVE_PLAY_TYPE, PreferenceStore.get(ConfigKey.PLAY_TYPE, 0)))
			defaultPlayerConfig.put("ijk", PreferenceStore.get(ConfigKey.IJK_CODEC, "硬解码"))
			defaultPlayerConfig.put("pr", PreferenceStore.get(ConfigKey.PLAY_RENDER, 0))
			defaultPlayerConfig.put("sc", PreferenceStore.get(ConfigKey.PLAY_SCALE, 0))
		} catch (e: JSONException) {
			e.printStackTrace()
		}
		getDefaultLiveChannelPlayer(videoView)
	}

	fun getDefaultLiveChannelPlayer(videoView: VideoView?) {
		PlayerHelper.updateCfg(videoView, defaultPlayerConfig)
		try {
			currentPlayerConfig = JSONObject(defaultPlayerConfig.toString())
		} catch (e: JSONException) {
			e.printStackTrace()
		}
	}

	fun getLiveChannelPlayer(videoView: VideoView, channelName: String?) {
		val cfgKey = currentCfgKey(channelName)
		val jsonStr = PreferenceStore.get(cfgKey, "")
		val playerConfig = if (jsonStr.isEmpty()) null else JSONObject(jsonStr)
		if (playerConfig == null) {
			if (currentPlayerConfig.toString() != defaultPlayerConfig.toString()) {
				getDefaultLiveChannelPlayer(videoView)
			}
			return
		}
		if (playerConfig.toString() == currentPlayerConfig.toString()) return

		try {
			val curCfg = currentPlayerConfig ?: return
			if (playerConfig.getInt("pl") == curCfg.getInt("pl") &&
				playerConfig.getInt("pr") == curCfg.getInt("pr") &&
				playerConfig.getString("ijk") == curCfg.getString("ijk")
			) {
				videoView.setScreenScaleType(playerConfig.getInt("sc"))
			} else {
				PlayerHelper.updateCfg(videoView, playerConfig)
			}
		} catch (e: JSONException) {
			e.printStackTrace()
		}

		currentPlayerConfig = playerConfig
	}

	fun changeLivePlayerType(videoView: VideoView?, playerType: Int, channelName: String?) {
		val cfgKey = currentCfgKey(channelName)
		val playerConfig = currentPlayerConfig ?: return

		try {
			when (playerType) {
				0 -> {
					playerConfig.put("pl", 0)
					playerConfig.put("ijk", "软解码")
				}

				1 -> {
					playerConfig.put("pl", 1)
					playerConfig.put("ijk", "硬解码")
				}

				2 -> {
					playerConfig.put("pl", 1)
					playerConfig.put("ijk", "软解码")
				}

				3 -> {
					playerConfig.put("pl", 2)
					playerConfig.put("ijk", "软解码")
				}
			}
		} catch (e: JSONException) {
			e.printStackTrace()
		}
		PlayerHelper.updateCfg(videoView, playerConfig)

		if (playerConfig.toString() == defaultPlayerConfig.toString()) {
			PreferenceStore.delete(cfgKey)
		} else {
			PreferenceStore.put(cfgKey, playerConfig.toString())
		}

		currentPlayerConfig = playerConfig
	}

	fun changeLivePlayerScale(videoView: VideoView, playerScale: Int, channelName: String?) {
		val cfgKey = currentCfgKey(channelName)
		videoView.setScreenScaleType(playerScale)

		val playerConfig = currentPlayerConfig ?: return
		try {
			playerConfig.put("sc", playerScale)
		} catch (e: JSONException) {
			e.printStackTrace()
		}

		if (playerConfig.toString() == defaultPlayerConfig.toString()) {
			PreferenceStore.delete(cfgKey)
		} else {
			PreferenceStore.put(cfgKey, playerConfig.toString())
		}

		currentPlayerConfig = playerConfig
	}

	private fun currentCfgKey(channelName: String?): String {
		return "${currentApi}_$channelName"
	}
}
