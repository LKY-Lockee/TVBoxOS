package com.github.tvbox.osc.ui.fragment

import android.content.Intent
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.chad.library.adapter.base.BaseQuickAdapter
import com.github.tvbox.osc.R
import com.github.tvbox.osc.base.BaseLazyFragment
import com.github.tvbox.osc.bean.Movie
import com.github.tvbox.osc.ui.activity.DetailActivity
import com.github.tvbox.osc.ui.adapter.SearchAdapter
import com.github.tvbox.osc.ui.tv.widget.AutoFitGridLayoutManager
import com.github.tvbox.osc.util.FastClickCheckUtil

class SearchResultFragment : BaseLazyFragment() {
	private var dataList: ArrayList<Movie.Video?>? = null

	private var searchAdapter: SearchAdapter? = null
	private var mSwipe: SwipeRefreshLayout? = null
	private var onRefreshListener: Runnable? = null

	fun setOnRefreshListener(listener: Runnable?) {
		this.onRefreshListener = listener
	}

	override val layoutResID: Int
		get() = R.layout.fragment_grid

	override fun init() {
		if (rootView == null) {
			return
		}

		val mGridView = rootView?.findViewById<RecyclerView>(R.id.mGridView)
		mGridView?.setHasFixedSize(true)

		mSwipe = rootView?.findViewById(R.id.mSwipe)
		mSwipe?.setOnChildScrollUpCallback { parent: SwipeRefreshLayout?, child: View? -> mGridView?.canScrollVertically(-1) == true }
		mSwipe?.setOnRefreshListener {
			onRefreshListener?.run()
			mSwipe?.isRefreshing = false
		}

		val minColumnWidthDp = 150
		mGridView?.setLayoutManager(AutoFitGridLayoutManager(mContext, minColumnWidthDp))

		searchAdapter = SearchAdapter()
		mGridView?.setAdapter(searchAdapter)

		searchAdapter?.setOnItemClickListener { adapter: BaseQuickAdapter<*, *>?, view: View?, position: Int ->
			FastClickCheckUtil.check(requireView())
			val video = searchAdapter?.data[position]
			if (video != null) {
				val intent = Intent(mContext, DetailActivity::class.java)
				intent.putExtra("id", video.id)
				intent.putExtra("sourceKey", video.sourceKey)
				startActivity(intent)
			}
		}

		setLoadSir(rootView)

		if (dataList != null && dataList?.isEmpty() != true) {
			showSuccess()
			searchAdapter?.setNewData(dataList)
		} else {
			showEmpty()
		}
	}

	fun updateData(newData: ArrayList<Movie.Video?>?) {
		this.dataList = newData
		if (searchAdapter != null && newData != null && isAdded) {
			if (!newData.isEmpty()) {
				showSuccess()
				searchAdapter?.setNewData(newData)
			} else {
				showEmpty()
			}
		}
	}
}
