package com.github.tvbox.osc.ui.adapter

import android.graphics.Color
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.github.tvbox.osc.R
import com.github.tvbox.osc.bean.LiveEpgDate

class LiveEpgDateAdapter : BaseQuickAdapter<LiveEpgDate, BaseViewHolder>(R.layout.item_live_channel_group, ArrayList<LiveEpgDate?>()) {
	var selectedIndex = -1
		private set
	private var focusedIndex = -1

	override fun convert(p0: BaseViewHolder, p1: LiveEpgDate) {
		val tvGroupName = p0.getView<TextView>(R.id.tvChannelGroupName)
		tvGroupName.text = p1.datePresented
		tvGroupName.setBackgroundColor(Color.TRANSPARENT)
		if (p1.index == selectedIndex && p1.index != focusedIndex) {
			tvGroupName.setTextColor(mContext.resources.getColor(R.color.color_1890FF))
		} else {
			tvGroupName.setTextColor(mContext.resources.getColor(R.color.color_CCFFFFFF))
		}
	}

	fun setSelectedIndex(selectedIndex: Int) {
		if (selectedIndex == this.selectedIndex) return
		val preSelectedIndex = this.selectedIndex
		this.selectedIndex = selectedIndex
		if (preSelectedIndex != -1) notifyItemChanged(preSelectedIndex)
		if (this.selectedIndex != -1) notifyItemChanged(this.selectedIndex)
	}

	fun setFocusedIndex(focusedIndex: Int) {
		val preSelectedIndex = this.selectedIndex
		this.focusedIndex = focusedIndex
		if (preSelectedIndex != -1) notifyItemChanged(preSelectedIndex)
		if (this.focusedIndex != -1) notifyItemChanged(this.focusedIndex)
	}
}
