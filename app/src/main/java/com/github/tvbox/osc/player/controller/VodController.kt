package com.github.tvbox.osc.player.controller

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.webkit.WebView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.DiffUtil
import com.github.tvbox.osc.R
import com.github.tvbox.osc.api.ApiConfig
import com.github.tvbox.osc.base.App
import com.github.tvbox.osc.bean.IJKCode
import com.github.tvbox.osc.bean.ParseBean
import com.github.tvbox.osc.bean.SourceBean
import com.github.tvbox.osc.data.ConfigKey
import com.github.tvbox.osc.data.PreferenceStore
import com.github.tvbox.osc.server.ControlManager
import com.github.tvbox.osc.server.RemoteServer
import com.github.tvbox.osc.subtitle.widget.SimpleSubtitleView
import com.github.tvbox.osc.ui.adapter.ParseAdapter
import com.github.tvbox.osc.ui.adapter.SelectDialogAdapter.SelectDialogInterface
import com.github.tvbox.osc.ui.dialog.SelectDialog
import com.github.tvbox.osc.util.FastClickCheckUtil
import com.github.tvbox.osc.util.M3U8
import com.github.tvbox.osc.util.PlayerHelper
import com.github.tvbox.osc.util.ScreenUtils
import com.github.tvbox.osc.util.SubtitleHelper
import com.github.tvbox.osc.util.TVBoxRuntimeLog
import com.github.tvbox.osc.util.VideoParseRuler
import com.github.tvbox.osc.util.thunder.JianPian.finish
import com.github.tvbox.osc.util.thunder.Thunder.stop
import com.lzy.okgo.OkGo
import com.lzy.okgo.callback.AbsCallback
import com.lzy.okgo.model.HttpHeaders
import com.lzy.okgo.model.Response
import com.owen.tvrecyclerview.widget.TvRecyclerView
import com.owen.tvrecyclerview.widget.V7LinearLayoutManager
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import xyz.doikki.videoplayer.player.VideoView
import xyz.doikki.videoplayer.util.PlayerUtils
import java.net.MalformedURLException
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VodController(context: Context) : BaseController(context) {
	private val lockRunnable: LockRunnable = LockRunnable()
	val myHandleSeconds: Int = 10000 //闲置多少毫秒秒关闭底栏  默认6秒
	private val mmHandler = Handler(Looper.getMainLooper())
	var mPlayerSpeedBtn: TextView? = null
	var mPlayerTimeStartEndText: TextView? = null
	var mPlayerTimeStartBtn: TextView? = null
	var mPlayerTimeSkipBtn: TextView? = null
	var mPlayerTimeResetBtn: TextView? = null
	var mSubtitleView: SimpleSubtitleView? = null
	var mLandscapePortraitBtn: TextView? = null
	var mFullscreenBtn: ImageView? = null
	var mSeekBar: SeekBar? = null
	var mCurrentTime: TextView? = null
	var mTotalTime: TextView? = null
	var mIsDragging: Boolean = false
	var mProgressRoot: LinearLayout? = null
	var mProgressText: TextView? = null
	var mProgressIcon: ImageView? = null
	var mLockView: ImageView? = null
	var mBottomRoot: LinearLayout? = null
	var mPlayBtnGroup: LinearLayout? = null
	var mTopContainer: ConstraintLayout? = null
	var mTopRoot1: LinearLayout? = null
	var mTopRoot2: LinearLayout? = null
	var mParseRoot: LinearLayout? = null
	var mGridParseView: TvRecyclerView? = null
	var mPlayTitle1: TextView? = null
	var mPlayLoadNetSpeedRightTop: TextView? = null
	var mNextBtn: TextView? = null
	var mPreBtn: TextView? = null
	var mPlayerScaleBtn: TextView? = null
	var mPlayerBtn: TextView? = null
	var mPlayerIJKBtn: TextView? = null
	var mPlayerRetry: TextView? = null
	var mPlayRefresh: TextView? = null
	var mPlayPauseTime: TextView? = null
	var mPlayLoadNetSpeed: TextView? = null
	var mVideoSize: TextView? = null
	var mZiMuBtn: TextView? = null
	var mAudioTrackBtn: TextView? = null
	var seekTime: TextView? = null //右上角进度时间显示
	var mScreenDisplay: TextView? = null //增加屏显开关
	var tvScreenDisplay: LinearLayout? = null //增加屏显布局
	var netPlaySpeed: TextView? = null
	private val myRunnable2: Runnable = object : Runnable {
		override fun run() {
			val date = Date()
			val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
			mPlayPauseTime?.text = timeFormat.format(date)
			val mSpeed = mControlWrapper.tcpSpeed
			val speed = PlayerHelper.getDisplaySpeed(mSpeed, false)
			val speedBps = PlayerHelper.getDisplaySpeedBps(mSpeed, true)
			mPlayLoadNetSpeedRightTop?.text = speedBps
			mPlayLoadNetSpeed?.text = speed
			netPlaySpeed?.text = speedBps
			val mVideoSizes = mControlWrapper.videoSize
			val width = mVideoSizes[0].toString()
			val height = mVideoSizes[1].toString()
			mVideoSize?.text = "[ $width X $height ]"

			mHandler?.postDelayed(this, 1000)
		}
	}
	var myHandle: Handler? = null
	var myRunnable: Runnable? = null
	var videoPlayState: Int = 0
	private var backBtn: View? = null //返回键
	private var isClickBackBtn = false
	private var isLock = false
	private var mPlayerConfig: JSONObject? = null
	private var listener: VodControlListener? = null
	private var skipEnd = true
	private var simSlideStart = false
	private var simSeekPosition = 0
	private var simSlideOffset: Long = 0
	private var lastSlideTime: Long = 0
	private var fromLongPress = false
	private var speedOld = 1.0f
	private var mLongPressRunnable: Runnable? = null
	private var isLongPressTriggered = false

	init {
		mHandlerCallback = HandlerCallback { msg: Message? ->
			when (msg?.what) {
				1000 -> {
					// seek 刷新
					mProgressRoot?.visibility = VISIBLE
				}

				1001 -> {
					// seek 关闭
					mProgressRoot?.visibility = GONE
				}

				1002 -> {
					// 显示底部菜单
					mBottomRoot?.visibility = VISIBLE
					mTopContainer?.visibility = VISIBLE
					mTopRoot1?.visibility = VISIBLE
					mTopRoot2?.visibility = VISIBLE
					mPlayLoadNetSpeedRightTop?.visibility = VISIBLE
					if (PreferenceStore.get(ConfigKey.SCREEN_DISPLAY, GONE) == GONE) {
						mPlayPauseTime?.visibility = VISIBLE
					} else {
						netPlaySpeed?.visibility = GONE
					}
					backBtn?.visibility = if (ScreenUtils.isTv(context)) INVISIBLE else VISIBLE
					showLockView()
				}

				1003 -> {
					// 隐藏底部菜单
					mBottomRoot?.visibility = GONE
					mTopContainer?.visibility = GONE
					mTopRoot1?.visibility = GONE
					mTopRoot2?.visibility = GONE
					mPlayLoadNetSpeedRightTop?.visibility = GONE
					if (PreferenceStore.get(ConfigKey.SCREEN_DISPLAY, GONE) == GONE) {
						mPlayPauseTime?.visibility = GONE
					} else {
						netPlaySpeed?.visibility = VISIBLE
					}
					backBtn?.visibility = INVISIBLE
				}

				1004 -> {
					// 设置速度
					if (isInPlaybackState) {
						try {
							val speed = mPlayerConfig?.getDouble("sp")?.toFloat() ?: 1.0f
							mControlWrapper.speed = speed
						} catch (e: JSONException) {
							e.printStackTrace()
						}
					} else mHandler?.sendEmptyMessageDelayed(1004, 100)
				}
			}
		}
	}

	private fun showLockView() {
		mLockView?.visibility = if (ScreenUtils.isTv(context)) INVISIBLE else VISIBLE
		mHandler?.removeCallbacks(lockRunnable)
		mHandler?.postDelayed(lockRunnable, 3000)
	}

	override fun initView() {
		super.initView()
		mCurrentTime = findViewById(R.id.curr_time)
		mTotalTime = findViewById(R.id.total_time)
		mPlayTitle1 = findViewById(R.id.tv_info_name1)
		mPlayLoadNetSpeedRightTop = findViewById(R.id.tv_play_load_net_speed_right_top)
		mSeekBar = findViewById(R.id.seekBar)
		mProgressRoot = findViewById(R.id.tv_progress_container)
		mProgressIcon = findViewById(R.id.tv_progress_icon)
		mProgressText = findViewById(R.id.tv_progress_text)
		mBottomRoot = findViewById(R.id.bottom_container)
		mTopContainer = findViewById(R.id.tv_top_container)
		mTopRoot1 = findViewById(R.id.tv_top_l_container)
		mTopRoot2 = findViewById(R.id.tv_top_r_container)
		mPlayBtnGroup = findViewById(R.id.play_btn_group)
		tvScreenDisplay = findViewById(R.id.tv_screen_display)
		netPlaySpeed = findViewById(R.id.net_play_speed)
		mParseRoot = findViewById(R.id.parse_root)
		mGridParseView = findViewById(R.id.mGridParseView)
		mPlayerRetry = findViewById(R.id.play_retry)
		mPlayRefresh = findViewById(R.id.play_refresh)
		mNextBtn = findViewById(R.id.play_next)
		mPreBtn = findViewById(R.id.play_pre)
		mPlayerScaleBtn = findViewById(R.id.play_scale)
		mPlayerSpeedBtn = findViewById(R.id.play_speed)
		mPlayerBtn = findViewById(R.id.play_player)
		mPlayerIJKBtn = findViewById(R.id.play_ijk)
		mPlayerTimeStartEndText = findViewById(R.id.play_time_start_end_text)
		mPlayerTimeStartBtn = findViewById(R.id.play_time_start)
		mPlayerTimeSkipBtn = findViewById(R.id.play_time_end)
		mPlayerTimeResetBtn = findViewById(R.id.play_time_reset)
		mPlayPauseTime = findViewById(R.id.tv_sys_time)
		mPlayLoadNetSpeed = findViewById(R.id.tv_play_load_net_speed)
		mVideoSize = findViewById(R.id.tv_videosize)
		mSubtitleView = findViewById(R.id.subtitle_view)
		mZiMuBtn = findViewById(R.id.zimu_select)
		mAudioTrackBtn = findViewById(R.id.audio_track_select)
		mLandscapePortraitBtn = findViewById(R.id.landscape_portrait)
		mFullscreenBtn = findViewById(R.id.tv_fullscreen)
		backBtn = findViewById(R.id.tv_back)
		seekTime = findViewById(R.id.tv_seek_time)
		mScreenDisplay = findViewById(R.id.screen_display)
		backBtn?.setOnClickListener {
			if (context is Activity) {
				isClickBackBtn = true
				(context as Activity).onBackPressed()
			}
		}
		mLockView = findViewById(R.id.tv_lock)
		mLockView?.setOnClickListener {
			isLock = !isLock
			mLockView?.setImageResource(if (isLock) R.drawable.icon_lock else R.drawable.icon_unlock)
			if (isLock) {
				val obtain = Message.obtain()
				obtain.what = 1003 //隐藏底部菜单
				mHandler?.sendMessage(obtain)
			}
			showLockView()
		}
		val rootView = findViewById<View>(R.id.rootView)
		rootView.setOnTouchListener { _, event ->
			if (isLock) {
				if (event?.action == MotionEvent.ACTION_UP) {
					showLockView()
				}
			}
			isLock
		}

		initSubtitleInfo()

		myHandle = Handler(Looper.getMainLooper())
		myRunnable = Runnable { this.hideBottom() }

		mPlayPauseTime?.post { mHandler?.post(myRunnable2) }

		mGridParseView?.setLayoutManager(V7LinearLayoutManager(context, 0, false))
		val parseAdapter = ParseAdapter()
		parseAdapter.setOnItemClickListener { _, _, position ->
			val parseBean = parseAdapter.getItem(position)
			parseBean?.let {
				// 当前默认解析需要刷新
				val currentDefault = parseAdapter.data.indexOf(ApiConfig.instance.defaultParse)
				parseAdapter.notifyItemChanged(currentDefault)
				ApiConfig.instance.defaultParse = it
				parseAdapter.notifyItemChanged(position)
				listener?.changeParse(it)
				hideBottom()
			}
		}
		mGridParseView?.adapter = parseAdapter
		parseAdapter.setNewData(ApiConfig.instance.parseBeanList)

		mSeekBar?.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
			override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
				if (!fromUser) {
					return
				}

				val duration = mControlWrapper.duration
				val newPosition = (duration * progress) / seekBar.max
				mCurrentTime?.text = PlayerUtils.stringForTime(newPosition.toInt())
			}

			override fun onStartTrackingTouch(seekBar: SeekBar?) {
				mIsDragging = true
				mControlWrapper.stopProgress()
				mControlWrapper.stopFadeOut()
			}

			override fun onStopTrackingTouch(seekBar: SeekBar) {
				val handle = myHandle ?: return
				val runnable = myRunnable ?: return
				handle.removeCallbacks(runnable)
				handle.postDelayed(runnable, myHandleSeconds.toLong())
				val duration = mControlWrapper.duration
				val newPosition = (duration * seekBar.progress) / seekBar.max
				mControlWrapper.seekTo(newPosition.toInt().toLong())
				mIsDragging = false
				mControlWrapper.startProgress()
				mControlWrapper.startFadeOut()
			}
		})
		mPlayerRetry?.setOnClickListener {
			listener?.replay(true)
			hideBottom()
		}
		mPlayRefresh?.setOnClickListener {
			listener?.replay(false)
			hideBottom()
		}
		mNextBtn?.setOnClickListener {
			listener?.playNext(false)
			hideBottom()
		}
		mPreBtn?.setOnClickListener {
			listener?.playPre()
			hideBottom()
		}
		mPlayerScaleBtn?.setOnClickListener {
			val handle = myHandle ?: return@setOnClickListener
			val runnable = myRunnable ?: return@setOnClickListener
			handle.removeCallbacks(runnable)
			handle.postDelayed(runnable, myHandleSeconds.toLong())
			try {
				var scaleType = mPlayerConfig?.getInt("sc") ?: return@setOnClickListener
				scaleType++
				if (scaleType > 5) scaleType = 0
				mPlayerConfig?.put("sc", scaleType)
				updatePlayerCfgView()
				listener?.updatePlayerCfg()
				mControlWrapper.setScreenScaleType(scaleType)
			} catch (e: JSONException) {
				e.printStackTrace()
			}
		}
		mPlayerSpeedBtn?.setOnClickListener {
			val handle = myHandle ?: return@setOnClickListener
			val runnable = myRunnable ?: return@setOnClickListener
			handle.removeCallbacks(runnable)
			handle.postDelayed(runnable, myHandleSeconds.toLong())
			try {
				var speed = mPlayerConfig?.getDouble("sp")?.toFloat() ?: return@setOnClickListener
				speed += 0.25f
				if (speed > 3) speed = 0.5f
				mPlayerConfig?.put("sp", speed.toDouble())
				updatePlayerCfgView()
				listener?.updatePlayerCfg()
				speedOld = speed
				mControlWrapper.speed = speed
			} catch (e: JSONException) {
				e.printStackTrace()
			}
		}

		mPlayerSpeedBtn?.setOnLongClickListener {
			try {
				mPlayerConfig?.put("sp", 1.0)
				updatePlayerCfgView()
				listener?.updatePlayerCfg()
				speedOld = 1.0f
				mControlWrapper.speed = 1.0f
			} catch (e: JSONException) {
				e.printStackTrace()
			}
			true
		}
		mPlayerBtn?.setOnClickListener {
			val handle = myHandle ?: return@setOnClickListener
			val runnable = myRunnable ?: return@setOnClickListener
			handle.removeCallbacks(runnable)
			handle.postDelayed(runnable, myHandleSeconds.toLong())
			try {
				var playerType = mPlayerConfig?.getInt("pl") ?: return@setOnClickListener
				val existPlayerTypes = PlayerHelper.existPlayerTypes
				var playerTypeIdx = 0
				val playerTypeSize = existPlayerTypes.size
				for (i in 0..<playerTypeSize) {
					if (playerType == existPlayerTypes[i]) {
						playerTypeIdx = if (i == playerTypeSize - 1) {
							0
						} else {
							i + 1
						}
					}
				}
				playerType = existPlayerTypes[playerTypeIdx]
				mPlayerConfig?.put("pl", playerType)
				updatePlayerCfgView()
				listener?.updatePlayerCfg()
				listener?.replay(false)
				listener?.setAllowSwitchPlayer(false)
				hideBottom()
			} catch (e: JSONException) {
				e.printStackTrace()
			}
		}

		mPlayerBtn?.setOnLongClickListener { view ->
			val handle = myHandle ?: return@setOnLongClickListener true
			val runnable = myRunnable ?: return@setOnLongClickListener true
			handle.removeCallbacks(runnable)
			handle.postDelayed(runnable, myHandleSeconds.toLong())
			FastClickCheckUtil.check(view)
			try {
				val playerType = mPlayerConfig?.getInt("pl") ?: return@setOnLongClickListener true
				var defaultPos = 0
				val players = PlayerHelper.existPlayerTypes
				val renders = ArrayList<Int>()
				for (p in players.indices) {
					renders.add(p)
					if (players[p] == playerType) {
						defaultPos = p
					}
				}
				val activity = mActivity ?: return@setOnLongClickListener true
				val dialog = SelectDialog<Int>(activity)
				dialog.setTip("请选择播放器")
				dialog.setAdapter(object : SelectDialogInterface<Int> {
					override fun click(value: Int, pos: Int) {
						try {
							dialog.cancel()
							val thisPlayType = players[pos]
							if (thisPlayType != playerType) {
								mPlayerConfig?.put("pl", thisPlayType)
								updatePlayerCfgView()
								listener?.updatePlayerCfg()
								listener?.replay(false)
								listener?.setAllowSwitchPlayer(false)
								hideBottom()
							}
						} catch (e: Exception) {
							e.printStackTrace()
						}
					}

					override fun getDisplay(`val`: Int): String {
						val playerType = players[`val`]
						return PlayerHelper.getPlayerName(playerType)
					}
				}, object : DiffUtil.ItemCallback<Int>() {
					override fun areItemsTheSame(oldItem: Int, newItem: Int): Boolean {
						return oldItem == newItem
					}

					override fun areContentsTheSame(oldItem: Int, newItem: Int): Boolean {
						return oldItem == newItem
					}
				}, renders, defaultPos)
				dialog.show()
			} catch (e: JSONException) {
				e.printStackTrace()
			}
			true
		}
		mPlayerIJKBtn?.setOnClickListener {
			val handle = myHandle ?: return@setOnClickListener
			val runnable = myRunnable ?: return@setOnClickListener
			handle.removeCallbacks(runnable)
			handle.postDelayed(runnable, myHandleSeconds.toLong())
			try {
				var ijk: String? = mPlayerConfig?.getString("ijk") ?: return@setOnClickListener
				val codecs: List<IJKCode> = ApiConfig.instance.ijkCodes
				for (i in codecs.indices) {
					if (ijk == codecs[i].name) {
						ijk = if (i >= codecs.size - 1) codecs[0].name
						else {
							codecs[i + 1].name
						}
						break
					}
				}
				mPlayerConfig?.put("ijk", ijk)
				updatePlayerCfgView()
				listener?.updatePlayerCfg()
				listener?.replay(false)
				hideBottom()
			} catch (e: JSONException) {
				e.printStackTrace()
			}
		}
		//        增加播放页面片头片尾时间重置
		mPlayerTimeResetBtn?.setOnClickListener {
			val handle = myHandle ?: return@setOnClickListener
			val runnable = myRunnable ?: return@setOnClickListener
			handle.removeCallbacks(runnable)
			handle.postDelayed(runnable, myHandleSeconds.toLong())
			try {
				mPlayerConfig?.put("et", 0)
				mPlayerConfig?.put("st", 0)
				updatePlayerCfgView()
				listener?.updatePlayerCfg()
			} catch (e: JSONException) {
				e.printStackTrace()
			}
		}
		mPlayerTimeStartBtn?.setOnClickListener {
			val handle = myHandle ?: return@setOnClickListener
			val runnable = myRunnable ?: return@setOnClickListener
			handle.removeCallbacks(runnable)
			handle.postDelayed(runnable, myHandleSeconds.toLong())
			try {
				val current = mControlWrapper.currentPosition.toInt()
				val duration = mControlWrapper.duration.toInt()
				if (current > duration / 2) return@setOnClickListener
				mPlayerConfig?.put("st", current / 1000)
				updatePlayerCfgView()
				listener?.updatePlayerCfg()
			} catch (e: JSONException) {
				e.printStackTrace()
			}
		}
		mPlayerTimeStartBtn?.setOnLongClickListener {
			try {
				mPlayerConfig?.put("st", 0)
				updatePlayerCfgView()
				listener?.updatePlayerCfg()
			} catch (e: JSONException) {
				e.printStackTrace()
			}
			true
		}
		mPlayerTimeSkipBtn?.setOnClickListener {
			val handle = myHandle ?: return@setOnClickListener
			val runnable = myRunnable ?: return@setOnClickListener
			handle.removeCallbacks(runnable)
			handle.postDelayed(runnable, myHandleSeconds.toLong())
			try {
				val current = mControlWrapper.currentPosition.toInt()
				val duration = mControlWrapper.duration.toInt()
				if (current < duration / 2) return@setOnClickListener
				mPlayerConfig?.put("et", (duration - current) / 1000)
				updatePlayerCfgView()
				listener?.updatePlayerCfg()
			} catch (e: JSONException) {
				e.printStackTrace()
			}
		}
		mPlayerTimeSkipBtn?.setOnLongClickListener {
			try {
				mPlayerConfig?.put("et", 0)
				updatePlayerCfgView()
				listener?.updatePlayerCfg()
			} catch (e: JSONException) {
				e.printStackTrace()
			}
			true
		}
		mZiMuBtn?.setOnClickListener {
			FastClickCheckUtil.check(it)
			listener?.selectSubtitle()
			hideBottom()
		}
		mZiMuBtn?.setOnLongClickListener {
			mSubtitleView?.apply {
				visibility = GONE
				destroy()
				clearSubtitleCache()
				isInternal = false
			}
			hideBottom()
			Toast.makeText(context, "字幕已关闭", Toast.LENGTH_SHORT).show()
			true
		}
		mAudioTrackBtn?.setOnClickListener {
			FastClickCheckUtil.check(it)
			listener?.selectAudioTrack()
			hideBottom()
		}
		mLandscapePortraitBtn?.setOnClickListener {
			FastClickCheckUtil.check(it)
			setLandscapePortrait()
			hideBottom()
		}
		mFullscreenBtn?.setOnClickListener {
			FastClickCheckUtil.check(it)
			setLandscapePortrait()
			hideBottom()
		}
		//屏显
		val disPlay = PreferenceStore.get(ConfigKey.SCREEN_DISPLAY, GONE)
		seekTime?.visibility = disPlay
		netPlaySpeed?.visibility = disPlay
		mPlayPauseTime?.visibility = disPlay
		mScreenDisplay?.setTextColor(if (disPlay == VISIBLE) resources.getColor(R.color.color_02F8E1) else Color.WHITE)
		mScreenDisplay?.setOnClickListener {
			val disPlay1 = if (PreferenceStore.get(ConfigKey.SCREEN_DISPLAY, GONE) == VISIBLE) GONE else VISIBLE
			seekTime?.visibility = disPlay1
			netPlaySpeed?.visibility = disPlay1
			if (disPlay1 == VISIBLE) mPlayPauseTime?.visibility = VISIBLE
			PreferenceStore.put(ConfigKey.SCREEN_DISPLAY, disPlay1)
			mScreenDisplay?.setTextColor(if (disPlay1 == VISIBLE) resources.getColor(R.color.color_02F8E1) else Color.WHITE)
			hideBottom()
		}
		mNextBtn?.nextFocusLeftId = R.id.screen_display
		mScreenDisplay?.nextFocusRightId = R.id.play_next
	}

	private fun hideLiveAboutBtn() {
		if (mControlWrapper != null && mControlWrapper.duration == 0L) {
			mPlayerSpeedBtn?.visibility = GONE
			mPlayerTimeStartEndText?.visibility = GONE
			mPlayerTimeStartBtn?.visibility = GONE
			mPlayerTimeSkipBtn?.visibility = GONE
			mPlayerTimeResetBtn?.visibility = GONE
		} else {
			mPlayerSpeedBtn?.visibility = VISIBLE
			mPlayerTimeStartEndText?.visibility = VISIBLE
			mPlayerTimeStartBtn?.visibility = VISIBLE
			mPlayerTimeSkipBtn?.visibility = VISIBLE
			mPlayerTimeResetBtn?.visibility = VISIBLE
		}
	}

	fun initLandscapePortraitBtnInfo() {
		val activity = mActivity ?: return
		if (mControlWrapper != null) {
			val width = mControlWrapper.videoSize[0]
			val height = mControlWrapper.videoSize[1]
			val screenSqrt = ScreenUtils.getSqrt(activity)
			if (screenSqrt < 10.0 && width <= height) {
				mLandscapePortraitBtn?.visibility = VISIBLE
				mLandscapePortraitBtn?.text = "竖屏"
			}
			val currentOrientation = resources.configuration.orientation
			if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
				mFullscreenBtn?.setImageResource(R.drawable.icon_fullscreen_exit)
			} else {
				mFullscreenBtn?.setImageResource(R.drawable.icon_fullscreen)
			}
		}
	}

	fun setLandscapePortrait() {
		val currentOrientation = resources.configuration.orientation
		if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
			// 当前是横屏，切换到竖屏
			mLandscapePortraitBtn?.text = "横屏"
			mFullscreenBtn?.setImageResource(R.drawable.icon_fullscreen)
			mActivity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
		} else {
			// 当前是竖屏，切换到横屏
			mLandscapePortraitBtn?.text = "竖屏"
			mFullscreenBtn?.setImageResource(R.drawable.icon_fullscreen_exit)
			mActivity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
		}
	}

	fun initSubtitleInfo() {
		val subtitleTextSize = SubtitleHelper.getTextSize(mActivity ?: return)
		mSubtitleView?.setTextSize(subtitleTextSize.toFloat())
	}

	override fun getLayoutId(): Int {
		return R.layout.player_vod_control_view
	}

	override fun onConfigurationChanged(newConfig: Configuration?) {
		// 保存当前状态
		val currentTitle = mPlayTitle1?.text?.toString().orEmpty()
		val currentLockState = isLock
		val currentBottomVisibility = mBottomRoot?.visibility ?: GONE
		var currentAdapter: ParseAdapter? = null
		if (mGridParseView != null && mGridParseView?.adapter is ParseAdapter) {
			currentAdapter = mGridParseView?.adapter as? ParseAdapter
		}

		// 移除所有子视图，避免两套布局同时存在
		removeAllViews()

		// 重新初始化视图（initView会自动inflate布局，应用正确的布局变种如layout-land）
		initView()

		// 恢复保存的状态
		if (mPlayerConfig != null) {
			updatePlayerCfgView()
		}
		if (currentTitle.isNotEmpty()) {
			mPlayTitle1?.text = currentTitle
		}
		// 恢复锁定状态
		isLock = currentLockState
		mLockView?.setImageResource(if (isLock) R.drawable.icon_lock else R.drawable.icon_unlock)
		// 更新全屏按钮图标
		val currentOrientation = resources.configuration.orientation
		if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
			mFullscreenBtn?.setImageResource(R.drawable.icon_fullscreen_exit)
		} else {
			mFullscreenBtn?.setImageResource(R.drawable.icon_fullscreen)
		}
		// 恢复解析适配器
		if (currentAdapter != null) {
			mGridParseView?.adapter = currentAdapter
		}
		// 恢复控制栏可见性
		if (currentBottomVisibility == VISIBLE) {
			mHandler?.sendEmptyMessage(1002) // 显示底部菜单
		}
	}

	fun showParse(userJxList: Boolean) {
		mParseRoot?.visibility = if (userJxList) VISIBLE else GONE
	}

	fun setPlayerConfig(playerCfg: JSONObject?) {
		this.mPlayerConfig = playerCfg
		updatePlayerCfgView()
	}

	fun updatePlayerCfgView() {
		try {
			val cfg = mPlayerConfig ?: return
			val playerType = cfg.getInt("pl")
			mPlayerBtn?.text = PlayerHelper.getPlayerName(playerType)
			mPlayerScaleBtn?.text = PlayerHelper.getScaleName(cfg.getInt("sc"))
			mPlayerIJKBtn?.text = cfg.getString("ijk")
			mPlayerIJKBtn?.visibility = if (playerType == 1) VISIBLE else GONE
			mPlayerScaleBtn?.text = PlayerHelper.getScaleName(cfg.getInt("sc"))
			mPlayerSpeedBtn?.text = "x${cfg.getDouble("sp")}"
			mPlayerTimeStartBtn?.text = PlayerUtils.stringForTime(cfg.getInt("st") * 1000)
			mPlayerTimeSkipBtn?.text = PlayerUtils.stringForTime(cfg.getInt("et") * 1000)
			mAudioTrackBtn?.visibility = if (playerType == 1 || playerType == 2) VISIBLE else GONE
		} catch (e: JSONException) {
			e.printStackTrace()
		}
	}

	fun setTitle(playTitleInfo: String?) {
		mPlayTitle1?.text = playTitleInfo
	}

	fun resetSpeed() {
		skipEnd = true
		mHandler?.removeMessages(1004)
		mHandler?.sendEmptyMessageDelayed(1004, 100)
	}

	fun setListener(listener: VodControlListener) {
		this.listener = listener
	}

	override fun setProgress(duration: Int, position: Int) {
		if (mIsDragging) {
			return
		}
		super.setProgress(duration, position)
		if (skipEnd && position != 0 && duration != 0) {
			var et = 0
			try {
				et = mPlayerConfig?.getInt("et") ?: 0
			} catch (e: JSONException) {
				e.printStackTrace()
			}
			if (et > 0 && position + (et * 1000) >= duration) {
				skipEnd = false
				listener?.playNext(true)
			}
		}
		mCurrentTime?.text = PlayerUtils.stringForTime(position)
		mTotalTime?.text = PlayerUtils.stringForTime(duration)
		seekTime?.text = "${PlayerUtils.stringForTime(position)} | ${PlayerUtils.stringForTime(duration)}" //右上角进度条时间显示
		val seekBar = mSeekBar ?: return
		if (duration > 0) {
			seekBar.isEnabled = true
			val pos = (position * 1.0 / duration * seekBar.max).toInt()
			seekBar.progress = pos
		} else {
			seekBar.isEnabled = false
		}
		val percent = mControlWrapper.bufferedPercentage
		if (percent >= 95) {
			seekBar.secondaryProgress = seekBar.max
		} else {
			seekBar.secondaryProgress = percent * 10
		}
	}

	fun tvSlideStop() {
		if (!simSlideStart) return
		mControlWrapper.seekTo(simSeekPosition.toLong())
		if (!mControlWrapper.isPlaying) mControlWrapper.start()
		simSlideStart = false
		simSeekPosition = 0
		simSlideOffset = 0
	}

	fun tvSlideStart(dir: Int) {
		val duration = mControlWrapper.duration.toInt()
		if (duration <= 0) return

		val currentTime = System.currentTimeMillis()
		val baseSkip = 10000 // 基础跳转10秒
		val accelerationFactor = 2.0f // 连续操作时的加速因子
		val threshold: Long = 800 // 操作间隔阈值500ms

		if (!simSlideStart) {
			simSlideStart = true
			simSlideOffset = baseSkip.toLong() * dir
		} else {
			if (currentTime - lastSlideTime <= threshold) {
				simSlideOffset += (baseSkip * accelerationFactor * dir).toLong()
			} else {
				simSlideOffset = baseSkip.toLong() * dir
			}
		}
		lastSlideTime = currentTime
		val currentPosition = mControlWrapper.currentPosition.toInt()
		var position = (currentPosition + simSlideOffset).toInt()
		if (position > duration) position = duration
		if (position < 0) position = 0
		updateSeekUI(currentPosition, position, duration)
		simSeekPosition = position
	}

	override fun updateSeekUI(curr: Int, seekTo: Int, duration: Int) {
		super.updateSeekUI(curr, seekTo, duration)
		if (seekTo > curr) {
			mProgressIcon?.setImageResource(R.drawable.icon_forward)
		} else {
			mProgressIcon?.setImageResource(R.drawable.icon_back)
		}
		mProgressText?.text = "${PlayerUtils.stringForTime(seekTo)} / ${PlayerUtils.stringForTime(duration)}"
		mHandler?.sendEmptyMessage(1000)
		mHandler?.removeMessages(1001)
		mHandler?.sendEmptyMessageDelayed(1001, 1000)
	}

	override fun onPlayStateChanged(playState: Int) {
		super.onPlayStateChanged(playState)
		videoPlayState = playState
		when (playState) {
			VideoView.STATE_IDLE -> {}
			VideoView.STATE_PLAYING -> {
				initLandscapePortraitBtnInfo()
				startProgress()
			}

			VideoView.STATE_PAUSED -> mPlayLoadNetSpeedRightTop?.visibility = GONE
			VideoView.STATE_ERROR -> listener?.errReplay()
			VideoView.STATE_PREPARED -> {
				mPlayLoadNetSpeed?.visibility = GONE
				hideLiveAboutBtn()
				listener?.prepared()
			}

			VideoView.STATE_BUFFERED -> mPlayLoadNetSpeed?.visibility = GONE
			VideoView.STATE_PREPARING, VideoView.STATE_BUFFERING -> if (mProgressRoot?.isGone == true) mPlayLoadNetSpeed?.visibility = VISIBLE
			VideoView.STATE_PLAYBACK_COMPLETED -> listener?.playNext(true)
		}
	}

	val isBottomVisible: Boolean
		get() = mBottomRoot?.isVisible ?: false

	fun showBottom() {
		mHandler?.removeMessages(1003)
		mHandler?.sendEmptyMessage(1002)
		mNextBtn?.requestFocus()
	}

	fun showUpBottom() {
		mHandler?.removeMessages(1003)
		mHandler?.sendEmptyMessage(1002)
		mPlayerTimeStartBtn?.requestFocus()
	}

	fun hideBottom() {
		mHandler?.removeMessages(1002)
		mHandler?.sendEmptyMessage(1003)
	}

	override fun onKeyEvent(event: KeyEvent?): Boolean {
		val handle = myHandle ?: return super.onKeyEvent(event)
		val runnable = myRunnable ?: return super.onKeyEvent(event)
		handle.removeCallbacks(runnable)
		if (super.onKeyEvent(event)) {
			return true
		}
		val keyCode = event?.keyCode
		val action = event?.action
		if (this.isBottomVisible) {
			mHandler?.removeMessages(1002)
			mHandler?.removeMessages(1003)
			handle.postDelayed(runnable, myHandleSeconds.toLong())
			return super.dispatchKeyEvent(event)
		}
		val isInPlayback = isInPlaybackState
		if (action == KeyEvent.ACTION_DOWN) {
			if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
				if (isInPlayback) {
					tvSlideStart(if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) 1 else -1)
					return true
				}
			} else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
				if (isInPlayback) {
					togglePlay()
					return true
				}
			} else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_MENU) {
				if (!this.isBottomVisible) {
					showBottom()
					handle.postDelayed(runnable, myHandleSeconds.toLong())
					return true
				}
			}
		} else if (action == KeyEvent.ACTION_UP) {
			if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
				if (isInPlayback) {
					tvSlideStop()
					return true
				}
			}
		}
		return super.dispatchKeyEvent(event)
	}

	private fun speedPlayStart() {
		fromLongPress = true
		try {
			speedOld = mPlayerConfig?.getDouble("sp")?.toFloat() ?: return
			val speed = 3.0f
			mPlayerConfig?.put("sp", speed.toDouble())
			updatePlayerCfgView()
			listener?.updatePlayerCfg()
			mControlWrapper.speed = speed
			findViewById<View>(R.id.play_speed_3_container)?.visibility = VISIBLE
		} catch (f: JSONException) {
			f.printStackTrace()
		}
	}

	private fun speedPlayEnd() {
		if (fromLongPress) {
			fromLongPress = false
			try {
				val speed = speedOld
				mPlayerConfig?.put("sp", speed.toDouble())
				updatePlayerCfgView()
				listener?.updatePlayerCfg()
				mControlWrapper.speed = speed
			} catch (f: JSONException) {
				f.printStackTrace()
			}
			findViewById<View>(R.id.play_speed_3_container)?.visibility = GONE
		}
	}

	override fun onLongPress(p0: MotionEvent) {
		if (videoPlayState != VideoView.STATE_PAUSED) {
			speedPlayStart()
		}
	}

	override fun onTouchEvent(event: MotionEvent?): Boolean {
		if (event?.action == MotionEvent.ACTION_UP) {
			speedPlayEnd()
		}
		return super.onTouchEvent(event)
	}

	private fun setMinPlayTimeChange(typeEt: String, increase: Boolean): Boolean {
		val handle = myHandle ?: return false
		val runnable = myRunnable ?: return false
		handle.removeCallbacks(runnable)
		handle.postDelayed(runnable, myHandleSeconds.toLong())
		try {
			val currentValue = mPlayerConfig?.optInt(typeEt, 0) ?: 0
			if (currentValue != 0) {
				var newValue = if (increase) currentValue + 1 else currentValue - 1
				if (newValue < 0) {
					newValue = 0
				}
				mPlayerConfig?.put(typeEt, newValue)
				updatePlayerCfgView()
				listener?.updatePlayerCfg()
				return true
			}
		} catch (e: JSONException) {
			e.printStackTrace()
		}
		return false
	}

	override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
		if (this.isBottomVisible) {
			if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
				if (mPlayerTimeStartBtn?.hasFocus() == true) {
					if (setMinPlayTimeChange("st", true)) {
						return true
					}
				}
				val focusedView = mPlayBtnGroup?.findFocus()
				if (focusedView is TextView) {
					return true
				}
			}
			if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
				if (mPlayerTimeStartBtn?.hasFocus() == true) {
					if (setMinPlayTimeChange("st", false)) return true
				}
			}
			return super.onKeyDown(keyCode, event)
		}
		if (keyCode == KeyEvent.KEYCODE_DPAD_UP && event.repeatCount == 0) {
			isLongPressTriggered = false
			val longPressRunnable = Runnable {
				speedPlayStart()
				isLongPressTriggered = true
			}
			mLongPressRunnable = longPressRunnable
			mmHandler.postDelayed(longPressRunnable, LONG_PRESS_DELAY)
			return true
		}
		return super.onKeyDown(keyCode, event)
	}

	override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
		if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
			// 移除长按回调
			mLongPressRunnable?.let { mmHandler.removeCallbacks(it) }
			mLongPressRunnable = null
			if (isLongPressTriggered) {
				speedPlayEnd()
			} else {
				if (!this.isBottomVisible) {
					showUpBottom()
					val handle = myHandle ?: return true
					val runnable = myRunnable ?: return true
					handle.postDelayed(runnable, myHandleSeconds.toLong())
				} else {
					return super.onKeyUp(keyCode, event)
				}
			}
			return true
		}
		return super.onKeyUp(keyCode, event)
	}

	override fun onSingleTapConfirmed(p0: MotionEvent): Boolean {
		val handle = myHandle ?: return true
		val runnable = myRunnable ?: return true
		handle.removeCallbacks(runnable)
		if (!this.isBottomVisible) {
			showBottom()
			// 闲置计时关闭
			handle.postDelayed(runnable, myHandleSeconds.toLong())
		} else {
			hideBottom()
		}
		return true
	}

	override fun onBackPressed(): Boolean {
		if (isClickBackBtn) {
			isClickBackBtn = false
			if (this.isBottomVisible) {
				hideBottom()
			}
			return false
		}
		if (super.onBackPressed()) {
			return true
		}
		if (this.isBottomVisible) {
			hideBottom()
			return true
		}
		return false
	}

	override fun onDetachedFromWindow() {
		super.onDetachedFromWindow()
		mHandler?.removeCallbacks(myRunnable2)
	}

	fun encodeUrl(url: String?): String? {
		return try {
			URLEncoder.encode(url, "UTF-8")
		} catch (e: Exception) {
			url
		}
	}

	fun switchPlayer(): Boolean {
		try {
			val playerType = mPlayerConfig?.getInt("pl") ?: return true
			val pType = if (playerType == 1) playerType + 1 else if (playerType == 2) playerType - 1 else playerType
			if (pType != playerType) {
				Toast.makeText(context, "切换到" + (if (pType == 1) "IJK" else "EXO") + "播放器重试", Toast.LENGTH_SHORT).show()
				mPlayerConfig?.put("pl", pType)
				updatePlayerCfgView()
				listener?.updatePlayerCfg()
			} else {
				return true
			}
		} catch (e: Exception) {
			return true
		}
		if (switchPlayerCount == 1) {
			switchPlayerCount = 0
			return true
		}
		switchPlayerCount++
		return false
	}

	fun playM3u8(url: String, headers: HashMap<String, String>?) {
		if (url.contains("url=")) {
			listener?.startPlayUrl(url, headers)
			return
		}
		OkGo.getInstance().cancelTag("m3u8-1")
		OkGo.getInstance().cancelTag("m3u8-2")
		val okGoHeaders = HttpHeaders()
		headers?.let {
			for (entry in it.entries) {
				okGoHeaders.put(entry.key, entry.value)
			}
		}
		OkGo.get<String>(url)
			.tag("m3u8-1")
			.headers(okGoHeaders)
			.execute(object : AbsCallback<String>() {
				@UnstableApi
				override fun onSuccess(response: Response<String>?) {
					val content = response?.body()
					content?.let {
						if (!it.startsWith("#EXTM3U")) {
							listener?.startPlayUrl(url, headers)
							return
						}
					}
					val forwardUrl = extractForwardUrl(url, content.orEmpty())
					if (forwardUrl.isEmpty()) {
						TVBoxRuntimeLog.i("echo-m3u81-to-play")
						processM3u8Content(url, content, headers)
					} else {
						fetchAndProcessForwardUrl(forwardUrl, headers, okGoHeaders, url)
					}
				}

				override fun convertResponse(response: okhttp3.Response): String {
					return response.body.string()
				}

				override fun onError(response: Response<String>) {
					super.onError(response)
					TVBoxRuntimeLog.e("echo-m3u8请求错误1: " + response.exception)
					listener?.startPlayUrl(url, headers)
				}
			})
	}

	private fun extractForwardUrl(baseUrl: String?, content: String): String {
		val lines: List<String> = content.split("\\r?\\n".toRegex(), limit = 50)
		for (i in lines.indices) {
			val line = lines[i].trim { it <= ' ' }
			if (line.startsWith("#EXT-X-STREAM-INF")) {
				// 只需要找接下来的几行
				for (j in i + 1..<lines.size) {
					val targetLine = lines[j].trim { it <= ' ' }
					if (targetLine.isEmpty()) continue
					if (isValidM3u8Line(targetLine)) {
						return resolveForwardUrl(baseUrl, targetLine)
					}
				}
			}
		}
		return ""
	}

	private fun isValidM3u8Line(line: String): Boolean {
		return !line.startsWith("#") && (line.endsWith(".m3u8") || line.contains(".m3u8?"))
	}

	@UnstableApi
	private fun processM3u8Content(url: String, content: String?, headers: HashMap<String, String>?) {
		val basePath = getBasePath(url)
		RemoteServer.m3u8Content = M3U8.purify(basePath, content)
		if (RemoteServer.m3u8Content == null || M3U8.currentAdCount == 0) {
			TVBoxRuntimeLog.i("echo-m3u8内容解析：未检测到广告")
			listener?.startPlayUrl(url, headers)
		} else {
			listener?.startPlayUrl(ControlManager.instance.getAddress(true) + "proxyM3u8", headers)
			Toast.makeText(context, "已移除视频广告 " + M3U8.currentAdCount + " 条", Toast.LENGTH_SHORT).show()
		}
	}

	private fun fetchAndProcessForwardUrl(
		forwardUrl: String,
		headers: HashMap<String, String>?,
		okGoHeaders: HttpHeaders?,
		fallbackUrl: String?
	) {
		OkGo.get<String>(forwardUrl)
			.tag("m3u8-2")
			.headers(okGoHeaders)
			.execute(object : AbsCallback<String>() {
				@UnstableApi
				override fun onSuccess(response: Response<String>) {
					val content = response.body()
					TVBoxRuntimeLog.i("echo-m3u82-to-play")
					processM3u8Content(forwardUrl, content, headers)
				}

				override fun convertResponse(response: okhttp3.Response): String {
					return response.body.string()
				}

				override fun onError(response: Response<String>) {
					super.onError(response)
					TVBoxRuntimeLog.e("echo-重定向 m3u8 请求错误: " + response.exception)
					listener?.startPlayUrl(fallbackUrl, headers)
				}
			})
	}

	private fun getBasePath(url: String): String {
		val iLast = url.lastIndexOf('/')
		return url.substring(0, iLast + 1)
	}

	private fun resolveForwardUrl(baseUrl: String?, line: String): String {
		try {
			// 使用 URL 构造器自动解析相对路径
			val base = URL(baseUrl)
			val resolved = URL(base, line)
			return resolved.toString()
		} catch (e: MalformedURLException) {
			// 出现异常时可以记录日志，并返回原始 line
			TVBoxRuntimeLog.e("echo-resolveForwardUrl异常: " + e.message)
			return line
		}
	}

	fun firstUrlByArray(url: String?): String? {
		var result = url
		try {
			val urlArray = JSONArray(url)
			for (i in 0..<urlArray.length()) {
				val item = urlArray.getString(i)
				if (item.contains("http")) {
					result = item
					break // 找到第一个立即终止循环
				}
			}
		} catch (ignored: JSONException) {
		}
		return result
	}

	fun evaluateScript(sourceBean: SourceBean, url: String, webView: WebView?) {
		var clickSelector = sourceBean.clickSelector?.trim { it <= ' ' } ?: return
		clickSelector = clickSelector.ifEmpty { VideoParseRuler.getHostScript(url) }
		if (clickSelector.isNotEmpty()) {
			val selector: String
			if (clickSelector.contains(";") && !clickSelector.endsWith(";")) {
				val parts: List<String> = clickSelector.split(";".toRegex(), limit = 2)
				if (!url.contains(parts[0])) {
					return
				}
				selector = parts[1].trim { it <= ' ' }
			} else {
				selector = clickSelector.trim { it <= ' ' }
			}
			// 构造点击的 JS 代码
			val js = selector
			//            if(!selector.contains("click()"))js+=".click();";
			TVBoxRuntimeLog.i("echo-javascript:$js")
			webView?.evaluateJavascript(js, null)
		}
	}

	fun stopOther() {
		stop(false) //停止磁力下载
		finish() //停止p2p下载
		App.dashData = null
	}

	interface VodControlListener {
		fun playNext(rmProgress: Boolean)

		fun playPre()

		fun prepared()

		fun changeParse(pb: ParseBean)

		fun updatePlayerCfg()

		fun replay(replay: Boolean)

		fun errReplay()

		fun selectSubtitle()

		fun selectAudioTrack()

		fun startPlayUrl(url: String?, headers: HashMap<String, String>?)

		fun setAllowSwitchPlayer(isAllow: Boolean)
	}

	private inner class LockRunnable : Runnable {
		override fun run() {
			mLockView?.visibility = INVISIBLE
		}
	}

	companion object {
		private const val LONG_PRESS_DELAY: Long = 800
		private var switchPlayerCount = 0
	}
}
