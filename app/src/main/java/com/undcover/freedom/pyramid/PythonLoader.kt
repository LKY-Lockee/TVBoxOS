package com.undcover.freedom.pyramid

import android.app.Application
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.github.catvod.crawler.Spider
import com.github.catvod.crawler.SpiderNull
import com.github.tvbox.osc.util.OkGoHelper
import com.github.tvbox.osc.util.urlhttp.OKCallBack.OKCallBackDefault
import com.github.tvbox.osc.util.urlhttp.OkHttpUtil
import okhttp3.Call
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class PythonLoader {
	var pyInstance: Python? = null
	var androidPlatform: Python.Platform? = null
	var port: Int = -1
	var streamCallback: FileStreamCallback? = null
	var stringCallback: FileStringCallback? = null
	var cache: String = "/storage/emulated/0/plugin/"
	lateinit var pyApp: PyObject
	private val spiders = ConcurrentHashMap<String, Spider>()
	private val siteMap = HashMap<String, JSONObject>()
	private var app: Application? = null

	fun setConfig(config: String) {
		try {
			val configJo = JSONObject(config)
			val siteList = configJo.getJSONArray("sites")
			for (i in 0..<siteList.length()) {
				val jo = siteList.getJSONObject(i)
				val key = jo.optString("api")
				if (key.isNotEmpty()) {
					siteMap[key] = jo
				}
			}
		} catch (e: JSONException) {
			e.printStackTrace()
		}
	}

	fun setApplication(app: Application): PythonLoader {
		this.app = app
		setSdk()
		if (pyInstance == null) {
			if (!Python.isStarted()) {
				val platform = AndroidPlatform(app)
				androidPlatform = platform
				Python.start(platform)
			}
			val instance = Python.getInstance()
			pyInstance = instance
			pyApp = instance.getModule("app")
		}
		return this
	}

	fun setPluginConfig(config: String): PythonLoader {
		this.cache = config
		return this
	}

	fun getUrlByApi(api: String?): String {
		val jo = siteMap[api] ?: return ""
		val key = jo.optString("key")
		val url = jo.optString("ext")
		return if (key.isNotEmpty() && url.isNotEmpty()) {
			if (spiders.containsKey(key)) "" else url
		} else {
			""
		}
	}

	fun getSpider(key: String, url: String?): Spider? {
		val app = this.app ?: throw Exception("set application first")
		if (spiders.containsKey(key)) {
			PyLog.d("$key :缓存加载成功！")
			return spiders[key]
		}

		// 使用ExecutorService来管理线程
		val executor = Executors.newSingleThreadExecutor()
		var future: Future<*>? = null
		try {
			val sp = PythonSpider(key, cache)

			// 提交初始化任务
			future = executor.submit {
				try {
					sp.init(app, url)
				} catch (e: Exception) {
					e.printStackTrace()
				}
			}

			// 等待线程完成，最多10秒
			future.get(10, TimeUnit.SECONDS)

			// 任务成功，缓存并返回
			spiders[key] = sp
			return sp
		} catch (_: TimeoutException) {
			PyLog.e("echo-init方法执行超时")
		} catch (_: ExecutionException) {
			PyLog.e("echo-init:ExecutionException")
		} catch (_: InterruptedException) {
			PyLog.e("echo-init:InterruptedException")
		} finally {
			if (future != null && !future.isDone) {
				future.cancel(true)
			}
			executor.shutdown()
		}
		return SpiderNull()
	}

	fun getPort() {
		if (port <= 0) {
			for (i in 9978..9999) {
				if (OkHttpUtil.string("http://127.0.0.1:$i/proxy?do=ck&api=python", null) == "ok") {
					port = i
					return
				}
			}
		}
	}

	fun localProxyUrl(): String {
		getPort()
		return "http://127.0.0.1:$port/proxy"
	}

	fun str2map(header: String?): Map<String, String> {
		val map = mutableMapOf<String, String>()
		if (header.isNullOrEmpty()) return map
		try {
			val jo = JSONObject(header)
			val it = jo.keys()
			while (it.hasNext()) {
				val key = it.next()
				val value = jo.optString(key)
				map[key] = value
			}
		} catch (e: JSONException) {
			e.printStackTrace()
		}
		return map
	}

	fun getFileStream(url: String?, param: String?, header: String?): InputStream {
		return streamCallback?.get(url, str2map(param), str2map(header))
			?: run {
				val callBack: OKCallBackDefault = object : OKCallBackDefault() {
					override fun onFailure(call: Call?, e: Exception?) {
					}

					override fun onResponse(response: Response?) {
					}
				}
				OkHttpUtil.get(OkGoHelper.getDefaultClient(), url, str2map(param), str2map(header), callBack)
				callBack.result.body.byteStream()
			}
	}

	fun getFileString(url: String?, header: String?): String? {
		return stringCallback?.get(url, str2map(header))
			?: OkHttpUtil.string(url, str2map(header))
	}

	fun setFileStreamCallback(callback: FileStreamCallback?): PythonLoader {
		streamCallback = callback
		return this
	}

	fun setFileStringCallback(callback: FileStringCallback?): PythonLoader {
		stringCallback = callback
		return this
	}

	private fun setSdk() {
		PyLog.instance.setLogLevel(PyLog.LEVEL_V).setFilter(PyLog.FILTER_NW or PyLog.FILTER_LC)
	}

	interface FileStreamCallback {
		fun get(url: String?, paramsMap: Map<String, String>?, headerMap: Map<String, String>?): InputStream?
	}

	interface FileStringCallback {
		fun get(url: String?, headerMap: Map<String, String>?): String?
	}

	companion object {
		val instance: PythonLoader by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
			PythonLoader()
		}
	}
}
