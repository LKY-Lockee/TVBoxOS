package com.github.tvbox.osc.ui.adapter

import android.text.TextUtils
import android.widget.ImageView
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.github.tvbox.osc.R
import com.github.tvbox.osc.api.ApiConfig.Companion.instance
import com.github.tvbox.osc.cache.VodCollect
import com.github.tvbox.osc.picasso.RoundTransformation
import com.github.tvbox.osc.ui.tv.widget.AspectRatioImageView
import com.github.tvbox.osc.util.DefaultConfig.checkReplaceProxy
import com.github.tvbox.osc.util.MD5.string2MD5
import com.squareup.picasso.Picasso
import me.jessyan.autosize.utils.AutoSizeUtils

class CollectAdapter : BaseQuickAdapter<VodCollect, BaseViewHolder>(R.layout.item_grid, ArrayList<VodCollect?>()) {
	override fun convert(p0: BaseViewHolder, p1: VodCollect) {
		p0.setVisible(R.id.tvLang, false)
		p0.setVisible(R.id.tvArea, false)
		p0.setVisible(R.id.tvNote, false)
		p0.setText(R.id.tvName, p1.name)
		val tvYear = p0.getView<TextView>(R.id.tvYear)
		val source = instance.getSource(p1.sourceKey)
		tvYear.text = if (source != null) source.name else ""

		val ivThumb = p0.getView<ImageView>(R.id.ivThumb)
		//由于部分电视机使用glide报错
		if (!TextUtils.isEmpty(p1.pic)) {
			Picasso.get()
				.load(checkReplaceProxy(p1.pic))
				.transform(
					RoundTransformation(string2MD5(p1.pic))
						.centerCorp(true)
						.override(AutoSizeUtils.mm2px(mContext, 240f), AutoSizeUtils.mm2px(mContext, 336f))
						.roundRadius(AutoSizeUtils.mm2px(mContext, 10f), RoundTransformation.RoundType.ALL)
				)
				.placeholder(R.drawable.img_loading_placeholder)
				.noFade()
				.error(R.drawable.img_loading_placeholder)
				.into(ivThumb)
		} else {
			ivThumb.setImageResource(R.drawable.img_loading_placeholder)
		}
		// 动态设置宽高
		if (ivThumb is AspectRatioImageView) {
			ivThumb.setAspectRatio(214f / 280f)
		}
	}
}
