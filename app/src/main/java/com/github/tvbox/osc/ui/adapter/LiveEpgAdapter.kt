package com.github.tvbox.osc.ui.adapter

import android.graphics.Color
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.github.tvbox.osc.R
import com.github.tvbox.osc.bean.EpgInfo
import com.github.tvbox.osc.ui.tv.widget.AudioWaveView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LiveEpgAdapter : BaseQuickAdapter<EpgInfo, BaseViewHolder>(R.layout.epglist_item, ArrayList<EpgInfo?>()) {
	val timeFormat: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
	private val defaultShiyiSelection = 0
	private val currentEpgDate: String? = null
	private val focusSelection = -1
	var selectedIndex: Int = -1
		private set
	private var focusedEpgIndex = -1
	private var shiyiSelection = false
	private var shiyiDate: String? = null
	private var sourceIncludeBack = false

	fun canBack(sourceIncludeBack: Boolean) {
		this.sourceIncludeBack = sourceIncludeBack
	}

	override fun convert(p0: BaseViewHolder, p1: EpgInfo) {
		val textview = p0.getView<TextView>(R.id.tv_epg_name)
		val timeview = p0.getView<TextView>(R.id.tv_epg_time)
		val shiyi = p0.getView<TextView>(R.id.shiyi)
		val wqddgAudioWaveView = p0.getView<AudioWaveView>(R.id.wqddg_AudioWaveView)
		wqddgAudioWaveView.visibility = View.GONE
		if (p1.index == this.selectedIndex && p1.index != focusedEpgIndex && (p1.currentEpgDate == shiyiDate || p1.currentEpgDate == timeFormat.format(Date()))) {
			textview.setTextColor(ContextCompat.getColor(mContext, R.color.color_1890FF))
			timeview.setTextColor(ContextCompat.getColor(mContext, R.color.color_1890FF))
		} else {
			textview.setTextColor(Color.WHITE)
			timeview.setTextColor(Color.WHITE)
		}
		if (Date() >= p1.startDateTime && Date() <= p1.endDateTime) {
			shiyi.visibility = View.VISIBLE
			shiyi.setBackgroundColor(Color.YELLOW)
			shiyi.text = "直播中"
			shiyi.setTextColor(Color.RED)
		} else if (Date() > p1.endDateTime && sourceIncludeBack) {
			shiyi.visibility = View.VISIBLE
			shiyi.setBackgroundColor(Color.BLUE)
			shiyi.setTextColor(Color.WHITE)
			shiyi.text = "回看"
		} else if (Date() < p1.startDateTime) {
			shiyi.visibility = View.GONE
			//            shiyi.setBackgroundColor(Color.GRAY);
//            shiyi.setTextColor(Color.BLACK);
//            shiyi.setText("");
		} else {
			shiyi.visibility = View.GONE
		}
		textview.text = p1.title
		timeview.text = p1.start + "--" + p1.end
		if (!shiyiSelection) {
			val now = Date()
			if (now >= p1.startDateTime && now <= p1.endDateTime) {
				wqddgAudioWaveView.visibility = View.VISIBLE
				textview.freezesText = true
				timeview.freezesText = true
			} else {
				wqddgAudioWaveView.visibility = View.GONE
			}
		} else {
			if (p1.index == this.selectedIndex && p1.currentEpgDate == shiyiDate) {
				wqddgAudioWaveView.visibility = View.VISIBLE
				textview.freezesText = true
				timeview.freezesText = true
				shiyi.text = "回看中"
				shiyi.setTextColor(Color.RED)
				shiyi.setBackgroundColor(Color.rgb(12, 255, 0))
				if (Date() >= p1.startDateTime && Date() <= p1.endDateTime) {
					shiyi.visibility = View.VISIBLE
					shiyi.setBackgroundColor(Color.YELLOW)
					shiyi.text = "直播中"
					shiyi.setTextColor(Color.RED)
				}
			} else {
				wqddgAudioWaveView.visibility = View.GONE
			}
		}
	}

	fun setShiyiSelection(i: Int, t: Boolean, currentEpgDate: String?) {
		this.selectedIndex = i
		this.shiyiDate = if (t) currentEpgDate else null
		shiyiSelection = t
		notifyItemChanged(this.selectedIndex)
	}

	fun setSelectedEpgIndex(selectedEpgIndex: Int) {
		if (selectedEpgIndex == this.selectedIndex) return
		this.selectedIndex = selectedEpgIndex
		if (this.selectedIndex != -1) notifyItemChanged(this.selectedIndex)
	}

	fun getFocusedEpgIndex(): Int {
		return focusedEpgIndex
	}

	fun setFocusedEpgIndex(focusedEpgIndex: Int) {
		this.focusedEpgIndex = focusedEpgIndex
		if (this.focusedEpgIndex != -1) notifyItemChanged(this.focusedEpgIndex)
	}

	companion object {
		var fontSize: Float = 20f
	}
}
