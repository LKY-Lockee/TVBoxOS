package com.github.tvbox.osc.ui.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.net.http.SslError
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.DiffUtil
import com.github.catvod.crawler.Spider
import com.github.tvbox.osc.R
import com.github.tvbox.osc.api.ApiConfig
import com.github.tvbox.osc.base.App
import com.github.tvbox.osc.base.BaseLazyFragment
import com.github.tvbox.osc.bean.ParseBean
import com.github.tvbox.osc.bean.SourceBean
import com.github.tvbox.osc.bean.Subtitle
import com.github.tvbox.osc.bean.VodInfo
import com.github.tvbox.osc.cache.CacheManager.delete
import com.github.tvbox.osc.cache.CacheManager.getCache
import com.github.tvbox.osc.cache.CacheManager.save
import com.github.tvbox.osc.event.RefreshEvent
import com.github.tvbox.osc.player.ExoPlayer
import com.github.tvbox.osc.player.IjkMediaPlayer
import com.github.tvbox.osc.player.MyVideoView
import com.github.tvbox.osc.player.TrackInfo
import com.github.tvbox.osc.player.TrackInfoBean
import com.github.tvbox.osc.player.controller.VodController
import com.github.tvbox.osc.player.controller.VodController.VodControlListener
import com.github.tvbox.osc.server.ControlManager
import com.github.tvbox.osc.ui.adapter.SelectDialogAdapter.SelectDialogInterface
import com.github.tvbox.osc.ui.dialog.SearchSubtitleDialog
import com.github.tvbox.osc.ui.dialog.SelectDialog
import com.github.tvbox.osc.ui.dialog.SubtitleDialog
import com.github.tvbox.osc.ui.dialog.SubtitleDialog.LocalFileChooserListener
import com.github.tvbox.osc.ui.dialog.SubtitleDialog.SearchSubtitleListener
import com.github.tvbox.osc.ui.dialog.SubtitleDialog.SubtitleViewListener
import com.github.tvbox.osc.util.AdBlocker.createEmptyResource
import com.github.tvbox.osc.util.AdBlocker.isAd
import com.github.tvbox.osc.util.DefaultConfig.checkReplaceProxy
import com.github.tvbox.osc.util.DefaultConfig.noAd
import com.github.tvbox.osc.util.FileUtils.hasExtension
import com.github.tvbox.osc.util.HawkConfig
import com.github.tvbox.osc.util.MD5.string2MD5
import com.github.tvbox.osc.util.PlayerHelper
import com.github.tvbox.osc.util.PlayerHelper.getPlayerName
import com.github.tvbox.osc.util.TVBoxRuntimeLog.e
import com.github.tvbox.osc.util.TVBoxRuntimeLog.i
import com.github.tvbox.osc.util.VideoParseRuler.checkIsVideoForParse
import com.github.tvbox.osc.util.VideoParseRuler.isFilter
import com.github.tvbox.osc.util.parser.SuperParse
import com.github.tvbox.osc.util.parser.SuperParse.stopJsonJx
import com.github.tvbox.osc.util.thunder.JianPian
import com.github.tvbox.osc.util.thunder.Thunder
import com.github.tvbox.osc.util.thunder.Thunder.ThunderCallback
import com.github.tvbox.osc.viewmodel.SourceViewModel
import com.google.android.material.loadingindicator.LoadingIndicator
import com.lzy.okgo.OkGo
import com.lzy.okgo.callback.AbsCallback
import com.lzy.okgo.model.HttpHeaders
import com.obsez.android.lib.filechooser.ChooserDialog
import com.orhanobut.hawk.Hawk
import me.jessyan.autosize.AutoSize
import okhttp3.Response
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONException
import org.json.JSONObject
import tv.danmaku.ijk.media.player.IMediaPlayer
import tv.danmaku.ijk.media.player.IjkTimedText
import xyz.doikki.videoplayer.player.ProgressManager
import java.util.LinkedList
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

class PlayFragment : BaseLazyFragment() {
	private val videoDuration: Long = -1
	private val loadedUrls: MutableMap<String?, Boolean?> = HashMap()
	private val loadFoundCount = AtomicInteger(0)
	var parseThreadPool: ExecutorService? = null
	var player: MyVideoView? = null
		private set
	private var mPlayLoadTip: TextView? = null
	private var mPlayLoadErr: ImageView? = null
	private var mPlayLoading: LoadingIndicator? = null
	private var mController: VodController? = null
	private var sourceViewModel: SourceViewModel? = null
	private var mHandler: Handler? = null
	private var mVodInfo: VodInfo? = null
	private var mVodPlayerCfg: JSONObject? = null
	private var sourceKey: String? = null
	private var sourceBean: SourceBean? = null
	private var autoRetryCount = 0
	private var lastRetryTime: Long = 0 // 记录上次调用时间（毫秒）
	private var allowSwitchPlayer = true
	private var playSubtitle: String? = null
	private var subtitleCacheKey: String? = null
	private var progressKey: String? = null
	private var parseFlag: String? = null
	private var webUrl: String? = null
	private var webUserAgent: String? = null
	private var webHeaderMap: HashMap<String, String>? = null
	private var webPlayUrl: String? = null

	// webview
	private var mSysWebView: WebView? = null
	private var loadFoundVideoUrls: LinkedList<String?>? = LinkedList<String?>()
	private var loadFoundVideoUrlsHeader = HashMap<String?, HashMap<String, String>>()

	override val layoutResID: Int
		get() = R.layout.activity_play

	@Subscribe(threadMode = ThreadMode.MAIN)
	fun refresh(event: RefreshEvent) {
		if (event.type == RefreshEvent.TYPE_SUBTITLE_SIZE_CHANGE) {
			mController?.mSubtitleView?.setTextSize((event.obj as Int).toFloat())
		}
	}

	override fun init() {
		initView()
		initViewModel()
		Hawk.put(HawkConfig.PLAYER_IS_LIVE, false)
	}

	fun getSavedProgress(url: String?): Long {
		var st = 0
		try {
			mVodPlayerCfg?.let { st = it.getInt("st") }
		} catch (e: JSONException) {
			e.printStackTrace()
		}
		val skip = st * 1000L
		val theCache = getCache(string2MD5(url)) ?: return skip
		var rec: Long = 0
		when (theCache) {
			is Long -> {
				rec = theCache
			}

			is String -> {
				try {
					rec = theCache.toLong()
				} catch (e: NumberFormatException) {
					i("echo-String value is not a valid long.")
				}
			}

			else -> {
				i("echo-Value cannot be converted to long.")
			}
		}
		return max(rec, skip)
	}

	private fun initView() {
		EventBus.getDefault().register(this)
		mHandler = Handler { msg ->
			when (msg.what) {
				100 -> {
					stopParse()
					errorWithRetry("嗅探错误", false)
				}
			}
			false
		}
		this.player = findViewById(R.id.mVideoView)
		mPlayLoadTip = findViewById(R.id.play_load_tip)
		mPlayLoading = findViewById(R.id.play_loading)
		mPlayLoadErr = findViewById(R.id.play_load_error)
		mController = VodController(requireContext())
		mController?.setCanChangePosition(true)
		mController?.setEnableInNormal(true)
		mController?.setGestureEnabled(true)
		val progressManager: ProgressManager = object : ProgressManager() {
			override fun saveProgress(url: String?, progress: Long) {
				save<Long?>(string2MD5(url), progress)
			}

			override fun getSavedProgress(url: String?): Long {
				return this@PlayFragment.getSavedProgress(url)
			}
		}
		player?.setProgressManager(progressManager)
		mController?.setListener(object : VodControlListener {
			override fun playNext(rmProgress: Boolean) {
				val preProgressKey = progressKey
				this@PlayFragment.playNext(rmProgress)
				if (rmProgress && preProgressKey != null) delete<Int?>(string2MD5(preProgressKey), 0)
			}

			override fun playPre() {
				this@PlayFragment.playPrevious()
			}

			override fun changeParse(pb: ParseBean) {
				autoRetryCount = 0
				doParse(pb)
			}

			override fun updatePlayerCfg() {
				mVodInfo?.playerCfg = mVodPlayerCfg.toString()
				EventBus.getDefault().post(RefreshEvent(RefreshEvent.TYPE_REFRESH, mVodPlayerCfg))
			}

			override fun replay(replay: Boolean) {
				autoRetryCount = 0
				if (replay) {
					play(true)
				} else {
					if (webPlayUrl != null && webPlayUrl?.isEmpty() != true) {
						stopParse()
						initParseLoadFound()
						player?.release()
						goPlayUrl(webPlayUrl, webHeaderMap)
					} else {
						play(false)
					}
				}
			}

			override fun errReplay() {
				errorWithRetry("视频播放出错", false)
			}

			override fun selectSubtitle() {
				try {
					selectMySubtitle()
				} catch (e: Exception) {
					e.printStackTrace()
				}
			}

			@UnstableApi
			override fun selectAudioTrack() {
				selectMyAudioTrack()
			}

			@UnstableApi
			override fun prepared() {
				initSubtitleView()
			}

			override fun startPlayUrl(url: String?, headers: HashMap<String, String>?) {
				goPlayUrl(url, headers)
			}

			override fun setAllowSwitchPlayer(isAllow: Boolean) {
				allowSwitchPlayer = isAllow
			}
		})
		player?.setVideoController(mController)
	}

	//设置字幕
	fun setSubtitle(path: String?) {
		if (!path.isNullOrEmpty()) {
			// 设置字幕
			mController?.mSubtitleView?.visibility = View.GONE
			mController?.mSubtitleView?.setSubtitlePath(path)
			mController?.mSubtitleView?.visibility = View.VISIBLE
		}
	}

	@Throws(Exception::class)
	fun selectMySubtitle() {
		val subtitleDialog = SubtitleDialog(requireActivity())
		val playerType = mVodPlayerCfg?.getInt("pl")
		if (mController?.mSubtitleView?.hasInternal == true && playerType == 1) {
			subtitleDialog.selectInternal?.visibility = View.VISIBLE
		} else {
			subtitleDialog.selectInternal?.visibility = View.GONE
		}
		subtitleDialog.setSubtitleViewListener(object : SubtitleViewListener {
			override fun setTextSize(size: Int) {
				mController?.mSubtitleView?.setTextSize(size.toFloat())
			}

			override fun setSubtitleDelay(milliseconds: Int) {
				mController?.mSubtitleView?.setSubtitleDelay(milliseconds)
			}

			override fun selectInternalSubtitle() {
				selectMyInternalSubtitle()
			}

			override fun setTextStyle(style: Int) {
				setSubtitleViewTextStyle(style)
			}
		})
		subtitleDialog.setSearchSubtitleListener(object : SearchSubtitleListener {
			override fun openSearchSubtitleDialog() {
				val searchSubtitleDialog = SearchSubtitleDialog(requireActivity())
				searchSubtitleDialog.setSubtitleLoader(object : SearchSubtitleDialog.SubtitleLoader {
					override fun loadSubtitle(subtitle: Subtitle) {
						if (!isAdded) return
						requireActivity().runOnUiThread {
							val zimuUrl = subtitle.url
							i("echo-Remote Subtitle Url: $zimuUrl")
							setSubtitle(zimuUrl) //设置字幕
							searchSubtitleDialog.dismiss()
						}
					}
				})
				if (mVodInfo?.playFlag?.contains("Ali") == true || mVodInfo?.playFlag?.contains("parse") == true) {
					searchSubtitleDialog.setSearchWord((mVodInfo ?: return).playNote)
				} else {
					searchSubtitleDialog.setSearchWord((mVodInfo ?: return).name ?: return)
				}
				searchSubtitleDialog.show()
			}
		})
		subtitleDialog.setLocalFileChooserListener(object : LocalFileChooserListener {
			override fun openLocalFileChooserDialog() {
				ChooserDialog(activity)
					.withFilter(false, false, "srt", "ass", "scc", "stl", "ttml")
					.withStartFile("/storage/emulated/0/Download")
					.withChosenListener { path, pathFile ->
						i("echo-Local Subtitle Path: $path")
						setSubtitle(path) //设置字幕
					}
					.build()
					.show()
			}
		})
		subtitleDialog.show()
	}

	@SuppressLint("UseCompatLoadingForColorStateLists")
	fun setSubtitleViewTextStyle(style: Int) {
		if (style == 0) {
			mController?.mSubtitleView?.setTextColor(requireContext().resources.getColorStateList(R.color.color_FFFFFF))
		} else if (style == 1) {
			mController?.mSubtitleView?.setTextColor(requireContext().resources.getColorStateList(R.color.color_FFB6C1))
		}
	}

	@UnstableApi
	fun selectMyAudioTrack() {
		val mediaPlayer = player?.mediaPlayer
		var trackInfo: TrackInfo? = null
		if (mediaPlayer is IjkMediaPlayer) {
			trackInfo = mediaPlayer.trackInfo
		}
		if (mediaPlayer is ExoPlayer) {
			trackInfo = mediaPlayer.trackInfo
		}
		if (trackInfo == null) {
			Toast.makeText(mContext, "没有音轨", Toast.LENGTH_SHORT).show()
			return
		}
		val bean = trackInfo.audio
		if (bean.isEmpty()) return
		val dialog = SelectDialog<TrackInfoBean>(requireActivity())
		dialog.setTip("切换音轨")
		dialog.setAdapter(object : SelectDialogInterface<TrackInfoBean> {
			override fun click(value: TrackInfoBean, pos: Int) {
				try {
					for (audio in bean) {
						audio.selected = audio.index == value.index
					}
					mediaPlayer?.let {
						it.pause()
						val progress = it.currentPosition //保存当前进度，ijk 切换轨道 会有快进几秒
						if (it is IjkMediaPlayer) it.setTrack(value.index, progressKey!!)
						if (it is ExoPlayer) it.setTrack(value.groupIndex, value.index, progressKey!!)
						Handler().postDelayed({
							if (it is IjkMediaPlayer) it.seekTo(progress)
							it.start()
						}, 200)
					}
					dialog.dismiss()
				} catch (e: Exception) {
					e("切换音轨出错")
				}
			}

			override fun getDisplay(`val`: TrackInfoBean): String {
				return (`val`.groupIndex + `val`.index).toString() + " . " + `val`.language + " : " + `val`.name
			}
		}, object : DiffUtil.ItemCallback<TrackInfoBean>() {
			override fun areItemsTheSame(oldItem: TrackInfoBean, newItem: TrackInfoBean): Boolean {
				return oldItem.index == newItem.index
			}

			override fun areContentsTheSame(oldItem: TrackInfoBean, newItem: TrackInfoBean): Boolean {
				return oldItem.index == newItem.index
			}
		}, bean, trackInfo.getAudioSelected(false))
		dialog.show()
	}

	fun selectMyInternalSubtitle() {
		val mediaPlayer = player?.mediaPlayer
		if (mediaPlayer !is IjkMediaPlayer) {
			return
		}
		val trackInfo = mediaPlayer.trackInfo
		if (trackInfo == null) {
			Toast.makeText(mContext, "没有内置字幕", Toast.LENGTH_SHORT).show()
			return
		}
		val bean = trackInfo.subtitle
		if (bean.isEmpty()) return
		val dialog = SelectDialog<TrackInfoBean>(requireActivity())
		dialog.setTip("切换内置字幕")
		dialog.setAdapter(object : SelectDialogInterface<TrackInfoBean> {
			override fun click(value: TrackInfoBean, pos: Int) {
				try {
					for (subtitle in bean) {
						subtitle.selected = subtitle.index == value.index
					}
					mediaPlayer.pause()
					val progress = mediaPlayer.currentPosition //保存当前进度，ijk 切换轨道 会有快进几秒
					mController?.mSubtitleView?.destroy()
					mController?.mSubtitleView?.clearSubtitleCache()
					mController?.mSubtitleView?.isInternal = true
					mediaPlayer.setTrack(value.index)
					Handler().postDelayed({
						mediaPlayer.seekTo(progress)
						mediaPlayer.start()
					}, 800)
					dialog.dismiss()
				} catch (e: Exception) {
					e("切换内置字幕出错")
				}
			}

			override fun getDisplay(`val`: TrackInfoBean): String {
				return `val`.index.toString() + " : " + `val`.language
			}
		}, object : DiffUtil.ItemCallback<TrackInfoBean>() {
			override fun areItemsTheSame(oldItem: TrackInfoBean, newItem: TrackInfoBean): Boolean {
				return oldItem.index == newItem.index
			}

			override fun areContentsTheSame(oldItem: TrackInfoBean, newItem: TrackInfoBean): Boolean {
				return oldItem.index == newItem.index
			}
		}, bean, trackInfo.getSubtitleSelected(false))
		dialog.show()
	}

	fun setTip(msg: String?, loading: Boolean, err: Boolean) {
		if (!isAdded) return
		requireActivity().runOnUiThread {
			mPlayLoadTip?.text = msg
			mPlayLoadTip?.visibility = View.VISIBLE
			mPlayLoading?.visibility = if (loading) View.VISIBLE else View.GONE
			mPlayLoadErr?.visibility = if (err) View.VISIBLE else View.GONE
		}
	}

	fun hideTip() {
		mPlayLoadTip?.visibility = View.GONE
		mPlayLoading?.visibility = View.GONE
		mPlayLoadErr?.visibility = View.GONE
	}

	fun errorWithRetry(err: String?, finish: Boolean) {
		if (!autoRetry()) {
			if (!isAdded) return
			requireActivity().runOnUiThread {
				if (finish) {
					setTip(err, loading = false, err = true)
					Toast.makeText(mContext, err, Toast.LENGTH_SHORT).show()
				} else {
					setTip(err, loading = false, err = true)
				}
			}
		}
	}

	fun playUrl(url: String, headers: HashMap<String, String>?) {
		if (!url.startsWith("data:application")) EventBus.getDefault().post(RefreshEvent(RefreshEvent.TYPE_REFRESH, url)) //更新播放地址

		if (!Hawk.get(HawkConfig.M3U8_PURIFY, false)) {
			goPlayUrl(url, headers)
			return
		}
		if (url.startsWith("http://127.0.0.1") || !url.contains(".m3u8")) {
			goPlayUrl(url, headers)
			return
		}
		if (noAd((mVodInfo ?: return).playFlag)) {
			goPlayUrl(url, headers)
			return
		}
		i("echo-playM3u8:$url")
		mController?.playM3u8(url, headers)
	}

	fun goPlayUrl(url: String?, headers: HashMap<String, String>?) {
		i("echo-goPlayUrl:$url")
		if (autoRetryCount == 0) webPlayUrl = url
		if (!isAdded) return
		val finalUrl = url
		requireActivity().runOnUiThread(object : Runnable {
			override fun run() {
				stopParse()
				player?.let { it ->
					it.release()
					if (finalUrl != null) {
						var url: String? = finalUrl
						try {
							val playerType = (mVodPlayerCfg ?: return@let).getInt("pl")
							if (playerType >= 10) {
								val vs = (((mVodInfo ?: return@let).seriesMap ?: return@let)[(mVodInfo ?: return@let).playFlag ?: return@let] ?: return@let)[(mVodInfo ?: return@let).playIndex]
								val playTitle = (mVodInfo ?: return@let).name + " " + vs.name
								setTip("调用外部播放器" + getPlayerName(playerType) + "进行播放", loading = true, err = false)
								var callResult: Boolean
								val progress = getSavedProgress(progressKey)
								callResult = PlayerHelper.runExternalPlayer(playerType, requireActivity(), url, playTitle, playSubtitle, headers, progress)
								setTip("调用外部播放器" + getPlayerName(playerType) + (if (callResult) "成功" else "失败"), callResult, !callResult)
								return
							}
						} catch (e: JSONException) {
							e.printStackTrace()
						}
						hideTip()
						if (url.startsWith("data:application/dash+xml;base64,")) {
							PlayerHelper.updateCfg(it, mVodPlayerCfg ?: return@let, 2)
							App.dashData = url.split("base64,".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
							url = ControlManager.instance.getAddress(true) + "dash/proxy.mpd"
						} else if (url.contains(".mpd") || url.contains("type=mpd")) {
							PlayerHelper.updateCfg(it, mVodPlayerCfg ?: return@let, 2)
						} else {
							PlayerHelper.updateCfg(it, mVodPlayerCfg ?: return@let)
						}
						it.setProgressKey(progressKey)
						if (headers != null) {
							it.setUrl(url, headers)
						} else {
							it.setUrl(url)
						}
						it.start()
						mController?.resetSpeed()
					}
				}
			}
		})
	}

	@UnstableApi
	private fun initSubtitleView() {
		var trackInfo: TrackInfo? = null
		val mediaPlayer = player?.mediaPlayer
		if (mediaPlayer is IjkMediaPlayer) {
			trackInfo = mediaPlayer.trackInfo
			if (trackInfo != null && trackInfo.subtitle.isNotEmpty()) {
				mController?.mSubtitleView?.hasInternal = true
			}
			//默认选中第一个音轨 一般第一个音轨是国语 && 加载上一次选中的
			mediaPlayer.loadDefaultTrack(trackInfo, progressKey)
			mediaPlayer.setOnTimedTextListener(object : IMediaPlayer.OnTimedTextListener {
				override fun onTimedText(mp: IMediaPlayer?, text: IjkTimedText?) {
					if (text == null) return
					if (mController?.mSubtitleView?.isInternal == true) {
						val subtitle = com.github.tvbox.osc.subtitle.model.Subtitle()
						subtitle.content = text.text
						mController?.mSubtitleView?.onSubtitleChanged(subtitle)
					}
				}
			})
		}
		if (mediaPlayer is ExoPlayer) {
			//加载上一次选中的
			mediaPlayer.loadDefaultTrack(progressKey ?: return)
		}
		mController?.mSubtitleView?.bindToMediaPlayer((player ?: return).mediaPlayer ?: return)
		mController?.mSubtitleView?.playSubtitleCacheKey = subtitleCacheKey
		val subtitlePathCache = getCache(string2MD5(subtitleCacheKey)) as String?
		if (!subtitlePathCache.isNullOrEmpty()) {
			mController?.mSubtitleView?.setSubtitlePath(subtitlePathCache)
		} else {
			if (playSubtitle != null && playSubtitle?.isNotEmpty() == true) {
				mController?.mSubtitleView?.setSubtitlePath(playSubtitle ?: return)
			} else {
				if (mController?.mSubtitleView?.hasInternal == true) {
					mController?.mSubtitleView?.isInternal = true
					if (trackInfo != null && trackInfo.subtitle.isNotEmpty()) {
						val subtitleTrackList = trackInfo.subtitle
						val selectedIndex = trackInfo.getSubtitleSelected(true)
						var hasCh = false
						for (subtitleTrackInfoBean in subtitleTrackList) {
							val lowerLang = subtitleTrackInfoBean.language.lowercase(Locale.getDefault())
							if (lowerLang.contains("zh") || lowerLang.contains("ch")) {
								hasCh = true
								if (selectedIndex != subtitleTrackInfoBean.index) {
									(player?.mediaPlayer as? IjkMediaPlayer)?.setTrack(subtitleTrackInfoBean.index)
									break
								}
							}
						}
						if (!hasCh) (player?.mediaPlayer as? IjkMediaPlayer)?.setTrack(subtitleTrackList[0].index)
					}
				}
			}
		}
	}

	private fun initViewModel() {
		sourceViewModel = ViewModelProvider(this)[SourceViewModel::class.java]
		sourceViewModel?.playResult?.observe(this, Observer { info ->
			webPlayUrl = null
			if (info != null) {
				try {
					progressKey = info.optString("proKey", null)
					val parse = info.optString("parse", "1") == "1"
					val jx = info.optString("jx", "0") == "1"
					playSubtitle = info.optString(
						"subt",  /*"https://dash.akamaized.net/akamai/test/caption_test/ElephantsDream/ElephantsDream_en.vtt"*/
						""
					)
					if (playSubtitle?.isEmpty() == true && info.has("subs")) {
						try {
							val obj = info.getJSONArray("subs").optJSONObject(0)
							var url = obj.optString("url", "")
							if (!TextUtils.isEmpty(url) && !hasExtension(url)) {
								val format = obj.optString("format", "")
								val name = obj.optString("name", "字幕")
								var ext = ".srt"
								when (format) {
									"text/x-ssa" -> ext = ".ass"
									"text/vtt" -> ext = ".vtt"
									"application/x-subrip" -> ext = ".srt"
									"text/lrc" -> ext = ".lrc"
								}
								val filename = name + (if (name.lowercase(Locale.getDefault()).endsWith(ext)) "" else ext)
								url += "#" + mController?.encodeUrl(filename)
							}
							playSubtitle = url
						} catch (th: Throwable) {
						}
					}
					subtitleCacheKey = info.optString("subtKey", null)
					val playUrl = info.optString("playUrl", "")
					val msg = info.optString("msg", "")
					if (!msg.isEmpty()) {
						Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show()
					}
					val flag = info.optString("flag")
					var url = info.getString("url")
					if (url.startsWith("[")) {
						url = mController?.firstUrlByArray(url).orEmpty()
					}
					val headers: HashMap<String, String> = HashMap()
					webUserAgent = null
					webHeaderMap = null
					if (info.has("header")) {
						try {
							val hds = JSONObject(info.getString("header"))
							val keys = hds.keys()
							while (keys.hasNext()) {
								val key = keys.next()
								headers[key] = hds.getString(key)
								if (key.equals("user-agent", ignoreCase = true)) {
									webUserAgent = hds.getString(key).trim { it <= ' ' }
								}
							}
							webHeaderMap = headers
						} catch (th: Throwable) {
						}
					}
					if (parse || jx) {
						val userJxList = (playUrl.isEmpty() && ApiConfig.instance.vipParseFlags.contains(flag)) || jx
						initParse(flag, userJxList, playUrl, url)
					} else {
						mController?.showParse(false)
						playUrl(playUrl + url, headers)
					}
				} catch (th: Throwable) {
				}
			} else {
				//                    获取播放信息错误后只需再重试一次
				errorWithRetry("获取播放信息错误", true)
			}
		})
	}

	fun setData(bundle: Bundle) {
		mVodInfo = App.instance.vodInfo
		sourceKey = bundle.getString("sourceKey")
		sourceBean = ApiConfig.instance.getSource(sourceKey)
		initPlayerCfg()
		play(false)
	}

	fun initPlayerCfg() {
		mVodPlayerCfg = try {
			JSONObject((mVodInfo ?: return).playerCfg)
		} catch (th: Throwable) {
			JSONObject()
		}
		try {
			if (mVodPlayerCfg?.has("pl") != true) {
				mVodPlayerCfg?.put("pl", if (sourceBean?.playerType == -1) Hawk.get(HawkConfig.PLAY_TYPE, 1) as Int else sourceBean?.playerType)
			}
			if (mVodPlayerCfg?.has("pr") != true) {
				mVodPlayerCfg?.put("pr", Hawk.get(HawkConfig.PLAY_RENDER, 0))
			}
			if (mVodPlayerCfg?.has("ijk") != true) {
				mVodPlayerCfg?.put("ijk", Hawk.get(HawkConfig.IJK_CODEC, "硬解码"))
			}
			if (mVodPlayerCfg?.has("sc") != true) {
				mVodPlayerCfg?.put("sc", Hawk.get(HawkConfig.PLAY_SCALE, 0))
			}
			if (mVodPlayerCfg?.has("sp") != true) {
				mVodPlayerCfg?.put("sp", 1.0)
			}
			if (mVodPlayerCfg?.has("st") != true) {
				mVodPlayerCfg?.put("st", 0)
			}
			if (mVodPlayerCfg?.has("et") != true) {
				mVodPlayerCfg?.put("et", 0)
			}
		} catch (th: Throwable) {
		}
		mController?.setPlayerConfig(mVodPlayerCfg)
	}

	fun onBackPressed(): Boolean {
		val requestedOrientation = requireActivity().requestedOrientation
		if (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT || requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT || requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT) {
			requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
			mController?.mLandscapePortraitBtn?.text = "竖屏"
		}
		return mController!!.onBackPressed()
	}

	fun dispatchKeyEvent(event: KeyEvent?): Boolean {
		if (event != null) {
			return mController!!.onKeyEvent(event)
		}
		return false
	}

	fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
		if (event != null) {
			return mController!!.onKeyDown(keyCode, event)
		}
		return false
	}

	fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
		if (event != null) {
			return mController!!.onKeyUp(keyCode, event)
		}
		return false
	}

	override fun onPause() {
		super.onPause()
		if (this.player != null) {
			player?.pause()
		}
	}

	override fun onResume() {
		super.onResume()
		if (this.player != null) {
			player?.resume()
		}
	}

	override fun onHiddenChanged(hidden: Boolean) {
		if (hidden) {
			if (this.player != null) {
				player?.pause()
			}
		} else {
			if (this.player != null) {
				player?.resume()
			}
		}
		super.onHiddenChanged(hidden)
	}

	override fun onDestroyView() {
		super.onDestroyView()
		EventBus.getDefault().unregister(this)
		if (this.player != null) {
			player?.release()
			this.player = null
		}
		stopLoadWebView(true)
		stopParse()
		mController?.stopOther()
	}

	private fun playNext(isProgress: Boolean) {
		val hasNext: Boolean = if (mVodInfo == null || (mVodInfo?.seriesMap ?: return)[(mVodInfo ?: return).playFlag ?: return] == null) {
			false
		} else {
			(mVodInfo ?: return).playIndex + 1 < (((mVodInfo ?: return).seriesMap ?: return)[(mVodInfo ?: return).playFlag ?: return] ?: return).size
		}
		if (!hasNext) {
			Toast.makeText(requireContext(), "已经是最后一集了!", Toast.LENGTH_SHORT).show()
			return
		} else {
			mVodInfo?.playIndex++
		}
		play(false)
	}

	private fun playPrevious() {
		val hasPre: Boolean = if (mVodInfo == null || (mVodInfo?.seriesMap ?: return)[(mVodInfo ?: return).playFlag ?: return] == null) {
			false
		} else {
			(mVodInfo ?: return).playIndex - 1 >= 0
		}
		if (!hasPre) {
			Toast.makeText(requireContext(), "已经是第一集了!", Toast.LENGTH_SHORT).show()
			return
		}
		mVodInfo?.playIndex--
		play(false)
	}

	fun autoRetry(): Boolean {
		val currentTime = System.currentTimeMillis()
		if (currentTime - lastRetryTime > 60000) {
			i("echo-reset-autoRetryCount")
			autoRetryCount = 0
			allowSwitchPlayer = false
		}

		lastRetryTime = currentTime // 更新上次调用时间
		if (loadFoundVideoUrls != null && loadFoundVideoUrls?.isNotEmpty() == true) {
			autoRetryFromLoadFoundVideoUrls()
			return true
		}
		if (autoRetryCount < 2) {
			if (autoRetryCount == 1) {
				//第二次重试时重新调用接口
				play(false)
				autoRetryCount++
			} else {
				//第一次重试直接带着原地址继续播放
				if (webPlayUrl != null) {
					if (allowSwitchPlayer) {
						//切换播放器不占用重试次数
						if (mController?.switchPlayer() == true) autoRetryCount++
					} else {
						autoRetryCount++
						allowSwitchPlayer = true
					}
					stopParse()
					initParseLoadFound()
					if (this.player != null) player?.release()
					webHeaderMap?.let { playUrl(webPlayUrl ?: return@let, it) }
				} else {
					play(false)
					autoRetryCount++
				}
			}
			return true
		} else {
			autoRetryCount = 0
			return false
		}
	}

	fun autoRetryFromLoadFoundVideoUrls() {
		val videoUrl = loadFoundVideoUrls?.poll()
		val header = loadFoundVideoUrlsHeader[videoUrl]
		header?.let { playUrl(videoUrl ?: return@let, it) }
	}

	fun initParseLoadFound() {
		loadFoundCount.set(0)
		loadFoundVideoUrls = LinkedList<String?>()
		loadFoundVideoUrlsHeader = HashMap()
	}

	fun setPlayTitle(show: Boolean) {
		if (show) {
			var playTitleInfo = ""
			if (mVodInfo != null) {
				playTitleInfo = mVodInfo?.name + " " + ((mVodInfo?.seriesMap ?: return)[(mVodInfo ?: return).playFlag ?: return] ?: return)[(mVodInfo ?: return).playIndex].name
			}
			mController?.setTitle(playTitleInfo)
		} else {
			mController?.setTitle("")
		}
	}

	fun play(reset: Boolean) {
		if (mVodInfo == null) return
		val vs = ((mVodInfo?.seriesMap ?: return)[(mVodInfo ?: return).playFlag ?: return] ?: return)[(mVodInfo ?: return).playIndex]
		EventBus.getDefault().post(RefreshEvent(RefreshEvent.TYPE_REFRESH, (mVodInfo ?: return).playIndex))
		setTip("正在获取播放信息", loading = true, err = false)
		val playTitleInfo = mVodInfo?.name + " " + vs.name
		mController?.setTitle(playTitleInfo)

		stopParse()
		initParseLoadFound()
		allowSwitchPlayer = true
		mController?.stopOther()
		if (this.player != null) player?.release()
		subtitleCacheKey = mVodInfo?.sourceKey + "-" + mVodInfo?.id + "-" + mVodInfo?.playFlag + "-" + mVodInfo?.playIndex + "-" + vs.name + "-subt"
		progressKey = mVodInfo?.sourceKey + mVodInfo?.id + mVodInfo?.playFlag + mVodInfo?.playIndex + vs.name
		//重新播放清除现有进度
		if (reset) {
			delete<Int?>(string2MD5(progressKey), 0)
			delete<Int?>(string2MD5(subtitleCacheKey), 0)
		} else {
			try {
				val playerType = mVodPlayerCfg?.getInt("pl")
				if (playerType == 1) {
					mController?.mSubtitleView?.visibility = View.VISIBLE
				} else {
					mController?.mSubtitleView?.visibility = View.GONE
				}
			} catch (e: JSONException) {
				e.printStackTrace()
			}
		}

		if (JianPian.isJpUrl(vs.url)) { //荐片地址特殊判断
			val jpUrl = vs.url
			mController?.showParse(false)
			if (vs.url.startsWith("tvbox-xg:")) {
				playUrl(JianPian.jpUrlDec(jpUrl.substring(9)), null)
			} else {
				playUrl(JianPian.jpUrlDec(jpUrl), null)
			}
			return
		}
		if (Thunder.play(vs.url, object : ThunderCallback {
				override fun status(code: Int, info: String) {
					if (code < 0) {
						setTip(info, loading = false, err = true)
					} else {
						setTip(info, loading = true, err = false)
					}
				}

				override fun list(urlMap: Map<Int, String>) {
				}

				override fun play(url: String) {
					playUrl(url, null)
				}
			})) {
			mController?.showParse(false)
			return
		}
		sourceViewModel?.getPlay(sourceKey, (mVodInfo ?: return).playFlag, progressKey, vs.url, subtitleCacheKey)
	}

	private fun initParse(flag: String?, useParse: Boolean, playUrl: String, url: String) {
		parseFlag = flag
		webUrl = url
		var parseBean: ParseBean? = null
		mController?.showParse(useParse)
		if (useParse) {
			parseBean = ApiConfig.instance.defaultParse
		} else {
			if (playUrl.startsWith("json:")) {
				parseBean = ParseBean()
				parseBean.type = 1
				parseBean.url = playUrl.substring(5)
			} else if (playUrl.startsWith("parse:")) {
				val parseRedirect = playUrl.substring(6)
				for (pb in ApiConfig.instance.parseBeanList) {
					if (pb.name == parseRedirect) {
						parseBean = pb
						break
					}
				}
			}
			if (parseBean == null) {
				parseBean = ParseBean()
				parseBean.type = 0
				parseBean.url = playUrl
			}
		}
		doParse(parseBean ?: return)
	}

	@Throws(JSONException::class)
	fun jsonParse(input: String?, json: String): JSONObject? {
		val jsonPlayData = JSONObject(json)
		//小窗版解析方法改到这了  之前那个位置data解析无效
		var url: String?
		url = if (jsonPlayData.has("data")) {
			jsonPlayData.getJSONObject("data").getString("url")
		} else {
			jsonPlayData.getString("url")
		}
		if (url.startsWith("//")) {
			url = "http:$url"
		}
		if (!url.startsWith("http")) {
			return null
		}
		val headers = JSONObject()
		val ua = jsonPlayData.optString("user-agent", "")
		if (ua.trim { it <= ' ' }.isNotEmpty()) {
			headers.put("User-Agent", " $ua")
		}
		val referer = jsonPlayData.optString("referer", "")
		if (referer.trim { it <= ' ' }.isNotEmpty()) {
			headers.put("Referer", " $referer")
		}
		val taskResult = JSONObject()
		taskResult.put("header", headers)
		taskResult.put("url", url)
		return taskResult
	}

	fun stopParse() {
		mHandler?.removeMessages(100)
		stopLoadWebView(false)
		OkGo.getInstance().cancelTag("json_jx")
		if (parseThreadPool != null) {
			try {
				parseThreadPool?.shutdown()
				parseThreadPool = null
			} catch (th: Throwable) {
				th.printStackTrace()
			}
		}
	}

	private fun doParse(pb: ParseBean) {
		stopParse()
		initParseLoadFound()
		when (pb.type) {
			4 -> {
				parseMix(pb, true)
			}

			0 -> {
				setTip("正在嗅探播放地址", loading = true, err = false)
				mHandler?.removeMessages(100)
				mHandler?.sendEmptyMessageDelayed(100, (20 * 1000).toLong())
				try {
					val reqHeaders = HashMap<String, String>()
					val jsonObject = JSONObject(pb.ext)
					if (jsonObject.has("header")) {
						val headerJson = jsonObject.optJSONObject("header")
						val keys = (headerJson ?: return).keys()
						while (keys.hasNext()) {
							val key = keys.next()
							if (key.equals("user-agent", ignoreCase = true)) {
								webUserAgent = headerJson.getString(key).trim { it <= ' ' }
							} else {
								reqHeaders[key] = headerJson.optString(key, "")
							}
						}
						if (reqHeaders.isNotEmpty()) webHeaderMap = reqHeaders
					}
				} catch (e: Throwable) {
					e.printStackTrace()
				}
				loadWebView(pb.url + webUrl)
			}

			1 -> { // json 解析
				setTip("正在解析播放地址", loading = true, err = false)
				// 解析ext
				val reqHeaders = HttpHeaders()
				try {
					val jsonObject = JSONObject(pb.ext)
					if (jsonObject.has("header")) {
						val headerJson = jsonObject.optJSONObject("header")
						val keys = (headerJson ?: return).keys()
						while (keys.hasNext()) {
							val key: String? = keys.next()
							reqHeaders.put(key, headerJson.optString(key, ""))
						}
					}
				} catch (e: Throwable) {
					e.printStackTrace()
				}
				OkGo.get<String?>(pb.url + (mController ?: return).encodeUrl(webUrl))
					.tag("json_jx")
					.headers(reqHeaders)
					.execute(object : AbsCallback<String>() {
						@Throws(Throwable::class)
						override fun convertResponse(response: Response): String {
							return response.body.string()
						}

						override fun onSuccess(response: com.lzy.okgo.model.Response<String>) {
							val json = response.body()
							try {
								val rs = jsonParse(webUrl, json)
								var headers: HashMap<String, String>? = null
								if ((rs ?: return).has("header")) {
									try {
										val hds = rs.getJSONObject("header")
										val keys = hds.keys()
										while (keys.hasNext()) {
											val key = keys.next()
											if (headers == null) {
												headers = HashMap()
											}
											headers[key] = hds.getString(key)
										}
									} catch (th: Throwable) {
									}
								}
								playUrl(rs.getString("url"), headers)
							} catch (e: Throwable) {
								e.printStackTrace()
								errorWithRetry("解析错误", false)
								//                                setTip("解析错误", false, true);
							}
						}

						override fun onError(response: com.lzy.okgo.model.Response<String?>?) {
							super.onError(response)
							errorWithRetry("解析错误", false)
							//                            setTip("解析错误", false, true);
						}
					})
			}

			2 -> { // json 扩展
				setTip("正在解析播放地址", loading = true, err = false)
				parseThreadPool = Executors.newSingleThreadExecutor()
				val jxs = LinkedHashMap<String, String>()
				for (p in ApiConfig.instance.parseBeanList) {
					if (p.type == 1) {
						jxs[p.name] = p.mixUrl()
					}
				}
				parseThreadPool?.execute(object : Runnable {
					override fun run() {
						val rs: JSONObject? = ApiConfig.instance.jsonExt(pb.url, jxs, webUrl)
						if (rs == null || !rs.has("url") || rs.optString("url").isEmpty()) {
							//                        errorWithRetry("解析错误", false);
							setTip("解析错误", loading = false, err = true)
						} else {
							var headers: HashMap<String, String>? = null
							if (rs.has("header")) {
								try {
									val hds = rs.getJSONObject("header")
									val keys = hds.keys()
									while (keys.hasNext()) {
										val key = keys.next()
										if (headers == null) {
											headers = HashMap()
										}
										headers[key] = hds.getString(key)
									}
								} catch (th: Throwable) {
								}
							}
							if (rs.has("jxFrom")) {
								if (!isAdded) return
								requireActivity().runOnUiThread { Toast.makeText(mContext, "解析来自:" + rs.optString("jxFrom"), Toast.LENGTH_SHORT).show() }
							}
							val parseWV = rs.optInt("parse", 0) == 1
							if (parseWV) {
								val wvUrl = checkReplaceProxy(rs.optString("url", ""))
								loadUrl(wvUrl)
							} else {
								playUrl(rs.optString("url", ""), headers)
							}
						}
					}
				})
			}

			3 -> { // json 聚合
				parseMix(pb, false)
			}
		}
	}

	private fun parseMix(pb: ParseBean, isSuper: Boolean) {
		setTip("正在解析播放地址", loading = true, err = false)
		parseThreadPool = Executors.newSingleThreadExecutor()
		val jxs = LinkedHashMap<String, HashMap<String, String>>()
		val jsonJxs = LinkedHashMap<String?, String?>()
		var extendName: String? = ""
		for (p in ApiConfig.instance.parseBeanList) {
			val data = HashMap<String, String>()
			data["url"] = p.url
			if (p.url == pb.url) {
				extendName = p.name
			}
			data["type"] = p.type.toString() + ""
			data["ext"] = p.ext
			jxs[p.name] = data

			if (p.type == 1) {
				jsonJxs[p.name] = p.mixUrl()
			}
		}
		val finalExtendName = extendName
		parseThreadPool?.execute(object : Runnable {
			override fun run() {
				if (isSuper) {
					//并发执行 嗅探和json
					val rs = SuperParse.parse(jxs, parseFlag + "123", webUrl ?: return)
					if (!rs.has("url") || rs.optString("url").isEmpty()) {
						setTip("解析错误", loading = false, err = true)
					} else {
						if (rs.has("parse") && rs.optInt("parse", 0) == 1) {
							if (rs.has("ua")) {
								webUserAgent = rs.optString("ua").trim { it <= ' ' }
							}
							setTip("超级解析中", loading = true, err = false)

							if (!isAdded) return
							requireActivity().runOnUiThread {
								val mixParseUrl = checkReplaceProxy(rs.optString("url", ""))
								stopParse()
								mHandler?.removeMessages(100)
								mHandler?.sendEmptyMessageDelayed(100, (20 * 1000).toLong())
								loadWebView(mixParseUrl)
							}
							parseThreadPool?.execute {
								val res = SuperParse.doJsonJx(webUrl ?: return@execute)
								rsJsonJX(res, true)
							}
						} else {
							rsJsonJX(rs, false)
						}
					}
				} else {
					val rs: JSONObject? = ApiConfig.instance.jsonExtMix(parseFlag + "111", pb.url, finalExtendName, jxs, webUrl)
					if (rs == null || !rs.has("url") || rs.optString("url").isEmpty()) {
//                        errorWithRetry("解析错误", false);
						setTip("解析错误", loading = false, err = true)
					} else {
						if (rs.has("parse") && rs.optInt("parse", 0) == 1) {
							if (rs.has("ua")) {
								webUserAgent = rs.optString("ua").trim { it <= ' ' }
							}
							if (!isAdded) return
							requireActivity().runOnUiThread {
								val mixParseUrl = checkReplaceProxy(rs.optString("url", ""))
								stopParse()
								setTip("正在嗅探播放地址", loading = true, err = false)
								mHandler?.removeMessages(100)
								(mHandler ?: return@runOnUiThread).sendEmptyMessageDelayed(100, (20 * 1000).toLong())
								loadWebView(mixParseUrl)
							}
						} else {
							rsJsonJX(rs, false)
						}
					}
				}
			}
		})
	}

	private fun rsJsonJX(rs: JSONObject?, isSuper: Boolean) {
		if (isSuper) {
			if (rs == null || !rs.has("url")) return
			stopLoadWebView(false)
		}
		var headers: HashMap<String, String>? = null
		if ((rs ?: return).has("header")) {
			try {
				val hds = rs.getJSONObject("header")
				val keys = hds.keys()
				while (keys.hasNext()) {
					val key = keys.next()
					if (headers == null) {
						headers = HashMap()
					}
					headers[key] = hds.getString(key)
				}
			} catch (th: Throwable) {
				th.printStackTrace()
			}
		}
		if (rs.has("jxFrom")) {
			if (!isAdded) return
			requireActivity().runOnUiThread { Toast.makeText(mContext, "解析来自:" + rs.optString("jxFrom"), Toast.LENGTH_SHORT).show() }
		}
		playUrl(rs.optString("url", ""), headers)
	}

	fun loadWebView(url: String) {
		if (mSysWebView == null) {
			initWebView()
			loadUrl(url)
		} else {
			loadUrl(url)
		}
	}

	fun initWebView() {
		mSysWebView = MyWebView(mContext)
		configWebViewSys(mSysWebView)
	}

	fun loadUrl(url: String) {
		if (!isAdded) return
		requireActivity().runOnUiThread {
			if (mSysWebView != null) {
				mSysWebView?.stopLoading()
				if (webUserAgent != null) {
					mSysWebView?.settings?.setUserAgentString(webUserAgent)
				}
				//mSysWebView.clearCache(true);
				if (webHeaderMap != null) {
					mSysWebView?.loadUrl(url, webHeaderMap ?: return@runOnUiThread)
				} else {
					mSysWebView?.loadUrl(url)
				}
			}
		}
	}

	fun stopLoadWebView(destroy: Boolean) {
		if (!isAdded) return
		requireActivity().runOnUiThread {
			if (mSysWebView != null) {
				mSysWebView?.stopLoading()
				mSysWebView?.loadUrl("about:blank")
				if (destroy) {
					mSysWebView?.clearCache(true)
					mSysWebView?.removeAllViews()
					mSysWebView?.destroy()
					mSysWebView = null
				}
			}
		}
	}

	fun checkVideoFormat(url: String): Boolean {
		try {
			if (url.contains("url=http") || url.contains(".html")) {
				return false
			}
			sourceBean?.let {
				if (it.type == 3) {
					val sp: Spider? = ApiConfig.instance.getCSP(it)
					if (sp != null && sp.manualVideoCheck()) {
						return sp.isVideoFormat(url)
					}
				}
			}
			return checkIsVideoForParse(webUrl, url)
		} catch (e: Exception) {
			return false
		}
	}

	@SuppressLint("SetJavaScriptEnabled")
	private fun configWebViewSys(webView: WebView?) {
		if (webView == null) {
			return
		}
		val layoutParams = if (Hawk.get(HawkConfig.DEBUG_OPEN, false))
			ViewGroup.LayoutParams(800, 400)
		else
			ViewGroup.LayoutParams(1, 1)
		webView.setFocusable(false)
		webView.isFocusableInTouchMode = false
		webView.clearFocus()
		webView.overScrollMode = View.OVER_SCROLL_ALWAYS
		if (!isAdded) return
		requireActivity().addContentView(webView, layoutParams)
		/* 添加webView配置 */
		val settings = webView.settings
		settings.setNeedInitialFocus(false)
		settings.allowContentAccess = true
		settings.allowFileAccess = true
		settings.allowUniversalAccessFromFileURLs = true
		settings.allowFileAccessFromFileURLs = true
		settings.databaseEnabled = true
		settings.domStorageEnabled = true
		settings.javaScriptEnabled = true

		settings.mediaPlaybackRequiresUserGesture = false
		settings.blockNetworkImage = !Hawk.get(HawkConfig.DEBUG_OPEN, false)
		settings.useWideViewPort = true
		settings.domStorageEnabled = true
		settings.javaScriptCanOpenWindowsAutomatically = true
		settings.setSupportMultipleWindows(false)
		settings.loadWithOverviewMode = true
		settings.builtInZoomControls = true
		settings.setSupportZoom(false)
		settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
		//        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
		settings.cacheMode = WebSettings.LOAD_DEFAULT
		/* 添加webView配置 */
		//设置编码
		settings.defaultTextEncodingName = "utf-8"
		settings.setUserAgentString(webView.settings.userAgentString)

		//         settings.setUserAgentString(ANDROID_UA);
		webView.webChromeClient = object : WebChromeClient() {
			override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
				return false
			}

			override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
				return true
			}

			override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
				return true
			}

			override fun onJsPrompt(view: WebView?, url: String?, message: String?, defaultValue: String?, result: JsPromptResult?): Boolean {
				return true
			}
		}
		val mSysWebClient = SysWebClient()
		webView.webViewClient = mSysWebClient
		webView.setBackgroundColor(Color.BLACK)
	}

	internal inner class MyWebView(context: Context) : WebView(context) {
		override fun setOverScrollMode(mode: Int) {
			super.setOverScrollMode(mode)
			if (mContext is Activity) AutoSize.autoConvertDensityOfCustomAdapt(mContext as Activity, this@PlayFragment)
		}

		override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
			return false
		}
	}

	private inner class SysWebClient : WebViewClient() {
		@SuppressLint("WebViewClientOnReceivedSslError")
		override fun onReceivedSslError(webView: WebView?, sslErrorHandler: SslErrorHandler, sslError: SslError?) {
			sslErrorHandler.proceed()
		}

		override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
			return false
		}

		@Deprecated("Deprecated in Java")
		override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
			return false
		}

		override fun onPageFinished(view: WebView?, url: String) {
			super.onPageFinished(view, url)
			i("echo-onPageFinished url:$url")
			if (url != "about:blank") {
				mController?.evaluateScript(sourceBean ?: return, url, view)
			}
		}

		fun checkIsVideo(url: String, headers: HashMap<String, String>): WebResourceResponse? {
			var url = url
			if (url.endsWith("/favicon.ico")) {
				if (url.startsWith("http://127.0.0.1")) {
					return WebResourceResponse("image/x-icon", "UTF-8", null)
				}
				return null
			}

			val isFilter = isFilter(webUrl, url)
			if (isFilter) {
				i("shouldInterceptLoadRequest filter:$url")
				return null
			}

			val ad: Boolean
			if (!loadedUrls.containsKey(url)) {
				ad = isAd(url)
				loadedUrls[url] = ad
			} else {
				ad = loadedUrls[url] == true
			}

			if (!ad) {
				if (checkVideoFormat(url)) {
					loadFoundVideoUrls?.add(url)
					loadFoundVideoUrlsHeader[url] = headers
					i("echo-loadFoundVideoUrl:$url")
					if (loadFoundCount.incrementAndGet() == 1) {
						stopLoadWebView(false)
						stopJsonJx()
						url = ((loadFoundVideoUrls ?: return null).poll() ?: return null)
						(mHandler ?: return null).removeMessages(100)
						val cookie = CookieManager.getInstance().getCookie(url)
						if (!TextUtils.isEmpty(cookie)) headers["Cookie"] = " $cookie" //携带cookie

						playUrl(url, headers)
					}
				}
			}

			return if (ad || loadFoundCount.get() > 0) createEmptyResource() else null
		}

		@Deprecated("Deprecated in Java")
		override fun shouldInterceptRequest(view: WebView?, url: String?): WebResourceResponse? {
//            WebResourceResponse response = checkIsVideo(url, new HashMap<>());
			return null
		}

		override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest): WebResourceResponse? {
			val url = request.url.toString()
			i("echo-shouldInterceptRequest url:$url")
			val webHeaders = HashMap<String, String>()
			val hds = request.requestHeaders
			if (hds != null && hds.isNotEmpty()) {
				for (k in hds.keys) {
					if (k.equals("user-agent", ignoreCase = true)
						|| k.equals("referer", ignoreCase = true)
						|| k.equals("origin", ignoreCase = true)
					) {
						webHeaders[k] = " " + hds[k]
					}
				}
			}
			return checkIsVideo(url, webHeaders)
		}
	}
}
