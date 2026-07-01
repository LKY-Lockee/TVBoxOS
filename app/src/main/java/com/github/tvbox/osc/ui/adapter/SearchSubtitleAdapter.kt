package com.github.tvbox.osc.ui.adapter

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.github.tvbox.osc.R
import com.github.tvbox.osc.bean.Subtitle

class SearchSubtitleAdapter : BaseQuickAdapter<Subtitle, BaseViewHolder>(R.layout.item_search_subtitle_result, ArrayList<Subtitle?>()) {
	override fun convert(p0: BaseViewHolder, p1: Subtitle) {
		p0.setText(R.id.subtitleName, p1.name)
		p0.setText(R.id.subtitleNameInfo, if (p1.isZip) "压缩包" else "文件")
	}
}
