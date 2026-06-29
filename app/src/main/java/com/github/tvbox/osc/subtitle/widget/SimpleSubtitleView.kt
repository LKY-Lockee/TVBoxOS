/*
 *                       Copyright (C) of Avery
 *
 *                              _ooOoo_
 *                             o8888888o
 *                             88" . "88
 *                             (| -_- |)
 *                             O\  =  /O
 *                          ____/`- -'\____
 *                        .'  \\|     |//  `.
 *                       /  \\|||  :  |||//  \
 *                      /  _||||| -:- |||||-  \
 *                      |   | \\\  -  /// |   |
 *                      | \_|  ''\- -/''  |   |
 *                      \  .-\__  `-`  ___/-. /
 *                    ___`. .' /- -.- -\  `. . __
 *                 ."" '<  `.___\_<|>_/___.'  >'"".
 *                | | :  `- \`.;`\ _ /`;.`/ - ` : | |
 *                \  \ `-.   \_ __\ /__ _/   .-` /  /
 *           ======`-.____`-.___\_____/___.-`____.-'======
 *                              `=- -='
 *           ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
 *              Buddha bless, there will never be bug!!!
 */

package com.github.tvbox.osc.subtitle.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.Html
import android.text.TextUtils
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.TextView
import com.github.tvbox.osc.cache.CacheManager.delete
import com.github.tvbox.osc.subtitle.DefaultSubtitleEngine
import com.github.tvbox.osc.subtitle.SubtitleEngine
import com.github.tvbox.osc.subtitle.SubtitleEngine.OnSubtitleChangeListener
import com.github.tvbox.osc.subtitle.SubtitleEngine.OnSubtitlePreparedListener
import com.github.tvbox.osc.subtitle.model.Subtitle
import com.github.tvbox.osc.util.MD5
import xyz.doikki.videoplayer.player.AbstractPlayer

/**
 * @author AveryZhong.
 */
@SuppressLint("AppCompatCustomView")
class SimpleSubtitleView : TextView, SubtitleEngine, OnSubtitleChangeListener, OnSubtitlePreparedListener {
	var isInternal: Boolean = false
	var hasInternal: Boolean = false
	private val backGroundText: TextView //用于描边的TextView
	private val mSubtitleEngine: SubtitleEngine = DefaultSubtitleEngine()

	constructor(context: Context?) : super(context) {
		backGroundText = TextView(context)
		init()
	}

	constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
		backGroundText = TextView(context, attrs)
		init()
	}

	constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
		backGroundText = TextView(context, attrs, defStyleAttr)
		init()
	}

	private fun init() {
		mSubtitleEngine.setOnSubtitlePreparedListener(this)
		mSubtitleEngine.setOnSubtitleChangeListener(this)
	}

	override fun onSubtitlePrepared(subtitles: List<Subtitle>?) {
		start()
	}

	override fun onSubtitleChanged(subtitle: Subtitle?) {
		if (subtitle == null) {
			text = ""
			return
		}
		var text = subtitle.content
		text = text.replace("\\r\\n".toRegex(), "<br />")
		text = text.replace("\\r".toRegex(), "<br />")
		text = text.replace("\\n".toRegex(), "<br />")
		text = text.replace("\\\\N".toRegex(), "<br />")
		text = text.replace("\\{[\\s\\S]*?\\}".toRegex(), "")
		text = text.replace("^.*?,.*?,.*?,.*?,.*?,.*?,.*?,.*?,.*?,".toRegex(), "")
		setText(Html.fromHtml(text))
	}

	override fun setSubtitlePath(path: String) {
		isInternal = false
		mSubtitleEngine.setSubtitlePath(path)
	}

	override fun setSubtitleDelay(milliseconds: Int) {
		mSubtitleEngine.setSubtitleDelay(milliseconds)
	}

	override var playSubtitleCacheKey: String?
		get() = mSubtitleEngine.playSubtitleCacheKey
		set(value) {
			mSubtitleEngine.playSubtitleCacheKey = value
		}

	fun clearSubtitleCache() {
		val subtitleCacheKey = playSubtitleCacheKey
		if (!subtitleCacheKey.isNullOrEmpty()) {
			delete(MD5.string2MD5(subtitleCacheKey), "")
		}
	}

	override fun reset() {
		mSubtitleEngine.reset()
	}

	override fun start() {
		mSubtitleEngine.start()
	}

	override fun pause() {
		mSubtitleEngine.pause()
	}

	override fun resume() {
		mSubtitleEngine.resume()
	}

	override fun stop() {
		mSubtitleEngine.stop()
	}

	override fun destroy() {
		mSubtitleEngine.destroy()
	}

	override fun bindToMediaPlayer(mediaPlayer: AbstractPlayer) {
		mSubtitleEngine.bindToMediaPlayer(mediaPlayer)
	}

	override fun setOnSubtitlePreparedListener(listener: OnSubtitlePreparedListener) {
		mSubtitleEngine.setOnSubtitlePreparedListener(listener)
	}

	override fun setOnSubtitleChangeListener(listener: OnSubtitleChangeListener) {
		mSubtitleEngine.setOnSubtitleChangeListener(listener)
	}

	override fun onDetachedFromWindow() {
		destroy()
		super.onDetachedFromWindow()
	}

	override fun setLayoutParams(params: ViewGroup.LayoutParams?) {
		// 同步布局参数
		backGroundText.layoutParams = params
		super.setLayoutParams(params)
	}

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		val tt = backGroundText.text
		// 两个TextView上的文字必须一致
		if (TextUtils.isEmpty(tt) || tt != this.text) {
			backGroundText.text = text
			this.postInvalidate()
		}
		backGroundText.measure(widthMeasureSpec, heightMeasureSpec)
		super.onMeasure(widthMeasureSpec, heightMeasureSpec)
	}

	override fun setTextSize(size: Float) {
		super.setTextSize(size)
		backGroundText.textSize = size
	}

	override fun onTextChanged(text: CharSequence?, start: Int, lengthBefore: Int, lengthAfter: Int) {
		backGroundText.text = text
		super.onTextChanged(text, start, lengthBefore, lengthAfter)
	}

	override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
		backGroundText.layout(left, top, right, bottom)
		super.onLayout(changed, left, top, right, bottom)
	}

	override fun onDraw(canvas: Canvas) {
		// 其他地方，backGroundText和super的先后顺序影响不会很大，但是此处必须要先绘制backGroundText，
		drawBackGroundText()
		backGroundText.draw(canvas)
		super.onDraw(canvas)
	}

	private fun drawBackGroundText() {
		val tp = backGroundText.paint
		// 设置描边宽度
		tp.strokeWidth = 10f
		// 背景描边并填充全部
		tp.style = Paint.Style.FILL_AND_STROKE
		// 设置描边颜色
		backGroundText.setTextColor(Color.BLACK)
		// 将背景的文字对齐方式做同步
		backGroundText.gravity = gravity
	}
}
