package com.github.tvbox.osc.util

import com.github.tvbox.osc.api.ApiConfig
import com.orhanobut.hawk.Hawk

object SearchHelper {
	val sourcesForSearch: HashMap<String, String>?
		get() {
			var mCheckSources: HashMap<String, String>?
			try {
				val api = Hawk.get(HawkConfig.API_URL, "")
				if (api.isEmpty()) return null
				val mCheckSourcesForApi = Hawk.get(
					HawkConfig.SOURCES_FOR_SEARCH,
					HashMap<String, HashMap<String, String>>()
				)
				mCheckSources = mCheckSourcesForApi[api]
			} catch (e: Exception) {
				return null
			}
			if (mCheckSources.isNullOrEmpty()) {
				mCheckSources = sources
			}
			return mCheckSources
		}

	val sources: HashMap<String, String>
		get() {
			val mCheckSources = HashMap<String, String>()
			for (bean in ApiConfig.instance.getSourceBeanList()) {
				if (!bean.isSearchable) {
					continue
				}
				bean.key?.let { mCheckSources[it] = "1" }
			}
			return mCheckSources
		}

	fun splitWords(text: String): List<String> {
		val result: MutableList<String> = ArrayList()
		result.add(text)
		val parts = text.split("\\W+".toRegex()).filter { it.isNotEmpty() }
		if (parts.size > 1) {
			result.addAll(parts)
		}
		return result
	}
}
