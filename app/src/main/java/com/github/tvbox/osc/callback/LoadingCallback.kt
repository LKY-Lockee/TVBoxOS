package com.github.tvbox.osc.callback

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import com.github.tvbox.osc.R
import com.kingja.loadsir.callback.Callback
import kotlin.math.max

/**
 * @author pj567
 * @date 2020/12/24
 */
class LoadingCallback : Callback() {
	override fun onCreateView(): Int {
		return R.layout.loadsir_loading_layout
	}

	override fun onAttach(context: Context?, view: View) {
		super.onAttach(context, view)
		val loadingIndicator = view.findViewById<View>(R.id.loading_indicator)
		if (loadingIndicator != null) {
			val updatePosition = Runnable {
				if (loadingIndicator.height == 0 || !loadingIndicator.isAttachedToWindow) {
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

				val indicatorHeight = loadingIndicator.height
				val targetTop = contentCenterY - viewTop - indicatorHeight / 2

				val layoutParams = loadingIndicator.layoutParams as FrameLayout.LayoutParams
				layoutParams.gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
				layoutParams.topMargin = max(0, targetTop)
				loadingIndicator.layoutParams = layoutParams
			}

			loadingIndicator.viewTreeObserver.addOnGlobalLayoutListener { updatePosition.run() }
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
