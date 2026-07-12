package com.github.tvbox.osc.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.github.tvbox.osc.base.App
import com.github.tvbox.osc.bean.AbsXml
import com.github.tvbox.osc.bean.Movie
import com.github.tvbox.osc.bean.VodInfo
import com.github.tvbox.osc.cache.RoomDataManger
import com.github.tvbox.osc.event.RefreshEvent
import com.github.tvbox.osc.util.SubtitleHelper
import com.github.tvbox.osc.viewmodel.SourceViewModel
import com.lzy.okgo.OkGo
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlin.math.max

sealed class DetailUiState {
	data object Loading : DetailUiState()
	data class Success(val video: Movie.Video, val vodInfo: VodInfo) : DetailUiState()
	data object Empty : DetailUiState()
}

class DetailViewModel(app: Application) : AndroidViewModel(app) {

	private val sourceVm = SourceViewModel()

	private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
	val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

	private val _playUrl = MutableStateFlow("")
	val playUrl: StateFlow<String> = _playUrl.asStateFlow()

	private val _isFullscreen = MutableStateFlow(false)
	val isFullscreen: StateFlow<Boolean> = _isFullscreen.asStateFlow()

	private val _isCollected = MutableStateFlow(false)
	val isCollected: StateFlow<Boolean> = _isCollected.asStateFlow()

	private val _toast = MutableSharedFlow<String>()
	val toast: SharedFlow<String> = _toast.asSharedFlow()

	private val _playSignal = MutableStateFlow(0)
	val playSignal: StateFlow<Int> = _playSignal.asStateFlow()

	private val _refreshTick = MutableStateFlow(0)
	val refreshTick: StateFlow<Int> = _refreshTick.asStateFlow()

	var vodInfo: VodInfo? = null
		private set
	var video: Movie.Video? = null
		private set
	var sourceKey = ""
		private set
	var firstSourceKey = ""
		private set
	var vodId = ""
		private set

	private var vodPicture = ""
	private var previewVodInfo: VodInfo? = null

	init {
		EventBus.getDefault().register(this)
		sourceVm.detailResult.observeForever(::handleDetail)
	}

	fun loadDetail(key: String, vid: String, picture: String) {
		sourceKey = key
		firstSourceKey = key
		vodId = vid
		vodPicture = picture
		_uiState.value = DetailUiState.Loading
		updateCollectState()
		sourceVm.getDetail(key, vid)
	}

	private fun handleDetail(absXml: AbsXml?) {
		if (absXml == null || absXml.movie == null || absXml.movie!!.videoList.isNullOrEmpty()) {
			_uiState.value = DetailUiState.Empty
			return
		}
		val msg = absXml.msg
		if (!msg.isNullOrEmpty() && msg != "数据列表") {
			_toast.tryEmit(msg)
			_uiState.value = DetailUiState.Empty
			return
		}

		val v = absXml.movie!!.videoList!![0]
		v.id = vodId
		if (v.name.isNullOrEmpty()) v.name = "TVBox"
		if (v.pic.isNullOrEmpty() && vodPicture.isNotEmpty()) v.pic = vodPicture

		val info = VodInfo()
		info.setVideo(v)
		info.sourceKey = v.sourceKey
		sourceKey = v.sourceKey

		val record = RoomDataManger.getVodInfo(sourceKey, vodId)
		if (record != null) {
			info.playIndex = max(record.playIndex, 0)
			info.playFlag = record.playFlag
			info.playerCfg = record.playerCfg
			info.reverseSort = record.reverseSort
		}
		if (info.reverseSort) info.reverse()

		val map = info.seriesMap
		if (!map.isNullOrEmpty()) {
			if (!map.containsKey(info.playFlag)) {
				info.playFlag = map.keys.first()
			}
			_playUrl.value = map[info.playFlag]!![0].url
		}

		vodInfo = info
		video = v
		_uiState.value = DetailUiState.Success(v, info)
		updateCollectState()

		if (!map.isNullOrEmpty()) {
			jumpToPlay()
		}
	}

	fun jumpToPlay() {
		val info = vodInfo ?: return
		val map = info.seriesMap ?: return
		val list = map[info.playFlag] ?: return
		if (list.isEmpty() || info.playIndex >= list.size) return

		_playUrl.value = list[info.playIndex].url
		insertVod(firstSourceKey, info)

		if (previewVodInfo == null) {
			previewVodInfo = deepCopyVodInfo(info)
		}
		previewVodInfo?.let { pv ->
			pv.playerCfg = info.playerCfg
			pv.playFlag = info.playFlag
			pv.playIndex = info.playIndex
			pv.seriesMap = info.seriesMap
			App.instance.vodInfo = pv
		}

		_playSignal.value++
	}

	private fun deepCopyVodInfo(info: VodInfo): VodInfo? {
		return try {
			val bos = ByteArrayOutputStream()
			val oos = ObjectOutputStream(bos)
			oos.writeObject(info)
			oos.flush()
			oos.close()
			val ois = ObjectInputStream(ByteArrayInputStream(bos.toByteArray()))
			ois.readObject() as VodInfo
		} catch (e: Exception) {
			e.printStackTrace()
			null
		}
	}

	fun selectFlag(position: Int) {
		val info = vodInfo ?: return
		val flags = info.seriesFlags ?: return
		if (position >= flags.size) return

		val newFlag = flags[position].name
		if (info.playFlag != newFlag) {
			for (i in flags.indices) {
				flags[i].selected = (i == position)
			}
			val map = info.seriesMap ?: return
			val list = map[info.playFlag]
			if (list != null && list.size > info.playIndex) {
				list[info.playIndex].selected = false
			}
			info.playFlag = newFlag
			_refreshTick.value++
			jumpToPlay()
		}
	}

	fun selectSeries(position: Int) {
		val info = vodInfo ?: return
		val map = info.seriesMap ?: return
		val list = map[info.playFlag] ?: return
		if (list.isEmpty() || position >= list.size) return

		if (info.playIndex != position) {
			info.playIndex = position
			_refreshTick.value++
			jumpToPlay()
		}
	}

	fun toggleReverse() {
		val info = vodInfo ?: return
		info.reverseSort = !info.reverseSort
		info.reverse()
		_refreshTick.value++
	}

	fun setFullscreen(fullscreen: Boolean, activity: android.app.Activity? = null) {
		_isFullscreen.value = fullscreen
		toggleSubtitleTextSize(activity)
	}

	private fun toggleSubtitleTextSize(activity: android.app.Activity?) {
		if (activity == null) return
		var size = SubtitleHelper.getTextSize(activity)
		if (!_isFullscreen.value) {
			size = (size * 0.6).toInt()
		}
		EventBus.getDefault().post(RefreshEvent(RefreshEvent.TYPE_SUBTITLE_SIZE_CHANGE, size))
	}

	fun toggleCollect() {
		val info = vodInfo ?: return
		if (sourceKey.isEmpty() || vodId.isEmpty()) return

		if (RoomDataManger.isVodCollect(sourceKey, vodId)) {
			RoomDataManger.deleteVodCollect(sourceKey, null)
			updateCollectState()
		} else {
			RoomDataManger.insertVodCollect(sourceKey, info)
			updateCollectState()
		}
	}

	private fun updateCollectState() {
		if (sourceKey.isEmpty() || vodId.isEmpty()) {
			_isCollected.value = false
		} else {
			_isCollected.value = RoomDataManger.isVodCollect(sourceKey, vodId)
		}
	}

	private fun insertVod(key: String, info: VodInfo) {
		try {
			val map = info.seriesMap ?: return
			val list = map[info.playFlag] ?: return
			info.playNote = list[info.playIndex].name
		} catch (_: Throwable) {
			info.playNote = ""
		}
		RoomDataManger.insertVodRecord(key, info)
		EventBus.getDefault().post(RefreshEvent(RefreshEvent.TYPE_HISTORY_REFRESH))
	}

	@Subscribe(threadMode = ThreadMode.MAIN)
	fun refresh(event: RefreshEvent) {
		if (event.type == RefreshEvent.TYPE_REFRESH) {
			val info = vodInfo ?: return
			when (event.obj) {
				is Int -> {
					info.playIndex = event.obj as Int
					insertVod(firstSourceKey, info)
					_refreshTick.value++
				}

				is JSONObject -> {
					info.playerCfg = event.obj.toString()
					insertVod(firstSourceKey, info)
				}

				is String -> {
					_playUrl.value = event.obj as String
				}
			}
		} else if (event.type == RefreshEvent.TYPE_QUICK_SEARCH_SELECT) {
			val v = event.obj as? Movie.Video ?: return
			loadDetail(v.sourceKey, v.id, v.pic ?: "")
		}
	}

	override fun onCleared() {
		OkGo.getInstance().cancelTag("fenci")
		OkGo.getInstance().cancelTag("detail")
		EventBus.getDefault().unregister(this)
	}
}
