package com.github.tvbox.osc.ui.setting

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.tvbox.osc.api.ApiConfig
import com.github.tvbox.osc.bean.IJKCode
import com.github.tvbox.osc.bean.SourceBean
import com.github.tvbox.osc.data.ConfigKey
import com.github.tvbox.osc.data.PreferenceStore
import com.github.tvbox.osc.util.HistoryHelper
import com.github.tvbox.osc.util.OkGoHelper
import com.github.tvbox.osc.util.PlayerHelper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface SettingsExitAction {
	data object ReloadFull : SettingsExitAction
	data object ReloadCache : SettingsExitAction
	data object ReloadLive : SettingsExitAction
	data object JustFinish : SettingsExitAction
}

data class SettingsUiState(
	val apiUrl: String = "",
	val liveApiUrl: String = "",
	val homeSourceName: String = "",
	val dnsOpt: Int = 0,
	val playType: Int = 0,
	val ijkCodec: String = "硬解码",
	val playRender: Int = 0,
	val playScale: Int = 0,
	val homeRec: Int = 0,
	val defaultLoadLive: Boolean = false,
	val historyNum: Int = 0,
	val ijkCachePlay: Boolean = false,
	val m3u8Purify: Boolean = false,
	val debugOpen: Boolean = false
)

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

	private val _uiState = MutableStateFlow(SettingsUiState())
	val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

	private val _exitAction = MutableSharedFlow<SettingsExitAction>(extraBufferCapacity = 1)
	val exitAction: SharedFlow<SettingsExitAction> = _exitAction.asSharedFlow()

	private val _toast = MutableSharedFlow<String>(extraBufferCapacity = 1)
	val toast: SharedFlow<String> = _toast.asSharedFlow()

	private var entryApiUrl: String = ""
	private var entryLiveApiUrl: String = ""
	private var entryHomeSourceKey: String = ""
	private var entryHomeRec: Int = 0
	private var entryDnsOpt: Int = 0

	init {
		snapshotEntry()
		refreshState()
	}

	private fun snapshotEntry() {
		entryApiUrl = PreferenceStore.get(ConfigKey.API_URL, "")
		entryLiveApiUrl = PreferenceStore.get(ConfigKey.LIVE_API_URL, "")
		entryHomeSourceKey = ApiConfig.instance.homeSourceBean.key
		entryHomeRec = PreferenceStore.get(ConfigKey.HOME_REC, 0)
		entryDnsOpt = PreferenceStore.get(ConfigKey.DOH_URL, 0)
	}

	private fun refreshState() {
		_uiState.update {
			it.copy(
				apiUrl = PreferenceStore.get(ConfigKey.API_URL, ""),
				liveApiUrl = PreferenceStore.get(ConfigKey.LIVE_API_URL, ""),
				homeSourceName = ApiConfig.instance.homeSourceBean.name,
				dnsOpt = PreferenceStore.get(ConfigKey.DOH_URL, 0),
				playType = PreferenceStore.get(ConfigKey.PLAY_TYPE, 0),
				ijkCodec = PreferenceStore.get(ConfigKey.IJK_CODEC, "硬解码"),
				playRender = PreferenceStore.get(ConfigKey.PLAY_RENDER, 0),
				playScale = PreferenceStore.get(ConfigKey.PLAY_SCALE, 0),
				homeRec = PreferenceStore.get(ConfigKey.HOME_REC, 0),
				defaultLoadLive = PreferenceStore.get(ConfigKey.DEFAULT_LOAD_LIVE, false),
				historyNum = PreferenceStore.get(ConfigKey.HISTORY_NUM, 0),
				ijkCachePlay = PreferenceStore.get(ConfigKey.IJK_CACHE_PLAY, false),
				m3u8Purify = PreferenceStore.get(ConfigKey.M3U8_PURIFY, false),
				debugOpen = PreferenceStore.get(ConfigKey.DEBUG_OPEN, false)
			)
		}
	}

	fun getApiHistory(): ArrayList<String> =
		PreferenceStore.getObj(ConfigKey.API_HISTORY, arrayListOf<String>())

	fun getLiveApiHistory(): ArrayList<String> =
		PreferenceStore.getObj(ConfigKey.LIVE_API_HISTORY, arrayListOf<String>())

	fun clearApiHistory() {
		PreferenceStore.putObj(ConfigKey.API_HISTORY, arrayListOf<String>())
	}

	fun clearLiveApiHistory() {
		PreferenceStore.putObj(ConfigKey.LIVE_API_HISTORY, arrayListOf<String>())
	}

	fun setVodApi(url: String) {
		if (url.isEmpty()) return
		HistoryHelper.setApiHistory(url)
		PreferenceStore.put(ConfigKey.API_URL, url)
		refreshState()
	}

	fun setLiveApi(url: String) {
		if (url.isEmpty()) return
		HistoryHelper.setLiveApiHistory(url)
		PreferenceStore.put(ConfigKey.LIVE_API_URL, url)
		refreshState()
	}

	fun setHomeSource(bean: SourceBean) {
		ApiConfig.instance.setSourceBean(bean)
		refreshState()
	}

	fun setDns(index: Int) {
		PreferenceStore.put(ConfigKey.DOH_URL, index)
		refreshState()
	}

	fun setPlayType(type: Int) {
		PreferenceStore.put(ConfigKey.PLAY_TYPE, type)
		PlayerHelper.init()
		refreshState()
	}

	fun setCodec(codec: IJKCode) {
		codec.selected(true)
		refreshState()
	}

	fun setRender(index: Int) {
		PreferenceStore.put(ConfigKey.PLAY_RENDER, index)
		PlayerHelper.init()
		refreshState()
	}

	fun setScale(index: Int) {
		PreferenceStore.put(ConfigKey.PLAY_SCALE, index)
		refreshState()
	}

	fun setHomeRec(index: Int) {
		PreferenceStore.put(ConfigKey.HOME_REC, index)
		refreshState()
	}

	fun setDefaultLoadLive(value: Boolean) {
		PreferenceStore.put(ConfigKey.DEFAULT_LOAD_LIVE, value)
		refreshState()
	}

	fun setHistoryNum(index: Int) {
		PreferenceStore.put(ConfigKey.HISTORY_NUM, index)
		refreshState()
	}

	fun setIjkCachePlay(value: Boolean) {
		PreferenceStore.put(ConfigKey.IJK_CACHE_PLAY, value)
		refreshState()
	}

	fun setM3u8Purify(value: Boolean) {
		PreferenceStore.put(ConfigKey.M3U8_PURIFY, value)
		refreshState()
	}

	fun setDebugOpen(value: Boolean) {
		PreferenceStore.put(ConfigKey.DEBUG_OPEN, value)
		refreshState()
	}

	fun showToast(msg: String) {
		viewModelScope.launch { _toast.emit(msg) }
	}

	fun onBack() {
		viewModelScope.launch {
			val currentApi = PreferenceStore.get(ConfigKey.API_URL, "")
			val currentLiveApi = PreferenceStore.get(ConfigKey.LIVE_API_URL, "")
			val currentHomeKey = ApiConfig.instance.homeSourceBean.key
			val currentHomeRec = PreferenceStore.get(ConfigKey.HOME_REC, 0)
			val currentDns = PreferenceStore.get(ConfigKey.DOH_URL, 0)

			val action = when {
				currentApi != entryApiUrl -> SettingsExitAction.ReloadFull
				currentDns != entryDnsOpt -> SettingsExitAction.ReloadFull
				currentHomeKey != entryHomeSourceKey || currentHomeRec != entryHomeRec -> SettingsExitAction.ReloadCache
				currentLiveApi != entryLiveApiUrl -> SettingsExitAction.ReloadLive
				else -> SettingsExitAction.JustFinish
			}
			_exitAction.emit(action)
		}
	}

	fun createCacheBundle(): Bundle = Bundle().apply { putBoolean("useCache", true) }

	companion object {
		fun getHomeRecName(rec: Int): String = when (rec) {
			1 -> "站点推荐"
			2 -> "观看历史"
			else -> "豆瓣热播"
		}

		val dnsList: ArrayList<String> get() = OkGoHelper.dnsHttpsList
	}
}
