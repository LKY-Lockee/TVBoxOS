package com.github.tvbox.osc.util

import androidx.core.net.toUri
import com.github.tvbox.osc.util.DefaultConfig.isVideoFormat
import com.github.tvbox.osc.util.RegexUtils.getPattern
import com.github.tvbox.osc.util.TVBoxRuntimeLog.i

object VideoParseRuler {
	private val HOSTS_RULE = HashMap<String, ArrayList<ArrayList<String>?>>()
	private val HOSTS_FILTER = HashMap<String, ArrayList<ArrayList<String>?>>()
	val hostsRegex: HashMap<String, ArrayList<String>> = HashMap()
	private val HOSTS_SCRIPT = HashMap<String, ArrayList<String>>()

	fun clearRule() {
		HOSTS_RULE.clear()
		HOSTS_FILTER.clear()
		hostsRegex.clear()
		HOSTS_SCRIPT.clear()
	}

	fun addHostRule(host: String, rule: ArrayList<String>?) {
		val rules = HOSTS_RULE.getOrPut(host) { ArrayList() }
		if (rule != null) {
			rules.add(rule)
		}
	}

	fun getHostRules(host: String): ArrayList<ArrayList<String>?>? {
		return HOSTS_RULE[host]
	}

	fun addHostFilter(host: String, rule: ArrayList<String>?) {
		val filters = HOSTS_FILTER.getOrPut(host) { ArrayList() }
		if (rule != null) {
			filters.add(rule)
		}
	}

	fun getHostFilters(host: String): ArrayList<ArrayList<String>?>? {
		return HOSTS_FILTER[host]
	}

	fun addHostRegex(host: String, regex: ArrayList<String>?) {
		if (regex.isNullOrEmpty()) return
		val temp = hostsRegex.getOrPut(host) { ArrayList() }
		temp.addAll(regex)
	}

	fun checkIsVideoForParse(webUrl: String?, url: String): Boolean {
		try {
			var isVideo = isVideoFormat(url)
			if (HOSTS_RULE.isNotEmpty() && !isVideo && webUrl != null) {
				val uri = webUrl.toUri()
				val host = uri.host
				isVideo = if (host != null && getHostRules(host) != null) {
					checkVideoForOneHostRules(host, url)
				} else {
					checkVideoForOneHostRules("*", url)
				}
			}
			return isVideo
		} catch (e: Exception) {
			e.printStackTrace()
		}
		return false
	}

	private fun checkVideoForOneHostRules(host: String, url: String): Boolean {
		val hostRules = getHostRules(host) ?: return false
		for (i in hostRules.indices) {
			val rules = hostRules[i]
			if (!rules.isNullOrEmpty()) {
				var checkIsVideo = true
				for (j in rules.indices) {
					val rule = rules[j]
					val onePattern = getPattern(rule)
					if (!onePattern.matcher(url).find()) {
						checkIsVideo = false
						break
					}
					i("echo-VIDEO RULE:$rule")
				}
				if (checkIsVideo) {
					return true
				}
			}
		}
		return false
	}

	fun isFilter(webUrl: String?, url: String): Boolean {
		try {
			if (HOSTS_FILTER.isNotEmpty() && webUrl != null) {
				val uri = webUrl.toUri()
				val host = uri.host
				if (host != null && getHostFilters(host) != null) {
					return checkIsFilterForOneHostRules(host, url)
				}
			}
		} catch (e: Exception) {
			e.printStackTrace()
		}
		return false
	}

	private fun checkIsFilterForOneHostRules(host: String, url: String): Boolean {
		val hostFilters = getHostFilters(host) ?: return false
		for (i in hostFilters.indices) {
			val filters = hostFilters[i]
			if (!filters.isNullOrEmpty()) {
				var checkIsFilter = true
				for (j in filters.indices) {
					val rule = filters[j]
					val onePattern = getPattern(rule)
					if (!onePattern.matcher(url).find()) {
						checkIsFilter = false
						break
					}
					i("echo-FILTER RULE:$rule")
				}
				if (checkIsFilter) {
					return true
				}
			}
		}
		return false
	}

	fun addHostScript(host: String, script: ArrayList<String>?) {
		if (script.isNullOrEmpty()) return
		val temp = HOSTS_SCRIPT.getOrPut(host) { ArrayList() }
		temp.addAll(script)
	}

	fun getHostScript(url: String): String {
		for (entry in HOSTS_SCRIPT.entries) {
			val host = entry.key
			if (url.contains(host)) {
				val list = entry.value
				if (list.isNotEmpty()) {
					return list[0]
				}
			}
		}
		return ""
	}
}
