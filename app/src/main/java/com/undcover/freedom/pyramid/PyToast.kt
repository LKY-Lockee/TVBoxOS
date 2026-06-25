package com.undcover.freedom.pyramid

import android.content.*
import android.widget.*

/**
 * Created by UndCover on 16/9/7.
 */
object PyToast {
    private var innerToast: Toast? = null

    /**
     * 快速显示Toast,无需排队等待
     * 
     * @param msg
     * @param duration
     */
    fun showCancelableToast(context: Context, msg: String?, duration: Int = Toast.LENGTH_SHORT) {
        innerToast?.cancel()
        innerToast = Toast.makeText(context, msg, duration)
        innerToast?.show()
    }
}
