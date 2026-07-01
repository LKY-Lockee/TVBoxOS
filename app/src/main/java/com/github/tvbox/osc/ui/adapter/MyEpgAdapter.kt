package com.github.tvbox.osc.ui.adapter

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.github.tvbox.osc.R
import com.github.tvbox.osc.bean.EpgInfo
import com.github.tvbox.osc.ui.tv.widget.AudioWaveView

class MyEpgAdapter(private val data: MutableList<EpgInfo?>, private val context: Context?, private var defaultSelection: Int) : BaseAdapter() {
	fun setSelection(i: Int) {
		this.defaultSelection = i
		notifyDataSetChanged()
	}

	fun setShiyiSelection(i: Int) {
		notifyDataSetChanged()
	}

	fun setFontSize(f: Float) {
		fontSize = f
		notifyDataSetChanged()
	}

	override fun getCount(): Int {
		return data.size
	}

	override fun getItem(i: Int): Any? {
		return null
	}

	override fun getItemId(i: Int): Long {
		return i.toLong()
	}

	override fun getView(i: Int, view: View?, viewGroup: ViewGroup?): View {
		var view = view
		if (view == null) {
			view = LayoutInflater.from(context).inflate(R.layout.epglist_item, viewGroup, false)
		}
		val textview = view.findViewById<TextView>(R.id.tv_epg_name)
		val timeView = view.findViewById<TextView>(R.id.tv_epg_time)
		val wqddgAudioWaveView = view.findViewById<AudioWaveView>(R.id.wqddg_AudioWaveView)
		wqddgAudioWaveView.visibility = View.GONE
		if (i < data.size) {
			textview.text = data[i]!!.title
			timeView.text = data[i]!!.start + "--" + data[i]!!.end
			textview.setTextColor(Color.WHITE)
			timeView.setTextColor(Color.WHITE)
			Log.e("roinlong", "getView: $i")
			if (i == this.defaultSelection) {
				wqddgAudioWaveView.visibility = View.VISIBLE
				textview.setTextColor(Color.rgb(0, 153, 255))
				timeView.setTextColor(Color.rgb(0, 153, 255))
				textview.freezesText = true
				timeView.freezesText = true
			} else {
				wqddgAudioWaveView.visibility = View.GONE
			}
		}
		return view
	}

	companion object {
		var fontSize: Float = 20f
	}
}
