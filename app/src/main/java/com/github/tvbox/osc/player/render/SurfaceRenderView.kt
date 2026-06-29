package com.github.tvbox.osc.player.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import xyz.doikki.videoplayer.player.AbstractPlayer
import xyz.doikki.videoplayer.render.IRenderView
import xyz.doikki.videoplayer.render.MeasureHelper

class SurfaceRenderView : SurfaceView, IRenderView, SurfaceHolder.Callback {
	private val mMeasureHelper: MeasureHelper = MeasureHelper()
	private var mMediaPlayer: AbstractPlayer? = null

	init {
		holder.addCallback(this)
		holder.setFormat(PixelFormat.RGBA_8888)
	}

	constructor(context: Context) : super(context)

	constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

	constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

	override fun attachToPlayer(player: AbstractPlayer) {
		this.mMediaPlayer = player
	}

	override fun setVideoSize(videoWidth: Int, videoHeight: Int) {
		if (videoWidth > 0 && videoHeight > 0) {
			mMeasureHelper.setVideoSize(videoWidth, videoHeight)
			requestLayout()
		}
	}

	override fun setVideoRotation(degree: Int) {
		mMeasureHelper.setVideoRotation(degree)
		rotation = degree.toFloat()
	}

	override fun setScaleType(scaleType: Int) {
		mMeasureHelper.setScreenScale(scaleType)
		requestLayout()
	}

	override fun getView(): View {
		return this
	}

	override fun doScreenShot(): Bitmap? {
		return null
	}

	override fun release() {
	}

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		val measuredSize = mMeasureHelper.doMeasure(widthMeasureSpec, heightMeasureSpec)
		setMeasuredDimension(measuredSize[0], measuredSize[1])
	}

	override fun surfaceCreated(p0: SurfaceHolder) {
	}

	override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {
		mMediaPlayer?.setDisplay(p0)
	}

	override fun surfaceDestroyed(p0: SurfaceHolder) {
	}
}
