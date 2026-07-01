package com.github.tvbox.osc.ui.fragment

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.chad.library.adapter.base.BaseQuickAdapter
import com.github.tvbox.osc.R
import com.github.tvbox.osc.base.BaseLazyFragment
import com.github.tvbox.osc.bean.AbsXml
import com.github.tvbox.osc.bean.MovieSort
import com.github.tvbox.osc.bean.MovieSort.SortData
import com.github.tvbox.osc.event.RefreshEvent
import com.github.tvbox.osc.ui.activity.DetailActivity
import com.github.tvbox.osc.ui.activity.HomeActivity
import com.github.tvbox.osc.ui.adapter.GridAdapter
import com.github.tvbox.osc.ui.tv.widget.AutoFitGridLayoutManager
import com.github.tvbox.osc.ui.tv.widget.LoadMoreView
import com.github.tvbox.osc.util.FastClickCheckUtil
import com.github.tvbox.osc.viewmodel.SourceViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import org.greenrobot.eventbus.EventBus
import java.util.Stack

/**
 * @author pj567
 * @date :2020/12/21
 * @description:
 */
open class GridFragment : BaseLazyFragment {
	private val mGrids: Stack<GridInfo> = Stack<GridInfo>() //ui栈
	private var sortData: SortData? = null
	private var mGridView: RecyclerView? = null
	private var mSwipe: SwipeRefreshLayout? = null
	private var sourceViewModel: SourceViewModel? = null

	@JvmField
	protected var gridAdapter: GridAdapter? = null
	private var page = 1
	private var maxPage = 1
	private var isLoad = false
	var isTop: Boolean = true
		private set
	private var focusedView: View? = null
	private var filterChipScrollView: HorizontalScrollView? = null
	private var filterButtonContainer: LinearLayout? = null
	private var divider: View? = null
	private var currentBottomSheet: BottomSheetDialog? = null
	private var currentExpandedFilter: MovieSort.SortFilter? = null

	constructor()

	constructor(sortData: SortData?) {
		setArguments(sortData)
	}

	override val layoutResID: Int
		// --- BaseLazyFragment ---
		get() = R.layout.fragment_grid

	override fun init() {
		initView()
		initViewModel()
		initData()
	}

	// ----------------
	fun setArguments(sortData: SortData?): GridFragment {
		this.sortData = sortData
		return this
	}

	private fun changeView(id: String?, isFolder: Boolean) {
		this.sortData?.flag = if (isFolder) "1" else "2"
		initView()
		this.sortData?.id = id // 修改sortData.id为新的ID
		initViewModel()
		initData()
	}

	val isFolderMode: Boolean
		get() = (this.uITag == '1')

	val uITag: Char
		// 获取当前页面UI的显示模式 ‘0’ 正常模式 '1' 文件夹模式 '2' 显示缩略图的文件夹模式
		get() = if (sortData == null || sortData?.flag == null || sortData?.flag?.isEmpty() == true) '0' else sortData!!.flag!![0]

	// 保存当前页面
	private fun saveCurrentView() {
		if (this.mGridView == null) return
		val info = GridInfo()
		info.sortID = this.sortData?.id
		info.mGridView = this.mGridView
		info.gridAdapter = this.gridAdapter
		info.page = this.page
		info.maxPage = this.maxPage
		info.isLoad = this.isLoad
		info.focusedView = this.focusedView
		this.mGrids.push(info)
	}

	// 丢弃当前页面，将页面还原成上一个保存的页面
	fun restoreView(): Boolean {
		if (mGrids.empty()) return false
		this.showSuccess()
		(mGridView?.parent as? ViewGroup)?.removeView(this.mGridView) // 重父窗口移除当前控件
		val info = mGrids.pop() // 还原上次保存的控件
		this.sortData?.id = info.sortID
		this.mGridView = info.mGridView
		this.gridAdapter = info.gridAdapter
		this.page = info.page
		this.maxPage = info.maxPage
		this.isLoad = info.isLoad
		this.focusedView = info.focusedView
		this.mGridView?.visibility = View.VISIBLE
		if (mGridView != null) mGridView?.requestFocus()
		return true
	}

	// 更改当前页面
	private fun createView() {
		this.saveCurrentView() // 保存当前页面
		if (mGridView == null) { // 从layout中拿view
			mGridView = findViewById(R.id.mGridView)
		} else { // 复制当前view
			val v3 = RecyclerView(this.mContext)
			v3.layoutParams = mGridView?.layoutParams
			v3.setPadding((mGridView ?: return).paddingLeft, (mGridView ?: return).paddingTop, (mGridView ?: return).paddingRight, (mGridView ?: return).paddingBottom)
			v3.setClipToPadding((mGridView ?: return).clipToPadding)
			(mGridView?.parent as? ViewGroup)?.addView(v3)
			mGridView?.visibility = View.GONE
			mGridView = v3
			mGridView?.visibility = View.VISIBLE
		}
		mGridView?.setHasFixedSize(true)
		gridAdapter = GridAdapter(this.isFolderMode)
		this.page = 1
		this.maxPage = 1
		this.isLoad = false
	}

	private fun initView() {
		this.createView()

		// 初始化筛选 Split Button 组件
		filterChipScrollView = findViewById(R.id.filterChipScrollView)
		filterButtonContainer = findViewById(R.id.filterButtonContainer)
		divider = findViewById(R.id.divider)
		setupFilterChips()

		mSwipe = findViewById(R.id.mSwipe)
		mSwipe?.setOnRefreshListener {
			page = 1
			initData()
		}
		mSwipe?.setOnChildScrollUpCallback { parent: SwipeRefreshLayout?, child: View? -> mGridView?.canScrollVertically(-1) == true }

		mGridView?.setAdapter(gridAdapter)
		if (this.isFolderMode) {
			mGridView?.setLayoutManager(LinearLayoutManager(this.mContext, LinearLayoutManager.VERTICAL, false))
		} else {
			// 使用自适应网格布局管理器
			val minColumnWidthDp = 150
			mGridView?.setLayoutManager(AutoFitGridLayoutManager(mContext, minColumnWidthDp))
		}

		gridAdapter?.setOnLoadMoreListener({
			gridAdapter?.setEnableLoadMore(true)
			sourceViewModel?.getList(sortData ?: return@setOnLoadMoreListener, page)
		}, mGridView)
		gridAdapter?.setOnItemClickListener { adapter: BaseQuickAdapter<*, *>?, view: View?, position: Int ->
			FastClickCheckUtil.check(requireView())
			val video = (gridAdapter ?: return@setOnItemClickListener).data[position]
			if (video != null) {
				val bundle = Bundle()
				bundle.putString("id", video.id)
				bundle.putString("sourceKey", video.sourceKey)
				bundle.putString("title", video.name)
				if (video.tag != null && (video.tag == "folder" || video.tag == "cover")) {
					focusedView = view
					if (("12".indexOf(this.uITag) != -1)) {
						changeView(video.id, video.tag == "folder")
					} else {
						changeView(video.id, false)
					}
				} else {
					if (video.id == null || video.id?.isEmpty() == true || video.id?.startsWith("msearch:") == true) {
						(mActivity as? HomeActivity)?.switchToSearchAndSearch(video.name)
					} else {
						bundle.putString("picture", video.pic)
						jumpActivity(DetailActivity::class.java, bundle)
					}
				}
			}
		}
		gridAdapter?.setLoadMoreView(LoadMoreView())
		setLoadSir2(mGridView)
	}

	private fun initViewModel() {
		if (sourceViewModel != null) {
			return
		}
		sourceViewModel = ViewModelProvider(this)[SourceViewModel::class.java]
		sourceViewModel?.listResult?.observe(this, Observer { absXml: AbsXml? ->
			if (mSwipe != null && mSwipe?.isRefreshing == true) {
				mSwipe?.isRefreshing = false
			}
			if (absXml != null && absXml.movie != null && absXml.movie?.videoList != null && absXml.movie?.videoList?.isEmpty() != true) {
				if (page == 1) {
					showSuccess()
					isLoad = true
					gridAdapter?.setNewData(absXml.movie?.videoList)
				} else {
					gridAdapter?.addData(absXml.movie?.videoList ?: return@Observer)
				}
				page++
				maxPage = (absXml.movie ?: return@Observer).pageCount
				if (maxPage in 1..<page) {
					gridAdapter?.loadMoreEnd()
					gridAdapter?.setEnableLoadMore(false)
					if (page > 2) Toast.makeText(context, "没有更多了", Toast.LENGTH_SHORT).show()
				} else {
					gridAdapter?.loadMoreComplete()
					gridAdapter?.setEnableLoadMore(true)
				}
			} else {
				if (page == 1) {
					showEmpty()
				} else if (page > 2) {
					Toast.makeText(context, "没有更多了", Toast.LENGTH_SHORT).show()
				}
				gridAdapter?.loadMoreEnd()
				gridAdapter?.setEnableLoadMore(false)
			}
		})
	}

	protected open fun initData() {
		if (mSwipe != null && mSwipe?.isRefreshing != true) {
			mSwipe?.isRefreshing = true
		}
		isLoad = false
		scrollTop()
		setupFilterChips()
		toggleFilterColor()
		sourceViewModel?.getList(sortData ?: return, page)
	}

	private fun toggleFilterColor() {
		if (sortData != null && sortData?.filters?.isEmpty() != true) {
			val count = sortData?.filterSelectCount()
			EventBus.getDefault().post(RefreshEvent(RefreshEvent.TYPE_FILTER_CHANGE, count))
		}
	}

	fun scrollTop() {
		isTop = true
		mGridView?.scrollToPosition(0)
	}

	private fun setupFilterChips() {
		if (sortData == null || sortData?.filters?.isEmpty() == true) {
			filterChipScrollView?.visibility = View.GONE
			divider?.visibility = View.GONE
			return
		}

		filterChipScrollView?.visibility = View.VISIBLE
		divider?.visibility = View.VISIBLE
		filterButtonContainer?.removeAllViews()

		for (filter in (sortData ?: return).filters) {
			val splitButtonView = LayoutInflater.from(mContext).inflate(R.layout.item_filter_split_button, filterButtonContainer, false)

			val mainButton = splitButtonView.findViewById<MaterialButton>(R.id.splitButtonMain)
			val dropdownButton = splitButtonView.findViewById<MaterialButton>(R.id.splitButtonDropdown)

			var displayText = filter.name
			val selectedValue = sortData?.filterSelect[filter.key]

			if (selectedValue != null) {
				for (key in (filter.values ?: return).keys) {
					val value = (filter.values ?: return)[key]
					if (value != null && value == selectedValue) {
						displayText = filter.name + ": " + key
						break
					}
				}
			}

			mainButton.text = displayText

			val clickListener = View.OnClickListener { v: View? -> showFilterBottomSheet(filter) }
			mainButton.setOnClickListener(clickListener)
			dropdownButton.setOnClickListener(clickListener)

			// 如果这个筛选项当前是展开状态，恢复其展开状态
			if (currentExpandedFilter != null && currentExpandedFilter == filter) {
				dropdownButton.isChecked = true
			}

			filterButtonContainer?.addView(splitButtonView)
		}
	}

	private fun showFilterBottomSheet(filter: MovieSort.SortFilter) {
		if (currentBottomSheet != null && currentBottomSheet?.isShowing == true) {
			currentBottomSheet?.dismiss()
		}

		// 记录当前展开的筛选项
		currentExpandedFilter = filter

		currentBottomSheet = BottomSheetDialog(mContext)
		val view = LayoutInflater.from(mContext).inflate(R.layout.bottom_sheet_filter_options, currentBottomSheet?.findViewById(android.R.id.content), false)

		val titleView = view.findViewById<TextView>(R.id.filterTitle)
		titleView.text = filter.name

		val chipGroup = view.findViewById<ChipGroup>(R.id.chipGroup)
		chipGroup.removeAllViews()

		val currentSelection = sortData?.filterSelect[filter.key]

		// 添加选项 Chips
		val displayValues = ArrayList<String?>((filter.values ?: return).keys)
		val actualValues = ArrayList<String>((filter.values ?: return).values)

		for (i in displayValues.indices) {
			val displayValue = displayValues[i]
			val actualValue = actualValues[i]

			val optionChip = Chip(mContext)
			optionChip.text = displayValue
			optionChip.isCheckable = true
			optionChip.isChecked = actualValue == currentSelection

			optionChip.setOnClickListener { v: View? ->
				if (actualValue == sortData?.filterSelect[filter.key]) {
					sortData?.filterSelect?.remove(filter.key)
				} else {
					sortData?.filterSelect[filter.key ?: return@setOnClickListener] = actualValue
				}
				// 立即刷新数据，setupFilterChips 会自动恢复箭头展开状态
				setupFilterChips()
				forceRefresh()
			}

			chipGroup.addView(optionChip)
		}

		currentBottomSheet?.setOnDismissListener { dialog: DialogInterface? ->
			// 清除展开状态记录
			currentExpandedFilter = null
			// 重新查找按钮（因为 setupFilterChips 可能已经重新创建了按钮）
			val triggerButton = findDropdownButtonForFilter(filter)
			// 播放收起动画
			if (triggerButton != null) {
				triggerButton.isChecked = false
			}
		}

		currentBottomSheet?.setContentView(view)
		currentBottomSheet?.show()

		// 播放展开动画
		val triggerButton = findDropdownButtonForFilter(filter)
		if (triggerButton != null) {
			triggerButton.isChecked = true
		}
	}

	private fun findDropdownButtonForFilter(filter: MovieSort.SortFilter?): MaterialButton? {
		if (sortData == null) {
			return null
		}

		val filterIndex = (sortData ?: return null).filters.indexOf(filter)
		if (filterIndex < 0 || filterIndex >= (filterButtonContainer ?: return null).childCount) {
			return null
		}

		val splitButtonView = filterButtonContainer?.getChildAt(filterIndex)
		return splitButtonView?.findViewById(R.id.splitButtonDropdown)
	}

	fun forceRefresh() {
		page = 1
		initData()
	}

	private class GridInfo {
		var sortID: String? = ""
		var mGridView: RecyclerView? = null
		var gridAdapter: GridAdapter? = null
		var page: Int = 1
		var maxPage: Int = 1
		var isLoad: Boolean = false
		var focusedView: View? = null
	}
}
