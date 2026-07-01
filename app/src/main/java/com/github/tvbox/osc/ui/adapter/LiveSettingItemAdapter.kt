package com.github.tvbox.osc.ui.adapter

import android.graphics.Color
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.github.tvbox.osc.R
import com.github.tvbox.osc.bean.LiveSettingItem

/**
 * @author pj567
 * @date 2021/1/12
 */
class LiveSettingItemAdapter : BaseQuickAdapter<LiveSettingItem, BaseViewHolder>(R.layout.item_live_setting, ArrayList<LiveSettingItem?>()) {
	private var focusedItemIndex = -1

	override fun convert(p0: BaseViewHolder, p1: LiveSettingItem) {
		val tvItemName = p0.getView<TextView>(R.id.tvSettingItemName)
		tvItemName.text = p1.itemName
		val itemIndex = p1.itemIndex
		if (p1.isItemSelected && itemIndex != focusedItemIndex) {
			tvItemName.setTextColor(mContext.resources.getColor(R.color.color_1890FF))
		} else {
			tvItemName.setTextColor(Color.WHITE)
		}
	}

	fun selectItem(selectedItemIndex: Int, select: Boolean, unselectPreItemIndex: Boolean) {
		if (unselectPreItemIndex) {
			val preSelectedItemIndex = this.selectedItemIndex
			if (preSelectedItemIndex != -1) {
				data[preSelectedItemIndex].isItemSelected = false
				notifyItemChanged(preSelectedItemIndex)
			}
		}
		if (selectedItemIndex != -1) {
			data[selectedItemIndex].isItemSelected = select
			notifyItemChanged(selectedItemIndex)
		}
	}

	fun setFocusedItemIndex(focusedItemIndex: Int) {
		val preFocusItemIndex = this.focusedItemIndex
		this.focusedItemIndex = focusedItemIndex
		if (preFocusItemIndex != -1) notifyItemChanged(preFocusItemIndex)
		if (this.focusedItemIndex != -1) notifyItemChanged(this.focusedItemIndex)
	}

	val selectedItemIndex: Int
		get() {
			for (item in data) {
				if (item.isItemSelected) return item.itemIndex
			}
			return -1
		}
}
