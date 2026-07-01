package com.github.tvbox.osc.ui.tv.widget

import android.annotation.SuppressLint
import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.widget.TextView

/**
 * Created by acer on 2018/7/13.
 */
@SuppressLint("AppCompatCustomView")
class MarqueeTextView @JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : TextView(context, attrs, defStyleAttr) {
	init {
		isSelected = true
		isSingleLine = true
		marqueeRepeatLimit = -1
		ellipsize = TextUtils.TruncateAt.MARQUEE
	}

	override fun isFocused(): Boolean {
		return true
	}
}
