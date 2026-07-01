package com.github.tvbox.osc.player

import android.content.Context
import com.github.tvbox.osc.api.ApiConfig
import com.github.tvbox.osc.bean.IJKCode
import com.github.tvbox.osc.data.ConfigKey
import com.github.tvbox.osc.data.PreferenceStore
import com.github.tvbox.osc.server.ControlManager
import com.github.tvbox.osc.util.AudioTrackMemory
import com.github.tvbox.osc.util.FileUtils
import com.github.tvbox.osc.util.MD5
import com.github.tvbox.osc.util.TVBoxRuntimeLog
import tv.danmaku.ijk.media.player.IMediaPlayer
import tv.danmaku.ijk.media.player.IjkMediaPlayer
import tv.danmaku.ijk.media.player.misc.ITrackInfo
import xyz.doikki.videoplayer.ijk.IjkPlayer
import java.io.File
import java.net.URI
import java.net.URLEncoder
import java.util.Locale

open class IjkMediaPlayer(context: Context, private val codec: IJKCode?) : IjkPlayer(context) {
	protected var currentPlayPath: String? = null

	init {
		memory = AudioTrackMemory.getInstance(context)
	}

	override fun setOptions() {
		super.setOptions()
		val codecTmp = this.codec ?: ApiConfig.instance.currentIJKCode
		val options: LinkedHashMap<String, String> = codecTmp?.option ?: return
		for (key in options.keys) {
			val value = options[key] ?: continue
			val opt = key.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
			val category = opt[0].trim { it <= ' ' }.toInt()
			val name = opt[1].trim { it <= ' ' }
			try {
				val valLong = value.toLong()
				mMediaPlayer.setOption(category, name, valLong)
			} catch (e: Exception) {
				mMediaPlayer.setOption(category, name, value)
			}
		}
		mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "max-fps", 30)

		// 设置视频流格式
//        mMediaPlayer.setOption(tv.danmaku.ijk.media.player.IjkMediaPlayer.OPT_CATEGORY_PLAYER, "overlay-format", tv.danmaku.ijk.media.player.IjkMediaPlayer.SDL_FCC_RV32);

		//开启内置字幕
		mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "subtitle", 1)
		mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "dns_cache_clear", 1)
		mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "dns_cache_timeout", -1)
		mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "safe", 0)

		if (PreferenceStore.get(ConfigKey.PLAYER_IS_LIVE, false)) {
			TVBoxRuntimeLog.i("echo-type-直播")
			mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "max_cached_duration", 300)
			mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "flush_packets", 1)
			mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "min-frames", 1)
			mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "threads", "1")
		} else {
			TVBoxRuntimeLog.i("echo-type-点播")
			// 降低延迟
			mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "max_cached_duration", 3000)
			mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "infbuf", 0)
			mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "threads", "2")
		}
		//        mMediaPlayer.setOption(tv.danmaku.ijk.media.player.IjkMediaPlayer.OPT_CATEGORY_PLAYER, "sync-av-start", 1);//强制音画同步
	}

	override fun setDataSource(path: String?, headers: MutableMap<String?, String?>?) {
		var resolvedPath = path ?: return
		try {
			when (getStreamType(resolvedPath)) {
				RTSP_UDP_RTP -> {
					mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "infbuf", 1)
					mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "rtsp_transport", "tcp")
					mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "rtsp_flags", "prefer_tcp")
					mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "probesize", (512 * 1000).toLong())
					mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "analyzeduration", (2 * 1000 * 1000).toLong())
				}

				CACHE_VIDEO -> if (PreferenceStore.get(ConfigKey.IJK_CACHE_PLAY, false)) {
					val cachePath = FileUtils.cachePath + "/ijkcaches/"
					val cacheFile = File(cachePath)
					if (!cacheFile.exists()) cacheFile.mkdirs()
					val tmpMd5 = MD5.string2MD5(resolvedPath)
					val cacheFilePath = "$cachePath$tmpMd5.file"
					val cacheMapPath = "$cachePath$tmpMd5.map"

					mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "cache_file_path", cacheFilePath)
					mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "cache_map_path", cacheMapPath)
					mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "parse_cache_map", 1)
					mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "auto_save_map", 1)
					mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "cache_max_capacity", (60 * 1024 * 1024).toLong())
					resolvedPath = "ijkio:cache:ffio:$resolvedPath"
				}

				M3U8 -> // 直播且是ijk的时候自动自动走代理解决DNS
					if (PreferenceStore.get(ConfigKey.PLAYER_IS_LIVE, false)) {
						val uri = URI(resolvedPath)
						val host = uri.host
						if (ITV_TARGET_DOMAIN.equals(host, ignoreCase = true)) resolvedPath = ControlManager.instance.getAddress(true) + "proxy?go=live&type=m3u8&url=" + URLEncoder.encode(resolvedPath, "UTF-8")
					}

				else -> {}
			}
		} catch (e: Exception) {
			e.printStackTrace()
		}
		setDataSourceHeader(headers)
		mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "protocol_whitelist", "ijkio,ffio,async,cache,crypto,file,dash,http,https,ijkhttphook,ijkinject,ijklivehook,ijklongurl,ijksegment,ijktcphook,pipe,rtp,tcp,tls,udp,ijkurlhook,data")
		currentPlayPath = resolvedPath
		super.setDataSource(resolvedPath, null)
	}

	private fun getStreamType(path: String?): Int {
		if (path.isNullOrEmpty()) {
			return OTHER
		}
		// 低成本检查 RTSP/UDP/RTP 类型
		val lowerPath = path.lowercase(Locale.getDefault())
		if (lowerPath.startsWith("rtsp://") || lowerPath.startsWith("udp://") || lowerPath.startsWith("rtp://")) {
			return RTSP_UDP_RTP
		}
		val cleanUrl = path.split("\\?".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
		if (cleanUrl.endsWith(".m3u8")) {
			return M3U8
		}
		if (cleanUrl.endsWith(".mp4") || cleanUrl.endsWith(".mkv") || cleanUrl.endsWith(".avi")) {
			return CACHE_VIDEO
		}
		return OTHER
	}

	private fun setDataSourceHeader(headers: MutableMap<String?, String?>?) {
		if (!headers.isNullOrEmpty()) {
			val userAgent = headers["User-Agent"]
			if (!userAgent.isNullOrEmpty()) {
				mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "user_agent", userAgent)
				// 移除header中的User-Agent，防止重复
				headers.remove("User-Agent")
			}
			if (headers.isNotEmpty()) {
				val sb = StringBuilder()
				for (entry in headers.entries) {
					val value = entry.value
					if (!value.isNullOrEmpty()) {
						sb.append(entry.key)
						sb.append(": ")
						sb.append(value)
						sb.append("\r\n")
					}
				}
				mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "headers", sb.toString())
			}
		}
	}

	val trackInfo: TrackInfo?
		get() {
			val trackInfo = mMediaPlayer.trackInfo ?: return null
			val data = TrackInfo()
			val subtitleSelected = mMediaPlayer.getSelectedTrack(ITrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT)
			val audioSelected = mMediaPlayer.getSelectedTrack(ITrackInfo.MEDIA_TRACK_TYPE_AUDIO)
			for ((index, info) in trackInfo.withIndex()) {
				if (info.trackType == ITrackInfo.MEDIA_TRACK_TYPE_AUDIO) { //音轨信息
					val a = TrackInfoBean()
					val name = processAudioName(info.infoInline)
					a.language = info.language.orEmpty()
					if (name.startsWith("aac")) a.language = "中文"
					a.name = name
					a.index = index
					a.selected = index == audioSelected
					// 如果需要，还可以检查轨道的描述或标题以获取更多信息
					data.addAudio(a)
				} else if (info.trackType == ITrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT) { //内置字幕
					val t = TrackInfoBean()
					t.name = info.infoInline.orEmpty()
					t.language = info.language.orEmpty()
					t.index = index
					t.selected = index == subtitleSelected
					data.addSubtitle(t)
				}
			}
			return data
		}

	// 处理音轨名称格式
	private fun processAudioName(rawName: String): String {
		return rawName.replace("AUDIO,", "")
			.replace("N/A,", "")
			.replace(" ", "")
	}

	fun setTrack(trackIndex: Int) {
		val audioSelected = mMediaPlayer.getSelectedTrack(ITrackInfo.MEDIA_TRACK_TYPE_AUDIO)
		val subtitleSelected = mMediaPlayer.getSelectedTrack(ITrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT)
		if (trackIndex != audioSelected && trackIndex != subtitleSelected) {
			mMediaPlayer.selectTrack(trackIndex)
		}
	}

	fun setTrack(trackIndex: Int, playKey: String) {
		val audioSelected = mMediaPlayer.getSelectedTrack(ITrackInfo.MEDIA_TRACK_TYPE_AUDIO)
		if (trackIndex != audioSelected) {
			if (playKey.isNotEmpty()) {
				memory.save(playKey, trackIndex)
			}
			mMediaPlayer.selectTrack(trackIndex)
		}
	}

	fun setOnTimedTextListener(listener: IMediaPlayer.OnTimedTextListener?) {
		mMediaPlayer.setOnTimedTextListener(listener)
	}

	fun loadDefaultTrack(trackInfo: TrackInfo?, playKey: String?) {
		if (trackInfo != null && trackInfo.audio.size > 1) {
			val trackIndex: Int = memory.ijkLoad(playKey ?: return)
			if (trackIndex == -1) {
				val firstIndex = trackInfo.audio[0].index
				setTrack(firstIndex)
				return
			}
			setTrack(trackIndex)
		}
	}

	companion object {
		private const val ITV_TARGET_DOMAIN = "gslbserv.itv.cmvideo.cn"

		/**
		 * 解析 URL
		 */
		private const val RTSP_UDP_RTP = 1
		private const val CACHE_VIDEO = 2
		private const val M3U8 = 3
		private const val OTHER = 0
		private lateinit var memory: AudioTrackMemory
	}
}
