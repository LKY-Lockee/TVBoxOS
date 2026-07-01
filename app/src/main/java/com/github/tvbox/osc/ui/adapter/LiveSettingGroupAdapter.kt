package com.github.tvbox.osc.ui.adapter

import android.graphics.Color
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.github.tvbox.osc.R
import com.github.tvbox.osc.bean.LiveSettingGroup

/**
 * @author pj567
 * @date 2021/1/12
 */
class LiveSettingGroupAdapter : BaseQuickAdapter<LiveSettingGroup, BaseViewHolder>(R.layout.item_live_setting_group, ArrayList<LiveSettingGroup?>()) {
	var selectedGroupIndex = -1
		private set
	private var focusedGroupIndex = -1

	override fun convert(p0: BaseViewHolder, p1: LiveSettingGroup) {
		val tvGroupName = p0.getView<TextView>(R.id.tvSettingGroupName)
		tvGroupName.text = p1.groupName
		val groupIndex = p1.groupIndex
		if (groupIndex == selectedGroupIndex && groupIndex != focusedGroupIndex) {
			tvGroupName.setTextColor(mContext.resources.getColor(R.color.color_1890FF))
		} else {
			tvGroupName.setTextColor(Color.WHITE)
		}
	}

	fun getSelectedGroupIndex(): Int {
		return selectedGroupIndex
	}

	fun setSelectedGroupIndex(selectedGroupIndex: Int) {
		val preSelectedGroupIndex = this.selectedGroupIndex
		this.selectedGroupIndex = selectedGroupIndex
		if (preSelectedGroupIndex != -1) notifyItemChanged(preSelectedGroupIndex)
		if (this.selectedGroupIndex != -1) notifyItemChanged(this.selectedGroupIndex)
	}

	fun setFocusedGroupIndex(focusedGroupIndex: Int) {
		this.focusedGroupIndex = focusedGroupIndex
		if (this.focusedGroupIndex != -1) notifyItemChanged(this.focusedGroupIndex)
		else if (this.selectedGroupIndex != -1) notifyItemChanged(this.selectedGroupIndex)
	}
}
