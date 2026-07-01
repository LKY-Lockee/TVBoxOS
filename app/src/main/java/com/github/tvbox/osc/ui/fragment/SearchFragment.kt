package com.github.tvbox.osc.ui.fragment

import android.content.Context
import android.content.DialogInterface
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.github.catvod.crawler.JsLoader
import com.github.tvbox.osc.R
import com.github.tvbox.osc.api.ApiConfig
import com.github.tvbox.osc.base.BackPressProvider
import com.github.tvbox.osc.base.BaseLazyFragment
import com.github.tvbox.osc.base.ToolbarMenuProvider
import com.github.tvbox.osc.bean.AbsXml
import com.github.tvbox.osc.bean.Movie
import com.github.tvbox.osc.bean.SourceBean
import com.github.tvbox.osc.data.ConfigKey
import com.github.tvbox.osc.data.PreferenceStore
import com.github.tvbox.osc.event.RefreshEvent
import com.github.tvbox.osc.event.ServerEvent
import com.github.tvbox.osc.ui.activity.HomeActivity
import com.github.tvbox.osc.ui.adapter.PinyinAdapter
import com.github.tvbox.osc.ui.adapter.PinyinAdapter.SearchItem
import com.github.tvbox.osc.util.FastClickCheckUtil
import com.github.tvbox.osc.util.HistoryHelper.setSearchHistory
import com.github.tvbox.osc.util.SearchHelper.sourcesForSearch
import com.github.tvbox.osc.viewmodel.SourceViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.search.SearchBar
import com.google.android.material.search.SearchView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.lzy.okgo.OkGo
import com.lzy.okgo.callback.AbsCallback
import com.lzy.okgo.model.Response
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.Objects
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs

class SearchFragment : BaseLazyFragment(), BackPressProvider, ToolbarMenuProvider {
	private var mTabLayout: TabLayout? = null
	private var searchBar: SearchBar? = null
	private var btnStopSearch: MaterialButton? = null
	private var searchBarContainer: CoordinatorLayout? = null
	private var searchView: SearchView? = null
	private var rvSearchWords: RecyclerView? = null
	private var wordAdapter: PinyinAdapter? = null
	private var searchProgressIndicator: LinearProgressIndicator? = null

	private var resultFragment: SearchResultFragment? = null
	private var currentSourceFilter = "all"

	private var searchTitle = ""
	private val searchResults = HashMap<String, ArrayList<Movie.Video?>>()
	private var pauseRunnable: MutableList<Runnable?>? = null
	private var searchExecutorService: ExecutorService? = null
	private val allRunCount = AtomicInteger(0)
	private var totalSourceCount = 0
	private var preDrawListener: ViewTreeObserver.OnPreDrawListener? = null

	// --- BackPressProvider ---
	override fun handleBackPress(): Boolean {
		if (currentSourceFilter != "all") {
			mTabLayout?.selectTab(mTabLayout?.getTabAt(0))
			return true
		}
		return false
	}

	override val layoutResID: Int
		// ----------------
		get() = R.layout.fragment_search

	override fun init() {
		EventBus.getDefault().register(this)

		mTabLayout = rootView?.findViewById(R.id.mTabLayout)
		searchBar = rootView?.findViewById(R.id.search_bar)
		btnStopSearch = rootView?.findViewById(R.id.btn_stop_search)
		searchBarContainer = rootView?.findViewById(R.id.search_bar_container)
		searchView = rootView?.findViewById(R.id.search_view)
		searchProgressIndicator = rootView?.findViewById(R.id.search_progress)

		// 设置停止搜索按钮点击事件
		btnStopSearch?.setOnClickListener { v: View? -> cancel() }

		// 动态定位搜索框到底部导航栏上方
		updateSearchBarPosition()

		searchView?.addTransitionListener { searchView: SearchView?, previousState: SearchView.TransitionState?, newState: SearchView.TransitionState? ->
			if (activity == null) return@addTransitionListener
			if (newState == SearchView.TransitionState.SHOWING) {
				(mActivity as? HomeActivity)?.collapseBottomNav()
			} else if (newState == SearchView.TransitionState.HIDDEN) {
				(mActivity as? HomeActivity)?.expandBottomNav()
			}

			val spacerView = searchView?.findViewById<View?>(R.id.open_search_view_status_bar_spacer)
			if (spacerView != null) {
				val parent = spacerView.parent as ViewGroup?
				parent?.removeView(spacerView)
			}
		}

		rvSearchWords = rootView?.findViewById(R.id.rv_search_words)

		initSearchViews()

		resultFragment = SearchResultFragment()
		resultFragment?.let {
			it.setOnRefreshListener {
				if (!TextUtils.isEmpty(searchTitle)) {
					search(searchTitle)
				}
			}
			getChildFragmentManager().beginTransaction()
				.replace(R.id.searchResultContainer, it)
				.commit()
		}

		mTabLayout?.addOnTabSelectedListener(object : OnTabSelectedListener {
			override fun onTabSelected(tab: TabLayout.Tab) {
				val sourceKey = tab.tag as String?
				if (sourceKey != null) {
					filterBySource(sourceKey)
				}
			}

			override fun onTabUnselected(tab: TabLayout.Tab?) {
			}

			override fun onTabReselected(tab: TabLayout.Tab?) {
			}
		})
		mTabLayout?.removeAllTabs()
		mTabLayout?.visibility = View.GONE
	}

	override fun onDestroy() {
		super.onDestroy()
		cancel()
		try {
			if (searchExecutorService != null) {
				searchExecutorService?.shutdownNow()
				searchExecutorService = null
				JsLoader.stopAll()
			}
		} catch (th: Throwable) {
			th.printStackTrace()
		}

		// 移除监听器
		if (rootView != null && preDrawListener != null) {
			if (rootView?.viewTreeObserver?.isAlive == true) {
				rootView?.viewTreeObserver?.removeOnPreDrawListener(preDrawListener)
			}
			preDrawListener = null
		}

		EventBus.getDefault().unregister(this)
	}

	override fun onResume() {
		super.onResume()
		if (pauseRunnable != null && pauseRunnable?.isEmpty() != true) {
			searchExecutorService = Executors.newFixedThreadPool(5)
			for (runnable in pauseRunnable) {
				searchExecutorService?.execute(runnable)
			}
			pauseRunnable?.clear()
			pauseRunnable = null
		}
		// 确保搜索框位置正确
		updateSearchBarPosition()
	}

	override val menuResId: Int
		// ----------------
		get() = R.menu.search_fragment_menu

	override fun onMenuItemClick(itemId: Int): Boolean {
		if (itemId == R.id.action_clear_search_history) {
			showClearHistoryDialog()
			return true
		}
		return false
	}

	override val toolbarTitle: String
		get() = "搜索"

	override fun enableAppBarScroll(): Boolean {
		return true
	}

	// ----------------
	private fun initSearchViews() {
		rvSearchWords?.setHasFixedSize(true)
		rvSearchWords?.setLayoutManager(LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false))
		wordAdapter = PinyinAdapter()
		rvSearchWords?.setAdapter(wordAdapter)

		wordAdapter?.setOnItemClickListener { adapter: BaseQuickAdapter<*, *>?, view: View?, position: Int ->
			FastClickCheckUtil.check(requireView())
			val item = wordAdapter?.getItem(position) ?: return@setOnItemClickListener
			val keyword = item.title
			searchView?.setText(keyword)
			searchView?.hide()
			search(keyword ?: return@setOnItemClickListener)
		}

		// 设置长按监听器（仅对历史记录生效）
		wordAdapter?.setOnItemLongClickListener { position: Int, item: SearchItem? ->
			if (item?.type == 0) {
				showDeleteHistoryItemDialog(item.title, position)
			}
		}

		searchView?.setupWithSearchBar(searchBar)
		searchView?.getEditText()?.setOnEditorActionListener { v: TextView?, actionId: Int, event: KeyEvent? ->
			if (actionId == EditorInfo.IME_ACTION_SEARCH ||
				(event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
			) {
				val keyword = searchView?.text.toString().trim { it <= ' ' }
				searchView?.hide()
				search(keyword)
				return@setOnEditorActionListener true
			}
			false
		}

		searchView?.getEditText()?.addTextChangedListener(object : TextWatcher {
			override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
			}

			override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
			}

			override fun afterTextChanged(s: Editable) {
				val text = s.toString().trim { it <= ' ' }
				if (!text.isEmpty()) {
					loadSearchSuggestions(text)
				} else {
					loadHistoryAndHotWords()
				}
			}
		})

		searchView?.addTransitionListener { searchView: SearchView?, previousState: SearchView.TransitionState?, newState: SearchView.TransitionState? ->
			if (newState == SearchView.TransitionState.SHOWING) {
				loadHistoryAndHotWords()
			}
		}
	}

	private fun loadHistoryAndHotWords() {
		val historyList = PreferenceStore.getObj(ConfigKey.SEARCH_HISTORY, ArrayList<String?>())

		val combinedList = ArrayList<SearchItem?>()
		for (s in historyList) {
			combinedList.add(SearchItem(s, 0))
		}

		if (hots != null && hots?.isEmpty() != true) {
			for (s in hots) {
				combinedList.add(SearchItem(s, 1))
			}
			wordAdapter?.setNewData(combinedList)
			return
		}

		wordAdapter?.setNewData(combinedList)

		OkGo.get<String?>("https://node.video.qq.com/x/api/hot_search")
			.params("channdlId", "0")
			.params("_", System.currentTimeMillis())
			.execute(object : AbsCallback<String?>() {
				override fun onSuccess(response: Response<String?>) {
					try {
						hots = ArrayList()
						val itemList = JsonParser.parseString(response.body())
							.getAsJsonObject().get("data").getAsJsonObject()
							.get("mapResult").getAsJsonObject()
							.get("0").getAsJsonObject()
							.get("listInfo").getAsJsonArray()
						for (ele in itemList) {
							val obj = ele as JsonObject
							hots?.add(obj.get("title").asString.trim { it <= ' ' }
								.replace("[<>《》\\-]".toRegex(), "").split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0])
						}
						val updatedList = ArrayList<SearchItem?>()
						for (s in historyList) {
							updatedList.add(SearchItem(s, 0))
						}
						for (s in hots ?: return) {
							updatedList.add(SearchItem(s, 1))
						}
						wordAdapter?.setNewData(updatedList)
					} catch (th: Throwable) {
						th.printStackTrace()
					}
				}

				override fun convertResponse(response: okhttp3.Response): String {
					return Objects.requireNonNull(response.body).string()
				}
			})
	}

	private fun loadSearchSuggestions(key: String?) {
		OkGo.get<Any?>("https://tv.aiseet.atianqi.com/i-tvbin/qtv_video/search/get_search_smart_box")
			.params("format", "json")
			.params("page_num", 0)
			.params("page_size", 20)
			.params("key", key)
			.execute(object : AbsCallback<Any?>() {
				override fun onSuccess(response: Response<Any?>) {
					try {
						val suggestions = ArrayList<SearchItem?>()
						val result = response.body() as String?
						val gson = Gson()
						val json = gson.fromJson(result, JsonElement::class.java)
						val groupDataArr = json.getAsJsonObject()
							.get("data").getAsJsonObject()
							.get("search_data").getAsJsonObject()
							.get("vecGroupData").getAsJsonArray()
							.get(0).getAsJsonObject()
							.get("group_data").getAsJsonArray()
						for (groupDataElement in groupDataArr) {
							val groupData = groupDataElement.getAsJsonObject()
							val keywordTxt = groupData.getAsJsonObject("dtReportInfo")
								.getAsJsonObject("reportData")
								.get("keyword_txt").asString
							suggestions.add(SearchItem(keywordTxt.trim { it <= ' ' }, 2))
						}
						wordAdapter?.setNewData(suggestions)
						rvSearchWords?.smoothScrollToPosition(0)
					} catch (th: Throwable) {
						th.printStackTrace()
					}
				}

				override fun convertResponse(response: okhttp3.Response): String {
					return Objects.requireNonNull(response.body).string()
				}
			})
	}

	private fun updateSearchBarPosition() {
		if (rootView == null || activity == null) return
		rootView?.post {
			if (activity == null || searchBarContainer == null) return@post
			val bottomNav = requireActivity().findViewById<View?>(R.id.bottom_navigation)

			searchBarContainer?.let {
				val params = it.layoutParams as CoordinatorLayout.LayoutParams
				params.gravity = Gravity.NO_GRAVITY
				params.setMargins(params.leftMargin, 0, params.rightMargin, 0)
				it.layoutParams = params
			}

			if (preDrawListener != null) {
				rootView?.viewTreeObserver?.removeOnPreDrawListener(preDrawListener)
			}

			// 使用 OnPreDrawListener 监听每一帧
			preDrawListener = ViewTreeObserver.OnPreDrawListener {
				if (activity == null) return@OnPreDrawListener true
				searchBarContainer?.let {
					if (rootView != null) {
						val targetY: Int

						if (bottomNav != null && bottomNav.isVisible) {
							// 有底部导航栏时，定位在导航栏上方
							val navLocation = IntArray(2)
							bottomNav.getLocationInWindow(navLocation)
							val navTopInScreen = navLocation[1]

							val rootLocation = IntArray(2)
							rootView?.getLocationInWindow(rootLocation)
							val rootTopInScreen = rootLocation[1]

							val searchBarHeight = it.height
							targetY = navTopInScreen - rootTopInScreen - searchBarHeight
						} else {
							// 没有底部导航栏时，定位在屏幕底部
							val rootLocation = IntArray(2)
							rootView?.getLocationInWindow(rootLocation)
							val rootTopInScreen = rootLocation[1]

							val screenHeight = resources.displayMetrics.heightPixels
							val searchBarHeight = it.height
							targetY = screenHeight - rootTopInScreen - searchBarHeight
						}

						// 使用setY设置绝对位置
						if (abs(it.y - targetY) > 0.5f) {
							it.y = targetY.toFloat()
						}
					}
				}
				true
			}
			rootView?.viewTreeObserver?.addOnPreDrawListener(preDrawListener)
		}
	}

	private fun showEmptyState() {
		mTabLayout?.removeAllTabs()
		mTabLayout?.visibility = View.GONE
		currentSourceFilter = "all"
		if (searchProgressIndicator != null) {
			searchProgressIndicator?.visibility = View.GONE
		}
		if (resultFragment != null && resultFragment?.isAdded == true) {
			resultFragment?.updateData(ArrayList())
		}
	}

	private fun showStopSearchButton() {
		if (btnStopSearch == null) return

		btnStopSearch?.visibility = View.VISIBLE
		btnStopSearch?.animate()
			?.alpha(1f)
			?.setDuration(200)
			?.setInterpolator(DecelerateInterpolator())
			?.start()
	}

	private fun hideStopSearchButton() {
		if (btnStopSearch == null) return

		btnStopSearch?.animate()
			?.alpha(0f)
			?.setDuration(200)
			?.setInterpolator(AccelerateInterpolator())
			?.withEndAction {
				if (btnStopSearch != null) {
					(btnStopSearch ?: return@withEndAction).visibility = View.GONE
				}
			}
			?.start()
	}

	fun search(keyword: String) {
		if (TextUtils.isEmpty(keyword)) {
			if (isAdded) {
				Toast.makeText(mContext, "输入内容不能为空", Toast.LENGTH_SHORT).show()
			}
			return
		}

		if (!isAdded || searchBar == null) {
			return
		}

		this.searchTitle = keyword
		searchBar?.setText(keyword)

		setSearchHistory(keyword)

		searchResults.clear()
		showEmptyState()

		hideSoftInput()

		searchResult()
	}

	private fun cancel() {
		OkGo.getInstance().cancelTag("search")

		try {
			if (searchExecutorService != null) {
				searchExecutorService?.shutdownNow()
				searchExecutorService = null
				JsLoader.stopAll()
			}
		} catch (th: Throwable) {
			th.printStackTrace()
		}

		allRunCount.set(0)

		if (searchProgressIndicator != null) {
			searchProgressIndicator?.visibility = View.GONE
		}

		hideStopSearchButton()
	}

	private fun searchResult() {
		try {
			if (searchExecutorService != null) {
				searchExecutorService?.shutdownNow()
				searchExecutorService = null
				JsLoader.stopAll()
			}
		} catch (th: Throwable) {
			th.printStackTrace()
		} finally {
			allRunCount.set(0)
		}

		searchExecutorService = Executors.newFixedThreadPool(5)
		val searchRequestList = ApiConfig.instance.getSourceBeanList().toMutableList()
		val home: SourceBean = ApiConfig.instance.homeSourceBean
		searchRequestList.remove(home)
		searchRequestList.add(0, home)

		val mCheckSources = sourcesForSearch
		val siteKey = ArrayList<String?>()
		for (bean in searchRequestList) {
			if (!bean.isSearchable) {
				continue
			}
			if (mCheckSources != null && !mCheckSources.containsKey(bean.key)) {
				continue
			}
			siteKey.add(bean.key)
			allRunCount.incrementAndGet()
		}

		if (siteKey.isEmpty()) {
			Toast.makeText(mContext, "没有指定搜索源", Toast.LENGTH_SHORT).show()
			return
		}

		totalSourceCount = siteKey.size
		if (searchProgressIndicator != null) {
			searchProgressIndicator?.max = totalSourceCount
			searchProgressIndicator?.progress = 0
			searchProgressIndicator?.visibility = View.VISIBLE
		}

		showStopSearchButton()

		val sourceViewModel =
			ViewModelProvider(requireActivity())[SourceViewModel::class.java]

		for (key in siteKey) {
			searchExecutorService?.execute { sourceViewModel.getSearch(key, searchTitle) }
		}
	}

	private fun searchData(absXml: AbsXml?) {
		var hasNewResults = false

		if (absXml != null && absXml.movie != null && absXml.movie?.videoList != null && absXml.movie?.videoList?.isEmpty() != true) {
			val sourceKey = ((absXml.movie ?: return).videoList ?: return)[0].sourceKey
			val sourceResults = searchResults.computeIfAbsent(sourceKey ?: return) { k: String? -> ArrayList() }

			val oldSize = sourceResults.size
			for (video in (absXml.movie ?: return).videoList ?: return) {
				if (matchSearchResult(video.name, searchTitle)) {
					sourceResults.add(video)
				}
			}

			hasNewResults = sourceResults.size > oldSize
		}

		val count = allRunCount.decrementAndGet()

		if (searchProgressIndicator != null && totalSourceCount > 0) {
			val searchedCount = totalSourceCount - count
			searchProgressIndicator?.progress = searchedCount
		}

		if (hasNewResults) {
			if ((mTabLayout ?: return).tabCount <= 0) {
				createTabsFromResults()
			} else {
				updateTabsWithNewResults()
			}
		}

		if (count <= 0) {
			if (searchProgressIndicator != null) {
				searchProgressIndicator?.visibility = View.GONE
			}

			hideStopSearchButton()

			if ((mTabLayout ?: return).tabCount <= 0) {
				createTabsFromResults()
			}
			cancel()
		}
	}

	private fun filterBySource(sourceKey: String) {
		currentSourceFilter = sourceKey

		var filteredResults: ArrayList<Movie.Video?>?
		if ("all" == sourceKey) {
			filteredResults = ArrayList()
			for (videos in searchResults.values) {
				filteredResults.addAll(videos)
			}
		} else {
			filteredResults = searchResults[sourceKey]
			if (filteredResults == null) {
				filteredResults = ArrayList()
			}
		}

		if (resultFragment != null && resultFragment?.isAdded == true) {
			resultFragment?.updateData(filteredResults)
		}
	}

	private fun matchSearchResult(name: String?, searchTitle: String?): Boolean {
		var searchTitle = searchTitle
		if (TextUtils.isEmpty(name) || TextUtils.isEmpty(searchTitle)) return false
		searchTitle = searchTitle?.trim { it <= ' ' }
		val arr = searchTitle!!.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
		var matchNum = 0
		for (one in arr) {
			if (name?.contains(one) == true) matchNum++
		}
		return matchNum == arr.size
	}

	private fun createTabsFromResults() {
		mTabLayout?.removeAllTabs()

		for (sourceKey in searchResults.keys) {
			val videos = searchResults[sourceKey]
			if (!videos.isNullOrEmpty()) {
				val source: SourceBean? = ApiConfig.instance.getSource(sourceKey)
				if (source != null) {
					val tab = mTabLayout?.newTab()
					tab?.let {
						it.text = source.name + " (" + videos.size + ")"
						it.tag = sourceKey
						mTabLayout?.addTab(tab)
					}
				}
			}
		}

		if ((mTabLayout ?: return).tabCount > 0) {
			val allTab = mTabLayout?.newTab()
			allTab?.let {
				allTab.text = "全部"
				allTab.tag = "all"
				mTabLayout?.addTab(allTab, 0)
				mTabLayout?.visibility = View.VISIBLE
			}

			val firstTab = mTabLayout?.getTabAt(0)
			firstTab?.select()
			filterBySource("all")
		} else {
			mTabLayout?.visibility = View.GONE
		}
	}

	private fun updateTabsWithNewResults() {
		for (sourceKey in searchResults.keys) {
			var tabExists = false
			for (i in 0..<(mTabLayout ?: return).tabCount) {
				val tab = mTabLayout?.getTabAt(i)
				if (tab != null && sourceKey == tab.tag) {
					val videos = searchResults[sourceKey]
					if (videos != null) {
						val source: SourceBean? = ApiConfig.instance.getSource(sourceKey)
						if (source != null) {
							tab.text = source.name + " (" + videos.size + ")"
						}
					}
					tabExists = true
					break
				}
			}

			if (!tabExists) {
				val videos = searchResults[sourceKey]
				if (!videos.isNullOrEmpty()) {
					val source: SourceBean? = ApiConfig.instance.getSource(sourceKey)
					if (source != null) {
						val tab = (mTabLayout ?: return).newTab()
						tab.text = source.name + " (" + videos.size + ")"
						tab.tag = sourceKey
						mTabLayout?.addTab(tab)
					}
				}
			}
		}

		filterBySource(currentSourceFilter)
	}

	private fun showDeleteHistoryItemDialog(keyword: String?, position: Int) {
		if (activity == null) return

		MaterialAlertDialogBuilder(requireActivity())
			.setTitle("删除搜索记录")
			.setMessage("确定要删除「$keyword」吗？")
			.setPositiveButton("删除") { dialog: DialogInterface?, which: Int ->
				// 从 Hawk 中获取历史记录
				val historyList = PreferenceStore.getObj(ConfigKey.SEARCH_HISTORY, ArrayList<String?>())
				historyList.remove(keyword)
				PreferenceStore.putObj(ConfigKey.SEARCH_HISTORY, historyList)

				// 从 Adapter 中移除
				wordAdapter?.remove(position)
			}
			.setNegativeButton("取消", null)
			.show()
	}

	private fun showClearHistoryDialog() {
		if (activity == null) return

		val historyList = PreferenceStore.getObj(ConfigKey.SEARCH_HISTORY, ArrayList<String?>())
		if (historyList.isEmpty()) {
			Toast.makeText(mContext, "暂无搜索记录", Toast.LENGTH_SHORT).show()
			return
		}

		MaterialAlertDialogBuilder(requireActivity())
			.setTitle("清空搜索记录")
			.setMessage("确定要清空所有搜索记录吗？")
			.setPositiveButton("清空") { dialog: DialogInterface?, which: Int ->
				PreferenceStore.delete(ConfigKey.SEARCH_HISTORY)
				val newList = ArrayList<SearchItem?>()
				if (hots != null && hots?.isEmpty() != false) {
					for (s in hots) {
						newList.add(SearchItem(s, 1))
					}
				}
				wordAdapter?.setNewData(newList)
				Toast.makeText(mContext, "已清空搜索记录", Toast.LENGTH_SHORT).show()
			}
			.setNegativeButton("取消", null)
			.show()
	}

	private fun hideSoftInput() {
		try {
			val imm = mContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
			if (imm != null && searchView != null) {
				imm.hideSoftInputFromWindow(searchView?.windowToken, 0)
			}
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}

	@Subscribe(threadMode = ThreadMode.MAIN)
	fun server(event: ServerEvent) {
		if (event.type == ServerEvent.SERVER_SEARCH) {
			val title = event.obj as String?
			search(title ?: return)
		}
	}

	@Subscribe(threadMode = ThreadMode.MAIN)
	fun refresh(event: RefreshEvent) {
		if (event.type == RefreshEvent.TYPE_SEARCH_RESULT) {
			try {
				searchData(if (event.obj == null) null else event.obj as AbsXml)
			} catch (e: Exception) {
				searchData(null)
			}
		}
	}

	companion object {
		private var hots: ArrayList<String?>? = null
	}
}
