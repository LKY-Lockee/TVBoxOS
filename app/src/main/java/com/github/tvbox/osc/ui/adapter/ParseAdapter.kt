package com.github.tvbox.osc.ui.adapter

import android.graphics.Color
import android.view.View
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.github.tvbox.osc.R
import com.github.tvbox.osc.bean.ParseBean

class ParseAdapter : BaseQuickAdapter<ParseBean, BaseViewHolder>(R.layout.item_play_parse, ArrayList<ParseBean?>()) {
	override fun convert(p0: BaseViewHolder, p1: ParseBean) {
		val tvParse = p0.getView<TextView>(R.id.tvParse)
		tvParse.visibility = View.VISIBLE
		if (p1.isDefault) {
			tvParse.setTextColor(mContext.resources.getColor(R.color.color_02F8E1))
		} else {
			tvParse.setTextColor(Color.WHITE)
		}
		tvParse.text = p1.name
		if (p0.layoutPosition == 0) {
			p0.itemView.nextFocusLeftId = R.id.screen_display
		}
	}
}
