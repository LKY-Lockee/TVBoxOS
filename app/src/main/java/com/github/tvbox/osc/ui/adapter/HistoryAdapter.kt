package com.github.tvbox.osc.ui.adapter

import android.text.TextUtils
import android.widget.ImageView
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.github.tvbox.osc.R
import com.github.tvbox.osc.api.ApiConfig.Companion.instance
import com.github.tvbox.osc.bean.VodInfo
import com.github.tvbox.osc.picasso.RoundTransformation
import com.github.tvbox.osc.ui.tv.widget.AspectRatioImageView
import com.github.tvbox.osc.util.DefaultConfig
import com.github.tvbox.osc.util.ImgUtil
import com.github.tvbox.osc.util.MD5.string2MD5
import com.squareup.picasso.Picasso
import me.jessyan.autosize.utils.AutoSizeUtils

/**
 * @author pj567
 * @date 2020/12/21
 */
class HistoryAdapter : BaseQuickAdapter<VodInfo, BaseViewHolder>(R.layout.item_grid, ArrayList<VodInfo?>()) {
	override fun convert(p0: BaseViewHolder, p1: VodInfo) {
		val tvYear = p0.getView<TextView>(R.id.tvYear)
		val bean = instance.getSource(p1.sourceKey)
		if (bean != null) {
			tvYear.text = bean.name
		} else {
			tvYear.text = "搜"
		}
		p0.setVisible(R.id.tvLang, false)
		p0.setVisible(R.id.tvArea, false)
		if (p1.note == null || (p1.note ?: return).isEmpty()) {
			p0.setVisible(R.id.tvNote, false)
		} else {
			p0.setText(R.id.tvNote, p1.note)
		}
		p0.setText(R.id.tvName, p1.name)
		val ivThumb = p0.getView<ImageView>(R.id.ivThumb)
		// 由于部分电视机使用glide报错
		if (!TextUtils.isEmpty(p1.pic)) {
			Picasso.get()
				.load(DefaultConfig.checkReplaceProxy(p1.pic ?: return))
				.transform(
					RoundTransformation(string2MD5(p1.pic))
						.centerCorp(true)
						.override(AutoSizeUtils.mm2px(mContext, ImgUtil.DEFAULT_WIDTH.toFloat()), AutoSizeUtils.mm2px(mContext, ImgUtil.DEFAULT_HEIGHT.toFloat()))
						.roundRadius(AutoSizeUtils.mm2px(mContext, 10f), RoundTransformation.RoundType.ALL)
				)
				.placeholder(R.drawable.img_loading_placeholder)
				.noFade()
				.error(ImgUtil.createTextDrawable(p1.name ?: return))
				.into(ivThumb)
		} else {
			ivThumb.setImageDrawable(ImgUtil.createTextDrawable(p1.name ?: return))
		}
		// 动态设置宽高
		if (ivThumb is AspectRatioImageView) {
			ivThumb.setAspectRatio(214f / 280f)
		}
	}
}
