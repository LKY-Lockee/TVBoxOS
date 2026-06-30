package com.github.tvbox.osc.util

import com.orhanobut.hawk.Hawk

object HistoryHelper {
	private val hisNumArray = arrayOf(30, 50, 100)

	fun getHistoryNumName(index: Int): String {
		return getHisNum(index).toString() + "条"
	}

	fun getHisNum(index: Int): Int {
		return if (index >= 0 && index < hisNumArray.size) {
			hisNumArray[index]
		} else {
			hisNumArray[0]
		}
	}

	fun setSearchHistory(title: String) {
		// 读取历史记录
		val history = Hawk.get(HawkConfig.SEARCH_HISTORY, ArrayList<String>())
		history.remove(title)
		history.add(0, title)
		// 保证最多只保留 15 条，超过的就删除最后一条
		if (history.size > 15) {
			history.removeAt(history.size - 1)
		}
		Hawk.put(HawkConfig.SEARCH_HISTORY, history)
	}

	fun setLiveApiHistory(value: String) {
		val history = Hawk.get(HawkConfig.LIVE_API_HISTORY, ArrayList<String>())
		if (!history.contains(value)) {
			history.add(0, value)
		}
		if (history.size > 30) {
			history.removeAt(30)
		}
		Hawk.put(HawkConfig.LIVE_API_HISTORY, history)
	}

	fun setApiHistory(value: String) {
		val history = Hawk.get(HawkConfig.API_HISTORY, ArrayList<String>())
		if (!history.contains(value)) {
			history.add(0, value)
		}
		if (history.size > 30) {
			history.removeAt(30)
		}
		Hawk.put(HawkConfig.API_HISTORY, history)
	}
}
