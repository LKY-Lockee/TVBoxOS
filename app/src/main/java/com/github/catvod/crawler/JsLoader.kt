package com.github.catvod.crawler

import android.util.*
import com.github.tvbox.osc.base.*
import com.github.tvbox.osc.util.*
import com.github.tvbox.osc.util.js.*
import com.lzy.okgo.*
import dalvik.system.*
import java.io.*
import java.nio.file.*
import java.util.concurrent.*

class JsLoader {
	/**
	 * 当前的Js爬虫key
	 */
	private var recentKey = ""

	fun clear() {
		spiders.clear()
		classes.clear()
	}

	fun getSpider(key: String, api: String?, ext: String?, jar: String): Spider? {
		if (spiders.containsKey(key)) {
			Log.i("JSLoader", "echo-getSpider cached")
			return spiders[key]
		}
		var classLoader: Class<*>? = null
		if (jar.isNotEmpty()) {
			val urls = jar.split(";md5;").filter { it.isNotEmpty() }
			val jarUrl = urls.firstOrNull() ?: return SpiderNull()
			val jarKey = MD5.string2MD5(jarUrl)
			val jarMd5 = urls.getOrNull(1)?.trim() ?: ""
			classLoader = loadJarInternal(jarUrl, jarMd5, jarKey)
		}
		recentKey = key
		try {
			Log.i("JSLoader", "echo-getSpider load")
			val sp: Spider = JsSpider(key, api, classLoader)
			sp.init(App.instance, ext)
			spiders[key] = sp
			return sp
		} catch (th: Throwable) {
			LOG.i("echo-getSpider-error " + th.message)
		}
		return SpiderNull()
	}

	fun proxyInvoke(params: Map<String, String>?): Array<Any?>? {
		try {
			val proxyFun = spiders[recentKey] ?: return null
			return proxyFun.proxyLocal(params)
		} catch (_: Throwable) {
		}
		return null
	}

	private fun loadClassLoader(jar: String?, key: String): Boolean {
		var success = false
		try {
			val cacheDir = File(App.instance.cacheDir.absolutePath + "/catvod_jsapi")
			if (!cacheDir.exists()) cacheDir.mkdirs()
			val classLoader = DexClassLoader(jar, cacheDir.absolutePath, null, App.instance.classLoader)
			var count = 0
			do {
				try {
					val classInit = classLoader.loadClass("com.github.catvod.js.Method")
					Log.i("JSLoader", "echo-自定义jsapi代码加载成功!")
					classes[key] = classInit
					success = true
					break
				} catch (th: Throwable) {
					th.printStackTrace()
				}
				count++
			} while (count < 5)
		} catch (th: Throwable) {
			th.printStackTrace()
		}
		return success
	}

	private fun loadJarInternal(jar: String?, md5: String, key: String): Class<*>? {
		if (classes.containsKey(key)) {
			Log.i("JSLoader", "echo-loadJarInternal cached")
			return classes[key]
		}
		val cache = File(App.instance.filesDir.absolutePath + "/csp/" + key + ".jar")
		if (md5.isNotEmpty()) {
			if (cache.exists() && MD5.getFileMd5(cache).equals(md5, ignoreCase = true)) {
				loadClassLoader(cache.absolutePath, key)
				return classes[key]
			}
		} else {
			if (cache.exists() && !FileUtils.isWeekAgo(cache)) {
				if (loadClassLoader(cache.absolutePath, key)) {
					return classes[key]
				}
			}
		}
		try {
			val jarUrl = jar ?: return null
			val response = OkGo.get<File>(jarUrl).execute()
			response.body.byteStream().use { input ->
				Files.newOutputStream(cache.toPath()).use { output ->
					val buffer = ByteArray(2048)
					var length: Int
					while ((input.read(buffer).also { length = it }) > 0) {
						output.write(buffer, 0, length)
					}
				}
			}
			loadClassLoader(cache.absolutePath, key)
			return classes[key]
		} catch (e: Throwable) {
			e.printStackTrace()
		}
		return null
	}

	companion object {
		private val spiders = ConcurrentHashMap<String, Spider>()
		private val classes = ConcurrentHashMap<String, Class<*>>()

		fun destroy() {
			for (spider in spiders.values) {
				spider.cancelByTag()
				spider.destroy()
			}
		}

		fun stopAll() {
			for (spider in spiders.values) {
				spider.cancelByTag()
			}
		}
	}
}
