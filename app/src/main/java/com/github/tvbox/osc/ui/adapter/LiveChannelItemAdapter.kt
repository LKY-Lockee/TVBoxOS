package com.github.tvbox.osc.ui.adapter

import android.graphics.Color
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.github.tvbox.osc.R
import com.github.tvbox.osc.bean.LiveChannelItem

/**
 * @author pj567
 * @date :2021/1/12
 * @description:
 */
class LiveChannelItemAdapter : BaseQuickAdapter<LiveChannelItem, BaseViewHolder>(R.layout.item_live_channel, ArrayList<LiveChannelItem?>()) {
	private var selectedChannelIndex = -1
	private var focusedChannelIndex = -1

	override fun convert(p0: BaseViewHolder, p1: LiveChannelItem) {
		val tvChannelNum = p0.getView<TextView>(R.id.tvChannelNum)
		val tvChannel = p0.getView<TextView>(R.id.tvChannelName)
		tvChannelNum.text = String.format("%s", p1.channelNum)
		tvChannel.text = p1.channelName
		val channelIndex = p1.channelIndex
		if (channelIndex == selectedChannelIndex && channelIndex != focusedChannelIndex) {
			tvChannelNum.setTextColor(mContext.resources.getColor(R.color.color_1890FF))
			tvChannel.setTextColor(mContext.resources.getColor(R.color.color_1890FF))
		} else {
			tvChannelNum.setTextColor(Color.WHITE)
			tvChannel.setTextColor(Color.WHITE)
		}
	}

	fun setSelectedChannelIndex(selectedChannelIndex: Int) {
		if (selectedChannelIndex == this.selectedChannelIndex) return
		val preSelectedChannelIndex = this.selectedChannelIndex
		this.selectedChannelIndex = selectedChannelIndex
		if (preSelectedChannelIndex != -1) notifyItemChanged(preSelectedChannelIndex)
		if (this.selectedChannelIndex != -1) notifyItemChanged(this.selectedChannelIndex)
	}

	fun setFocusedChannelIndex(focusedChannelIndex: Int) {
		val preFocusedChannelIndex = this.focusedChannelIndex
		this.focusedChannelIndex = focusedChannelIndex
		if (preFocusedChannelIndex != -1) notifyItemChanged(preFocusedChannelIndex)
		if (this.focusedChannelIndex != -1) notifyItemChanged(this.focusedChannelIndex)
		else if (this.selectedChannelIndex != -1) notifyItemChanged(this.selectedChannelIndex)
	}
}