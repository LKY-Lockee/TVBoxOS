package com.github.tvbox.osc.ui.tv.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Message
import android.util.AttributeSet
import android.view.View
import java.util.Random

class AudioWaveView : View {
	/**
	 * 条间距
	 */
	private val space = 8
	private val handler: Handler = object : Handler() {
		override fun handleMessage(msg: Message) {
			invalidate()
		}
	}
	private var paint = Paint()
	private var rectF1 = RectF()
	private var rectF2 = RectF()
	private var rectF3 = RectF()
	private var rectF4 = RectF()
	private var rectF5 = RectF()
	private var viewHeight = 0

	/**
	 * 每个条的宽度
	 */
	private var rectWidth = 0
	private var random = Random()

	constructor(context: Context?) : super(context) {
		init()
	}

	constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
		init()
	}

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec)

		val viewWidth = MeasureSpec.getSize(widthMeasureSpec)
		viewHeight = MeasureSpec.getSize(heightMeasureSpec)

		/**
		 * 条数
		 */
		val columnCount = 7
		rectWidth = (viewWidth - space * (columnCount - 1)) / columnCount
	}

	private fun init() {
		paint.color = Color.RED //字节跳动颜色
		paint.style = Paint.Style.FILL
	}

	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)

		val left = rectWidth + space

		//画每个条之前高度都重新随机生成
		/**
		 * 条随机高度
		 */
		var randomHeight = random.nextInt(viewHeight)
		rectF1.set(0f, randomHeight.toFloat(), rectWidth.toFloat(), viewHeight.toFloat())
		randomHeight = random.nextInt(viewHeight)
		rectF2.set(left.toFloat(), randomHeight.toFloat(), (left + rectWidth).toFloat(), viewHeight.toFloat())
		randomHeight = random.nextInt(viewHeight)
		rectF3.set((left * 2).toFloat(), randomHeight.toFloat(), (left * 2 + rectWidth).toFloat(), viewHeight.toFloat())
		randomHeight = random.nextInt(viewHeight)
		rectF4.set((left * 3).toFloat(), randomHeight.toFloat(), (left * 3 + rectWidth).toFloat(), viewHeight.toFloat())
		randomHeight = random.nextInt(viewHeight)
		rectF5.set((left * 4).toFloat(), randomHeight.toFloat(), (left * 4 + rectWidth).toFloat(), viewHeight.toFloat())

		canvas.drawRect(rectF1, paint)
		canvas.drawRect(rectF2, paint)
		canvas.drawRect(rectF3, paint)
		canvas.drawRect(rectF4, paint)
		canvas.drawRect(rectF5, paint)

		handler.sendEmptyMessageDelayed(0, 200) //每间隔200毫秒发送消息刷新
	}
}
