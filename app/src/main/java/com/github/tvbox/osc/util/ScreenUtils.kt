package com.github.tvbox.osc.util

import android.app.Activity
import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.telephony.TelephonyManager
import android.util.DisplayMetrics
import kotlin.math.pow
import kotlin.math.sqrt

object ScreenUtils {
	fun getSqrt(activity: Activity): Double {
		val wm = activity.windowManager
		val dm = DisplayMetrics()
		wm.defaultDisplay.getMetrics(dm)
		val x = (dm.widthPixels / dm.xdpi).toDouble().pow(2.0)
		val y = (dm.heightPixels / dm.ydpi).toDouble().pow(2.0)
		// 屏幕尺寸
		return sqrt(x + y)
	}

	private fun checkScreenLayoutIsTv(context: Context): Boolean {
		return (context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) > Configuration.SCREENLAYOUT_SIZE_LARGE
	}

	private fun checkIsPhone(context: Context): Boolean {
		val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
		return telephonyManager.phoneType != TelephonyManager.PHONE_TYPE_NONE
	}

	fun isTv(context: Context): Boolean {
		val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
		return uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION || (checkScreenLayoutIsTv(context) && !checkIsPhone(context))
	}
}
