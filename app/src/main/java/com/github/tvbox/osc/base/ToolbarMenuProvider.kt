package com.github.tvbox.osc.base

import androidx.annotation.MenuRes

interface ToolbarMenuProvider {
	@get:MenuRes
	val menuResId: Int
		get() = 0

	val toolbarTitle: String?
		get() = null

	fun onMenuItemClick(itemId: Int): Boolean {
		return false
	}

	fun enableAppBarScroll(): Boolean {
		return false
	}
}
