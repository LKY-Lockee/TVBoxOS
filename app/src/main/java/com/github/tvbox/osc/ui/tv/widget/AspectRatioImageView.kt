package com.github.tvbox.osc.ui.tv.widget

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

class AspectRatioImageView : AppCompatImageView {
	private var aspectRatio = 0f // 宽高比 (宽/高)，0表示使用默认或图片实际比例

	constructor(context: Context) : super(context)

	constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

	constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

	fun setAspectRatio(ratio: Float) {
		if (this.aspectRatio != ratio) {
			this.aspectRatio = ratio
			requestLayout()
		}
	}

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec)

		val width = measuredWidth
		if (width > 0) {
			val height: Int

			if (aspectRatio > 0) {
				height = (width / aspectRatio).toInt()
			} else {
				val drawable = getDrawable()
				if (drawable != null && drawable.intrinsicWidth > 0 && drawable.intrinsicHeight > 0) {
					val imageRatio = drawable.intrinsicWidth.toFloat() / drawable.intrinsicHeight
					height = (width / imageRatio).toInt()
				} else {
					val defaultAspectRatio = 214f / 280f
					height = (width / defaultAspectRatio).toInt()
				}
			}

			setMeasuredDimension(width, height)
		}
	}

	override fun setImageDrawable(drawable: Drawable?) {
		super.setImageDrawable(drawable)
		if (aspectRatio == 0f) {
			post { this.requestLayout() }
		}
	}
}
