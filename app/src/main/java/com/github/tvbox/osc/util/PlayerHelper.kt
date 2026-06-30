package com.github.tvbox.osc.util

import android.app.Activity
import com.github.tvbox.osc.api.ApiConfig
import com.github.tvbox.osc.player.ExoMediaPlayerFactory
import com.github.tvbox.osc.player.IjkMediaPlayer
import com.github.tvbox.osc.player.render.SurfaceRenderViewFactory
import com.github.tvbox.osc.player.thirdparty.Kodi
import com.github.tvbox.osc.player.thirdparty.MXPlayer
import com.github.tvbox.osc.player.thirdparty.ReexPlayer
import com.github.tvbox.osc.player.thirdparty.RemoteTVBox
import com.github.tvbox.osc.player.thirdparty.VlcPlayer
import com.orhanobut.hawk.Hawk
import org.json.JSONException
import org.json.JSONObject
import xyz.doikki.videoplayer.player.AndroidMediaPlayerFactory
import xyz.doikki.videoplayer.player.PlayerFactory
import xyz.doikki.videoplayer.player.VideoView
import xyz.doikki.videoplayer.render.RenderViewFactory
import xyz.doikki.videoplayer.render.TextureRenderViewFactory
import java.text.DecimalFormat

object PlayerHelper {
	private var mPlayersInfo: HashMap<Int, String>? = null
	private var mPlayersExistInfo: HashMap<Int, Boolean>? = null

	fun updateCfg(videoView: VideoView?, playerCfg: JSONObject, forcePlayerType: Int = -1) {
		var playerType = Hawk.get(HawkConfig.PLAY_TYPE, 0)
		var renderType = Hawk.get(HawkConfig.PLAY_RENDER, 0)
		var ijkCode = Hawk.get(HawkConfig.IJK_CODEC, "硬解码")
		var scale = Hawk.get(HawkConfig.PLAY_SCALE, 0)
		try {
			playerType = playerCfg.getInt("pl")
			renderType = playerCfg.getInt("pr")
			ijkCode = playerCfg.getString("ijk")
			scale = playerCfg.getInt("sc")
		} catch (e: JSONException) {
			e.printStackTrace()
		}
		if (forcePlayerType >= 0) playerType = forcePlayerType
		val codec = ApiConfig.instance.getIJKCodec(ijkCode)
		val playerFactory = when (playerType) {
			1 -> {
				try {
					tv.danmaku.ijk.media.player.IjkMediaPlayer.loadLibrariesOnce { s: String ->
						try {
							System.loadLibrary(s)
						} catch (th: Throwable) {
							th.printStackTrace()
						}
					}
				} catch (th: Throwable) {
					th.printStackTrace()
				}
				object : PlayerFactory<IjkMediaPlayer>() {
					override fun createPlayer(context: android.content.Context): IjkMediaPlayer {
						return IjkMediaPlayer(context, codec)
					}
				}
			}
			2 -> ExoMediaPlayerFactory.create()
			else -> AndroidMediaPlayerFactory.create()
		}
		val renderViewFactory: RenderViewFactory = when (renderType) {
			1 -> SurfaceRenderViewFactory.create()
			else -> TextureRenderViewFactory.create()
		}
		videoView?.apply {
			@Suppress("UNCHECKED_CAST")
			setPlayerFactory(playerFactory as PlayerFactory<xyz.doikki.videoplayer.player.AbstractPlayer>)
			setRenderViewFactory(renderViewFactory)
			setScreenScaleType(scale)
		}
	}

	fun updateCfg(videoView: VideoView) {
		val playType = Hawk.get(HawkConfig.PLAY_TYPE, 0)
		val playerFactory = when (playType) {
			1 -> {
				try {
					tv.danmaku.ijk.media.player.IjkMediaPlayer.loadLibrariesOnce { s: String ->
						try {
							System.loadLibrary(s)
						} catch (th: Throwable) {
							th.printStackTrace()
						}
					}
				} catch (th: Throwable) {
					th.printStackTrace()
				}
				object : PlayerFactory<IjkMediaPlayer>() {
					override fun createPlayer(context: android.content.Context): IjkMediaPlayer {
						return IjkMediaPlayer(context, null)
					}
				}
			}
			2 -> ExoMediaPlayerFactory.create()
			else -> AndroidMediaPlayerFactory.create()
		}
		val renderType = Hawk.get(HawkConfig.PLAY_RENDER, 0)
		val renderViewFactory: RenderViewFactory = when (renderType) {
			1 -> SurfaceRenderViewFactory.create()
			else -> TextureRenderViewFactory.create()
		}
		@Suppress("UNCHECKED_CAST")
		videoView.setPlayerFactory(playerFactory as PlayerFactory<xyz.doikki.videoplayer.player.AbstractPlayer>)
		videoView.setRenderViewFactory(renderViewFactory)
	}

	fun init() {
		try {
			tv.danmaku.ijk.media.player.IjkMediaPlayer.loadLibrariesOnce { s: String ->
				try {
					System.loadLibrary(s)
				} catch (th: Throwable) {
					th.printStackTrace()
				}
			}
		} catch (th: Throwable) {
			th.printStackTrace()
		}
	}

	fun getPlayerName(playType: Int): String {
		return playersInfo.getOrDefault(playType, "系统播放器")
	}

	val playersInfo: HashMap<Int, String>
		get() {
			var info = mPlayersInfo
			if (info == null) {
				info = HashMap<Int, String>().apply {
					this[0] = "系统播放器"
					this[1] = "IJK播放器"
					this[2] = "Exo播放器"
					this[10] = "MX播放器"
					this[11] = "Reex播放器"
					this[12] = "Kodi播放器"
					this[13] = "附近TVBox"
					this[14] = "VLC播放器"
				}
				mPlayersInfo = info
			}
			return info
		}

	val playersExistInfo: HashMap<Int, Boolean>
		get() {
			var exist = mPlayersExistInfo
			if (exist == null) {
				exist = HashMap<Int, Boolean>().apply {
					this[0] = true
					this[1] = true
					this[2] = true
					this[10] = MXPlayer.packageInfo != null
					this[11] = ReexPlayer.packageInfo != null
					this[12] = Kodi.packageInfo != null
					this[13] = RemoteTVBox.available != null
					this[14] = VlcPlayer.packageInfo != null
				}
				mPlayersExistInfo = exist
			}
			return exist
		}

	fun getPlayerExist(playType: Int): Boolean {
		return playersExistInfo.getOrDefault(playType, false)
	}

	val existPlayerTypes: List<Int>
		get() {
			val existPlayers = ArrayList<Int>()
			for ((playerType, exists) in playersExistInfo) {
				if (exists) {
					existPlayers.add(playerType)
				}
			}
			return existPlayers
		}

	fun runExternalPlayer(
		playerType: Int,
		activity: Activity,
		url: String?,
		title: String?,
		subtitle: String?,
		headers: HashMap<String, String>?,
		progress: Long = 0
	): Boolean {
		return when (playerType) {
			10 -> MXPlayer.run(activity, url, title, subtitle, headers)
			11 -> ReexPlayer.run(activity, url, title, subtitle, headers)
			12 -> Kodi.run(activity, url, title, subtitle, headers)
			13 -> RemoteTVBox.run(activity, url, title, subtitle, headers)
			14 -> VlcPlayer.run(activity, url, title, subtitle, progress)
			else -> false
		}
	}

	fun getRenderName(renderType: Int): String {
		return if (renderType == 1) "SurfaceView" else "TextureView"
	}

	fun getScaleName(screenScaleType: Int): String {
		return when (screenScaleType) {
			VideoView.SCREEN_SCALE_DEFAULT -> "默认"
			VideoView.SCREEN_SCALE_16_9 -> "16:9"
			VideoView.SCREEN_SCALE_4_3 -> "4:3"
			VideoView.SCREEN_SCALE_MATCH_PARENT -> "填充"
			VideoView.SCREEN_SCALE_ORIGINAL -> "原始"
			VideoView.SCREEN_SCALE_CENTER_CROP -> "裁剪"
			else -> "默认"
		}
	}

	fun getDisplaySpeed(speed: Long, show: Boolean): String {
		return when {
			speed > 1048576 -> DecimalFormat("#.00").format(speed / 1048576.0) + "Mb/s"
			speed > 1024 -> (speed / 1024).toString() + "Kb/s"
			speed > 0 -> speed.toString() + "B/s"
			else -> if (show) "0B/s" else ""
		}
	}

	fun getDisplaySpeedBps(speed: Long, show: Boolean): String {
		val bitSpeed = speed * 8 // 字节转比特
		return when {
			bitSpeed >= 1000000000 -> DecimalFormat("0.00").format(bitSpeed / 1000000000.0) + "Gbps"
			bitSpeed >= 1000 -> {
				val mbps = bitSpeed / 1000000.0
				val df = if (mbps < 0.1) DecimalFormat("0.00") else DecimalFormat("0.0")
				df.format(mbps) + "Mbps"
			}
			else -> if (show) "0bps" else ""
		}
	}
}
