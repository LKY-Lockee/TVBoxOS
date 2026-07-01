package com.github.tvbox.osc.ui.activity

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.IntEvaluator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.github.tvbox.osc.R
import com.github.tvbox.osc.api.ApiConfig
import com.github.tvbox.osc.base.App
import com.github.tvbox.osc.base.BaseActivity
import com.github.tvbox.osc.bean.EpgInfo
import com.github.tvbox.osc.bean.LiveChannelGroup
import com.github.tvbox.osc.bean.LiveChannelItem
import com.github.tvbox.osc.bean.LiveDayListGroup
import com.github.tvbox.osc.bean.LiveEpgDate
import com.github.tvbox.osc.bean.LivePlayerManager
import com.github.tvbox.osc.bean.LiveSettingGroup
import com.github.tvbox.osc.bean.LiveSettingItem
import com.github.tvbox.osc.player.controller.LiveController
import com.github.tvbox.osc.player.controller.LiveController.LiveControlListener
import com.github.tvbox.osc.ui.adapter.LiveChannelGroupAdapter
import com.github.tvbox.osc.ui.adapter.LiveChannelItemAdapter
import com.github.tvbox.osc.ui.adapter.LiveEpgAdapter
import com.github.tvbox.osc.ui.adapter.LiveEpgDateAdapter
import com.github.tvbox.osc.ui.adapter.LiveSettingGroupAdapter
import com.github.tvbox.osc.ui.adapter.LiveSettingItemAdapter
import com.github.tvbox.osc.ui.adapter.MyEpgAdapter
import com.github.tvbox.osc.ui.dialog.LivePasswordDialog
import com.github.tvbox.osc.ui.tv.widget.ViewObj
import com.github.tvbox.osc.util.DefaultConfig.safeJsonString
import com.github.tvbox.osc.util.EpgUtil.getEpgInfo
import com.github.tvbox.osc.util.FastClickCheckUtil
import com.github.tvbox.osc.util.HawkConfig
import com.github.tvbox.osc.util.PlayerHelper.getDisplaySpeed
import com.github.tvbox.osc.util.RegexUtils.getPattern
import com.github.tvbox.osc.util.TVBoxRuntimeLog.i
import com.github.tvbox.osc.util.live.TxtSubscribe
import com.github.tvbox.osc.util.urlhttp.CallBackUtil.CallBackString
import com.github.tvbox.osc.util.urlhttp.UrlHttpUtil.get
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.lzy.okgo.OkGo
import com.lzy.okgo.callback.AbsCallback
import com.orhanobut.hawk.Hawk
import com.owen.tvrecyclerview.widget.TvRecyclerView
import com.owen.tvrecyclerview.widget.TvRecyclerView.OnItemListener
import com.owen.tvrecyclerview.widget.V7LinearLayoutManager
import com.squareup.picasso.Picasso
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import xyz.doikki.videoplayer.player.VideoView
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Hashtable
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * @author pj567
 * @date :2021/1/12
 * @description:
 */
class LivePlayActivity : BaseActivity() {
	val timeFormat: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
	private val videoWidth = 1920
	private val videoHeight = 1080
	private val mmHandler = Handler(Looper.getMainLooper())
	private val mHandler = Handler(Looper.getMainLooper())
	private val liveChannelGroupList: MutableList<LiveChannelGroup> = ArrayList()
	private val livePlayerManager = LivePlayerManager()
	private val channelGroupPasswordConfirmed = ArrayList<Int>()
	private val liveDayList: MutableList<LiveDayListGroup> = ArrayList()
	var epgStringAddress: String? = ""
	var llEpg: RelativeLayout? = null
	var tvChannelNum: TextView? = null
	var tipChName: TextView? = null
	var tipEpg1: TextView? = null
	var tipEpg2: TextView? = null
	var tvSrcInfo: TextView? = null
	var tvCurEpgLeft: TextView? = null
	var tvNextEpgLeft: TextView? = null
	private var mVideoView: VideoView? = null
	private var tvChannelInfo: TextView? = null
	private val mHideChannelInfoRun: Runnable = Runnable { (tvChannelInfo ?: return@Runnable).visibility = View.INVISIBLE }
	private var tvTime: TextView? = null
	private val mUpdateTimeRun: Runnable = object : Runnable {
		override fun run() {
			val day = Date()
			val df = SimpleDateFormat("hh:mm a", Locale.getDefault())
			(tvTime ?: return).text = df.format(day)
			mHandler.postDelayed(this, 1000)
		}
	}
	private var tvNetSpeed: TextView? = null
	private val mUpdateNetSpeedRun: Runnable = object : Runnable {
		override fun run() {
			if (mVideoView == null) return
			val speed = getDisplaySpeed((mVideoView ?: return).tcpSpeed, true)
			(tvNetSpeed ?: return).text = speed
			//            tv_right_top_tipnetspeed.setText(speed);
			mHandler.postDelayed(this, 1000)
		}
	}
	private var tvLeftChannelListLayout: LinearLayout? = null
	private val mHideChannelListRun: Runnable = Runnable {
		val params = (tvLeftChannelListLayout ?: return@Runnable).layoutParams as MarginLayoutParams
		if ((tvLeftChannelListLayout ?: return@Runnable).isVisible) {
			val viewObj = ViewObj(tvLeftChannelListLayout, params)
			val animator = ObjectAnimator.ofObject(viewObj, "marginLeft", IntEvaluator(), 0, -(tvLeftChannelListLayout ?: return@Runnable).layoutParams.width)
			animator.duration = 200
			animator.addListener(object : AnimatorListenerAdapter() {
				override fun onAnimationEnd(animation: Animator) {
					super.onAnimationEnd(animation)
					tvLeftChannelListLayout?.visibility = View.INVISIBLE
				}
			})
			animator.start()
		}
	}
	private var mChannelGroupView: TvRecyclerView? = null
	private var mLiveChannelView: TvRecyclerView? = null
	private var liveChannelGroupAdapter: LiveChannelGroupAdapter? = null
	private var liveChannelItemAdapter: LiveChannelItemAdapter? = null
	private var tvRightSettingLayout: LinearLayout? = null
	private var mSettingGroupView: TvRecyclerView? = null
	private var mSettingItemView: TvRecyclerView? = null
	private var liveSettingGroupAdapter: LiveSettingGroupAdapter? = null
	private val mHideSettingLayoutRun: Runnable = Runnable {
		val params = (tvRightSettingLayout ?: return@Runnable).layoutParams as MarginLayoutParams
		if ((tvRightSettingLayout ?: return@Runnable).isVisible) {
			val viewObj = ViewObj(tvRightSettingLayout, params)
			val animator = ObjectAnimator.ofObject(viewObj, "marginRight", IntEvaluator(), 0, -(tvRightSettingLayout ?: return@Runnable).layoutParams.width)
			animator.duration = 200
			animator.addListener(object : AnimatorListenerAdapter() {
				override fun onAnimationEnd(animation: Animator) {
					super.onAnimationEnd(animation)
					(tvRightSettingLayout ?: return).visibility = View.INVISIBLE
					(liveSettingGroupAdapter ?: return).setSelectedGroupIndex(-1)
				}
			})
			animator.start()
		}
	}
	private val mFocusAndShowSettingGroup: Runnable = object : Runnable {
		override fun run() {
			if ((mSettingGroupView ?: return).isScrolling || (mSettingItemView ?: return).isScrolling || (mSettingGroupView ?: return).isComputingLayout || (mSettingItemView ?: return).isComputingLayout) {
				mHandler.postDelayed(this, 100)
			} else {
				val holder = (mSettingGroupView ?: return).findViewHolderForAdapterPosition(0)
				holder?.itemView?.requestFocus()
				(tvRightSettingLayout ?: return).visibility = View.VISIBLE
				val params = (tvRightSettingLayout ?: return).layoutParams as MarginLayoutParams
				if ((tvRightSettingLayout ?: return).isVisible) {
					val viewObj = ViewObj(tvRightSettingLayout, params)
					val animator = ObjectAnimator.ofObject(viewObj, "marginRight", IntEvaluator(), -(tvRightSettingLayout ?: return).layoutParams.width, 0)
					animator.duration = 200
					animator.addListener(object : AnimatorListenerAdapter() {
						override fun onAnimationEnd(animation: Animator) {
							super.onAnimationEnd(animation)
							mHandler.postDelayed(mHideSettingLayoutRun, POST_TIMEOUT.toLong())
						}
					})
					animator.start()
				}
			}
		}
	}
	private var liveSettingItemAdapter: LiveSettingItemAdapter? = null
	private var liveSettingGroupList: MutableList<LiveSettingGroup> = ArrayList()
	private var currentLiveChannelIndex = -1
	private val mFocusCurrentChannelAndShowChannelList: Runnable = object : Runnable {
		override fun run() {
			if ((mChannelGroupView ?: return).isScrolling || (mLiveChannelView ?: return).isScrolling || (mChannelGroupView ?: return).isComputingLayout || (mLiveChannelView ?: return).isComputingLayout) {
				mHandler.postDelayed(this, 100)
			} else {
				(liveChannelGroupAdapter ?: return).setSelectedGroupIndex(currentChannelGroupIndex)
				(liveChannelItemAdapter ?: return).setSelectedChannelIndex(currentLiveChannelIndex)
				val holder = (mLiveChannelView ?: return).findViewHolderForAdapterPosition(currentLiveChannelIndex)
				holder?.itemView?.requestFocus()
				(tvLeftChannelListLayout ?: return).visibility = View.VISIBLE
				val viewObj = ViewObj(tvLeftChannelListLayout, (tvLeftChannelListLayout ?: return).layoutParams as MarginLayoutParams?)
				val animator = ObjectAnimator.ofObject(viewObj, "marginLeft", IntEvaluator(), -(tvLeftChannelListLayout ?: return).layoutParams.width, 0)
				animator.duration = 200
				animator.addListener(object : AnimatorListenerAdapter() {
					override fun onAnimationEnd(animation: Animator) {
						super.onAnimationEnd(animation)
						mHandler.removeCallbacks(mHideChannelListRun)
						mHandler.postDelayed(mHideChannelListRun, POST_TIMEOUT.toLong())
					}
				})
				animator.start()
			}
		}
	}
	private var currentLiveLookBackIndex = -1
	private var currentLiveChangeSourceTimes = 0
	private var currentLiveChannelItem: LiveChannelItem? = null
	private var countDownTimer: CountDownTimer? = null
	private var backPressedCallback: OnBackPressedCallback? = null

	//    private CountDownTimer countDownTimerRightTop;
	private var llRightTopLoading: View? = null
	private var llRightTopHuiKan: View? = null
	private var divLoadEpg: View? = null
	private var divLoadEpgLeft: View? = null
	private var divEpg: LinearLayout? = null
	private val myAdapter: MyEpgAdapter? = null
	private val tvRightTopTipNetSpeed: TextView? = null
	private var tvRightTopChannelName: TextView? = null
	private var tvRightTopEpgName: TextView? = null
	private val tvRightTopType: TextView? = null
	private var mEpgDateGridView: TvRecyclerView? = null
	private var mRightEpgList: TvRecyclerView? = null
	private var liveEpgDateAdapter: LiveEpgDateAdapter? = null
	private var epgListAdapter: LiveEpgAdapter? = null
	private var isSHIYI = false
	private var isBack = false

	//kenson
	private var imgLiveIcon: ImageView? = null
	private var liveIconNullBg: FrameLayout? = null
	private var liveIconNullText: TextView? = null
	private var backController: View? = null
	private var countDownTimer3: CountDownTimer? = null
	private var tvCurrentPos: TextView? = null
	private var tvDuration: TextView? = null
	private var sBar: SeekBar? = null
	private var ivPlayPause: View? = null
	private var ivPlay: View? = null

	// 遥控器数字键输入的要切换的频道号码
	private var selectedChannelNumber = 0
	private var tvSelectedChannel: TextView? = null
	private var mLongPressRunnable: Runnable? = null
	private var mLastChannelGroupIndex = -1
	private var mLastChannelList: List<LiveChannelItem> = emptyList()
	private var catchup: JsonObject? = null
	private var hasCatchup = false
	private var logoUrl: String? = null
	private val mPlaySelectedChannel: Runnable = Runnable {
		var currentTotal = 0
		var groupIndex = 0
		var channelIndex = -1
		for (group in liveChannelGroupList) {
			val groupChannelCount = (group.liveChannels ?: return@Runnable).size
			if (currentTotal + groupChannelCount >= selectedChannelNumber) {
				channelIndex = selectedChannelNumber - currentTotal - 1 // 转换为0-based索引
				break
			}
			currentTotal += groupChannelCount
			groupIndex++
		}
		(tvSelectedChannel ?: return@Runnable).visibility = View.INVISIBLE
		(tvSelectedChannel ?: return@Runnable).text = ""
		if (channelIndex >= 0) {
			loadChannelGroupDataAndPlay(groupIndex, channelIndex)
		} else {
			playChannel(currentChannelGroupIndex, currentLiveChannelIndex, false)
		}
		selectedChannelNumber = 0
	}
	private val mConnectTimeoutChangeSourceRun = Runnable {
		currentLiveChangeSourceTimes++
		if ((currentLiveChannelItem ?: return@Runnable).sourceNum == currentLiveChangeSourceTimes) {
			currentLiveChangeSourceTimes = 0
			val groupChannelIndex = getNextChannel(if (Hawk.get(HawkConfig.LIVE_CHANNEL_REVERSE, false)) -1 else 1)
			playChannel(groupChannelIndex[0], groupChannelIndex[1], false)
		} else {
			playNextSource()
		}
	}

	override val layoutResID: Int
		get() = R.layout.activity_live_play

	override fun init() {
		epgStringAddress = Hawk.get(HawkConfig.EPG_URL, "")
		if (epgStringAddress == null || (epgStringAddress ?: return).length < 5) epgStringAddress = "http://epg.51zmt.top:8000/api/diyp/"

		setLoadSir(findViewById(R.id.live_root))
		mVideoView = findViewById(R.id.mVideoView)

		tvLeftChannelListLayout = findViewById(R.id.tvLeftChannnelListLayout)
		mChannelGroupView = findViewById(R.id.mGroupGridView)
		mLiveChannelView = findViewById(R.id.mChannelGridView)
		tvRightSettingLayout = findViewById(R.id.tvRightSettingLayout)
		mSettingGroupView = findViewById(R.id.mSettingGroupView)
		mSettingItemView = findViewById(R.id.mSettingItemView)
		tvChannelInfo = findViewById(R.id.tvChannel)
		tvTime = findViewById(R.id.tvTime)
		tvNetSpeed = findViewById(R.id.tvNetSpeed)

		//EPG  findViewById  by 龍
		tipChName = findViewById(R.id.tv_channel_bar_name) //底部名称
		tvChannelNum = findViewById(R.id.tv_channel_bottom_number) //底部数字
		tipEpg1 = findViewById(R.id.tv_current_program_time) //底部EPG当前节目信息
		tipEpg2 = findViewById(R.id.tv_next_program_time) //底部EPG当下个节目信息
		tvSrcInfo = findViewById(R.id.tv_source) //线路状态
		tvCurEpgLeft = findViewById(R.id.tv_current_program) //当前节目
		tvNextEpgLeft = findViewById(R.id.tv_next_program) //下一节目
		llEpg = findViewById(R.id.ll_epg)
		//        tv_right_top_tipnetspeed = (TextView)findViewById(R.id.tv_right_top_tipnetspeed);
		tvRightTopChannelName = findViewById(R.id.tv_right_top_channel_name)
		tvRightTopEpgName = findViewById(R.id.tv_right_top_epg_name)
		//        tv_right_top_type = (TextView)findViewById(R.id.tv_right_top_type);
		val ivCircleBg = findViewById<ImageView?>(R.id.iv_circle_bg)
		llRightTopLoading = findViewById(R.id.ll_right_top_loading)
		llRightTopHuiKan = findViewById(R.id.ll_right_top_huikan)
		divLoadEpg = findViewById(R.id.divLoadEpg)
		divLoadEpgLeft = findViewById(R.id.divLoadEpgleft)
		divEpg = findViewById(R.id.divEPG)
		//右上角图片旋转
		val objectAnimator = ObjectAnimator.ofFloat(ivCircleBg, "rotation", 360.0f)
		objectAnimator.duration = POST_TIMEOUT.toLong()
		objectAnimator.repeatCount = -1
		objectAnimator.start()

		//laodao 7day replay
		mEpgDateGridView = findViewById(R.id.mEpgDateGridView)
		Hawk.put(HawkConfig.NOW_DATE, formatDate.format(Date()))
		day = formatDate.format(Date())
		nowDay = Date()

		mRightEpgList = findViewById(R.id.lv_epg)
		//EPG频道名称
		imgLiveIcon = findViewById(R.id.img_live_icon)
		liveIconNullBg = findViewById(R.id.live_icon_null_bg)
		liveIconNullText = findViewById(R.id.live_icon_null_text)
		(imgLiveIcon ?: return).visibility = View.INVISIBLE
		(liveIconNullText ?: return).visibility = View.INVISIBLE
		(liveIconNullBg ?: return).visibility = View.INVISIBLE

		sBar = findViewById(R.id.pb_progressbar)
		tvCurrentPos = findViewById(R.id.tv_currentpos)
		backController = findViewById(R.id.backcontroller)
		tvDuration = findViewById(R.id.tv_duration)
		ivPlayPause = findViewById(R.id.iv_playpause)
		ivPlay = findViewById(R.id.iv_play)

		tvSelectedChannel = findViewById(R.id.tv_selected_channel)

		(backController ?: return).visibility = View.GONE
		(llEpg ?: return).visibility = View.VISIBLE


		(ivPlay ?: return).setOnClickListener { arg0: View? ->
			(mVideoView ?: return@setOnClickListener).start()
			(ivPlay ?: return@setOnClickListener).visibility = View.INVISIBLE
			(countDownTimer ?: return@setOnClickListener).start()
			(ivPlayPause ?: return@setOnClickListener).background = ContextCompat.getDrawable(this@LivePlayActivity, R.drawable.vod_pause)
		}

		(ivPlayPause ?: return).setOnClickListener { arg0: View? ->
			if ((mVideoView ?: return@setOnClickListener).isPlaying) {
				(mVideoView ?: return@setOnClickListener).pause()
				(countDownTimer ?: return@setOnClickListener).cancel()
				(ivPlay ?: return@setOnClickListener).visibility = View.VISIBLE
				(ivPlayPause ?: return@setOnClickListener).background = ContextCompat.getDrawable(this@LivePlayActivity, R.drawable.icon_play)
			} else {
				(mVideoView ?: return@setOnClickListener).start()
				(ivPlay ?: return@setOnClickListener).visibility = View.INVISIBLE
				(countDownTimer ?: return@setOnClickListener).start()
				(ivPlayPause ?: return@setOnClickListener).background = ContextCompat.getDrawable(this@LivePlayActivity, R.drawable.vod_pause)
			}
		}
		(sBar ?: return).setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
			override fun onStopTrackingTouch(arg0: SeekBar?) {
			}

			override fun onStartTrackingTouch(arg0: SeekBar?) {
			}

			override fun onProgressChanged(sb: SeekBar?, progress: Int, fromuser: Boolean) {
				if (!fromuser) {
					return
				}
				if (countDownTimer != null) {
					(mVideoView ?: return).seekTo(progress.toLong())
					(countDownTimer ?: return).cancel()
					(countDownTimer ?: return).start()
				}
			}
		})
		(sBar ?: return).setOnKeyListener { _: View?, keycode: Int, event: KeyEvent? ->
			if (event?.action == KeyEvent.ACTION_DOWN && (keycode == KeyEvent.KEYCODE_DPAD_CENTER || keycode == KeyEvent.KEYCODE_ENTER)) {
				toggleReplayPause()
			}
			false
		}
		initEpgDateView()
		initEpgListView()
		initDayList()
		initVideoView()
		initChannelGroupView()
		initLiveChannelView()
		initSettingGroupView()
		initSettingItemView()
		initLiveChannelList()
		initLiveSettingGroupList()
		Hawk.put(HawkConfig.PLAYER_IS_LIVE, true)

		backPressedCallback = object : OnBackPressedCallback(true) {
			override fun handleOnBackPressed() {
				this@LivePlayActivity.handleBackPressed()
			}
		}.also { callback ->
			onBackPressedDispatcher.addCallback(this, callback)
		}
	}

	private fun toggleReplayPause() {
		val videoView = mVideoView ?: return
		val timer = countDownTimer ?: return
		val playButton = ivPlay ?: return
		val pauseButton = ivPlayPause ?: return
		val appContext = this

		if (videoView.isPlaying) {
			videoView.pause()
			timer.cancel()
			playButton.visibility = View.VISIBLE
			pauseButton.background = ContextCompat.getDrawable(appContext, R.drawable.icon_play)
		} else {
			videoView.start()
			playButton.visibility = View.INVISIBLE
			timer.start()
			pauseButton.background = ContextCompat.getDrawable(appContext, R.drawable.vod_pause)
		}
	}

	private fun showEpg(date: Date?, arrayList: List<EpgInfo>?) {
		if (!arrayList.isNullOrEmpty()) {
			//获取EPG并存储 // 百川epg  DIYP epg   51zmt epg ------- 自建EPG格式输出格式请参考 51zmt
			(epgListAdapter ?: return).CanBack((currentLiveChannelItem ?: return).includeBack)
			(epgListAdapter ?: return).setNewData(arrayList)

			val i: Int
			var size = arrayList.size - 1
			while (size >= 0) {
				if (Date() >= arrayList[size].startDateTime) {
					break
				}
				size--
			}
			i = size
			if (i >= 0 && Date() <= arrayList[i].endDateTime) {
				(mRightEpgList ?: return).selectedPosition = i
				(mRightEpgList ?: return).setSelection(i)
				(epgListAdapter ?: return).setSelectedEpgIndex(i)
				(mRightEpgList ?: return).post { (mRightEpgList ?: return@post).smoothScrollToPosition(i) }
			}
		}
	}

	private fun getFirstPartBeforeSpace(str: String?): String {
		if (str.isNullOrEmpty()) {
			return ""
		}
		val spaceIndex = str.indexOf(' ')
		return if (spaceIndex == -1) {
			str
		} else {
			str.substring(0, spaceIndex)
		}
	}

	fun getEpg(date: Date) {
		val channelName: String? = (channel_Name ?: return).channelName
		val channelNameReal = getFirstPartBeforeSpace(channelName)
		val timeFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
		timeFormat.timeZone = TimeZone.getTimeZone("GMT+8:00")
		var epgTagName = channelNameReal
		if (logoUrl == null || (logoUrl ?: return).isEmpty()) {
			val epgInfo = getEpgInfo(channelNameReal)
			if (epgInfo != null && !epgInfo[1].isEmpty()) {
				epgTagName = epgInfo[1]
			}
			updateChannelIcon(channelName, epgInfo?.get(0))
		} else if (logoUrl == "false") {
			updateChannelIcon(channelName, null)
		} else {
			val logo = (logoUrl ?: return).replace("{name}", epgTagName)
			updateChannelIcon(channelName, logo)
		}
		(epgListAdapter ?: return).CanBack((currentLiveChannelItem ?: return).includeBack)
		val url = if ((epgStringAddress ?: return).contains("{name}") && (epgStringAddress ?: return).contains("{date}")) {
			(epgStringAddress ?: return).replace("{name}", URLEncoder.encode(epgTagName, StandardCharsets.UTF_8.name())).replace("{date}", timeFormat.format(date))
		} else {
			epgStringAddress + "?ch=" + URLEncoder.encode(epgTagName, StandardCharsets.UTF_8.name()) + "&date=" + timeFormat.format(date)
		}

		val selectedEpgDate = selectedEpgDate() ?: return
		val savedEpgKey = channelName + "_" + selectedEpgDate.datePresented
		if (hsEpg.containsKey(savedEpgKey)) {
			showEpg(date, hsEpg[savedEpgKey])
			showBottomEpg()
			return
		}
		get(url, object : CallBackString() {
			override fun onFailure(code: Int, errorMessage: String?) {
			}

			override fun onResponse(response: String?) {
				i("echo-epgTagName:$channelNameReal")
				val arrayList = mutableListOf<EpgInfo>()
				try {
					if ((response ?: return).contains("epg_data")) {
						val jSONArray = JSONObject(response).optJSONArray("epg_data")
						if (jSONArray != null) for (b in 0..<jSONArray.length()) {
							val jSONObject = jSONArray.getJSONObject(b)
							val epgBcInfo = EpgInfo(date, jSONObject.optString("title"), date, jSONObject.optString("start"), jSONObject.optString("end"), b)
							arrayList.add(epgBcInfo)
						}
					}
				} catch (jSONException: JSONException) {
					jSONException.printStackTrace()
				}
				hsEpg[savedEpgKey] = arrayList
				showEpg(date, arrayList)
				showBottomEpg()
			}
		})
	}

	private fun selectedEpgDate(): LiveEpgDate? {
		val adapter = liveEpgDateAdapter ?: return null
		return adapter.getItem(adapter.selectedIndex)
	}

	private fun selectedEpgDateValue(): Date {
		val adapter = liveEpgDateAdapter ?: return Date()
		val selectedIndex = adapter.selectedIndex
		return if (selectedIndex < 0) Date() else adapter.data[selectedIndex].dateParamVal ?: Date()
	}

	//显示底部EPG
	@SuppressLint("SetTextI18n")
	private fun showBottomEpg() {
		if (isSHIYI) {
			return
		}
		val channel = channel_Name ?: return
		if (channel.channelName != null) {
			(tipChName ?: return).text = channel.channelName
			(tvChannelNum ?: return).text = "" + channel.channelNum
			val tvCurrentProgramName = findViewById<TextView>(R.id.tv_current_program_name)
			val tvNextProgramName = findViewById<TextView>(R.id.tv_next_program_name)
			(tipEpg1 ?: return).text = "暂无信息"
			tvCurrentProgramName.text = ""
			(tipEpg2 ?: return).text = "开源测试软件"
			tvNextProgramName.text = ""
			val selectedEpgDate = selectedEpgDate() ?: return
			val savedEpgKey = channel.channelName + "_" + selectedEpgDate.datePresented

			if (hsEpg.containsKey(savedEpgKey)) {
				val arrayList: List<EpgInfo>? = hsEpg[savedEpgKey]
				if (!arrayList.isNullOrEmpty()) {
					val date = Date()
					var size = arrayList.size - 1
					var hasInfo = false
					while (size >= 0) {
						if (date.after(arrayList[size].startDateTime) and date.before(arrayList[size].endDateTime)) {
							(tipEpg1 ?: return).text = arrayList[size].start + "-" + arrayList[size].end
							tvCurrentProgramName.text = arrayList[size].title
							if (size != arrayList.size - 1) {
								(tipEpg2 ?: return).text = arrayList[size + 1].start + "-" + arrayList[size + 1].end
								tvNextProgramName.text = arrayList[size + 1].title
							} else {
								(tipEpg2 ?: return).text = arrayList[size].end + "-23:59"
								tvNextProgramName.text = "精彩节目-暂无节目预告信息"
							}
							hasInfo = true
							break
						} else {
							size--
						}
					}
					if (!hasInfo) {
						(tipEpg1 ?: return).text = "00:00-" + arrayList[0].start
						tvCurrentProgramName.text = "精彩节目-暂无节目预告信息"
						(tipEpg2 ?: return).text = arrayList[0].start + "-" + arrayList[0].end
						tvNextProgramName.text = arrayList[0].title
					}
				}
				(epgListAdapter ?: return).CanBack((currentLiveChannelItem ?: return).includeBack)
				(epgListAdapter ?: return).setNewData(arrayList)
			} else {
				val selectedIndex = (liveEpgDateAdapter ?: return).selectedIndex
				if (selectedIndex < 0) getEpg(Date())
			}

			if (countDownTimer != null) {
				(countDownTimer ?: return).cancel()
			}
			if ((tipEpg1 ?: return).text != "暂无信息") {
				(llRightTopLoading ?: return).visibility = View.VISIBLE
				(llEpg ?: return).visibility = View.VISIBLE
				countDownTimer = object : CountDownTimer(POST_TIMEOUT.toLong(), 1000) {
					//底部epg隐藏时间设定
					override fun onTick(j: Long) {
					}

					override fun onFinish() {
						(llRightTopLoading ?: return).visibility = View.GONE
						(llRightTopHuiKan ?: return).visibility = View.GONE
						(llEpg ?: return).visibility = View.GONE
					}
				}
				(countDownTimer ?: return).start()
			} else {
				(llRightTopLoading ?: return).visibility = View.GONE
				(llRightTopHuiKan ?: return).visibility = View.GONE
				(llEpg ?: return).visibility = View.GONE
			}
			if (channel.sourceNum <= 0) {
				(findViewById<View?>(R.id.tv_source) as TextView).text = "1/1"
			} else {
				(findViewById<View?>(R.id.tv_source) as TextView).text = "[线路" + (channel.sourceIndex + 1) + "/" + channel.sourceNum + "]"
			}
			(tvRightTopChannelName ?: return).text = channel.channelName
			(tvRightTopEpgName ?: return).text = channel.channelName
		}
	}

	@SuppressLint("SetTextI18n")
	private fun updateChannelIcon(channelName: String?, logoUrl: String?) {
		if (logoUrl.isNullOrEmpty()) {
			(liveIconNullBg ?: return).visibility = View.VISIBLE
			(liveIconNullText ?: return).visibility = View.VISIBLE
			(imgLiveIcon ?: return).visibility = View.INVISIBLE
			(liveIconNullText ?: return).text = "" + (channel_Name ?: return).channelNum
		} else {
			(imgLiveIcon ?: return).visibility = View.VISIBLE
			Picasso.get().load(logoUrl).into(imgLiveIcon)
			(liveIconNullBg ?: return).visibility = View.INVISIBLE
			(liveIconNullText ?: return).visibility = View.INVISIBLE
		}
	}

	//频道列表
	@SuppressLint("NotifyDataSetChanged")
	fun divLoadEpgRight(view: View?) {
		mHandler.removeCallbacks(mHideChannelListRun)
		mHandler.postDelayed(mHideChannelListRun, POST_TIMEOUT.toLong())
		(mChannelGroupView ?: return).visibility = View.GONE
		(divEpg ?: return).visibility = View.VISIBLE
		(divLoadEpgLeft ?: return).visibility = View.VISIBLE
		(divLoadEpg ?: return).visibility = View.GONE
		(mRightEpgList ?: return).selectedPosition = (epgListAdapter ?: return).selectedIndex
		(epgListAdapter ?: return).notifyDataSetChanged()
	}

	//频道列表
	fun divLoadEpgLeft(view: View?) {
		mHandler.removeCallbacks(mHideChannelListRun)
		mHandler.postDelayed(mHideChannelListRun, POST_TIMEOUT.toLong())
		(mChannelGroupView ?: return).visibility = View.VISIBLE
		(divEpg ?: return).visibility = View.GONE
		(divLoadEpgLeft ?: return).visibility = View.GONE
		(divLoadEpg ?: return).visibility = View.VISIBLE
	}

	private fun handleBackPressed() {
		if ((tvLeftChannelListLayout ?: return).isVisible) {
			mHandler.removeCallbacks(mHideChannelListRun)
			mHandler.post(mHideChannelListRun)
		} else if ((tvRightSettingLayout ?: return).isVisible) {
			mHandler.removeCallbacks(mHideSettingLayoutRun)
			mHandler.post(mHideSettingLayoutRun)
		} else if ((backController ?: return).isVisible) { //
			(backController ?: return).visibility = View.GONE
		} else if (isBack) {
			isBack = false
			playPreSource()
		} else {
			mHandler.removeCallbacks(mConnectTimeoutChangeSourceRun)
			mHandler.removeCallbacks(mUpdateNetSpeedRun)
			backPressedCallback?.isEnabled = false
			onBackPressedDispatcher.onBackPressed()
			backPressedCallback?.isEnabled = true
		}
	}

	@SuppressLint("SetTextI18n")
	private fun numericKeyDown(digit: Int) {
		selectedChannelNumber = selectedChannelNumber * 10 + digit
		(tvSelectedChannel ?: return).text = selectedChannelNumber.toString()
		(llRightTopLoading ?: return).visibility = View.GONE
		(llRightTopHuiKan ?: return).visibility = View.GONE
		(tvSelectedChannel ?: return).visibility = View.VISIBLE

		mHandler.removeCallbacks(mPlaySelectedChannel)
		mHandler.postDelayed(mPlaySelectedChannel, 2500)
	}

	override fun dispatchKeyEvent(event: KeyEvent): Boolean {
		val keyCode = event.keyCode
		if (event.action == KeyEvent.ACTION_DOWN) {
			if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_INFO || keyCode == KeyEvent.KEYCODE_HELP) {
				showSettingGroup()
			} else if (!this.isListOrSettingLayoutVisible) {
				when (keyCode) {
					KeyEvent.KEYCODE_DPAD_UP -> if (Hawk.get(HawkConfig.LIVE_CHANNEL_REVERSE, false)) playNext()
					else playPrevious()

					KeyEvent.KEYCODE_DPAD_DOWN -> if (Hawk.get(HawkConfig.LIVE_CHANNEL_REVERSE, false)) playPrevious()
					else playNext()

					KeyEvent.KEYCODE_DPAD_LEFT -> if (isBack) {
						showProgressBars(true)
					} else {
						playPreSource()
					}

					KeyEvent.KEYCODE_DPAD_RIGHT -> if (isBack) {
						showProgressBars(true)
					} else {
						playNextSource()
					}

					KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {}
					else -> {
						val digitOffset = if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) {
							KeyEvent.KEYCODE_0
						} else if (keyCode >= KeyEvent.KEYCODE_NUMPAD_0 && keyCode <= KeyEvent.KEYCODE_NUMPAD_9) {
							KeyEvent.KEYCODE_NUMPAD_0
						} else {
							return super.dispatchKeyEvent(event)
						}
						numericKeyDown(keyCode - digitOffset)
					}
				}
			}
		} else if (event.action == KeyEvent.ACTION_UP) {
			if (!this.isListOrSettingLayoutVisible) {
				if ((keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) && event.repeatCount == 0) {
					showChannelList()
				}
			}
		}
		return super.dispatchKeyEvent(event)
	}

	override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
		if ((keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) && event.repeatCount == 0) {
			//实现长按调出菜单
			val longPressRunnable = Runnable { this.showSettingGroup() }
			mLongPressRunnable = longPressRunnable
			mmHandler.postDelayed(longPressRunnable, LONG_PRESS_DELAY)
		}
		return super.onKeyDown(keyCode, event)
	}

	override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
		if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
			mLongPressRunnable?.let(mmHandler::removeCallbacks)
			mLongPressRunnable = null
		}
		return super.onKeyUp(keyCode, event)
	}

	override fun onResume() {
		super.onResume()
		if (mVideoView != null) {
			(mVideoView ?: return).resume()
		}
	}

	override fun onPause() {
		super.onPause()
		if (mVideoView != null) {
			(mVideoView ?: return).pause()
		}
	}

	override fun onDestroy() {
		super.onDestroy()
		if (mVideoView != null) {
			(mVideoView ?: return).release()
			mVideoView = null
		}
	}

	private fun showChannelList() {
		if (liveChannelGroupList.isEmpty()) return
		if ((tvRightSettingLayout ?: return).isVisible) {
			mHandler.removeCallbacks(mHideSettingLayoutRun)
			mHandler.post(mHideSettingLayoutRun)
			return
		}
		if ((tvLeftChannelListLayout ?: return).isInvisible) {
			if (currentLiveLookBackIndex > -1) {
				(mRightEpgList ?: return).selectedPosition = currentLiveLookBackIndex
				(mRightEpgList ?: return).post { (mRightEpgList ?: return@post).smoothScrollToPosition(currentLiveLookBackIndex) }
			}
			refreshChannelList(currentChannelGroupIndex)

			mHandler.postDelayed(mFocusCurrentChannelAndShowChannelList, 50)
		} else {
			mHandler.removeCallbacks(mHideChannelListRun)
			mHandler.post(mHideChannelListRun)
		}
	}

	private fun refreshChannelList(currentChannelGroupIndex: Int) {
		val newChannels = getLiveChannels(currentChannelGroupIndex)
		// 2. 判断数据是否变化
		if (currentChannelGroupIndex == mLastChannelGroupIndex
			&& isSameData(newChannels, mLastChannelList)
		) {
			return  // 数据未变化，跳过刷新 解决部分直播频道过多时卡顿
		}
		if (currentLiveChannelIndex > -1) {
			(mLiveChannelView ?: return).scrollToPosition(currentLiveChannelIndex)
			(mLiveChannelView ?: return).setSelection(currentLiveChannelIndex)
		}
		(mChannelGroupView ?: return).scrollToPosition(currentChannelGroupIndex)
		(mChannelGroupView ?: return).setSelection(currentChannelGroupIndex)
		mLastChannelGroupIndex = currentChannelGroupIndex
		mLastChannelList = newChannels.toList()
		(liveChannelItemAdapter ?: return).setNewData(newChannels)
	}

	// 对比两个列表内容是否相同
	private fun isSameData(list1: List<LiveChannelItem>, list2: List<LiveChannelItem>): Boolean {
//        return list1.size() == list2.size();
		if (list1 === list2) return true
		if (list1.size != list2.size) return false
		for (i in list1.indices) {
			if (list1[i] != list2[i]) {
				return false
			}
		}
		return true
	}

	private fun showChannelInfo() {
		(tvChannelInfo ?: return).text = String.format(
			Locale.getDefault(), "%d %s %s(%d/%d)", (currentLiveChannelItem ?: return).channelNum,
			(currentLiveChannelItem ?: return).channelName, (currentLiveChannelItem ?: return).sourceName,
			(currentLiveChannelItem ?: return).sourceIndex + 1, (currentLiveChannelItem ?: return).sourceNum
		)

		val lParams = FrameLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
		if ((tvRightSettingLayout ?: return).isVisible) {
			lParams.gravity = Gravity.START
			lParams.leftMargin = 60
			lParams.topMargin = 30
		} else {
			lParams.gravity = Gravity.END
			lParams.rightMargin = 60
			lParams.topMargin = 30
		}
		(tvChannelInfo ?: return).layoutParams = lParams

		(tvChannelInfo ?: return).visibility = View.VISIBLE
		mHandler.removeCallbacks(mHideChannelInfoRun)
		mHandler.postDelayed(mHideChannelInfoRun, 3000)
	}

	private fun initLiveObj() {
		val position = Hawk.get(HawkConfig.LIVE_GROUP_INDEX, 0)
		val liveGroups = Hawk.get(HawkConfig.LIVE_GROUP_LIST, JsonArray())
		val livesOBJ = liveGroups.get(position).getAsJsonObject()
		val type = if (livesOBJ.has("type")) livesOBJ.get("type").asString else "0"

		if (livesOBJ.has("catchup")) {
			catchup = livesOBJ.getAsJsonObject("catchup")
			i("echo-catchup :$catchup")
			hasCatchup = true
		}
		if (livesOBJ.has("logo")) {
			logoUrl = livesOBJ.get("logo").asString
		}
		if (type == "3") {
			var pyJar = ""
			if (livesOBJ.has("jar")) {
				pyJar = if (livesOBJ.has("jar")) livesOBJ.get("jar").asString else ""
			} else if (livesOBJ.has("api")) {
				pyJar = if (livesOBJ.has("api")) livesOBJ.get("api").asString else ""
				//                String ext = livesOBJ.has("ext")?livesOBJ.get("ext").getAsJsonObject().toString():"";
				val ext = if (livesOBJ.has("ext") && (livesOBJ.get("ext").isJsonObject || livesOBJ.get("ext").isJsonArray)) {
					livesOBJ.get("ext").toString()
				} else {
					safeJsonString(livesOBJ, "ext", "")
				}
				i("echo-ext:$ext")
				if (!ext.isEmpty()) pyJar = "$pyJar?extend=$ext"
			}
			ApiConfig.instance.setLiveJar(pyJar)
		}
	}

	private fun liveWebHeader(): HashMap<String, String>? {
		return Hawk.get<HashMap<String, String>?>(HawkConfig.LIVE_WEB_HEADER)
	}

	private fun playChannel(channelGroupIndex: Int, liveChannelIndex: Int, changeSource: Boolean): Boolean {
		val currentItem = currentLiveChannelItem
		if ((channelGroupIndex == currentChannelGroupIndex && liveChannelIndex == currentLiveChannelIndex && !changeSource)
			|| (changeSource && currentItem?.sourceNum == 1)
		) {
			// showChannelInfo();
			return true
		}

		val videoView = mVideoView ?: return false
		videoView.release()
		val selectedItem: LiveChannelItem
		if (!changeSource) {
			currentChannelGroupIndex = channelGroupIndex
			currentLiveChannelIndex = liveChannelIndex
			selectedItem = getLiveChannels(currentChannelGroupIndex)[currentLiveChannelIndex]
			currentLiveChannelItem = selectedItem
			Hawk.put(HawkConfig.LIVE_CHANNEL, selectedItem.channelName)
			livePlayerManager.getLiveChannelPlayer(videoView, selectedItem.channelName)
		} else {
			selectedItem = currentItem ?: return false
		}

		channel_Name = selectedItem
		currentLiveLookBackIndex = -1
		(epgListAdapter ?: return false).setSelectedEpgIndex(-1)
		isSHIYI = false
		isBack = false
		val channelUrl = selectedItem.url ?: return false
		selectedItem.includeBack = hasCatchup || channelUrl.contains("PLTV/") || channelUrl.contains("TVOD/")
		showBottomEpg()
		getEpg(Date())
		(backController ?: return false).visibility = View.GONE
		(llRightTopHuiKan ?: return false).visibility = View.GONE
		if (liveWebHeader() != null) i("echo-" + liveWebHeader().toString())
		videoView.setUrl(channelUrl, liveWebHeader())
		videoView.start()
		return true
	}

	private fun playNext() {
		if (!this.isCurrentLiveChannelValid) return
		val groupChannelIndex = getNextChannel(1)
		playChannel(groupChannelIndex[0], groupChannelIndex[1], false)
	}

	private fun playPrevious() {
		if (!this.isCurrentLiveChannelValid) return
		val groupChannelIndex = getNextChannel(-1)
		playChannel(groupChannelIndex[0], groupChannelIndex[1], false)
	}

	fun playPreSource() {
		if (!this.isCurrentLiveChannelValid) return
		(currentLiveChannelItem ?: return).preSource()
		playChannel(currentChannelGroupIndex, currentLiveChannelIndex, true)
	}

	fun playNextSource() {
		if (!this.isCurrentLiveChannelValid) return
		(currentLiveChannelItem ?: return).nextSource()
		playChannel(currentChannelGroupIndex, currentLiveChannelIndex, true)
	}

	//显示设置列表
	private fun showSettingGroup() {
		if ((tvLeftChannelListLayout ?: return).isVisible) {
			mHandler.removeCallbacks(mHideChannelListRun)
			mHandler.post(mHideChannelListRun)
		}
		if ((tvRightSettingLayout ?: return).isInvisible) {
			if (!this.isCurrentLiveChannelValid) return
			//重新载入默认状态
			loadCurrentSourceList()
			(liveSettingGroupAdapter ?: return).setNewData(liveSettingGroupList)
			selectSettingGroup(0, false)
			(mSettingGroupView ?: return).scrollToPosition(0)
			(mSettingItemView ?: return).scrollToPosition((currentLiveChannelItem ?: return).sourceIndex)
			mHandler.postDelayed(mFocusAndShowSettingGroup, 50)
		} else {
			mHandler.removeCallbacks(mHideSettingLayoutRun)
			mHandler.post(mHideSettingLayoutRun)
		}
	}

	//laodao 7天Epg数据绑定和展示
	private fun initEpgListView() {
		(mRightEpgList ?: return).setHasFixedSize(true)
		(mRightEpgList ?: return).setLayoutManager(V7LinearLayoutManager(this.mContext, 1, false))
		epgListAdapter = LiveEpgAdapter()
		(mRightEpgList ?: return).adapter = epgListAdapter

		(mRightEpgList ?: return).addOnScrollListener(object : RecyclerView.OnScrollListener() {
			override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
				super.onScrollStateChanged(recyclerView, newState)
				mHandler.removeCallbacks(mHideChannelListRun)
				mHandler.postDelayed(mHideChannelListRun, POST_TIMEOUT.toLong())
			}
		})
		//电视
		(mRightEpgList ?: return).setOnItemListener(object : OnItemListener {
			override fun onItemPreSelected(parent: TvRecyclerView?, itemView: View?, position: Int) {
				(epgListAdapter ?: return).setFocusedEpgIndex(-1)
			}

			override fun onItemSelected(parent: TvRecyclerView?, itemView: View?, position: Int) {
				mHandler.removeCallbacks(mHideChannelListRun)
				mHandler.postDelayed(mHideChannelListRun, POST_TIMEOUT.toLong())
				(epgListAdapter ?: return).setFocusedEpgIndex(position)
			}

			@SuppressLint("NotifyDataSetChanged")
			override fun onItemClick(parent: TvRecyclerView?, itemView: View?, position: Int) {
				if (position == currentLiveLookBackIndex) return
				currentLiveLookBackIndex = position
				val date = selectedEpgDateValue()
				val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
				dateFormat.timeZone = TimeZone.getTimeZone("GMT+8:00")
				val selectedData = (epgListAdapter ?: return).getItem(position)
				val targetDate = dateFormat.format(date)
				checkNotNull(selectedData)
				val channelUrl = (currentLiveChannelItem ?: return).url ?: return
				val shiyiStartDate = targetDate + selectedData.originStart.replace(":", "") + "30"
				val shiyiEndDate = targetDate + selectedData.originEnd.replace(":", "") + "30"
				val now = Date()
				if (Date() < selectedData.startDateTime) {
					return
				}
				(epgListAdapter ?: return).setSelectedEpgIndex(position)
				if (now >= selectedData.startDateTime && now <= selectedData.endDateTime) {
					(mVideoView ?: return).release()
					isSHIYI = false
					(mVideoView ?: return).setUrl(channelUrl, liveWebHeader())
					(mVideoView ?: return).start()
					(epgListAdapter ?: return).setShiyiSelection(-1, false, timeFormat.format(date))
					(epgListAdapter ?: return).notifyDataSetChanged()
					showProgressBars(false)
					return
				}
				var shiyiUrl = channelUrl
				if (now >= selectedData.startDateTime) {
					if (hasCatchup || shiyiUrl.contains("PLTV/") || shiyiUrl.contains("TVOD/")) {
						shiyiUrl = shiyiUrl.replace("/PLTV/".toRegex(), "/TVOD/")
						mHandler.removeCallbacks(mHideChannelListRun)
						mHandler.postDelayed(mHideChannelListRun, 100)
						(mVideoView ?: return).release()
						shiyi_time = "$shiyiStartDate-$shiyiEndDate"
						isSHIYI = true
						//mCanSeek=true;
						if (hasCatchup) {
							val replace = (catchup ?: return).get("replace").asString
							val source = (catchup ?: return).get("source").asString
							val parts = replace.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
							val left = if (parts.isNotEmpty()) parts[0].trim { it <= ' ' } else ""
							val right = if (parts.size > 1) parts[1].trim { it <= ' ' } else ""
							shiyiUrl = shiyiUrl.replace(left.toRegex(), right)
							// 已知参数
							val startHHmm = selectedData.originStart.replace(":", "")
							val endHHmm = selectedData.originEnd.replace(":", "")
							// 正则表达式：匹配 ${(b)...} 或 ${(e)...}
							val pattern = getPattern("\\$\\{\\((b|e)\\)(.*?)\\}")
							val matcher = pattern.matcher(source)
							val valueMap: MutableMap<String, String> = HashMap()
							valueMap["b"] = targetDate + "T" + startHHmm
							valueMap["e"] = targetDate + "T" + endHHmm
							val result = StringBuffer()
							while (matcher.find()) {
								val type = matcher.group(1) ?: continue // 捕获 b 或 e
								// 生成替换值（如 "20231023T1500"）
								val replacement: String = checkNotNull(valueMap[type])
								matcher.appendReplacement(result, replacement)
							}
							matcher.appendTail(result)
							i("echo-shiyiurl:$shiyiUrl")
							if (shiyiUrl.endsWith("&")) shiyiUrl = shiyiUrl.substring(0, shiyiUrl.length - 1)
							shiyiUrl += result.toString()
						} else {
							if (shiyiUrl.indexOf("?") <= 0) {
								shiyiUrl += "?playseek=$shiyi_time"
							} else if (shiyiUrl.indexOf("playseek") > 0) {
								shiyiUrl = shiyiUrl.replace("playseek=(.*)".toRegex(), "playseek=$shiyi_time")
							} else {
								shiyiUrl += "&playseek=$shiyi_time"
							}
						}
						i("echo-回看地址playUrl :$shiyiUrl")
						playUrl = shiyiUrl

						(mVideoView ?: return).setUrl(playUrl, liveWebHeader())
						(mVideoView ?: return).start()
						(epgListAdapter ?: return).setShiyiSelection(position, true, timeFormat.format(date))
						(epgListAdapter ?: return).notifyDataSetChanged()
						(mRightEpgList ?: return).selectedPosition = position
						(mRightEpgList ?: return).post { (mRightEpgList ?: return@post).smoothScrollToPosition(position) }
						shiyi_time_c = getTime(formatDate.format(nowDay) + " " + selectedData.start + ":" + "30", formatDate.format(nowDay) + " " + selectedData.end + ":" + "30").toInt()
						val lp = (ivPlay ?: return).layoutParams
						lp.width = videoHeight / 7
						lp.height = videoHeight / 7
						sBar = findViewById(R.id.pb_progressbar)
						(sBar ?: return).max = shiyi_time_c * 1000
						(sBar ?: return).progress = (mVideoView ?: return).getCurrentPosition().toInt()
						(tvCurrentPos ?: return).text = durationToString((mVideoView ?: return).getCurrentPosition().toInt())
						(tvDuration ?: return).text = durationToString(shiyi_time_c * 1000)
						showProgressBars(true)
						isBack = true
					}
				}
			}
		})

		//手机/模拟器
		(epgListAdapter ?: return).setOnItemClickListener(object : BaseQuickAdapter.OnItemClickListener {
			@SuppressLint("NotifyDataSetChanged")
			override fun onItemClick(adapter: BaseQuickAdapter<*, *>?, view: View?, position: Int) {
				if (position == currentLiveLookBackIndex) return
				currentLiveLookBackIndex = position
				val date = selectedEpgDateValue()
				val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
				dateFormat.timeZone = TimeZone.getTimeZone("GMT+8:00")
				val selectedData = (epgListAdapter ?: return).getItem(position)
				val targetDate = dateFormat.format(date)
				checkNotNull(selectedData)
				val channelUrl = (currentLiveChannelItem ?: return).url ?: return
				i("echo-targetDate$targetDate")
				i("echo-targethm" + selectedData.originStart.replace(":", ""))
				val shiyiStartDate = targetDate + selectedData.originStart.replace(":", "") + "00"
				val shiyiEndDate = targetDate + selectedData.originEnd.replace(":", "") + "00"
				val now = Date()
				if (Date() < selectedData.startDateTime) {
					return
				}
				(epgListAdapter ?: return).setSelectedEpgIndex(position)
				if (now >= selectedData.startDateTime && now <= selectedData.endDateTime) {
					(mVideoView ?: return).release()
					isSHIYI = false
					(mVideoView ?: return).setUrl(channelUrl, liveWebHeader())
					(mVideoView ?: return).start()
					(epgListAdapter ?: return).setShiyiSelection(-1, false, timeFormat.format(date))
					(epgListAdapter ?: return).notifyDataSetChanged()
					showProgressBars(false)
					return
				}
				var shiyiUrl = channelUrl
				if (now >= selectedData.startDateTime) {
					if (hasCatchup || shiyiUrl.contains("PLTV/") || shiyiUrl.contains("TVOD/")) {
						shiyiUrl = shiyiUrl.replace("/PLTV/".toRegex(), "/TVOD/")
						mHandler.removeCallbacks(mHideChannelListRun)
						mHandler.postDelayed(mHideChannelListRun, 100)
						(mVideoView ?: return).release()
						shiyi_time = "$shiyiStartDate-$shiyiEndDate"
						isSHIYI = true
						//mCanSeek=true;
						if (hasCatchup) {
							val replace = (catchup ?: return).get("replace").asString
							val source = (catchup ?: return).get("source").asString
							val parts = replace.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
							val left = if (parts.isNotEmpty()) parts[0].trim { it <= ' ' } else ""
							val right = if (parts.size > 1) parts[1].trim { it <= ' ' } else ""
							shiyiUrl = shiyiUrl.replace(left.toRegex(), right)
							val startHHmm = selectedData.originStart.replace(":", "")
							val endHHmm = selectedData.originEnd.replace(":", "")
							// 正则表达式：匹配 ${(b)...} 或 ${(e)...}
							val pattern = getPattern("\\$\\{\\((b|e)\\)(.*?)\\}")
							val matcher = pattern.matcher(source)
							val valueMap: MutableMap<String, String> = HashMap()
							valueMap["b"] = targetDate + "T" + startHHmm
							valueMap["e"] = targetDate + "T" + endHHmm
							val result = StringBuffer()
							while (matcher.find()) {
								val type = matcher.group(1) ?: continue // 捕获 b 或 e
								// 生成替换值（如 "20231023T1500"）
								val replacement: String = checkNotNull(valueMap[type])
								matcher.appendReplacement(result, replacement)
							}
							matcher.appendTail(result)
							i("echo-shiyiurl:$shiyiUrl")
							if (shiyiUrl.endsWith("&")) shiyiUrl = shiyiUrl.substring(0, shiyiUrl.length - 1)
							shiyiUrl += result.toString()
						} else {
							if (shiyiUrl.indexOf("?") <= 0) {
								shiyiUrl += "?playseek=$shiyi_time"
							} else if (shiyiUrl.indexOf("playseek") > 0) {
								shiyiUrl = shiyiUrl.replace("playseek=(.*)".toRegex(), "playseek=$shiyi_time")
							} else {
								shiyiUrl += "&playseek=$shiyi_time"
							}
						}

						i("echo-回看地址playUrl :$shiyiUrl")
						playUrl = shiyiUrl
						if (liveWebHeader() != null) i("echo-liveWebHeader :" + liveWebHeader().toString())
						(mVideoView ?: return).setUrl(playUrl, liveWebHeader())
						(mVideoView ?: return).start()
						(epgListAdapter ?: return).setShiyiSelection(position, true, timeFormat.format(date))
						(epgListAdapter ?: return).notifyDataSetChanged()
						(mRightEpgList ?: return).selectedPosition = position
						(mRightEpgList ?: return).post { (mRightEpgList ?: return@post).smoothScrollToPosition(position) }
						shiyi_time_c = getTime(formatDate.format(nowDay) + " " + selectedData.start + ":" + "00", formatDate.format(nowDay) + " " + selectedData.end + ":" + "00").toInt()
						val lp = (ivPlay ?: return).layoutParams
						lp.width = videoHeight / 7
						lp.height = videoHeight / 7
						sBar = findViewById(R.id.pb_progressbar)
						(sBar ?: return).max = shiyi_time_c * 1000
						(sBar ?: return).progress = (mVideoView ?: return).getCurrentPosition().toInt()
						// long dd = mVideoView.getDuration();
						(tvCurrentPos ?: return).text = durationToString((mVideoView ?: return).getCurrentPosition().toInt())
						(tvDuration ?: return).text = durationToString(shiyi_time_c * 1000)
						showProgressBars(true)
						isBack = true
					}
				}
			}
		})
	}

	//laoda 生成7天回放日期列表数据
	private fun initDayList() {
		liveDayList.clear()
		val dayList = LiveDayListGroup()
		val newDay = Date((nowDay.time))
		val day = formatDate1.format(newDay)
		i("echo-date$day")
		dayList.groupIndex = 0
		dayList.groupName = day
		liveDayList.add(dayList)
	}

	//kens 7天回放数据绑定和展示
	private fun initEpgDateView() {
		(mEpgDateGridView ?: return).setHasFixedSize(true)
		(mEpgDateGridView ?: return).setLayoutManager(V7LinearLayoutManager(this.mContext, 1, false))
		liveEpgDateAdapter = LiveEpgDateAdapter()
		val calendar = Calendar.getInstance()
		calendar.setTime(Date())
		val datePresentFormat = SimpleDateFormat("MM-dd", Locale.getDefault())
		calendar.add(Calendar.DAY_OF_MONTH, 0)
		val dateIns = calendar.getTime()
		val epgDate = LiveEpgDate()
		epgDate.index = 0
		epgDate.datePresented = datePresentFormat.format(dateIns)
		epgDate.dateParamVal = dateIns
		(liveEpgDateAdapter ?: return).addData(epgDate)
		(mEpgDateGridView ?: return).adapter = liveEpgDateAdapter
		(mEpgDateGridView ?: return).addOnScrollListener(object : RecyclerView.OnScrollListener() {
			override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
				super.onScrollStateChanged(recyclerView, newState)
				mHandler.removeCallbacks(mHideChannelListRun)
				mHandler.postDelayed(mHideChannelListRun, POST_TIMEOUT.toLong())
			}
		})
		(liveEpgDateAdapter ?: return).setSelectedIndex(0)
		(mEpgDateGridView ?: return).visibility = View.GONE
	}

	private fun initVideoView() {
		val controller = LiveController(this)
		controller.setListener(object : LiveControlListener {
			override fun singleTap(): Boolean {
				showChannelList()
				return true
			}

			override fun longPress() {
				if (isBack) {  //手机换源和显示时移控制栏
					showProgressBars(true)
				} else {
					showSettingGroup()
				}
			}

			override fun playStateChanged(playState: Int) {
				mHandler.removeCallbacks(mConnectTimeoutChangeSourceRun)
				when (playState) {
					VideoView.STATE_IDLE, VideoView.STATE_PAUSED -> {}
					VideoView.STATE_PREPARED, VideoView.STATE_BUFFERED, VideoView.STATE_PLAYING ->                         // 播放状态：当播放器缓冲完成或正在正常播放时，表明当前源是可用的，
						currentLiveChangeSourceTimes = 0

					VideoView.STATE_ERROR, VideoView.STATE_PLAYBACK_COMPLETED ->                         // 错误或播放结束状态：播放器遇到错误或播放完毕时，
						// 启动自动换源任务，等待3秒后尝试切换至备选源
						mHandler.postDelayed(mConnectTimeoutChangeSourceRun, 3500)

					VideoView.STATE_PREPARING, VideoView.STATE_BUFFERING ->                         // 正在准备或缓冲状态：表示当前源正在加载中
						mHandler.postDelayed(mConnectTimeoutChangeSourceRun, (Hawk.get(HawkConfig.LIVE_CONNECT_TIMEOUT, 1) + 1) * 5000L)

					else -> i("echo-Unexpected live_play state: $playState")
				}
			}

			override fun changeSource(direction: Int) {
				if (direction > 0) if (isBack) {  //手机换源和显示时移控制栏
					showProgressBars(true)
				} else {
					playNextSource()
				}
				else playPreSource()
			}
		})
		controller.setCanChangePosition(false)
		controller.setEnableInNormal(true)
		controller.setGestureEnabled(true)
		controller.setDoubleTapTogglePlayEnabled(false)
		(mVideoView ?: return).setVideoController(controller)
		(mVideoView ?: return).setProgressManager(null)
	}

	private fun initChannelGroupView() {
		(mChannelGroupView ?: return).setHasFixedSize(true)
		(mChannelGroupView ?: return).setLayoutManager(V7LinearLayoutManager(this.mContext, 1, false))

		liveChannelGroupAdapter = LiveChannelGroupAdapter()
		(mChannelGroupView ?: return).adapter = liveChannelGroupAdapter
		(mChannelGroupView ?: return).addOnScrollListener(object : RecyclerView.OnScrollListener() {
			override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
				super.onScrollStateChanged(recyclerView, newState)
				mHandler.removeCallbacks(mHideChannelListRun)
				mHandler.postDelayed(mHideChannelListRun, POST_TIMEOUT.toLong())
			}
		})

		//电视
		(mChannelGroupView ?: return).setOnItemListener(object : OnItemListener {
			override fun onItemPreSelected(parent: TvRecyclerView?, itemView: View?, position: Int) {
			}

			override fun onItemSelected(parent: TvRecyclerView?, itemView: View?, position: Int) {
				selectChannelGroup(position, true, -1)
			}

			override fun onItemClick(parent: TvRecyclerView?, itemView: View?, position: Int) {
				if (isNeedInputPassword(position)) {
					showPasswordDialog(position, -1)
				}
			}
		})

		//手机/模拟器
		(liveChannelGroupAdapter ?: return).setOnItemClickListener { adapter: BaseQuickAdapter<*, *>?, view: View?, position: Int ->
			FastClickCheckUtil.check(view ?: return@setOnItemClickListener)
			selectChannelGroup(position, false, -1)
		}
	}

	private fun selectChannelGroup(groupIndex: Int, focus: Boolean, liveChannelIndex: Int) {
		mLastChannelGroupIndex = groupIndex
		if (focus) {
			(liveChannelGroupAdapter ?: return).setFocusedGroupIndex(groupIndex)
			(liveChannelItemAdapter ?: return).setFocusedChannelIndex(-1)
		}
		if ((groupIndex > -1 && groupIndex != (liveChannelGroupAdapter ?: return).selectedGroupIndex) || isNeedInputPassword(groupIndex)) {
			(liveChannelGroupAdapter ?: return).setSelectedGroupIndex(groupIndex)
			if (isNeedInputPassword(groupIndex)) {
				showPasswordDialog(groupIndex, liveChannelIndex)
				return
			}
			loadChannelGroupDataAndPlay(groupIndex, liveChannelIndex)
		}
		if ((tvLeftChannelListLayout ?: return).isVisible) {
			mHandler.removeCallbacks(mHideChannelListRun)
			mHandler.postDelayed(mHideChannelListRun, POST_TIMEOUT.toLong())
		}
	}

	private fun initLiveChannelView() {
		(mLiveChannelView ?: return).setHasFixedSize(true)
		(mLiveChannelView ?: return).setLayoutManager(V7LinearLayoutManager(this.mContext, 1, false))

		liveChannelItemAdapter = LiveChannelItemAdapter()
		(mLiveChannelView ?: return).adapter = liveChannelItemAdapter
		(mLiveChannelView ?: return).addOnScrollListener(object : RecyclerView.OnScrollListener() {
			override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
				super.onScrollStateChanged(recyclerView, newState)
				mHandler.removeCallbacks(mHideChannelListRun)
				mHandler.postDelayed(mHideChannelListRun, POST_TIMEOUT.toLong())
			}
		})

		//电视
		(mLiveChannelView ?: return).setOnItemListener(object : OnItemListener {
			override fun onItemPreSelected(parent: TvRecyclerView?, itemView: View?, position: Int) {
			}

			override fun onItemSelected(parent: TvRecyclerView?, itemView: View?, position: Int) {
				if (position < 0) return
				(liveChannelGroupAdapter ?: return).setFocusedGroupIndex(-1)
				(liveChannelItemAdapter ?: return).setFocusedChannelIndex(position)
			}

			override fun onItemClick(parent: TvRecyclerView?, itemView: View?, position: Int) {
				clickLiveChannel(position)
			}
		})

		//手机/模拟器
		(liveChannelItemAdapter ?: return).setOnItemClickListener { adapter: BaseQuickAdapter<*, *>?, view: View?, position: Int ->
			FastClickCheckUtil.check(view ?: return@setOnItemClickListener)
			(liveChannelItemAdapter ?: return@setOnItemClickListener).setSelectedChannelIndex(position)
			clickLiveChannel(position)
		}
	}

	private fun clickLiveChannel(position: Int) {
		if ((tvLeftChannelListLayout ?: return).isVisible) {
			mHandler.removeCallbacks(mHideChannelListRun)
			mHandler.postDelayed(mHideChannelListRun, POST_TIMEOUT.toLong())
		}
		playChannel((liveChannelGroupAdapter ?: return).selectedGroupIndex, position, false)
	}

	private fun initSettingGroupView() {
		(mSettingGroupView ?: return).setHasFixedSize(true)
		(mSettingGroupView ?: return).setLayoutManager(V7LinearLayoutManager(this.mContext, 1, false))

		liveSettingGroupAdapter = LiveSettingGroupAdapter()
		(mSettingGroupView ?: return).adapter = liveSettingGroupAdapter
		(mSettingGroupView ?: return).addOnScrollListener(object : RecyclerView.OnScrollListener() {
			override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
				super.onScrollStateChanged(recyclerView, newState)
				mHandler.removeCallbacks(mHideSettingLayoutRun)
				mHandler.postDelayed(mHideSettingLayoutRun, POST_TIMEOUT.toLong())
			}
		})

		//电视
		(mSettingGroupView ?: return).setOnItemListener(object : OnItemListener {
			override fun onItemPreSelected(parent: TvRecyclerView?, itemView: View?, position: Int) {
			}

			override fun onItemSelected(parent: TvRecyclerView?, itemView: View?, position: Int) {
				selectSettingGroup(position, true)
			}

			override fun onItemClick(parent: TvRecyclerView?, itemView: View?, position: Int) {
			}
		})

		//手机/模拟器
		(liveSettingGroupAdapter ?: return).setOnItemClickListener { adapter: BaseQuickAdapter<*, *>?, view: View?, position: Int ->
			FastClickCheckUtil.check(view ?: return@setOnItemClickListener)
			selectSettingGroup(position, false)
		}
	}

	private fun selectSettingGroup(position: Int, focus: Boolean) {
		if (!this.isCurrentLiveChannelValid) return
		if (focus) {
			(liveSettingGroupAdapter ?: return).setFocusedGroupIndex(position)
			(liveSettingItemAdapter ?: return).setFocusedItemIndex(-1)
		}
		if (position == (liveSettingGroupAdapter ?: return).selectedGroupIndex || position < -1) return

		(liveSettingGroupAdapter ?: return).setSelectedGroupIndex(position)
		(liveSettingItemAdapter ?: return).setNewData(liveSettingGroupList[position].liveSettingItems)

		when (position) {
			0 -> (liveSettingItemAdapter ?: return).selectItem((currentLiveChannelItem ?: return).sourceIndex, true, false)
			1 -> (liveSettingItemAdapter ?: return).selectItem(livePlayerManager.livePlayerScale, true, true)
			2 -> (liveSettingItemAdapter ?: return).selectItem(livePlayerManager.livePlayerType, true, true)
		}
		var scrollToPosition = (liveSettingItemAdapter ?: return).getSelectedItemIndex()
		if (scrollToPosition < 0) scrollToPosition = 0
		(mSettingItemView ?: return).scrollToPosition(scrollToPosition)
		mHandler.removeCallbacks(mHideSettingLayoutRun)
		mHandler.postDelayed(mHideSettingLayoutRun, POST_TIMEOUT.toLong())
	}

	private fun initSettingItemView() {
		(mSettingItemView ?: return).setHasFixedSize(true)
		(mSettingItemView ?: return).setLayoutManager(V7LinearLayoutManager(this.mContext, 1, false))

		liveSettingItemAdapter = LiveSettingItemAdapter()
		(mSettingItemView ?: return).adapter = liveSettingItemAdapter
		(mSettingItemView ?: return).addOnScrollListener(object : RecyclerView.OnScrollListener() {
			override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
				super.onScrollStateChanged(recyclerView, newState)
				mHandler.removeCallbacks(mHideSettingLayoutRun)
				mHandler.postDelayed(mHideSettingLayoutRun, POST_TIMEOUT.toLong())
			}
		})

		//电视
		(mSettingItemView ?: return).setOnItemListener(object : OnItemListener {
			override fun onItemPreSelected(parent: TvRecyclerView?, itemView: View?, position: Int) {
			}

			override fun onItemSelected(parent: TvRecyclerView?, itemView: View?, position: Int) {
				if (position < 0) return
				(liveSettingGroupAdapter ?: return).setFocusedGroupIndex(-1)
				(liveSettingItemAdapter ?: return).setFocusedItemIndex(position)
				mHandler.removeCallbacks(mHideSettingLayoutRun)
				mHandler.postDelayed(mHideSettingLayoutRun, POST_TIMEOUT.toLong())
			}

			override fun onItemClick(parent: TvRecyclerView?, itemView: View?, position: Int) {
				clickSettingItem(position)
			}
		})

		//手机/模拟器
		(liveSettingItemAdapter ?: return).setOnItemClickListener { adapter: BaseQuickAdapter<*, *>?, view: View?, position: Int ->
			FastClickCheckUtil.check(view ?: return@setOnItemClickListener)
			clickSettingItem(position)
		}
	}

	private fun clickSettingItem(position: Int) {
		val settingGroupIndex = (liveSettingGroupAdapter ?: return).selectedGroupIndex
		if (settingGroupIndex < 4) {
			if (position == (liveSettingItemAdapter ?: return).getSelectedItemIndex()) return
			(liveSettingItemAdapter ?: return).selectItem(position, true, true)
		}
		when (settingGroupIndex) {
			0 -> {
				(currentLiveChannelItem ?: return).sourceIndex = position
				playChannel(currentChannelGroupIndex, currentLiveChannelIndex, true)
			}

			1 -> livePlayerManager.changeLivePlayerScale(mVideoView ?: return, position, (currentLiveChannelItem ?: return).channelName)
			2 -> {
				val channel = currentLiveChannelItem ?: return
				val channelUrl = channel.url ?: return
				(mVideoView ?: return).release()
				livePlayerManager.changeLivePlayerType(mVideoView, position, channel.channelName)
				(mVideoView ?: return).setUrl(channelUrl, liveWebHeader())
				(mVideoView ?: return).start()
			}

			3 -> Hawk.put(HawkConfig.LIVE_CONNECT_TIMEOUT, position)
			4 -> {
				var select = false
				when (position) {
					0 -> {
						select = !Hawk.get(HawkConfig.LIVE_SHOW_TIME, false)
						Hawk.put(HawkConfig.LIVE_SHOW_TIME, select)
						showTime()
					}

					1 -> {
						select = !Hawk.get(HawkConfig.LIVE_SHOW_NET_SPEED, false)
						Hawk.put(HawkConfig.LIVE_SHOW_NET_SPEED, select)
						showNetSpeed()
					}

					2 -> {
						select = !Hawk.get(HawkConfig.LIVE_CHANNEL_REVERSE, false)
						Hawk.put(HawkConfig.LIVE_CHANNEL_REVERSE, select)
					}

					3 -> {
						select = !Hawk.get(HawkConfig.LIVE_CROSS_GROUP, false)
						Hawk.put(HawkConfig.LIVE_CROSS_GROUP, select)
					}
				}
				(liveSettingItemAdapter ?: return).selectItem(position, select, false)
			}

			5 -> {
				//TODO
				if (mVideoView != null) {
					(mVideoView ?: return).release()
					mVideoView = null
				}
				if (position == Hawk.get(HawkConfig.LIVE_GROUP_INDEX, 0)) return
				val liveGroups = Hawk.get(HawkConfig.LIVE_GROUP_LIST, JsonArray())
				val livesOBJ = liveGroups.get(position).getAsJsonObject()
				(liveSettingItemAdapter ?: return).selectItem(position, true, true)
				Hawk.put(HawkConfig.LIVE_GROUP_INDEX, position)
				ApiConfig.instance.loadLiveApi(livesOBJ)
				recreate()
				return
			}
		}
		mHandler.removeCallbacks(mHideSettingLayoutRun)
		mHandler.postDelayed(mHideSettingLayoutRun, POST_TIMEOUT.toLong())
	}

	private fun initLiveChannelList() {
		val list = ApiConfig.instance.channelGroupList
		if (list.isEmpty()) {
			setDefaultLiveChannelList()
			return
		}
		initLiveObj()
		if (list.size == 1 && (list[0].groupName ?: return).startsWith("http://127.0.0.1")) {
			loadProxyLives(list[0].groupName ?: return)
		} else {
			liveChannelGroupList.clear()
			liveChannelGroupList.addAll(list)
			showSuccess()
			initLiveState()
		}
	}

	fun loadProxyLives(url: String) {
		var url = url
		try {
			val parsedUrl = url.toUri()
			url = String(Base64.decode(parsedUrl.getQueryParameter("ext"), Base64.DEFAULT or Base64.URL_SAFE or Base64.NO_WRAP), StandardCharsets.UTF_8)
		} catch (th: Throwable) {
			if (!url.startsWith("http://127.0.0.1")) {
				setDefaultLiveChannelList()
				return
			}
		}
		showLoading()

		i("echo-live-url:$url")

		if (url.contains(".py")) {
			if (!hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
				// 权限不足时，直接设置默认播放列表
				Toast.makeText(App.instance, "该源需要存储权限", Toast.LENGTH_SHORT).show()
				setDefaultLiveChannelList()
				return
			}
			val finalUrl = url
			val waitResponse = Runnable {
				val executor = Executors.newSingleThreadExecutor()
				val future = executor.submit(Callable {
					i("echo--loadProxyLives-json--")
					val sp = ApiConfig.instance.getPyCSP(finalUrl)
					val json = sp.liveContent(finalUrl)
					i("echo--loadProxyLives-json--$json")
					json
				})
				var sortJson: String? = null
				try {
					sortJson = future.get(10, TimeUnit.SECONDS)
				} catch (e: TimeoutException) {
					e.printStackTrace()
					future.cancel(true)
				} catch (e: InterruptedException) {
					e.printStackTrace()
				} catch (e: ExecutionException) {
					e.printStackTrace()
				} finally {
					if (sortJson.isNullOrEmpty()) {
						// 频道列表为空时，使用默认播放列表
						mHandler.post { this.setDefaultLiveChannelList() }
					} else {
						val linkedHashMap = LinkedHashMap<String, LinkedHashMap<String, MutableList<String>>>()
						TxtSubscribe.parse(linkedHashMap, sortJson)
						val livesArray = TxtSubscribe.live2JsonArray(linkedHashMap)

						ApiConfig.instance.loadLives(livesArray)
						val list = ApiConfig.instance.channelGroupList
						if (list.isEmpty()) {
							mHandler.post { this.setDefaultLiveChannelList() }
						} else {
							liveChannelGroupList.clear()
							liveChannelGroupList.addAll(list)

							mHandler.post {
								this@LivePlayActivity.showSuccess()
								initLiveState()
							}
							try {
								executor.shutdown()
							} catch (th: Throwable) {
								th.printStackTrace()
							}
						}
					}
				}
			}
			Executors.newSingleThreadExecutor().execute(waitResponse)
		} else {
			OkGo.get<String?>(url).execute(object : AbsCallback<String?>() {
				override fun convertResponse(response: Response): String {
					checkNotNull(response.body)
					return response.body.string()
				}

				override fun onSuccess(response: com.lzy.okgo.model.Response<String?>) {
					val linkedHashMap = LinkedHashMap<String, LinkedHashMap<String, MutableList<String>>>()
					TxtSubscribe.parse(linkedHashMap, response.body() ?: return)
					val livesArray = TxtSubscribe.live2JsonArray(linkedHashMap)

					ApiConfig.instance.loadLives(livesArray)
					val list = ApiConfig.instance.channelGroupList
					if (list.isEmpty()) {
						mHandler.post { setDefaultLiveChannelList() }
						return
					}
					liveChannelGroupList.clear()
					liveChannelGroupList.addAll(list)

					mHandler.post {
						this@LivePlayActivity.showSuccess()
						initLiveState()
					}
				}

				override fun onError(response: com.lzy.okgo.model.Response<String?>?) {
					mHandler.post { setDefaultLiveChannelList() }
				}
			})
		}
	}

	private fun initLiveState() {
		val lastChannelName = Hawk.get(HawkConfig.LIVE_CHANNEL, "")

		var lastChannelGroupIndex = -1
		var lastLiveChannelIndex = -1
		for (liveChannelGroup in liveChannelGroupList) {
			for (liveChannelItem in liveChannelGroup.liveChannels ?: return) {
				if (liveChannelItem.channelName == lastChannelName) {
					lastChannelGroupIndex = liveChannelGroup.groupIndex
					lastLiveChannelIndex = liveChannelItem.channelIndex
					break
				}
			}
			if (lastChannelGroupIndex != -1) break
		}
		if (lastChannelGroupIndex == -1) {
			lastChannelGroupIndex = this.firstNoPasswordChannelGroup
			if (lastChannelGroupIndex == -1) lastChannelGroupIndex = 0
			lastLiveChannelIndex = 0
		}

		livePlayerManager.init(mVideoView)
		showTime()
		showNetSpeed()
		(tvLeftChannelListLayout ?: return).visibility = View.INVISIBLE
		(tvRightSettingLayout ?: return).visibility = View.INVISIBLE

		(liveChannelGroupAdapter ?: return).setNewData(liveChannelGroupList)
		selectChannelGroup(lastChannelGroupIndex, false, lastLiveChannelIndex)
	}

	private val isListOrSettingLayoutVisible: Boolean
		get() = tvLeftChannelListLayout?.isVisible == true || tvRightSettingLayout?.isVisible == true

	private fun initLiveSettingGroupList() {
		liveSettingGroupList = ApiConfig.instance.liveSettingGroupList
		(liveSettingGroupList[3].liveSettingItems ?: return)[Hawk.get(HawkConfig.LIVE_CONNECT_TIMEOUT, 1)].isItemSelected = true
		(liveSettingGroupList[4].liveSettingItems ?: return)[0].isItemSelected = Hawk.get(HawkConfig.LIVE_SHOW_TIME, false)
		(liveSettingGroupList[4].liveSettingItems ?: return)[1].isItemSelected = Hawk.get(HawkConfig.LIVE_SHOW_NET_SPEED, false)
		(liveSettingGroupList[4].liveSettingItems ?: return)[2].isItemSelected = Hawk.get(HawkConfig.LIVE_CHANNEL_REVERSE, false)
		(liveSettingGroupList[4].liveSettingItems ?: return)[3].isItemSelected = Hawk.get(HawkConfig.LIVE_CROSS_GROUP, false)
		(liveSettingGroupList[5].liveSettingItems ?: return)[Hawk.get(HawkConfig.LIVE_GROUP_INDEX, 0)].isItemSelected = true
	}

	private fun loadCurrentSourceList() {
		val currentSourceNames = (currentLiveChannelItem ?: return).channelSourceNames
		val liveSettingItemList = mutableListOf<LiveSettingItem>()
		for (j in (currentSourceNames ?: return).indices) {
			val liveSettingItem = LiveSettingItem()
			liveSettingItem.itemIndex = j
			liveSettingItem.itemName = currentSourceNames[j]
			liveSettingItemList.add(liveSettingItem)
		}
		liveSettingGroupList[0].liveSettingItems = liveSettingItemList
	}

	fun showTime() {
		if (Hawk.get(HawkConfig.LIVE_SHOW_TIME, false)) {
			mHandler.post(mUpdateTimeRun)
			(tvTime ?: return).visibility = View.VISIBLE
		} else {
			mHandler.removeCallbacks(mUpdateTimeRun)
			(tvTime ?: return).visibility = View.GONE
		}
	}

	private fun showNetSpeed() {
//        tv_right_top_tipnetspeed.setVisibility(View.VISIBLE);
		if (Hawk.get(HawkConfig.LIVE_SHOW_NET_SPEED, false)) {
			mHandler.post(mUpdateNetSpeedRun)
			(tvNetSpeed ?: return).visibility = View.VISIBLE
		} else {
			mHandler.removeCallbacks(mUpdateNetSpeedRun)
			(tvNetSpeed ?: return).visibility = View.GONE
		}
	}

	private fun showPasswordDialog(groupIndex: Int, liveChannelIndex: Int) {
		if ((tvLeftChannelListLayout ?: return).isVisible) mHandler.removeCallbacks(mHideChannelListRun)

		val dialog = LivePasswordDialog(this)
		dialog.setOnListener(object : LivePasswordDialog.OnListener {
			override fun onChange(password: String) {
				if (password == liveChannelGroupList[groupIndex].groupPassword) {
					channelGroupPasswordConfirmed.add(groupIndex)
					loadChannelGroupDataAndPlay(groupIndex, liveChannelIndex)
				} else {
					Toast.makeText(App.instance, "密码错误", Toast.LENGTH_SHORT).show()
				}

				if ((tvLeftChannelListLayout ?: return).isVisible) mHandler.postDelayed(mHideChannelListRun, POST_TIMEOUT.toLong())
			}

			override fun onCancel() {
				if ((tvLeftChannelListLayout ?: return).isVisible) {
					val groupIndex = (liveChannelGroupAdapter ?: return).selectedGroupIndex
					(liveChannelItemAdapter ?: return).setNewData(getLiveChannels(groupIndex))
				}
			}
		})
		dialog.show()
	}

	private fun loadChannelGroupDataAndPlay(groupIndex: Int, liveChannelIndex: Int) {
		(liveChannelItemAdapter ?: return).setNewData(getLiveChannels(groupIndex))
		if (groupIndex == currentChannelGroupIndex) {
			if (currentLiveChannelIndex > -1) (mLiveChannelView ?: return).scrollToPosition(currentLiveChannelIndex)
			(liveChannelItemAdapter ?: return).setSelectedChannelIndex(currentLiveChannelIndex)
		} else {
			(mLiveChannelView ?: return).scrollToPosition(0)
			(liveChannelItemAdapter ?: return).setSelectedChannelIndex(-1)
		}

		if (liveChannelIndex > -1) {
			clickLiveChannel(liveChannelIndex)
			(mChannelGroupView ?: return).scrollToPosition(groupIndex)
			(mLiveChannelView ?: return).scrollToPosition(liveChannelIndex)
			playChannel(groupIndex, liveChannelIndex, false)
		}
	}

	private fun isNeedInputPassword(groupIndex: Int): Boolean {
		return !liveChannelGroupList[groupIndex].groupPassword.isNullOrEmpty()
				&& !isPasswordConfirmed(groupIndex)
	}

	private fun isPasswordConfirmed(groupIndex: Int): Boolean {
		for (confirmedNum in channelGroupPasswordConfirmed) {
			if (confirmedNum == groupIndex) return true
		}
		return false
	}

	private fun getLiveChannels(groupIndex: Int): List<LiveChannelItem> {
		return if (!isNeedInputPassword(groupIndex)) {
			liveChannelGroupList[groupIndex].liveChannels.orEmpty()
		} else {
			emptyList()
		}
	}

	private fun getNextChannel(direction: Int): IntArray {
		var channelGroupIndex: Int = currentChannelGroupIndex
		var liveChannelIndex = currentLiveChannelIndex

		//跨选分组模式下跳过加密频道分组（遥控器上下键换台/超时换源）
		if (direction > 0) {
			liveChannelIndex++
			if (liveChannelIndex >= getLiveChannels(channelGroupIndex).size) {
				liveChannelIndex = 0
				if (Hawk.get(HawkConfig.LIVE_CROSS_GROUP, false)) {
					do {
						channelGroupIndex++
						if (channelGroupIndex >= liveChannelGroupList.size) channelGroupIndex = 0
					} while (!liveChannelGroupList[channelGroupIndex].groupPassword.isNullOrEmpty() || channelGroupIndex == currentChannelGroupIndex)
				}
			}
		} else {
			liveChannelIndex--
			if (liveChannelIndex < 0) {
				if (Hawk.get(HawkConfig.LIVE_CROSS_GROUP, false)) {
					do {
						channelGroupIndex--
						if (channelGroupIndex < 0) channelGroupIndex = liveChannelGroupList.size - 1
					} while (!liveChannelGroupList[channelGroupIndex].groupPassword.isNullOrEmpty() || channelGroupIndex == currentChannelGroupIndex)
				}
				liveChannelIndex = getLiveChannels(channelGroupIndex).size - 1
			}
		}

		return intArrayOf(channelGroupIndex, liveChannelIndex)
	}

	private val firstNoPasswordChannelGroup: Int
		get() {
			for (liveChannelGroup in liveChannelGroupList) {
				if (liveChannelGroup.groupPassword.isNullOrEmpty()) return liveChannelGroup.groupIndex
			}
			return -1
		}

	private val isCurrentLiveChannelValid: Boolean
		get() {
			if (currentLiveChannelItem == null) {
				Toast.makeText(App.instance, "请先选择频道", Toast.LENGTH_SHORT).show()
				return false
			}
			return true
		}

	private fun durationToString(duration: Int): String {
		val result: String
		val dur = duration / 1000
		val hour = dur / 3600
		val min = (dur / 60) % 60
		val sec = dur % 60
		if (hour > 0) {
			result = if (min > 9) {
				if (sec > 9) {
					"$hour:$min:$sec"
				} else {
					"$hour:$min:0$sec"
				}
			} else {
				if (sec > 9) {
					"$hour:0$min:$sec"
				} else {
					"$hour:0$min:0$sec"
				}
			}
		} else {
			result = if (min > 9) {
				if (sec > 9) {
					"$min:$sec"
				} else {
					"$min:0$sec"
				}
			} else {
				if (sec > 9) {
					"0$min:$sec"
				} else {
					"0$min:0$sec"
				}
			}
		}
		return result
	}

	fun showProgressBars(show: Boolean) {
		(sBar ?: return).requestFocus()
		if (show) {
			(llRightTopHuiKan ?: return).visibility = View.VISIBLE
			(backController ?: return).visibility = View.VISIBLE
			(llEpg ?: return).visibility = View.GONE
		} else {
			(backController ?: return).visibility = View.GONE
			(llRightTopHuiKan ?: return).visibility = View.GONE
			if ((tipEpg1 ?: return).text != "暂无信息") {
				(llEpg ?: return).visibility = View.VISIBLE
			}
		}


		(ivPlay ?: return).setOnClickListener { arg0: View? ->
			(mVideoView ?: return@setOnClickListener).start()
			(ivPlay ?: return@setOnClickListener).visibility = View.INVISIBLE
			(countDownTimer ?: return@setOnClickListener).start()
			(ivPlayPause ?: return@setOnClickListener).background = ContextCompat.getDrawable(this@LivePlayActivity, R.drawable.vod_pause)
		}

		(ivPlayPause ?: return).setOnClickListener { arg0: View? ->
			if ((mVideoView ?: return@setOnClickListener).isPlaying) {
				(mVideoView ?: return@setOnClickListener).pause()
				(countDownTimer ?: return@setOnClickListener).cancel()
				(ivPlay ?: return@setOnClickListener).visibility = View.VISIBLE
				(ivPlayPause ?: return@setOnClickListener).background = ContextCompat.getDrawable(this@LivePlayActivity, R.drawable.icon_play)
			} else {
				(mVideoView ?: return@setOnClickListener).start()
				(ivPlay ?: return@setOnClickListener).visibility = View.INVISIBLE
				(countDownTimer ?: return@setOnClickListener).start()
				(ivPlayPause ?: return@setOnClickListener).background = ContextCompat.getDrawable(this@LivePlayActivity, R.drawable.vod_pause)
			}
		}
		(sBar ?: return).setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
			override fun onStopTrackingTouch(arg0: SeekBar?) {
			}

			override fun onStartTrackingTouch(arg0: SeekBar?) {
			}

			override fun onProgressChanged(sb: SeekBar?, progress: Int, fromuser: Boolean) {
				if (fromuser) {
					if (countDownTimer != null) {
						(mVideoView ?: return).seekTo(progress.toLong())
						(countDownTimer ?: return).cancel()
						(countDownTimer ?: return).start()
					}
				}
			}
		})
		(sBar ?: return).setOnKeyListener { _: View?, keycode: Int, event: KeyEvent? ->
			if (event?.action == KeyEvent.ACTION_DOWN && (keycode == KeyEvent.KEYCODE_DPAD_CENTER || keycode == KeyEvent.KEYCODE_ENTER)) {
				toggleReplayPause()
			}
			false
		}
		if ((mVideoView ?: return).isPlaying) {
			(ivPlay ?: return).visibility = View.INVISIBLE
			(ivPlayPause ?: return).background = ContextCompat.getDrawable(this, R.drawable.vod_pause)
		} else {
			(ivPlay ?: return).visibility = View.VISIBLE
			(ivPlayPause ?: return).background = ContextCompat.getDrawable(this, R.drawable.icon_play)
		}
		if (countDownTimer3 == null) {
			countDownTimer3 = object : CountDownTimer(POST_TIMEOUT.toLong(), 1000) {
				override fun onTick(arg0: Long) {
					if (mVideoView != null) {
						(sBar ?: return).progress = (mVideoView ?: return).getCurrentPosition().toInt()
						(tvCurrentPos ?: return).text = durationToString((mVideoView ?: return).getCurrentPosition().toInt())
					}
				}

				override fun onFinish() {
					if ((backController ?: return).isVisible) {
						(backController ?: return).visibility = View.GONE
					}
				}
			}
		} else {
			(countDownTimer3 ?: return).cancel()
		}
		(countDownTimer3 ?: return).start()
	}

	/**
	 * 当播放列表为空或加载失败时，设置一个默认的播放列表，保证播放界面不会崩溃
	 */
	private fun setDefaultLiveChannelList() {
		liveChannelGroupList.clear()
		// 创建默认直播分组
		val defaultGroup = LiveChannelGroup()
		defaultGroup.groupIndex = 0
		defaultGroup.groupName = "default group"
		defaultGroup.groupPassword = ""
		val defaultChannel = LiveChannelItem()
		defaultChannel.channelName = "default channel"
		defaultChannel.channelIndex = 0
		defaultChannel.channelNum = 1
		val defaultSourceNames = ArrayList<String>()
		val defaultSourceUrls = ArrayList<String>()
		defaultSourceNames.add("default source")
		defaultSourceUrls.add("http://default.play.url/stream")
		defaultChannel.channelSourceNames = defaultSourceNames
		defaultChannel.channelUrls = defaultSourceUrls
		// 将默认频道添加到分组内
		val channels = ArrayList<LiveChannelItem>()
		channels.add(defaultChannel)
		defaultGroup.liveChannels = channels
		// 添加分组到全局列表
		liveChannelGroupList.add(defaultGroup)
		showSuccess()
		initLiveState()
	}

	companion object {
		//laodao 7day replay
		val formatDate: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
		val formatDate1: SimpleDateFormat = SimpleDateFormat("MM-dd", Locale.getDefault())
		private const val POST_TIMEOUT = 6000
		private const val LONG_PRESS_DELAY: Long = 800
		private val hsEpg = Hashtable<String, MutableList<EpgInfo>>()
		var currentChannelGroupIndex: Int = 0
		var day: String = formatDate.format(Date())
		var nowDay: Date = Date()
		var playUrl: String? = null

		//EPG   by 龍
		private var channel_Name: LiveChannelItem? = null
		private var shiyi_time: String? = null //时移时间
		private var shiyi_time_c = 0 //时移时间差值

		//计算两个时间相差的秒数
		fun getTime(startTime: String, endTime: String): Long {
			val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
			var eTime: Long = 0
			try {
				eTime = df.parse(endTime)?.time ?: 0L
			} catch (e: ParseException) {
				e.printStackTrace()
			}
			var sTime: Long = 0
			try {
				sTime = df.parse(startTime)?.time ?: 0L
			} catch (e: ParseException) {
				e.printStackTrace()
			}
			return (eTime - sTime) / 1000
		}
	}
}
