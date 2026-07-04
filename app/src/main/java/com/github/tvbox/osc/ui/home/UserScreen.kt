package com.github.tvbox.osc.ui.home

import android.content.Intent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.github.tvbox.osc.bean.Movie
import com.github.tvbox.osc.bean.MovieSort.SortData
import com.github.tvbox.osc.cache.RoomDataManger
import com.github.tvbox.osc.data.ConfigKey
import com.github.tvbox.osc.data.PreferenceStore
import com.github.tvbox.osc.event.RefreshEvent
import com.github.tvbox.osc.ui.activity.DetailActivity
import com.github.tvbox.osc.ui.compose.component.AdaptiveVodGrid
import com.github.tvbox.osc.ui.compose.component.ContentState
import com.github.tvbox.osc.ui.compose.component.StateBox
import com.github.tvbox.osc.ui.compose.util.rememberEventBusCallback
import com.github.tvbox.osc.util.UA
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.lzy.okgo.OkGo
import com.lzy.okgo.callback.AbsCallback
import com.lzy.okgo.model.Response
import java.util.Calendar
import java.util.Locale
import kotlin.math.min

@Composable
fun UserScreen(
	sortData: SortData,
	onSwitchToSearchAndSearch: (String?) -> Unit,
	modifier: Modifier = Modifier
) {
	val rec = PreferenceStore.get(ConfigKey.HOME_REC, 0)
	if (rec == 1) {
		CategoryGridScreen(sortData, onSwitchToSearchAndSearch, modifier)
		return
	}
	RecommendGrid(sortData, rec, onSwitchToSearchAndSearch, modifier)
}

@Composable
private fun RecommendGrid(
	sortData: SortData,
	rec: Int,
	onSwitchToSearchAndSearch: (String?) -> Unit,
	modifier: Modifier
) {
	val context = LocalContext.current
	var videos by remember { mutableStateOf<List<Movie.Video>>(emptyList()) }
	var state by remember { mutableStateOf<ContentState>(ContentState.Loading) }

	fun openVideo(video: Movie.Video) {
		val id = video.id
		if (id.isEmpty() || id.startsWith("msearch:")) {
			onSwitchToSearchAndSearch(video.name)
		} else {
			context.startActivity(
				Intent(context, DetailActivity::class.java).apply {
					putExtra("id", id)
					putExtra("sourceKey", video.sourceKey)
					putExtra("title", video.name)
					putExtra("picture", video.pic)
				}
			)
		}
	}

	fun loadHistory() {
		val list = RoomDataManger.getAllVodRecord(20).map { info ->
			Movie.Video().apply {
				id = info.id
				sourceKey = info.sourceKey
				name = info.name
				pic = info.pic
				if (info.playNote.isNotEmpty()) note = "上次看到" + info.playNote
			}
		}
		videos = list
		state = if (list.isEmpty()) ContentState.Empty else ContentState.Content("history")
	}

	fun loadDouban() {
		val cal = Calendar.getInstance()
		val year = cal.get(Calendar.YEAR)
		val today = String.format(Locale.getDefault(), "%d%d%d", year, cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DATE))
		val requestDay = PreferenceStore.get("home_hot_day", "")
		if (requestDay == today) {
			val json = PreferenceStore.get("home_hot", "")
			if (json.isNotEmpty()) {
				val hots = loadHots(json)
				videos = hots
				state = if (hots.isEmpty()) ContentState.Empty else ContentState.Content("douban")
				return
			}
		}
		val url = "https://movie.douban.com/j/new_search_subjects?sort=U&range=0,10&tags=&playable=1&start=0&year_range=$year,$year"
		OkGo.get<String?>(url)
			.headers("User-Agent", UA.randomOne())
			.execute(object : AbsCallback<String?>() {
				override fun onSuccess(response: Response<String?>) {
					val netJson = response.body().orEmpty()
					PreferenceStore.put("home_hot_day", today)
					PreferenceStore.put("home_hot", netJson)
					val hots = loadHots(netJson)
					videos = hots
					state = if (hots.isEmpty()) ContentState.Empty else ContentState.Content("douban")
				}

				override fun onError(response: Response<String?>?) {
					super.onError(response)
					videos = emptyList()
					state = ContentState.Empty
				}

				override fun convertResponse(response: okhttp3.Response): String = response.body.string()
			})
	}

	LaunchedEffect(rec) {
		if (rec == 2) loadHistory() else {
			state = ContentState.Loading; loadDouban()
		}
	}
	rememberEventBusCallback<RefreshEvent> { e ->
		if (e.type == RefreshEvent.TYPE_HISTORY_REFRESH && rec == 2) loadHistory()
	}

	StateBox(state = state, modifier = modifier.fillMaxSize()) {
		AdaptiveVodGrid(
			items = videos,
			name = { it.name },
			pic = { it.pic },
			year = { it.year },
			note = { it.note },
			onClick = ::openVideo,
			modifier = Modifier.fillMaxSize()
		)
	}
}

private fun loadHots(json: String): ArrayList<Movie.Video> {
	val result = ArrayList<Movie.Video>()
	try {
		val array = Gson().fromJson(json, JsonObject::class.java).getAsJsonArray("data")
		val limit = min(array.size(), 25)
		for (i in 0 until limit) {
			val obj = array.get(i).asJsonObject
			val vod = Movie.Video()
			vod.name = obj.get("title").asString
			vod.note = obj.get("rate").asString
			if (vod.note!!.isNotEmpty()) vod.note += " 分"
			vod.pic = obj.get("cover").asString + "@User-Agent=" + UA.randomOne() + "@Referer=https://www.douban.com/"
			result.add(vod)
		}
	} catch (ignored: Throwable) {
	}
	return result
}
