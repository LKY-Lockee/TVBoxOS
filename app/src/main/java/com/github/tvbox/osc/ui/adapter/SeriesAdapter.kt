package com.github.tvbox.osc.ui.adapter

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.github.tvbox.osc.R
import com.github.tvbox.osc.bean.VodInfo.VodSeries
import com.google.android.material.chip.Chip

/**
 * @author pj567
 * @date :2020/12/22
 * @description:
 */
class SeriesAdapter(private val mGridLayoutManager: GridLayoutManager) : BaseQuickAdapter<VodSeries, BaseViewHolder>(R.layout.item_series, ArrayList<VodSeries?>()) {
	private fun getActivityFromContext(context: Context?): Activity? {
		var context = context
		while (context is ContextWrapper) {
			if (context is Activity) {
				return context
			}
			context = context.baseContext
		}
		return null
	}

	override fun convert(p0: BaseViewHolder, p1: VodSeries) {
		val chipSeries = p0.itemView as Chip
		chipSeries.text = p1.name

		chipSeries.isSelected = p1.selected

		if (data.size == 1 && p0.layoutPosition == 0) {
			p0.itemView.nextFocusUpId = R.id.mGridViewFlag
		}

		val activity = getActivityFromContext(p0.itemView.context)
		if (activity != null) {
			val mSeriesGroupTv = activity.findViewById<View?>(R.id.mSeriesGroupTv)
			if (data.size > 1 && mSeriesGroupTv != null && mSeriesGroupTv.isVisible) {
				val spanCount = mGridLayoutManager.spanCount
				val position = p0.layoutPosition
				if (position < spanCount) {
					p0.itemView.nextFocusUpId = R.id.mSeriesSortTv
				}
			}
		}
	}
}
