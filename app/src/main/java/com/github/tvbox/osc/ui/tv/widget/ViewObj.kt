package com.github.tvbox.osc.ui.tv.widget

import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.annotation.Keep

/**
 * @author pj567
 * @since 2020/7/28
 */
class ViewObj(private val view: View, private val params: MarginLayoutParams) {
	@Keep
	fun setMarginLeft(left: Int) {
		params.leftMargin = left
		view.layoutParams = params
	}

	fun setMarginTop(top: Int) {
		params.topMargin = top
		view.layoutParams = params
	}

	@Keep
	fun setMarginRight(right: Int) {
		params.rightMargin = right
		view.layoutParams = params
	}

	fun setMarginBottom(bottom: Int) {
		params.bottomMargin = bottom
		view.layoutParams = params
	}

	fun setWidth(width: Int) {
		params.width = width
		view.layoutParams = params
	}

	fun setHeight(height: Int) {
		params.height = height
		view.layoutParams = params
	}
}
