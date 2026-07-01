package com.github.tvbox.osc.ui.tv.widget

import com.chad.library.adapter.base.loadmore.LoadMoreView
import com.github.tvbox.osc.R

class LoadMoreView : LoadMoreView() {
	override fun getLayoutId(): Int {
		return R.layout.item_view_load_more
	}

	override fun getLoadingViewId(): Int {
		return R.id.load_more_loading_view
	}

	override fun getLoadFailViewId(): Int {
		return R.id.load_more_load_fail_view
	}

	override fun getLoadEndViewId(): Int {
		return R.id.load_more_load_end_view
	}
}
