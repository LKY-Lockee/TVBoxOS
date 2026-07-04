package com.github.tvbox.osc.ui.home

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import com.github.tvbox.osc.api.ApiConfig
import com.github.tvbox.osc.api.ApiConfig.LoadConfigCallback
import com.github.tvbox.osc.data.ConfigKey
import com.github.tvbox.osc.data.PreferenceStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

enum class HomeTab { Home, History, Search, Collect }

sealed interface HomeUiState {
	data object Loading : HomeUiState
	data object Ready : HomeUiState
	data class Error(val message: String) : HomeUiState
}

class HomeViewModel(app: Application) : AndroidViewModel(app) {
	private val handler = Handler(Looper.getMainLooper())

	private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
	val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

	/** 外部（onNewIntent / 其他页面）请求切换到的页面。 */
	private val _requestedPage = MutableStateFlow<HomeTab?>(null)
	val requestedPage: StateFlow<HomeTab?> = _requestedPage.asStateFlow()

	/** 待执行的搜索关键词（切换到搜索页时携带）。 */
	private val _pendingSearch = MutableStateFlow<String?>(null)
	val pendingSearch: StateFlow<String?> = _pendingSearch.asStateFlow()

	/** 启动直播页的一次性事件。 */
	private val _launchLive = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
	val launchLive: SharedFlow<Unit> = _launchLive.asSharedFlow()

	/** 一次性 Toast 事件。 */
	private val _toast = MutableSharedFlow<String>(extraBufferCapacity = 4)
	val toast: SharedFlow<String> = _toast.asSharedFlow()

	private var dataInitOk = false
	private var jarInitOk = false

	fun consumeRequestedPage() {
		_requestedPage.value = null
	}

	fun consumePendingSearch(): String? {
		val v = _pendingSearch.value
		_pendingSearch.value = null
		return v
	}

	/** 请求切换到搜索页并搜索。 */
	fun requestSearch(keyword: String?) {
		_requestedPage.value = HomeTab.Search
		_pendingSearch.value = keyword
	}

	fun initData(useCache: Boolean = false) {
		if (dataInitOk && jarInitOk) {
			_uiState.value = HomeUiState.Ready
			if (!useCache && PreferenceStore.get(ConfigKey.DEFAULT_LOAD_LIVE, false)) {
				_launchLive.tryEmit(Unit)
			}
			return
		}
		_uiState.value = HomeUiState.Loading
		if (dataInitOk && !jarInitOk) {
			val spider = ApiConfig.instance.spider.orEmpty()
			if (spider.isNotEmpty()) {
				ApiConfig.instance.loadJar(useCache, spider, object : LoadConfigCallback {
					override fun success() {
						jarInitOk = true
						handler.postDelayed({ initData(useCache) }, 50)
					}

					override fun notice(msg: String?) {
						msg?.let { _toast.tryEmit(it) }
					}

					override fun error(msg: String?) {
						jarInitOk = true
						dataInitOk = true
						handler.postDelayed({
							_toast.tryEmit("$msg; 尝试加载最近一次的jar")
							initData(useCache)
						}, 50)
					}
				})
			}
			return
		}
		ApiConfig.instance.loadConfig(useCache, object : LoadConfigCallback {
			override fun success() {
				dataInitOk = true
				if (ApiConfig.instance.spider.orEmpty().isEmpty()) jarInitOk = true
				handler.postDelayed({ initData(useCache) }, 50)
			}

			override fun notice(msg: String?) {
				msg?.let { _toast.tryEmit(it) }
			}

			override fun error(msg: String?) {
				val m = msg.orEmpty()
				if (m.equals("-1", ignoreCase = true)) {
					handler.post {
						dataInitOk = true
						jarInitOk = true
						initData(useCache)
					}
					return
				}
				handler.post { _uiState.value = HomeUiState.Error(m) }
			}
		})
	}

	/** 错误对话框重试/取消：均继续推进到 Ready。 */
	fun dismissErrorAndContinue() {
		dataInitOk = true
		jarInitOk = true
		handler.post { initData(false) }
	}

	override fun onCleared() {
		handler.removeCallbacksAndMessages(null)
	}
}
