package com.github.tvbox.osc.player.controller

import android.content.Context
import android.view.MotionEvent
import android.widget.ProgressBar
import com.github.tvbox.osc.R
import kotlin.math.abs

/**
 * 直播控制器
 */
open class LiveController(context: Context) : BaseController(context) {
	protected var mLoading: ProgressBar? = null
	private var listener: LiveControlListener? = null

	override fun getLayoutId(): Int {
		return R.layout.player_live_control_view
	}

	override fun initView() {
		super.initView()
		mLoading = findViewById(R.id.loading)
	}

	fun setListener(listener: LiveControlListener?) {
		this.listener = listener
	}

	override fun onSingleTapConfirmed(p0: MotionEvent): Boolean {
		if (listener?.singleTap() == true) return true
		return super.onSingleTapConfirmed(p0)
	}

	override fun onLongPress(p0: MotionEvent) {
		listener?.longPress()
		super.onLongPress(p0)
	}

	override fun onPlayStateChanged(playState: Int) {
		super.onPlayStateChanged(playState)
		listener?.playStateChanged(playState)
	}

	override fun onFling(p0: MotionEvent?, p1: MotionEvent, p2: Float, p3: Float): Boolean {
		//最小识别速度
		val minFlingVelocity = 10
		//最小识别距离
		val minFlingDistance = 100
		val startX = (p0?.x ?: 0).toInt()
		val l = listener ?: return false
		if (startX - p1.x > minFlingDistance && abs(p2) > minFlingVelocity) {
			l.changeSource(-1) //左滑
		} else if (p1.x - startX > minFlingDistance && abs(p2) > minFlingVelocity) {
			l.changeSource(1) //右滑
		}
		return false
	}

	interface LiveControlListener {
		fun singleTap(): Boolean

		fun longPress()

		fun playStateChanged(playState: Int)

		fun changeSource(direction: Int)
	}
}
