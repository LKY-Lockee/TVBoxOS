package com.github.tvbox.osc.ui.fragment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.tvbox.osc.R
import com.github.tvbox.osc.api.ApiConfig
import com.github.tvbox.osc.bean.Movie
import com.github.tvbox.osc.cache.RoomDataManger
import com.github.tvbox.osc.picasso.RoundTransformation
import com.github.tvbox.osc.util.DefaultConfig
import com.github.tvbox.osc.util.MD5.string2MD5
import com.google.android.material.button.MaterialButton
import com.squareup.picasso.Picasso
import me.jessyan.autosize.utils.AutoSizeUtils

class DetailTabInfoFragment : Fragment() {
	private var contentView: View? = null
	private var ivThumb: ImageView? = null
	private var tvName: TextView? = null
	private var tvYear: TextView? = null
	private var tvSite: TextView? = null
	private var tvArea: TextView? = null
	private var tvLang: TextView? = null
	private var tvType: TextView? = null
	private var tvActor: TextView? = null
	private var tvDirector: TextView? = null
	private var tvPlayUrl: TextView? = null
	private var tvDes: TextView? = null
	private var tvCollect: MaterialButton? = null

	private var sourceKey: String? = null
	private var vodId: String? = null

	fun setContentView(view: View?) {
		this.contentView = view
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		if (contentView != null) {
			if ((contentView ?: return null).parent != null) {
				((contentView ?: return null).parent as ViewGroup).removeView(contentView)
			}
			initViews(contentView ?: return null)
			return contentView
		}
		val view = inflater.inflate(R.layout.fragment_detail_tab_info, container, false)
		initViews(view)
		return view
	}

	private fun initViews(view: View) {
		ivThumb = view.findViewById(R.id.ivThumb)
		tvName = view.findViewById(R.id.tvName)
		tvYear = view.findViewById(R.id.tvYear)
		tvSite = view.findViewById(R.id.tvSite)
		tvArea = view.findViewById(R.id.tvArea)
		tvLang = view.findViewById(R.id.tvLang)
		tvType = view.findViewById(R.id.tvType)
		tvActor = view.findViewById(R.id.tvActor)
		tvDirector = view.findViewById(R.id.tvDirector)
		tvPlayUrl = view.findViewById(R.id.tvPlayUrl)
		tvDes = view.findViewById(R.id.tvDes)
		tvCollect = view.findViewById(R.id.tvCollect)

		(tvCollect ?: return).setOnClickListener { v: View? -> onCollectClick() }
		(tvPlayUrl ?: return).setOnClickListener { v: View? -> onPlayUrlClick() }
	}

	fun setVideoInfo(video: Movie.Video?, sourceKey: String?, firstSourceKey: String, vodId: String?) {
		if (video == null) return

		this.sourceKey = sourceKey
		this.vodId = vodId

		if (tvName != null) (tvName ?: return).text = video.name
		setTextShow(tvSite, "来源：", ApiConfig.instance.getSource(firstSourceKey)?.name)
		setTextShow(tvYear, "年份：", if (video.year == 0) "" else video.year.toString())
		setTextShow(tvArea, "地区：", video.area)
		setTextShow(tvLang, "语言：", video.lang)

		if (firstSourceKey != sourceKey) {
			setTextShow(tvType, "类型：", "[" + ApiConfig.instance.getSource(sourceKey)?.name + "] 解析")
		} else {
			setTextShow(tvType, "类型：", video.type)
		}

		setTextShow(tvActor, "演员：", video.actor)
		setTextShow(tvDirector, "导演：", video.director)
		setTextShow(tvDes, "简介：", removeHtmlTag(video.des))

		if (!TextUtils.isEmpty(video.pic) && ivThumb != null) {
			Picasso.get()
				.load(DefaultConfig.checkReplaceProxy(video.pic ?: return))
				.transform(
					RoundTransformation(string2MD5(video.pic))
						.centerCorp(true)
						.override(AutoSizeUtils.mm2px(requireContext(), 300f), AutoSizeUtils.mm2px(requireContext(), 400f))
						.roundRadius(AutoSizeUtils.mm2px(requireContext(), 10f), RoundTransformation.RoundType.ALL)
				)
				.placeholder(R.drawable.img_loading_placeholder)
				.noFade()
				.error(R.drawable.img_loading_placeholder)
				.into(ivThumb)
		} else if (ivThumb != null) {
			(ivThumb ?: return).setImageResource(R.drawable.img_loading_placeholder)
		}

		updateCollectButton()
	}

	fun setPlayUrl(url: String?) {
		setTextShow(tvPlayUrl, "地址：", url)
	}

	fun updateCollectButton() {
		if (tvCollect == null || sourceKey == null || vodId == null) return

		val isVodCollect = RoomDataManger.isVodCollect(sourceKey ?: return, vodId ?: return)
		if (isVodCollect) {
			(tvCollect ?: return).setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.icon_collect_filled))
		} else {
			(tvCollect ?: return).setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.icon_collect))
		}
	}

	private fun onCollectClick() {
		if (sourceKey == null || vodId == null) return

		val isVodCollect = RoomDataManger.isVodCollect(sourceKey ?: return, vodId ?: return)
		if (isVodCollect) {
			RoomDataManger.deleteVodCollect(sourceKey ?: return, null)
			(tvCollect ?: return).setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.icon_collect))
			Toast.makeText(requireContext(), "已取消收藏", Toast.LENGTH_SHORT).show()
		} else {
			RoomDataManger.insertVodCollect(sourceKey ?: return, null)
			(tvCollect ?: return).setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.icon_collect_filled))
			Toast.makeText(requireContext(), "已加入收藏夹", Toast.LENGTH_SHORT).show()
		}
	}

	private fun onPlayUrlClick() {
		if (tvPlayUrl == null) return

		val cm = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
		cm.setPrimaryClip(ClipData.newPlainText(null, (tvPlayUrl ?: return).text.toString().replace("播放地址：", "")))
		Toast.makeText(requireContext(), "已复制", Toast.LENGTH_SHORT).show()
	}

	private fun setTextShow(view: TextView?, tag: String?, info: String?) {
		if (view == null) return

		if (info == null || info.trim { it <= ' ' }.isEmpty()) {
			view.visibility = View.GONE
			return
		}
		view.visibility = View.VISIBLE
		view.text = getHtml(tag, info)
	}

	private fun getHtml(label: String?, content: String?): String {
		var content = content
		if (content == null) {
			content = ""
		}
		return label + content
	}

	private fun removeHtmlTag(info: String?): String {
		if (info == null) return ""
		return info.replace("<.*?>".toRegex(), "").replace("\\s".toRegex(), "")
	}
}

