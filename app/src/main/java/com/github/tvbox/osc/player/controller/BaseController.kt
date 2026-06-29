package com.github.tvbox.osc.player.controller

import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.loadingindicator.LoadingIndicator
import xyz.doikki.videoplayer.controller.BaseVideoController
import xyz.doikki.videoplayer.controller.IGestureComponent
import xyz.doikki.videoplayer.player.VideoView
import xyz.doikki.videoplayer.util.PlayerUtils
import kotlin.math.abs

abstract class BaseController : BaseVideoController, GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener, OnTouchListener {
	protected var mHandler: Handler? = null
	protected var mHandlerCallback: HandlerCallback? = null
	private var mGestureDetector: GestureDetector? = null
	private var mAudioManager: AudioManager? = null
	private var mIsGestureEnabled = true
	private var mStreamVolume = 0
	private var mBrightness = 0f
	private var mSeekPosition = 0
	private var mFirstTouch = false
	private var mChangePosition = false
	private var mChangeBrightness = false
	private var mChangeVolume = false
	private var mCanChangePosition = true
	private var mEnableInNormal = false
	private var mCanSlide = false
	private var mCurPlayState = 0
	private var mIsDoubleTapTogglePlayEnabled = true
	private var mSlideInfo: TextView? = null
	private var mLoading: LoadingIndicator? = null
	private var mPauseRoot: ViewGroup? = null
	private var mPauseTime: TextView? = null

	constructor(context: Context) : super(context) {
		mHandler = Handler(Looper.getMainLooper()) { msg: Message ->
			when (msg.what) {
				100 -> {
					// 亮度+音量调整
					mSlideInfo?.apply {
						visibility = VISIBLE
						text = msg.obj.toString()
					}
				}

				101 -> {
					// 亮度+音量调整 关闭
					mSlideInfo?.visibility = GONE
				}

				else -> {
					mHandlerCallback?.callback(msg)
				}
			}
			false
		}
	}

	constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

	constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

	override fun initView() {
		super.initView()
		mAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
		mGestureDetector = GestureDetector(context, this)
		setOnTouchListener(this)
		mSlideInfo = findViewWithTag("vod_control_slide_info")
		mLoading = findViewWithTag("vod_control_loading")
		mPauseRoot = findViewWithTag("vod_control_pause")
		mPauseTime = findViewWithTag("vod_control_pause_t")
	}

	override fun setProgress(duration: Int, position: Int) {
		super.setProgress(duration, position)
		mPauseTime?.text = "${PlayerUtils.stringForTime(position)} / ${PlayerUtils.stringForTime(duration)}"
	}

	override fun onPlayStateChanged(playState: Int) {
		super.onPlayStateChanged(playState)
		when (playState) {
			VideoView.STATE_IDLE, VideoView.STATE_PREPARED, VideoView.STATE_ERROR, VideoView.STATE_BUFFERED -> mLoading?.visibility = GONE
			VideoView.STATE_PLAYING -> {
				mPauseRoot?.visibility = GONE
				mLoading?.visibility = GONE
			}

			VideoView.STATE_PAUSED -> {
				mPauseRoot?.visibility = VISIBLE
				mLoading?.visibility = GONE
			}

			VideoView.STATE_PREPARING, VideoView.STATE_BUFFERING -> mLoading?.visibility = VISIBLE
			VideoView.STATE_PLAYBACK_COMPLETED -> {
				mLoading?.visibility = GONE
				mPauseRoot?.visibility = GONE
			}
		}
	}

	/**
	 * 设置是否可以滑动调节进度，默认可以
	 */
	fun setCanChangePosition(canChangePosition: Boolean) {
		mCanChangePosition = canChangePosition
	}

	/**
	 * 是否在竖屏模式下开始手势控制，默认关闭
	 */
	fun setEnableInNormal(enableInNormal: Boolean) {
		mEnableInNormal = enableInNormal
	}

	/**
	 * 是否开启手势控制，默认开启，关闭之后，手势调节进度，音量，亮度功能将关闭
	 */
	fun setGestureEnabled(gestureEnabled: Boolean) {
		mIsGestureEnabled = gestureEnabled
	}

	/**
	 * 是否开启双击播放/暂停，默认开启
	 */
	fun setDoubleTapTogglePlayEnabled(enabled: Boolean) {
		mIsDoubleTapTogglePlayEnabled = enabled
	}

	override fun setPlayerState(playerState: Int) {
		super.setPlayerState(playerState)
		if (playerState == VideoView.PLAYER_NORMAL) {
			mCanSlide = mEnableInNormal
		} else if (playerState == VideoView.PLAYER_FULL_SCREEN) {
			mCanSlide = true
		}
	}

	override fun setPlayState(playState: Int) {
		super.setPlayState(playState)
		mCurPlayState = playState
	}

	protected val isInPlaybackState: Boolean
		get() = mControlWrapper != null && mCurPlayState != VideoView.STATE_ERROR && mCurPlayState != VideoView.STATE_IDLE && mCurPlayState != VideoView.STATE_PREPARING && mCurPlayState != VideoView.STATE_PREPARED && mCurPlayState != VideoView.STATE_START_ABORT && mCurPlayState != VideoView.STATE_PLAYBACK_COMPLETED

	override fun onTouch(p0: View?, p1: MotionEvent?): Boolean {
		return mGestureDetector?.onTouchEvent(p1 ?: return false) ?: false
	}

	/**
	 * 手指按下的瞬间
	 */
	override fun onDown(p0: MotionEvent): Boolean {
		if (!this.isInPlaybackState //不处于播放状态
			|| !mIsGestureEnabled //关闭了手势
			|| PlayerUtils.isEdge(context, p0)
		)  //处于屏幕边沿
			return true
		mStreamVolume = mAudioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
		val activity = PlayerUtils.scanForActivity(context)
		mBrightness = activity?.window?.attributes?.screenBrightness ?: 0f
		mFirstTouch = true
		mChangePosition = false
		mChangeBrightness = false
		mChangeVolume = false
		return true
	}

	/**
	 * 单击
	 */
	override fun onSingleTapConfirmed(p0: MotionEvent): Boolean {
		if (this.isInPlaybackState) {
			mControlWrapper.toggleShowState()
		}
		return true
	}

	/**
	 * 双击
	 */
	override fun onDoubleTap(p0: MotionEvent): Boolean {
		if (mIsDoubleTapTogglePlayEnabled && !isLocked && this.isInPlaybackState) togglePlay()
		return true
	}

	/**
	 * 在屏幕上滑动
	 */
	override fun onScroll(p0: MotionEvent?, p1: MotionEvent, p2: Float, p3: Float): Boolean {
		if (!this.isInPlaybackState //不处于播放状态
			|| !mIsGestureEnabled //关闭了手势
			|| !mCanSlide //关闭了滑动手势
			|| isLocked //锁住了屏幕
			|| PlayerUtils.isEdge(context, p0)
		)  //处于屏幕边沿
			return true
		val deltaX = (p0?.x ?: 0).toInt() - p1.x
		val deltaY = (p0?.y ?: 0).toInt() - p1.y
		if (mFirstTouch) {
			mChangePosition = abs(p2) >= abs(p3)
			if (!mChangePosition) {
				//半屏宽度
				val halfScreen = PlayerUtils.getScreenWidth(context, true) / 2
				if (p1.x > halfScreen) {
					mChangeVolume = true
				} else {
					mChangeBrightness = true
				}
			}

			if (mChangePosition) {
				//根据用户设置是否可以滑动调节进度来决定最终是否可以滑动调节进度
				mChangePosition = mCanChangePosition
			}

			if (mChangePosition || mChangeBrightness || mChangeVolume) {
				for (next in mControlComponents.entries) {
					val component = next.key
					if (component is IGestureComponent) {
						component.onStartSlide()
					}
				}
			}
			mFirstTouch = false
		}
		if (mChangePosition) {
			slideToChangePosition(deltaX)
		} else if (mChangeBrightness) {
			slideToChangeBrightness(deltaY)
		} else if (mChangeVolume) {
			slideToChangeVolume(deltaY)
		}
		return true
	}

	protected fun slideToChangePosition(deltaX: Float) {
		val adjustedDelta = -deltaX
		val width = measuredWidth
		val duration = mControlWrapper.duration.toInt()
		val currentPosition = mControlWrapper.currentPosition.toInt()
		var position = (adjustedDelta / width * 120000 + currentPosition).toInt()
		if (position > duration) position = duration
		if (position < 0) position = 0
		for (next in mControlComponents.entries) {
			val component = next.key
			if (component is IGestureComponent) {
				component.onPositionChange(position, currentPosition, duration)
			}
		}
		updateSeekUI(currentPosition, position, duration)
		mSeekPosition = position
	}

	protected open fun updateSeekUI(curr: Int, seekTo: Int, duration: Int) {
	}

	protected fun slideToChangeBrightness(deltaY: Float) {
		val activity = PlayerUtils.scanForActivity(context) ?: return
		val window = activity.window
		val attributes = window.attributes
		val height = measuredHeight
		if (mBrightness == -1.0f) mBrightness = 0.5f
		var brightness = deltaY * 2 / height + mBrightness
		if (brightness < 0) {
			brightness = 0f
		}
		if (brightness > 1.0f) brightness = 1.0f
		val percent = (brightness * 100).toInt()
		attributes.screenBrightness = brightness
		window.attributes = attributes
		for (next in mControlComponents.entries) {
			val component = next.key
			if (component is IGestureComponent) {
				component.onBrightnessChange(percent)
			}
		}
		val msg = Message.obtain()
		msg.what = 100
		msg.obj = "亮度$percent%"
		val handler = mHandler ?: return
		handler.sendMessage(msg)
		handler.removeMessages(101)
		handler.sendEmptyMessageDelayed(101, 1000)
	}

	protected fun slideToChangeVolume(deltaY: Float) {
		val audioManager = mAudioManager ?: return
		val streamMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
		val height = measuredHeight
		val deltaV = deltaY * 2 / height * streamMaxVolume
		var index = mStreamVolume + deltaV
		if (index > streamMaxVolume) index = streamMaxVolume.toFloat()
		if (index < 0) index = 0f
		val percent = (index / streamMaxVolume * 100).toInt()
		audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, index.toInt(), 0)
		for (next in mControlComponents.entries) {
			val component = next.key
			if (component is IGestureComponent) {
				component.onVolumeChange(percent)
			}
		}
		val msg = Message.obtain()
		msg.what = 100
		msg.obj = "音量$percent%"
		val handler = mHandler ?: return
		handler.sendMessage(msg)
		handler.removeMessages(101)
		handler.sendEmptyMessageDelayed(101, 1000)
	}

	override fun onTouchEvent(event: MotionEvent?): Boolean {
		//滑动结束时事件处理
		if (event != null) {
			val detector = mGestureDetector ?: return super.onTouchEvent(event)
			if (!detector.onTouchEvent(event)) {
				val action = event.action
				when (action) {
					MotionEvent.ACTION_UP -> {
						stopSlide()
						if (mSeekPosition > 0) {
							mControlWrapper.seekTo(mSeekPosition.toLong())
							mSeekPosition = 0
						}
					}

					MotionEvent.ACTION_CANCEL -> {
						stopSlide()
						mSeekPosition = 0
					}
				}
			}
		}
		return super.onTouchEvent(event)
	}

	private fun stopSlide() {
		for (next in mControlComponents.entries) {
			val component = next.key
			if (component is IGestureComponent) {
				component.onStopSlide()
			}
		}
	}

	override fun onFling(p0: MotionEvent?, p1: MotionEvent, p2: Float, p3: Float): Boolean {
		return false
	}

	override fun onLongPress(p0: MotionEvent) {
	}

	override fun onShowPress(p0: MotionEvent) {
	}

	override fun onDoubleTapEvent(p0: MotionEvent): Boolean {
		return false
	}

	override fun onSingleTapUp(p0: MotionEvent): Boolean {
		return false
	}

	open fun onKeyEvent(event: KeyEvent?): Boolean {
		return false
	}

	protected fun interface HandlerCallback {
		fun callback(msg: Message?)
	}
}
