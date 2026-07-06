package com.github.tvbox.osc.ui.home

import android.app.Application
import android.text.TextUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.catvod.crawler.JsLoader
import com.github.tvbox.osc.api.ApiConfig
import com.github.tvbox.osc.bean.AbsXml
import com.github.tvbox.osc.bean.Movie
import com.github.tvbox.osc.data.ConfigKey
import com.github.tvbox.osc.data.PreferenceStore
import com.github.tvbox.osc.event.RefreshEvent
import com.github.tvbox.osc.event.ServerEvent
import com.github.tvbox.osc.ui.compose.component.SearchWordItem
import com.github.tvbox.osc.util.HistoryHelper.setSearchHistory
import com.github.tvbox.osc.util.SearchHelper.sourcesForSearch
import com.github.tvbox.osc.viewmodel.SourceViewModel
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.lzy.okgo.OkGo
import com.lzy.okgo.callback.AbsCallback
import com.lzy.okgo.model.Response
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

data class SourceTab(val key: String, val name: String, val count: Int)

class SearchViewModel(app: Application) : AndroidViewModel(app) {

	private val sourceVm: SourceViewModel = SourceViewModel()

	private val _searchTitle = MutableStateFlow("")
	val searchTitle: StateFlow<String> = _searchTitle.asStateFlow()

	private val _isSearching = MutableStateFlow(false)
	val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

	private val _progress = MutableStateFlow(0 to 0)
	val progress: StateFlow<Pair<Int, Int>> = _progress.asStateFlow()

	private val _tabs = MutableStateFlow<List<SourceTab>>(emptyList())
	val tabs: StateFlow<List<SourceTab>> = _tabs.asStateFlow()

	private val _currentFilter = MutableStateFlow("all")
	val currentFilter: StateFlow<String> = _currentFilter.asStateFlow()

	private val _results = MutableStateFlow<List<Movie.Video?>>(emptyList())
	val results: StateFlow<List<Movie.Video?>> = _results.asStateFlow()

	private val _suggestions = MutableStateFlow<List<SearchWordItem>>(emptyList())
	val suggestions: StateFlow<List<SearchWordItem>> = _suggestions.asStateFlow()

	private val searchResults = LinkedHashMap<String, ArrayList<Movie.Video?>>()
	private val allRunCount = AtomicInteger(0)
	private var totalSourceCount = 0
	private var searchExecutor: ExecutorService? = null

	private var hots = ArrayList<String>()

	private val _toast = MutableStateFlow<String?>(null)
	val toast: StateFlow<String?> = _toast.asStateFlow()

	init {
		EventBus.getDefault().register(this)
	}

	override fun onCleared() {
		cancel()
		EventBus.getDefault().unregister(this)
		searchExecutor?.shutdownNow()
	}

	fun search(keyword: String) {
		if (TextUtils.isEmpty(keyword)) {
			_toast.value = "输入内容不能为空"
			return
		}
		_searchTitle.value = keyword
		setSearchHistory(keyword)
		searchResults.clear()
		_tabs.value = emptyList()
		_currentFilter.value = "all"
		_results.value = emptyList()
		showEmptyState()
		startSearch()
	}

	fun selectFilter(key: String) {
		_currentFilter.value = key
		applyFilter()
	}

	private fun applyFilter() {
		val filtered = ArrayList<Movie.Video?>()
		if (_currentFilter.value == "all") {
			searchResults.values.forEach { filtered.addAll(it) }
		} else {
			searchResults[_currentFilter.value]?.let { filtered.addAll(it) }
		}
		_results.value = filtered
	}

	private fun showEmptyState() {
		_tabs.value = emptyList()
		_currentFilter.value = "all"
		_results.value = emptyList()
	}

	private fun startSearch() {
		cancel()
		searchExecutor = Executors.newFixedThreadPool(5)
		val searchRequestList = ApiConfig.instance.getSourceBeanList().toMutableList()
		val home = ApiConfig.instance.homeSourceBean
		searchRequestList.remove(home)
		searchRequestList.add(0, home)

		val mCheckSources = sourcesForSearch
		val siteKey = ArrayList<String?>()
		for (bean in searchRequestList) {
			if (!bean.isSearchable) continue
			if (mCheckSources != null && !mCheckSources.containsKey(bean.key)) continue
			siteKey.add(bean.key)
			allRunCount.incrementAndGet()
		}
		if (siteKey.isEmpty()) {
			_toast.value = "没有指定搜索源"
			return
		}
		totalSourceCount = siteKey.size
		_progress.value = 0 to totalSourceCount
		_isSearching.value = true
		for (key in siteKey) {
			searchExecutor?.execute { sourceVm.getSearch(key, _searchTitle.value) }
		}
	}

	fun cancel() {
		OkGo.getInstance().cancelTag("search")
		searchExecutor?.shutdownNow()
		searchExecutor = null
		try {
			JsLoader.stopAll()
		} catch (t: Throwable) {
			t.printStackTrace()
		}
		allRunCount.set(0)
		_isSearching.value = false
	}

	@Subscribe(threadMode = ThreadMode.MAIN)
	fun onSearchResult(event: RefreshEvent) {
		if (event.type != RefreshEvent.TYPE_SEARCH_RESULT) return
		val absXml = event.obj as? AbsXml
		handleSearchData(absXml)
	}

	@Subscribe(threadMode = ThreadMode.MAIN)
	fun onServerSearch(event: ServerEvent) {
		if (event.type == ServerEvent.SERVER_SEARCH) {
			search(event.obj as? String ?: return)
		}
	}

	private fun handleSearchData(absXml: AbsXml?) {
		var hasNew = false
		if (absXml?.movie?.videoList?.isNotEmpty() == true) {
			val videos = absXml.movie!!.videoList!!
			val sourceKey = videos[0].sourceKey
			val list = searchResults.getOrPut(sourceKey) { ArrayList() }
			val oldSize = list.size
			for (v in videos) {
				if (matchSearchResult(v.name, _searchTitle.value)) list.add(v)
			}
			hasNew = list.size > oldSize
		}

		val remaining = allRunCount.decrementAndGet()
		val searched = if (totalSourceCount > 0) totalSourceCount - remaining else 0
		_progress.value = searched to totalSourceCount

		if (hasNew) rebuildTabs()

		if (remaining <= 0) {
			_isSearching.value = false
			if (_tabs.value.isEmpty()) rebuildTabs()
			cancel()
		}
	}

	private fun rebuildTabs() {
		val newTabs = ArrayList<SourceTab>()
		newTabs.add(SourceTab("all", "全部", searchResults.values.sumOf { it.size }))
		for ((key, videos) in searchResults.toSortedMap()) {
			if (videos.isEmpty()) continue
			val source = ApiConfig.instance.getSource(key) ?: continue
			newTabs.add(SourceTab(key, source.name, videos.size))
		}
		_tabs.value = newTabs
		if (_currentFilter.value !in newTabs.map { it.key }) {
			_currentFilter.value = "all"
		}
		applyFilter()
	}

	private fun matchSearchResult(name: String?, title: String?): Boolean {
		if (TextUtils.isEmpty(name) || TextUtils.isEmpty(title)) return false
		val arr = title!!.trim().split("\\s+".toRegex()).toTypedArray()
		var matchNum = 0
		for (one in arr) if (name?.contains(one) == true) matchNum++
		return matchNum == arr.size
	}

	fun loadHistoryAndHotWords() {
		viewModelScope.launch {
			val historyList = PreferenceStore.getObj(ConfigKey.SEARCH_HISTORY, ArrayList<String?>())
			val combined = ArrayList<SearchWordItem>()
			historyList.forEach { combined.add(SearchWordItem(it.orEmpty(), 0)) }
			if (hots.isNotEmpty()) {
				hots.forEach { combined.add(SearchWordItem(it, 1)) }
				_suggestions.value = combined
				return@launch
			}
			_suggestions.value = combined
			fetchHotWords(historyList)
		}
	}

	private fun fetchHotWords(historyList: ArrayList<String?>) {
		OkGo.get<String>("https://movie.douban.com/j/search_subjects?type=tv&tag=%E7%83%AD%E9%97%A8&sort=recommend&page_limit=20&page_start=0")
			.headers("User-Agent", "Mozilla/5.0")
			.execute(object : AbsCallback<String>() {
				override fun onSuccess(response: Response<String>) {
					try {
						val data = ArrayList<String>()
						val itemList = JsonParser.parseString(response.body()).asJsonObject
							.get("subjects").asJsonArray
						for (ele in itemList) {
							val obj = ele as JsonObject
							if (obj.has("title")) {
								val title = obj.get("title").asString.trim()
									.replace("[<>《》\\-]".toRegex(), "")
									.split(" ".toRegex()).first()
								if (title.isNotEmpty() && !data.contains(title)) {
									data.add(title)
								}
							}
						}
						val updated = ArrayList<SearchWordItem>()
						historyList.forEach { updated.add(SearchWordItem(it.orEmpty(), 0)) }
						data.forEach { updated.add(SearchWordItem(it, 1)) }
						_suggestions.value = updated
						hots = data
					} catch (t: Throwable) {
						t.printStackTrace()
					}
				}

				override fun convertResponse(response: okhttp3.Response): String {
					return response.body.string()
				}
			})
	}

	fun loadSearchSuggestions(key: String) {
		OkGo.get<Any>("https://tv.aiseet.atianqi.com/i-tvbin/qtv_video/search/get_search_smart_box")
			.params("format", "json")
			.params("page_num", 0)
			.params("page_size", 20)
			.params("key", key)
			.execute(object : AbsCallback<Any>() {
				override fun onSuccess(response: Response<Any>) {
					try {
						val result = response.body() as String?
						val json = Gson().fromJson(result, JsonElement::class.java)
						val arr = json.asJsonObject.get("data").asJsonObject
							.get("search_data").asJsonObject
							.get("vecGroupData").asJsonArray
							.get(0).asJsonObject
							.get("group_data").asJsonArray
						val out = ArrayList<SearchWordItem>()
						for (e in arr) {
							val txt = e.asJsonObject.getAsJsonObject("dtReportInfo")
								.getAsJsonObject("reportData")
								.get("keyword_txt").asString.trim()
							out.add(SearchWordItem(txt, 2))
						}
						_suggestions.value = out
					} catch (t: Throwable) {
						t.printStackTrace()
					}
				}

				override fun convertResponse(response: okhttp3.Response): String =
					response.body.string()
			})
	}

	fun clearSearchHistory() {
		PreferenceStore.delete(ConfigKey.SEARCH_HISTORY)
		val list = ArrayList<SearchWordItem>()
		hots.forEach { list.add(SearchWordItem(it, 1)) }
		_suggestions.value = list
	}

	fun deleteSearchWord(word: String) {
		val historyList = PreferenceStore.getObj(ConfigKey.SEARCH_HISTORY, ArrayList<String?>())
		historyList.remove(word)
		PreferenceStore.putObj(ConfigKey.SEARCH_HISTORY, historyList)
		loadHistoryAndHotWords()
	}

	fun consumeToast(): String? {
		val v = _toast.value
		_toast.value = null
		return v
	}
}
