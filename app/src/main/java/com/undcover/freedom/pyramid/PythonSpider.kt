package com.undcover.freedom.pyramid

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.core.net.toUri
import com.chaquo.python.PyObject
import com.github.catvod.crawler.Spider
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.File

class PythonSpider(private val name: String, private val cachePath: String?) : Spider() {
	var loadSuccess: Boolean = false
	lateinit var app: PyObject
	lateinit var pySpider: PyObject

	constructor(cache: String? = "/storage/emulated/0/plugin/") : this("", cache)

	fun getName(): String {
		return name.ifEmpty {
			app.callAttr("getName", pySpider).toString()
		}
	}

	fun map2json(extend: Map<*, *>?): JSONObject {
		val jo = JSONObject()
		try {
			extend?.forEach { (key, value) ->
				jo.put(key.toString(), value)
			}
		} catch (e: JSONException) {
			e.printStackTrace()
		}
		return jo
	}

	fun list2json(array: List<String>?): JSONArray {
		val ja = JSONArray()
		array?.forEach { str -> ja.put(str) }
		return ja
	}

	fun paramLog(vararg obj: Any?): String {
		val sb = StringBuilder()
		sb.append("request params:[")
		for (o in obj) {
			sb.append(o).append("-")
		}
		sb.append("]")
		return sb.toString()
	}

	fun replaceLocalUrl(content: String): String {
		return content.replace("http://127.0.0.1:UndCover/proxy", PythonLoader.instance.localProxyUrl())
	}

	override fun init(context: Context) {
		app.callAttr("init", pySpider)
	}

	override fun init(context: Context, extend: String?) {
		app = PythonLoader.instance.pyApp
		val retValue = app.callAttr("downloadPlugin", cachePath, extend)
		val uri = extend?.toUri()
		var extInfo = uri?.getQueryParameter("extend")
		if (extInfo == null) extInfo = ""
		val path = retValue.toString()
		Log.i("PyLoader", "echo-init path: $path")
		val file = File(path)
		if (file.exists()) {
			pySpider = app.callAttr("loadFromDisk", path)

			val poList = app.callAttr("getDependence", pySpider).asList()
			for (po in poList) {
				val api = po.toString()
				Log.i("PyLoader", "echo-init api: $api")
				val depUrl = PythonLoader.instance.getUrlByApi(api)
				if (depUrl.isNotEmpty()) {
					Log.i("PyLoader", "echo-init depUrl: $depUrl")
					val tmpPath = app.callAttr("downloadPlugin", cachePath, depUrl).toString()
					if (!File(tmpPath).exists()) {
						PyToast.showCancelableToast(context, api + "加载失败!")
						return
					} else {
						PyLog.d("$api: 加载插件依赖成功！")
					}
				}
			}
			app.callAttr("init", pySpider, extInfo)
			loadSuccess = true
			Log.i("PyLoader", "echo-init extInfo: $extend$extInfo")
			PyLog.d("$name: 下載插件成功！")
		} else {
			PyToast.showCancelableToast(context, name + "下载插件失败")
		}
	}

	/**
	 * 首页数据内容
	 * 
	 * @param filter 是否开启筛选
	 */
	override fun homeContent(filter: Boolean): String {
		PyLog.nw("homeContent-$name", paramLog(filter))
		val po = app.callAttr("homeContent", pySpider, filter)
		val rsp = po.toString()
		PyLog.nw("homeContent-$name", rsp)
		return rsp
	}

	/**
	 * 首页最近更新数据
	 */
	override fun homeVideoContent(): String {
		PyLog.nw("homeVideoContent-$name", "")
		val po = app.callAttr("homeVideoContent", pySpider)
		val rsp = po.toString()
		PyLog.nw("homeVideoContent-$name", rsp)
		return rsp
	}

	/**
	 * 分类数据
	 */
	override fun categoryContent(tid: String?, pg: String?, filter: Boolean, extend: Map<String, String>?): String {
		PyLog.nw("categoryContent-$name", paramLog(tid, pg, filter, map2json(extend).toString()))
		val po = app.callAttr("categoryContent", pySpider, tid, pg, filter, map2json(extend).toString())
		val rsp = po.toString()
		PyLog.nw("categoryContent-$name", rsp)
		return rsp
	}

	/**
	 * 详情数据
	 */
	override fun detailContent(ids: List<String>?): String {
		PyLog.nw("detailContent-$name", paramLog(list2json(ids).toString()))
		val po = app.callAttr("detailContent", pySpider, list2json(ids).toString())
		val rsp = po.toString()
		PyLog.nw("detailContent-$name", rsp)
		return rsp
	}

	/**
	 * 搜索数据内容
	 */
	override fun searchContent(key: String?, quick: Boolean): String {
		PyLog.nw("searchContent-$name", paramLog(key, quick))
		val po = app.callAttr("searchContent", pySpider, key, quick)
		val rsp = po.toString()
		PyLog.nw("searchContent-$name", rsp)
		return rsp
	}

	/**
	 * 播放信息
	 */
	override fun playerContent(flag: String?, id: String?, vipFlags: List<String>?): String {
		PyLog.nw("playerContent-$name", paramLog(flag, id, list2json(vipFlags).toString()))
		val po = app.callAttr("playerContent", pySpider, flag, id, list2json(vipFlags).toString())
		val rsp = replaceLocalUrl(po.toString())
		PyLog.nw("playerContent-$name", rsp)
		return rsp
	}

	/**
	 * 直播列表数据
	 */
	override fun liveContent(url: String?): String {
		PyLog.nw("liveContent-$name", "")
		val po = app.callAttr("liveContent", pySpider, url)
		val rsp = po.toString()
		PyLog.nw("liveContent-$name", rsp)
		return rsp
	}

	/**
	 * webview解析时使用 可自定义判断当前加载的 url 是否是视频
	 */
	override fun isVideoFormat(url: String?): Boolean {
		return false
	}

	/**
	 * 是否手动检测webview中加载的url
	 */
	override fun manualVideoCheck(): Boolean {
		return false
	}

	override fun proxyLocal(params: Map<String, String>?): Array<Any?> {
		Log.i("PyLoader", "echo-proxyLocal:param$params")
		val list = app.callAttr("localProxy", pySpider, map2json(params).toString()).asList()
		val base64 = list.size > 4 && list[4].toInt() == 1
		val headerAvailable = list.size > 3 && list[3] != null
		val result = arrayOfNulls<Any>(4)
		result[0] = list[0].toInt()
		result[1] = list[1].toString()
		result[2] = getStream(list[2], base64)
		result[3] = if (headerAvailable) getHeader(list[3]) else null
		return result
	}

	private fun getHeader(headerObj: PyObject?): Map<String, String>? {
		if (headerObj == null) {
			return null
		}
		val headerMap = mutableMapOf<String, String>()
		for (key in headerObj.asMap().keys) {
			val value = headerObj.asMap()[key]?.toString() ?: continue
			headerMap[key.toString()] = value
		}
		return headerMap
	}

	private fun getStream(o: PyObject?, base64: Boolean): ByteArrayInputStream {
		if (o == null) return ByteArrayInputStream(ByteArray(0))
		val typeStr = o.type().toString()
		if (typeStr.contains("bytes")) return ByteArrayInputStream(o.toJava(ByteArray::class.java))
		var content: String? = o.toString()
		if (base64 && content?.contains("base64,") == true) {
			content = content.split("base64,").lastOrNull()
		}
		return ByteArrayInputStream(
			if (base64) decode(content) else content?.toByteArray() ?: ByteArray(0)
		)
	}

	companion object {
		fun decode(s: String?, flags: Int = Base64.DEFAULT or Base64.NO_WRAP): ByteArray? {
			return Base64.decode(s, flags)
		}
	}
}
