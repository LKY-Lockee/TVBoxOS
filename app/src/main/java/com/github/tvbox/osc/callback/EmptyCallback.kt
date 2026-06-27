package com.github.tvbox.osc.callback

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.github.tvbox.osc.R
import com.kingja.loadsir.callback.Callback
import kotlin.math.max

/**
 * @author pj567
 * @date 2020/12/24
 */
class EmptyCallback : Callback() {
	override fun onCreateView(): Int {
		return R.layout.loadsir_empty_layout
	}

	override fun onAttach(context: Context?, view: View) {
		super.onAttach(context, view)
		val contentLayout = view.findViewById<LinearLayout>(R.id.empty_content)
		if (contentLayout != null) {
			val updatePosition = Runnable {
				if (contentLayout.height == 0 || !contentLayout.isAttachedToWindow) {
					return@Runnable
				}
				// 获取整个应用的内容区域
				val activity = getActivityFromContext(context) ?: return@Runnable

				val contentView = activity.findViewById<View>(android.R.id.content)
				if (contentView == null || contentView.height == 0) return@Runnable

				// 获取中心位置
				val contentLocation = IntArray(2)
				contentView.getLocationOnScreen(contentLocation)
				val contentCenterY = contentLocation[1] + contentView.height / 2

				val viewLocation = IntArray(2)
				view.getLocationOnScreen(viewLocation)
				val viewTop = viewLocation[1]

				val contentHeight = contentLayout.height
				val targetTop = contentCenterY - viewTop - contentHeight / 2

				val layoutParams = contentLayout.layoutParams as FrameLayout.LayoutParams
				layoutParams.gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
				layoutParams.topMargin = max(0, targetTop)
				contentLayout.layoutParams = layoutParams
			}

			contentLayout.viewTreeObserver.addOnGlobalLayoutListener { updatePosition.run() }
		}
	}

	private fun getActivityFromContext(context: Context?): Activity? {
		var context = context
		while (context is ContextWrapper) {
			if (context is Activity) {
				return context
			}
			context = context.baseContext
		}
		return null
	}
}
