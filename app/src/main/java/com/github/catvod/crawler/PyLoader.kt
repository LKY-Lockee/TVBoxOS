package com.github.catvod.crawler

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.github.catvod.crawler.python.IPyLoader
import com.github.tvbox.osc.base.App
import com.github.tvbox.osc.util.LOG
import com.github.tvbox.osc.util.MD5
import com.undcover.freedom.pyramid.PythonLoader
import com.undcover.freedom.pyramid.PythonSpider
import java.util.concurrent.ConcurrentHashMap

class PyLoader : IPyLoader {
	private val pythonLoader: PythonLoader = PythonLoader.instance.setApplication(App.instance)
	private val spiders = ConcurrentHashMap<String, Spider>()

	/**
	 * 记录上次的配置
	 */
	private var lastConfig: String? = null
	private var recentPyApi: String? = null

	override fun clear() {
		spiders.clear()
	}

	override fun setConfig(jsonStr: String?) {
		if (jsonStr != null && jsonStr != lastConfig) {
			Log.i("PyLoader", "echo-setConfig 初始化json ")
			pythonLoader.setConfig(jsonStr)
			lastConfig = jsonStr
		}
	}

	override fun setRecentPyKey(pyApi: String?) {
		recentPyApi = pyApi
	}

	override fun getSpider(key: String, cls: String, ext: String): Spider {
		if (spiders.containsKey(key)) {
			Log.i("PyLoader", "echo-getSpider spider缓存: $key")
			return spiders[key] ?: SpiderNull()
		}
		try {
			if (ContextCompat.checkSelfPermission(App.instance, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
				Log.i("PyLoader", "无存储权限，终止执行")
				return SpiderNull()
			}
			Log.i("PyLoader", "echo-getSpider url: " + getPyUrl(cls, ext))
			val sp: Spider = pythonLoader.getSpider(key, getPyUrl(cls, ext)) ?: return SpiderNull()
			spiders[key] = sp
			Log.i("PyLoader", "echo-getSpider 加载spider: $key")
			return sp
		} catch (th: Throwable) {
			th.printStackTrace()
		}
		return SpiderNull()
	}

	override fun proxyInvoke(params: Map<String, List<String>>): Array<Any?>? {
		val pyApi = recentPyApi ?: return null
		LOG.i("echo-recentPyApi$pyApi")
		try {
			val originalSpider = getSpider(MD5.string2MD5(pyApi), pyApi, "") as PythonSpider
			return originalSpider.proxyLocal(params)
		} catch (th: Throwable) {
			LOG.i("echo-proxyInvoke_Throwable:---" + th.message)
			th.printStackTrace()
		}
		return null
	}

	private fun getPyUrl(api: String, ext: String): String {
		val urlBuilder = StringBuilder(api)
		if (ext.isNotEmpty()) {
			urlBuilder.append(if (api.contains("?")) "&" else "?").append("extend=").append(ext)
		}
		return urlBuilder.toString()
	}
}
