package com.github.tvbox.osc.util

import com.github.tvbox.osc.base.App.Companion.instance
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.lang.reflect.Type
import java.nio.charset.StandardCharsets

object EpgUtil {
	private val epgHashMap = HashMap<String, JsonObject>()
	private var epgDoc: JsonObject? = null

	fun init() {
		if (epgDoc != null) return
		try {
			val assetManager = instance.assets // 获得assets资源管理器（assets中的文件无法直接访问，可以使用AssetManager访问）
			val inputStreamReader = InputStreamReader(assetManager.open("epg_data.json"), StandardCharsets.UTF_8) // 使用IO流读取json文件内容
			val br = BufferedReader(inputStreamReader) // 使用字符高效流
			val builder = StringBuilder()
			var line: String?
			while ((br.readLine().also { line = it }) != null) {
				builder.append(line)
			}
			br.close()
			inputStreamReader.close()
			if (builder.isNotEmpty()) {
				epgDoc = Gson().fromJson(builder.toString(), JsonObject::class.java as Type) // 从builder中读取了json中的数据。
				epgDoc?.let { doc ->
					for (opt in doc.get("epgs").getAsJsonArray()) {
						val obj = opt as JsonObject
						val name = obj.get("name").asString.trim()
						val names = name.split(",").toTypedArray()
						for (string in names) {
							epgHashMap[string] = obj
						}
					}
				}
			}
		} catch (e: IOException) {
			e.printStackTrace()
		}
	}

	fun getEpgInfo(channelName: String?): Array<String>? {
		return try {
			if (channelName != null && epgHashMap.containsKey(channelName)) {
				val obj = epgHashMap[channelName] ?: return null
				arrayOf(
					obj.get("logo").asString,
					obj.get("epgid").asString
				)
			} else null
		} catch (ex: Exception) {
			ex.printStackTrace()
			null
		}
	}
}
