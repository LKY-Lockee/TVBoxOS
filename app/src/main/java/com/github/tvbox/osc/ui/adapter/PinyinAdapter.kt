package com.github.tvbox.osc.ui.adapter

import android.view.View
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.github.tvbox.osc.R
import com.github.tvbox.osc.ui.adapter.PinyinAdapter.SearchItem

class PinyinAdapter : BaseQuickAdapter<SearchItem, BaseViewHolder>(R.layout.item_search_word, ArrayList<SearchItem?>()) {
	private var onItemLongClickListener: OnItemLongClickListener? = null

	override fun convert(p0: BaseViewHolder, p1: SearchItem) {
		p0.setText(R.id.tvSearchWord, p1.title)
		val iconRes = when (p1.type) {
			0 -> R.drawable.icon_history
			1 -> R.drawable.icon_hot
			else -> R.drawable.icon_search
		}
		p0.setImageResource(R.id.iv_icon, iconRes)

		// 设置长按监听器（仅对历史记录生效）
		if (p1.type == 0) {
			p0.itemView.setOnLongClickListener { v: View? ->
				if (onItemLongClickListener != null) {
					onItemLongClickListener!!.onItemLongClick(p0.layoutPosition, p1)
					return@setOnLongClickListener true
				}
				false
			}
		} else {
			p0.itemView.setOnLongClickListener(null)
		}
	}

	fun setOnItemLongClickListener(listener: OnItemLongClickListener?) {
		this.onItemLongClickListener = listener
	}

	fun interface OnItemLongClickListener {
		fun onItemLongClick(position: Int, item: SearchItem?)
	}

	class SearchItem(
		@JvmField var title: String?, // 0: 历史, 1: 热搜, 2: 搜索建议
		@JvmField var type: Int
	) {
		init {
			this.type = type
		}
	}
}
