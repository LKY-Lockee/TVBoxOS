package com.github.tvbox.osc.util.live

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.io.BufferedReader
import java.io.StringReader
import java.util.regex.Pattern

object TxtSubscribe {
	private val NAME_PATTERN: Pattern = Pattern.compile(".*,(.+?)$")
	private val GROUP_PATTERN: Pattern = Pattern.compile("group-title=\"(.*?)\"")

	fun parse(linkedHashMap: LinkedHashMap<String, LinkedHashMap<String, MutableList<String>>>, str: String) {
		if (str.startsWith("#EXTM3U")) {
			parseM3u(linkedHashMap, str)
		} else {
			parseTxt(linkedHashMap, str)
		}
	}

	//解析m3u后缀
	private fun parseM3u(linkedHashMap: LinkedHashMap<String, LinkedHashMap<String, MutableList<String>>>, str: String) {
		try {
			val bufferedReader = BufferedReader(StringReader(str))
			val linkedHashMap2 = LinkedHashMap<String, MutableList<String>>()
			var line: String?
			while ((bufferedReader.readLine().also { line = it }) != null) {
				val currentLine = line ?: break
				if (currentLine.isEmpty()) continue
				if (currentLine.startsWith("#EXTM3U")) continue
				if (isSetting(currentLine)) continue
				if (currentLine.startsWith("#EXTINF") || currentLine.contains("#EXTINF")) {
					val name = getStrByRegex(NAME_PATTERN, currentLine)
					val group = getStrByRegex(GROUP_PATTERN, currentLine)
					val url = bufferedReader.readLine()?.trim { it <= ' ' } ?: continue
					if (isUrl(url)) {
						val channelTemp = linkedHashMap.getOrPut(group) { LinkedHashMap() }
						val urls = channelTemp.getOrPut(name) { ArrayList() }
						if (!urls.contains(url)) urls.add(url)
					}
				}
			}
			bufferedReader.close()
			if (linkedHashMap2.isEmpty()) return
			linkedHashMap["未分组"] = linkedHashMap2
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}

	private fun getStrByRegex(pattern: Pattern, line: String): String {
		val matcher = pattern.matcher(line)
		if (matcher.find()) return matcher.group(1) ?: "未命名"
		return if (pattern.pattern() == GROUP_PATTERN.pattern()) "未分组" else "未命名"
	}

	private fun isUrl(url: String): Boolean {
		return url.isNotEmpty() && (url.startsWith("http") || url.startsWith("rtp") || url.startsWith("rtsp") || url.startsWith("rtmp"))
	}

	private fun isSetting(line: String): Boolean {
		return line.startsWith("ua") || line.startsWith("parse") || line.startsWith("click") || line.startsWith("player") || line.startsWith("header") || line.startsWith("format") || line.startsWith("origin") || line.startsWith("referer") || line.startsWith("#EXTHTTP:") || line.startsWith("#EXTVLCOPT:") || line.startsWith("#KODIPROP:")
	}

	//解析txt后缀
	fun parseTxt(linkedHashMap: LinkedHashMap<String, LinkedHashMap<String, MutableList<String>>>, str: String) {
		try {
			val bufferedReader = BufferedReader(StringReader(str))
			var readLine = bufferedReader.readLine()
			val linkedHashMap2 = LinkedHashMap<String, MutableList<String>>()
			var linkedHashMap3: LinkedHashMap<String, MutableList<String>> = linkedHashMap2
			while (readLine != null) {
				val currentLine = readLine
				if (currentLine.trim { it <= ' ' }.isEmpty() || currentLine.startsWith("#")) {
					readLine = bufferedReader.readLine()
				} else {
					val split = currentLine.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
					if (split.size >= 2) {
						if (currentLine.contains("#genre#")) {
							val trim = split[0].trim { it <= ' ' }
							linkedHashMap3 = linkedHashMap.getOrPut(trim) { LinkedHashMap() }
						} else {
							val trim2 = split[0].trim { it <= ' ' }
							for (str2 in split[1].trim { it <= ' ' }.split("#".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
								val trim3 = str2.trim { it <= ' ' }
								if (isUrl(trim3)) {
									val urls = linkedHashMap3.getOrPut(trim2) { ArrayList() }
									if (!urls.contains(trim3)) {
										urls.add(trim3)
									}
								}
							}
						}
					}
					readLine = bufferedReader.readLine()
				}
			}
			bufferedReader.close()
			if (linkedHashMap2.isEmpty()) {
				return
			}
			linkedHashMap["未分组"] = linkedHashMap2
		} catch (ignored: Throwable) {
		}
	}

	fun live2JsonArray(linkedHashMap: LinkedHashMap<String, LinkedHashMap<String, MutableList<String>>>): JsonArray {
		val jsonArr = JsonArray()
		for ((group, channels) in linkedHashMap) {
			if (channels.isEmpty()) continue
			val jsonArr2 = JsonArray()
			for ((name, urls) in channels) {
				if (urls.isEmpty()) continue
				val jsonArr3 = JsonArray()
				for (url in urls) {
					jsonArr3.add(url)
				}
				val jsonObj = JsonObject()
				try {
					jsonObj.addProperty("name", name)
					jsonObj.add("urls", jsonArr3)
				} catch (ignored: Throwable) {
				}
				jsonArr2.add(jsonObj)
			}
			val jsonObj2 = JsonObject()
			try {
				jsonObj2.addProperty("group", group)
				jsonObj2.add("channels", jsonArr2)
			} catch (ignored: Throwable) {
			}
			jsonArr.add(jsonObj2)
		}
		return jsonArr
	}
}
