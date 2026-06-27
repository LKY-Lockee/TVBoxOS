package com.undcover.freedom.pyramid

import android.content.Context
import android.widget.Toast

/**
 * Created by UndCover on 16/9/7.
 */
object PyToast {
	private var innerToast: Toast? = null

	/**
	 * 快速显示Toast,无需排队等待
	 */
	fun showCancelableToast(context: Context, msg: String?, duration: Int = Toast.LENGTH_SHORT) {
		innerToast?.cancel()
		innerToast = Toast.makeText(context, msg, duration)
		innerToast?.show()
	}
}
