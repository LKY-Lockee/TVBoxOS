package com.github.tvbox.osc.ui.adapter

import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.github.tvbox.osc.R
import com.github.tvbox.osc.api.ApiConfig.Companion.instance
import com.github.tvbox.osc.bean.Movie
import com.github.tvbox.osc.picasso.RoundTransformation
import com.github.tvbox.osc.ui.tv.widget.AspectRatioImageView
import com.github.tvbox.osc.util.DefaultConfig
import com.github.tvbox.osc.util.ImgUtil
import com.github.tvbox.osc.util.MD5.string2MD5
import com.squareup.picasso.Picasso
import me.jessyan.autosize.utils.AutoSizeUtils

class SearchAdapter : BaseQuickAdapter<Movie.Video, BaseViewHolder>(R.layout.item_grid, ArrayList<Movie.Video?>()) {
	override fun convert(p0: BaseViewHolder, p1: Movie.Video) {
		val tvYear = p0.getView<TextView>(R.id.tvYear)
		if (p1.year <= 0) {
			tvYear.visibility = View.GONE
		} else {
			tvYear.text = p1.year.toString()
			tvYear.visibility = View.VISIBLE
		}

		val tvLang = p0.getView<TextView>(R.id.tvLang)
		tvLang.visibility = View.GONE
		val tvArea = p0.getView<TextView>(R.id.tvArea)
		tvArea.visibility = View.GONE

		var noteText = (instance.getSource(p1.sourceKey) ?: return).name
		if (!TextUtils.isEmpty(p1.note)) {
			noteText += " · " + p1.note
		}
		p0.setVisible(R.id.tvNote, true)
		p0.setText(R.id.tvNote, noteText)

		p0.setText(R.id.tvName, p1.name)
		p0.setText(R.id.tvActor, p1.actor)

		val ivThumb = p0.getView<ImageView>(R.id.ivThumb)
		val newWidth = ImgUtil.DEFAULT_WIDTH
		val newHeight = ImgUtil.DEFAULT_HEIGHT

		if (!TextUtils.isEmpty(p1.pic)) {
			p1.pic = (p1.pic ?: return).trim { it <= ' ' }
			if (ImgUtil.isBase64Image(p1.pic ?: return)) {
				ivThumb.setImageBitmap(ImgUtil.decodeBase64ToBitmap(p1.pic ?: return))
			} else {
				Picasso.get()
					.load(DefaultConfig.checkReplaceProxy(p1.pic ?: return))
					.transform(
						RoundTransformation(string2MD5(p1.pic))
							.centerCorp(true)
							.override(AutoSizeUtils.mm2px(mContext, newWidth.toFloat()), AutoSizeUtils.mm2px(mContext, newHeight.toFloat()))
							.roundRadius(AutoSizeUtils.mm2px(mContext, 10f), RoundTransformation.RoundType.ALL)
					)
					.placeholder(R.drawable.img_loading_placeholder)
					.noFade()
					.error(ImgUtil.createTextDrawable(p1.name ?: return))
					.into(ivThumb)
			}
		} else {
			ivThumb.setImageDrawable(ImgUtil.createTextDrawable(p1.name ?: return))
		}
		// 动态设置宽高
		if (ivThumb is AspectRatioImageView) {
			ivThumb.setAspectRatio(214f / 280f)
		}
	}
}