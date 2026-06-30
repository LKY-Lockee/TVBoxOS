package com.github.tvbox.osc.util

import com.github.tvbox.osc.base.App.Companion.instance
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.lzy.okgo.OkGo
import com.lzy.okgo.callback.AbsCallback
import com.lzy.okgo.model.Response
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.lang.reflect.Type
import java.nio.charset.StandardCharsets
import java.util.Hashtable

object EpgNameFuzzyMatch {
	private val hsEpgName = Hashtable<String, JsonObject>()
	private var epgNameDoc: JsonObject? = null

	fun init() {
		if (epgNameDoc != null) return
		val gson = Gson()
		try {
			val assetManager = instance.assets // 获得assets资源管理器（assets中的文件无法直接访问，可以使用AssetManager访问）
			val inputStreamReader = InputStreamReader(assetManager.open("Roinlong_Epg.json"), StandardCharsets.UTF_8) // 使用IO流读取json文件内容
			val br = BufferedReader(inputStreamReader) // 使用字符高效流
			val builder = StringBuilder()
			var line: String?
			while ((br.readLine().also { line = it }) != null) {
				builder.append(line)
			}
			br.close()
			inputStreamReader.close()
			if (builder.isNotEmpty()) {
				// 从builder中读取了json中的数据。
				epgNameDoc = gson.fromJson(builder.toString(), JsonObject::class.java as Type)
				epgNameDoc?.let { hasAddData(it) }
				return
			}
		} catch (e: IOException) {
			e.printStackTrace()
		}

		// 上述两种途径都失败后,读取网络自定义文件中的内容
		val request = OkGo.get<String>("http://www.baidu.com/maotv/epg.json")
		request.headers("User-Agent", UA.random())
		request.execute(object : AbsCallback<String>() {
			override fun onSuccess(response: Response<String>) {
				try {
					val pageStr = response.body()
					epgNameDoc = gson.fromJson(pageStr, JsonObject::class.java as Type)
					epgNameDoc?.let { hasAddData(it) }
				} catch (ignored: Exception) {
				}
			}

			override fun onError(response: Response<String>?) {
				super.onError(response)
			}

			override fun onFinish() {
				super.onFinish()
			}

			override fun convertResponse(response: okhttp3.Response): String {
				return response.body.string()
			}
		})
	}

	private fun hasAddData(epgNameDoc: JsonObject) {
		for (opt in epgNameDoc.get("epgs").getAsJsonArray()) {
			val obj = opt as JsonObject
			val name = obj.get("name").asString.trim()
			val names = name.split(",").toTypedArray()
			for (string in names) {
				hsEpgName[string] = obj
			}
		}
	}

	fun getEpgNameInfo(channelName: String?): JsonObject? {
		return if (channelName != null && hsEpgName.containsKey(channelName)) {
			hsEpgName[channelName]
		} else {
			null
		}
	}
}
