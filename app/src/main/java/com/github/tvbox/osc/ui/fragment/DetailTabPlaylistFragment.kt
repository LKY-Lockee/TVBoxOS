package com.github.tvbox.osc.ui.fragment

import android.annotation.SuppressLint
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.github.tvbox.osc.R
import com.github.tvbox.osc.bean.VodInfo
import com.github.tvbox.osc.ui.adapter.SeriesAdapter
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import java.util.Locale

class DetailTabPlaylistFragment : Fragment() {
	private var contentView: View? = null
	private var mGridView: RecyclerView? = null
	private var mGridViewFlag: TabLayout? = null
	private var tvSeriesGroup: LinearLayout? = null
	private var tvSeriesSort: TextView? = null

	private var seriesAdapter: SeriesAdapter? = null
	private var seriesGroupAdapter: BaseQuickAdapter<String, BaseViewHolder>? = null
	private var mGridViewLayoutMgr: GridLayoutManager? = null
	private var smoothScroller: LinearSmoothScroller? = null

	private val seriesGroupOptions = ArrayList<String?>()
	private var currentSeriesGroupView: View? = null
	private var isReverse = false
	private var groupCount = 30

	private var onSeriesFlagSelectedListener: OnSeriesFlagSelectedListener? = null
	private var onSeriesSelectedListener: OnSeriesSelectedListener? = null

	fun interface OnSeriesFlagSelectedListener {
		fun onSeriesFlagSelected(flagName: String?, position: Int)
	}

	fun interface OnSeriesSelectedListener {
		fun onSeriesSelected(position: Int)
	}

	fun setOnSeriesFlagSelectedListener(listener: OnSeriesFlagSelectedListener?) {
		this.onSeriesFlagSelectedListener = listener
	}

	fun setOnSeriesSelectedListener(listener: OnSeriesSelectedListener?) {
		this.onSeriesSelectedListener = listener
	}

	fun setContentView(view: View?) {
		this.contentView = view
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		contentView?.let {
			(it.parent as? ViewGroup)?.removeView(it)
			initViews(it)
		}
		val view = inflater.inflate(R.layout.fragment_detail_tab_playlist, container, false)
		initViews(view)
		return view
	}

	private fun initViews(view: View) {
		mGridView = view.findViewById(R.id.mGridView)
		mGridViewFlag = view.findViewById(R.id.mGridViewFlag)
		tvSeriesGroup = view.findViewById(R.id.mSeriesGroupTv)
		tvSeriesSort = view.findViewById(R.id.mSeriesSortTv)
		val mSeriesGroupView = view.findViewById<RecyclerView>(R.id.mSeriesGroupView)

		mGridView?.setHasFixedSize(false)
		mGridViewLayoutMgr = GridLayoutManager(requireContext(), 6)
		mGridView?.setLayoutManager(mGridViewLayoutMgr)

		smoothScroller = object : LinearSmoothScroller(requireContext()) {
			override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
				return 100f / displayMetrics.densityDpi
			}

			override fun computeScrollVectorForPosition(targetPosition: Int): PointF? {
				return mGridViewLayoutMgr?.computeScrollVectorForPosition(targetPosition)
			}
		}

		seriesAdapter = SeriesAdapter(mGridViewLayoutMgr ?: return)
		mGridView?.setAdapter(seriesAdapter)

		mGridViewFlag?.addOnTabSelectedListener(object : OnTabSelectedListener {
			override fun onTabSelected(tab: TabLayout.Tab) {
				if (onSeriesFlagSelectedListener != null && tab.tag != null) {
					val position = tab.tag as Int
					onSeriesFlagSelectedListener?.onSeriesFlagSelected(tab.text.toString(), position)
				}
			}

			override fun onTabUnselected(tab: TabLayout.Tab?) {
			}

			override fun onTabReselected(tab: TabLayout.Tab?) {
			}
		})

		mSeriesGroupView.setHasFixedSize(true)
		mSeriesGroupView.setLayoutManager(LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false))
		seriesGroupAdapter = object : BaseQuickAdapter<String, BaseViewHolder>(R.layout.item_series_group, seriesGroupOptions) {
			override fun convert(p0: BaseViewHolder, item: String) {
				val btnSeries = p0.itemView as MaterialButton
				btnSeries.text = item
				if (p0.layoutPosition == data.size - 1) {
					p0.itemView.id = View.generateViewId()
					p0.itemView.nextFocusRightId = p0.itemView.id
				} else {
					p0.itemView.nextFocusRightId = View.NO_ID
				}
			}
		}
		mSeriesGroupView.setAdapter(seriesGroupAdapter)

		seriesAdapter?.setOnItemClickListener { adapter: BaseQuickAdapter<*, *>?, itemView: View?, position: Int ->
			if (onSeriesSelectedListener != null) {
				onSeriesSelectedListener?.onSeriesSelected(position)
			}
		}

		seriesGroupAdapter?.setOnItemClickListener { adapter: BaseQuickAdapter<*, *>?, itemView: View?, position: Int ->
			onSeriesGroupClick(itemView, position)
		}

		tvSeriesSort?.setOnClickListener { v: View? -> onSortClick() }
	}

	@SuppressLint("NotifyDataSetChanged")
	private fun onSortClick() {
		isReverse = !isReverse
		tvSeriesSort?.text = if (isReverse) "倒序" else "正序"
		onSeriesSelectedListener?.onSeriesSelected(-1)
	}

	private fun onSeriesGroupClick(itemView: View?, position: Int) {
		val btnSeriesGroup = itemView as MaterialButton
		btnSeriesGroup.isSelected = true

		val targetPos = position * groupCount
		customSeriesScrollPos(targetPos)

		(currentSeriesGroupView as? MaterialButton)?.isSelected = false
		currentSeriesGroupView = itemView
	}

	fun setVodInfo(vodInfo: VodInfo?) {
		if (vodInfo == null || vodInfo.seriesMap?.isEmpty() == true) {
			mGridViewFlag?.visibility = View.GONE
			mGridView?.visibility = View.GONE
			tvSeriesGroup?.visibility = View.GONE
			return
		}

		mGridViewFlag?.visibility = View.VISIBLE
		mGridView?.visibility = View.VISIBLE

		isReverse = vodInfo.reverseSort
		tvSeriesSort?.text = if (isReverse) "倒序" else "正序"

		mGridViewFlag?.removeAllTabs()
		var selectedTabIndex = 0
		for (j in (vodInfo.seriesFlags ?: return).indices) {
			val flag = (vodInfo.seriesFlags ?: return)[j]
			val tab = (mGridViewFlag ?: return).newTab()
			tab.text = flag.name
			tab.tag = j
			mGridViewFlag?.addTab(tab)

			if (flag.name == vodInfo.playFlag) {
				selectedTabIndex = j
				flag.selected = true
			} else {
				flag.selected = false
			}
		}

		if (selectedTabIndex < (mGridViewFlag ?: return).tabCount) {
			mGridViewFlag?.getTabAt(selectedTabIndex)?.select()
		}

		refreshList(vodInfo)
	}

	@SuppressLint("NotifyDataSetChanged")
	fun refreshList(vodInfo: VodInfo?) {
		if (vodInfo == null || vodInfo.playFlag == null) return

		if (((vodInfo.seriesMap ?: return)[vodInfo.playFlag ?: return] ?: return).size <= vodInfo.playIndex) {
			vodInfo.playIndex = 0
		}

		val list = (vodInfo.seriesMap ?: return)[vodInfo.playFlag ?: return]
		if (list != null) {
			var canSelect = true
			for (j in list.indices) {
				if (list[j].selected) {
					canSelect = false
					break
				}
			}
			if (canSelect && vodInfo.playIndex < list.size) {
				list[vodInfo.playIndex].selected = true
			}
		}

		val displayMetrics = DisplayMetrics()
		requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)

		val textSize = resources.getDimension(R.dimen.ts_20)

		val pFont = Paint()
		pFont.textSize = textSize
		val rect = Rect()

		val listSize = (list ?: return).size
		var maxTextWidth = 1
		for (i in 0..<listSize) {
			val name = list[i].name
			pFont.getTextBounds(name, 0, name.length, rect)
			if (maxTextWidth < rect.width()) {
				maxTextWidth = rect.width()
			}
		}

		val marginPx = (resources.getDimension(R.dimen.vs_5) * 2).toInt()
		val chipPadding = (40 * displayMetrics.density).toInt()
		val minItemWidth = maxTextWidth + chipPadding + marginPx + 50

		val screenWidth = displayMetrics.widthPixels
		var offset = screenWidth / minItemWidth

		if (offset < 1) offset = 1

		mGridViewLayoutMgr?.setSpanCount(offset)
		seriesAdapter?.setNewData(list)

		setSeriesGroupOptions(vodInfo, offset)

		mGridView?.postDelayed({ customSeriesScrollPos(vodInfo.playIndex) }, 100)
	}

	@SuppressLint("NotifyDataSetChanged")
	private fun setSeriesGroupOptions(vodInfo: VodInfo, offset: Int) {
		val list = (vodInfo.seriesMap ?: return)[vodInfo.playFlag ?: return] ?: return

		val listSize = list.size
		seriesGroupOptions.clear()
		var groupCount = if (offset == 3 || offset == 6) 30 else 20
		if (listSize in 101..400) groupCount = 60
		if (listSize > 400) groupCount = 120

		this@DetailTabPlaylistFragment.groupCount = groupCount

		if (listSize > 1) {
			tvSeriesGroup?.visibility = View.VISIBLE
			val remainedOptionSize = listSize % groupCount
			val optionSize = listSize / groupCount

			for (i in 0..<optionSize) {
				if (vodInfo.reverseSort) {
					seriesGroupOptions.add(
						String.format(
							Locale.getDefault(), "%d - %d",
							listSize - (i * groupCount + 1) + 1, listSize - (i * groupCount + groupCount) + 1
						)
					)
				} else {
					seriesGroupOptions.add(
						String.format(
							Locale.getDefault(), "%d - %d",
							i * groupCount + 1, i * groupCount + groupCount
						)
					)
				}
			}
			if (remainedOptionSize > 0) {
				if (vodInfo.reverseSort) {
					seriesGroupOptions.add(
						String.format(
							Locale.getDefault(), "%d - %d",
							listSize - (optionSize * groupCount + 1) + 1,
							listSize - (optionSize * groupCount + remainedOptionSize) + 1
						)
					)
				} else {
					seriesGroupOptions.add(
						String.format(
							Locale.getDefault(), "%d - %d",
							optionSize * groupCount + 1, optionSize * groupCount + remainedOptionSize
						)
					)
				}
			}

			seriesGroupAdapter?.notifyDataSetChanged()
		} else {
			tvSeriesGroup?.visibility = View.GONE
		}
	}

	private fun customSeriesScrollPos(targetPos: Int) {
		mGridViewLayoutMgr?.scrollToPositionWithOffset(targetPos, 0)
		mGridView?.postDelayed({
			smoothScroller?.targetPosition = targetPos
			mGridViewLayoutMgr?.startSmoothScroll(smoothScroller)
			mGridView?.smoothScrollToPosition(targetPos)
		}, 50)
	}

	fun updateSeriesSelection(oldIndex: Int, newIndex: Int) {
		if ((seriesAdapter ?: return).data.size > oldIndex && oldIndex >= 0) {
			seriesAdapter?.data[oldIndex]?.selected = false
			seriesAdapter?.notifyItemChanged(oldIndex)
		}
		if ((seriesAdapter ?: return).data.size > newIndex && newIndex >= 0) {
			seriesAdapter?.data[newIndex]?.selected = true
			seriesAdapter?.notifyItemChanged(newIndex)
			customSeriesScrollPos(newIndex)
		}
	}

	fun setSeriesGroupVisibility(visibility: Int) {
		tvSeriesGroup?.visibility = visibility
	}

	fun setPlaylistVisibility(visibility: Int) {
		mGridView?.visibility = visibility
		mGridViewFlag?.visibility = visibility
	}

	fun requestGridFocus() {
		mGridView?.requestFocus()
	}

	fun hasFocus(): Boolean {
		return mGridView != null && mGridView!!.hasFocus()
	}

	fun requestFlagFocus() {
		mGridViewFlag?.requestFocus()
	}
}
