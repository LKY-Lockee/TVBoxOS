package com.github.tvbox.osc.ui.dialog

import android.app.Dialog
import android.content.Context
import android.view.View
import android.view.WindowManager

open class BaseDialog(context: Context) : Dialog(context) {
	override fun show() {
		window?.setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
		super.show()
		hideSysBar()
		window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
	}

	private fun hideSysBar() {
		var uiOptions = window?.decorView?.systemUiVisibility
		if (uiOptions != null) {
			uiOptions = uiOptions or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
			uiOptions = uiOptions or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
			uiOptions = uiOptions or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
			uiOptions = uiOptions or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
			uiOptions = uiOptions or View.SYSTEM_UI_FLAG_FULLSCREEN
			uiOptions = uiOptions or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
			window?.decorView?.systemUiVisibility = uiOptions
		}
	}
}
