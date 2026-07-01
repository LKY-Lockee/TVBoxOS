package com.github.tvbox.osc.ui.fragment

import com.github.tvbox.osc.bean.Movie
import com.github.tvbox.osc.bean.MovieSort.SortData
import com.github.tvbox.osc.cache.RoomDataManger.getAllVodRecord
import com.github.tvbox.osc.data.ConfigKey
import com.github.tvbox.osc.data.PreferenceStore
import com.github.tvbox.osc.util.UA.randomOne
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.lzy.okgo.OkGo
import com.lzy.okgo.callback.AbsCallback
import com.lzy.okgo.model.Response
import java.util.Calendar
import java.util.Locale
import kotlin.math.min

/**
 * @author pj567
 * @date :2021/3/9
 * @description:
 */
class UserFragment : GridFragment {
	constructor()

	constructor(sortData: SortData?) : super(sortData)

	override fun onFragmentResume() {
		super.onFragmentResume()

		if (PreferenceStore.get(ConfigKey.HOME_REC, 0) == 2) {
			val allVodRecord = getAllVodRecord(20)
			val vodList: MutableList<Movie.Video?> = ArrayList()
			for (vodInfo in allVodRecord) {
				val vod = Movie.Video()
				vod.id = vodInfo.id
				vod.sourceKey = vodInfo.sourceKey
				vod.name = vodInfo.name
				vod.pic = vodInfo.pic
				if (!vodInfo.playNote.isEmpty()) vod.note = "上次看到" + vodInfo.playNote
				vodList.add(vod)
			}
			gridAdapter?.setNewData(vodList)

			if (vodList.isEmpty()) {
				showEmpty()
			} else {
				showSuccess()
			}
		}
	}

	override fun initData() {
		if (PreferenceStore.get(ConfigKey.HOME_REC, 0) == 1) {
			super.initData()
		} else {
			showLoading()
			setDouBanData()
		}
	}

	private fun setDouBanData() {
		try {
			val cal = Calendar.getInstance()
			val year = cal.get(Calendar.YEAR)
			val month = cal.get(Calendar.MONTH) + 1
			val day = cal.get(Calendar.DATE)
			val today = String.format(Locale.getDefault(), "%d%d%d", year, month, day)
			val requestDay = PreferenceStore.get("home_hot_day", "")
			if (requestDay == today) {
				val json = PreferenceStore.get("home_hot", "")
				if (!json.isEmpty()) {
					val hotMovies = loadHots(json)
					if (!hotMovies.isEmpty()) {
						gridAdapter?.setNewData(hotMovies)
						// 缓存数据加载完成，显示成功状态
						showSuccess()
					} else {
						// 缓存数据解析失败或为空，显示空状态
						gridAdapter?.setNewData(ArrayList<Movie.Video?>())
						showEmpty()
					}
					return
				}
			}
			val doubanUrl = "https://movie.douban.com/j/new_search_subjects?sort=U&range=0,10&tags=&playable=1&start=0&year_range=$year,$year"
			OkGo.get<String?>(doubanUrl)
				.headers("User-Agent", randomOne())
				.execute(object : AbsCallback<String?>() {
					override fun onSuccess(response: Response<String?>) {
						val netJson = response.body()
						PreferenceStore.put("home_hot_day", today)
						PreferenceStore.put("home_hot", netJson)
						mActivity.runOnUiThread {
							val hotMovies = loadHots(netJson)
							gridAdapter?.setNewData(hotMovies)
							if (hotMovies.isEmpty()) {
								showEmpty()
							} else {
								showSuccess()
							}
						}
					}

					override fun onError(response: Response<String?>?) {
						super.onError(response)
						// 加载失败显示空状态
						mActivity.runOnUiThread {
							gridAdapter?.setNewData(ArrayList<Movie.Video?>())
							showEmpty()
						}
					}

					override fun convertResponse(response: okhttp3.Response): String {
						return response.body.string()
					}
				})
		} catch (th: Throwable) {
			th.printStackTrace()
			gridAdapter?.setNewData(ArrayList<Movie.Video?>())
			showEmpty()
		}
	}

	private fun loadHots(json: String?): ArrayList<Movie.Video?> {
		val result = ArrayList<Movie.Video?>()
		try {
			val infoJson = Gson().fromJson(json, JsonObject::class.java)
			val array = infoJson.getAsJsonArray("data")
			val limit = min(array.size(), 25)
			for (i in 0..<limit) {
				val ele = array.get(i)
				val obj = ele.getAsJsonObject()
				val vod = Movie.Video()
				vod.name = obj.get("title").asString
				vod.note = obj.get("rate").asString
				if (vod.note?.isEmpty() != true) vod.note += " 分"
				vod.pic = (obj.get("cover").asString
						+ "@User-Agent=" + randomOne()
						+ "@Referer=https://www.douban.com/")
				result.add(vod)
			}
		} catch (ignored: Throwable) {
		}
		return result
	}
}
