package com.github.tvbox.osc.ui.dialog

import android.app.Activity
import android.content.Context
import android.text.TextUtils
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.chad.library.adapter.base.BaseQuickAdapter
import com.github.tvbox.osc.R
import com.github.tvbox.osc.bean.Subtitle
import com.github.tvbox.osc.bean.SubtitleData
import com.github.tvbox.osc.ui.adapter.SearchSubtitleAdapter
import com.github.tvbox.osc.util.FastClickCheckUtil
import com.github.tvbox.osc.viewmodel.SubtitleViewModel
import com.owen.tvrecyclerview.widget.TvRecyclerView
import com.owen.tvrecyclerview.widget.V7LinearLayoutManager
import kotlin.math.min

open class SearchSubtitleDialog(private val mContext: Context) : BaseDialog(mContext) {
	private val maxPage = 5
	private var mGridView: TvRecyclerView? = null
	private var searchAdapter: SearchSubtitleAdapter? = null
	private var subtitleSearchEt: EditText? = null
	private var mSubtitleLoader: SubtitleLoader? = null
	private var loadingBar: ProgressBar? = null
	private var subtitleViewModel: SubtitleViewModel? = null
	private var page = 1
	private var searchWord = ""

	private var zipSubtitles: MutableList<Subtitle>? = ArrayList()
	private var isSearchPag = true

	init {
		if (mContext is Activity) {
			setOwnerActivity(mContext)
		}
		setContentView(R.layout.dialog_search_subtitle)
		initView(mContext)
		initViewModel()
	}

	protected fun initView(context: Context?) {
		loadingBar = findViewById(R.id.loadingBar)
		mGridView = findViewById(R.id.mGridView)
		subtitleSearchEt = findViewById(R.id.input)
		val subtitleSearchBtn = findViewById<TextView>(R.id.inputSubmit)
		searchAdapter = SearchSubtitleAdapter()
		mGridView?.setHasFixedSize(true)
		mGridView?.setLayoutManager(V7LinearLayoutManager(getContext(), 1, false))
		mGridView?.adapter = searchAdapter
		searchAdapter?.setOnItemClickListener { adapter: BaseQuickAdapter<*, *>?, view: View?, position: Int ->
			FastClickCheckUtil.check(view ?: return@setOnItemClickListener)
			val subtitle = (searchAdapter ?: return@setOnItemClickListener).data[position]
			//加载字幕
			if (mSubtitleLoader != null) {
				if (subtitle.isZip) {
					isSearchPag = false
					loadingBar?.visibility = View.VISIBLE
					mGridView?.visibility = View.GONE
					subtitleViewModel?.getSearchResultSubtitleUrls(subtitle)
				} else {
					loadSubtitle(subtitle)
					dismiss()
				}
			}
		}

		searchAdapter?.setOnLoadMoreListener({
			if ((searchAdapter ?: return@setOnLoadMoreListener).data[0].isZip) {
				subtitleViewModel?.searchResult(searchWord, page)
			}
		}, mGridView)

		subtitleSearchBtn.setOnClickListener { v: View? ->
			FastClickCheckUtil.check(v ?: return@setOnClickListener)
			val wd = (subtitleSearchEt ?: return@setOnClickListener).text.toString().trim { it <= ' ' }
			search(wd)
		}
		searchAdapter?.setNewData(ArrayList<Subtitle?>())
	}

	fun setSearchWord(wd: String) {
		var wd = wd
		wd = wd.replace("（|\\(|\\[|【|\\.mp4|\\.mkv|\\.avi|\\.MP4|\\.MKV|\\.AVI".toRegex(), "")
		wd = wd.replace("[：:）)\\]】.]".toRegex(), " ")
		val len = wd.length
		val finalLen = min(len, 36)
		wd = wd.substring(0, finalLen).trim { it <= ' ' }
		subtitleSearchEt?.setText(wd)
		subtitleSearchEt?.setSelection(wd.length)
		subtitleSearchEt?.requestFocus()
	}

	fun search(wd: String) {
		isSearchPag = true
		searchAdapter?.setNewData(ArrayList<Subtitle?>())
		if (!TextUtils.isEmpty(wd)) {
			loadingBar?.visibility = View.VISIBLE
			mGridView?.visibility = View.GONE
			searchWord = wd
			subtitleViewModel?.searchResult(wd, 1.also { page = it })
		} else {
			Toast.makeText(context, "输入内容不能为空", Toast.LENGTH_SHORT).show()
		}
	}

	private fun initViewModel() {
		subtitleViewModel = ViewModelProvider((mContext as ViewModelStoreOwner?) ?: return)[SubtitleViewModel::class.java]
		subtitleViewModel?.searchResult?.observe(mContext as LifecycleOwner) { subtitleData: SubtitleData? ->
			val data = subtitleData?.subtitleList
			loadingBar?.visibility = View.GONE
			mGridView?.visibility = View.VISIBLE
			if (data == null) {
				mGridView?.post { Toast.makeText(context, "未查询到匹配字幕", Toast.LENGTH_SHORT).show() }
				return@observe
			}
			if (!data.isEmpty()) {
				mGridView?.requestFocus()
				if (subtitleData.isZip) {
					if (subtitleData.isNew) {
						searchAdapter?.setNewData(data)
						zipSubtitles = data as MutableList<Subtitle>?
					} else {
						searchAdapter?.addData(data)
						zipSubtitles?.addAll(data)
					}
					page++
					if (page > maxPage) {
						searchAdapter?.loadMoreEnd()
						searchAdapter?.setEnableLoadMore(false)
					} else {
						searchAdapter?.loadMoreComplete()
						searchAdapter?.setEnableLoadMore(true)
					}
				} else {
					searchAdapter?.loadMoreComplete()
					searchAdapter?.setNewData(data)
					searchAdapter?.setEnableLoadMore(false)
				}
			} else {
				if (page > maxPage) {
					searchAdapter?.loadMoreEnd()
				} else {
					searchAdapter?.loadMoreComplete()
				}
				searchAdapter?.setEnableLoadMore(false)
			}
		}
	}

	private fun loadSubtitle(subtitle: Subtitle) {
		subtitleViewModel?.getSubtitleUrl(subtitle, mSubtitleLoader ?: return)
	}

	fun setSubtitleLoader(subtitleLoader: SubtitleLoader?) {
		mSubtitleLoader = subtitleLoader
	}

	@Deprecated("Deprecated in Java")
	override fun onBackPressed() {
		if (!isSearchPag) {
			isSearchPag = true
			loadingBar?.visibility = View.GONE
			mGridView?.visibility = View.VISIBLE
			searchAdapter?.setNewData(zipSubtitles)
			searchAdapter?.setEnableLoadMore(page < maxPage)
			return
		}
		dismiss()
	}

	interface SubtitleLoader {
		fun loadSubtitle(subtitle: Subtitle)
	}
}
