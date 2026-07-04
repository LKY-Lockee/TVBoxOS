package com.github.catvod.crawler

import android.content.Context
import android.util.Log
import com.github.tvbox.osc.base.App
import com.github.tvbox.osc.util.FileUtils
import com.github.tvbox.osc.util.MD5
import com.lzy.okgo.OkGo
import dalvik.system.DexClassLoader
import org.json.JSONObject
import java.io.File
import java.lang.reflect.Method
import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap

class JarLoader {
	private val classLoaders = ConcurrentHashMap<String, DexClassLoader>()
	private val proxyMethods = ConcurrentHashMap<String, Method>()
	private val spiders = ConcurrentHashMap<String, Spider>()
	private var recentJarKey: String = ""

	/**
	 * 不要在主线程调用
	 */
	fun load(cache: String): Boolean {
		recentJarKey = "main"
		return loadClassLoader(cache, recentJarKey)
	}

	fun setRecentJarKey(key: String?) {
		if (!key.isNullOrEmpty()) {
			recentJarKey = key
		}
	}

	fun loadLiveJar(jarUrl: String) {
		val urls = jarUrl.split(";md5;").filter { it.isNotEmpty() }
		val url = urls.firstOrNull() ?: return
		val jarKey = MD5.string2MD5(url)
		val jarMd5 = urls.getOrNull(1)?.trim().orEmpty()
		loadJarInternal(url, jarMd5, jarKey)
	}

	fun clear() {
		spiders.clear()
		proxyMethods.clear()
		classLoaders.clear()
	}

	fun getSpider(key: String, cls: String, ext: String?, jar: String): Spider {
		if (spiders.containsKey(key)) {
			Log.i("JarLoader", "echo-getSpider spider缓存: $key")
			return spiders[key] ?: SpiderNull()
		}
		val clsKey = cls.replace("csp_", "")
		val jarUrl: String?
		val jarMd5: String
		val jarKey: String
		if (jar.isEmpty()) {
			jarUrl = null
			jarMd5 = ""
			jarKey = "main"
		} else {
			val urls = jar.split(";md5;").filter { it.isNotEmpty() }
			jarUrl = urls.firstOrNull()
			jarMd5 = urls.getOrNull(1)?.trim().orEmpty()
			jarKey = MD5.string2MD5(jarUrl ?: return SpiderNull())
		}
		recentJarKey = jarKey
		val classLoader = if (jarKey == "main") {
			classLoaders["main"]
		} else {
			loadJarInternal(jarUrl, jarMd5, jarKey)
		} ?: return SpiderNull()

		try {
			Log.i("JarLoader", "echo-getSpider 加载spider: $key")
			val sp = classLoader.loadClass("com.github.catvod.spider.$clsKey").getDeclaredConstructor().newInstance() as Spider
			sp.init(App.instance, ext)
			spiders[key] = sp
			return sp
		} catch (th: Throwable) {
			th.printStackTrace()
		}
		return SpiderNull()
	}

	fun jsonExt(key: String, jxs: LinkedHashMap<String, String>?, url: String?): JSONObject? {
		try {
			val classLoader = classLoaders["main"] ?: return null
			val clsKey = "Json$key"
			val hotClass = "com.github.catvod.parser.$clsKey"
			val jsonParserCls = classLoader.loadClass(hotClass)
			val mth = jsonParserCls.getMethod("parse", LinkedHashMap::class.java, String::class.java)
			return mth.invoke(null, jxs, url) as? JSONObject
		} catch (th: Throwable) {
			th.printStackTrace()
		}
		return null
	}

	fun jsonExtMix(
		flag: String?,
		key: String,
		name: String?,
		jxs: LinkedHashMap<String, HashMap<String, String>>,
		url: String?
	): JSONObject? {
		try {
			val classLoader = classLoaders["main"] ?: return null
			val clsKey = "Mix$key"
			val hotClass = "com.github.catvod.parser.$clsKey"
			val jsonParserCls = classLoader.loadClass(hotClass)
			val mth = jsonParserCls.getMethod(
				"parse",
				LinkedHashMap::class.java,
				String::class.java,
				String::class.java,
				String::class.java
			)
			return mth.invoke(null, jxs, name, flag, url) as? JSONObject
		} catch (th: Throwable) {
			th.printStackTrace()
		}
		return null
	}

	fun proxyInvoke(params: Map<String, List<String>>?): Array<Any?>? {
		try {
			val proxyFun = proxyMethods[recentJarKey] ?: return null
			return (proxyFun.invoke(null, params) as? List<*>)?.toTypedArray()
		} catch (th: Throwable) {
			th.printStackTrace()
		}
		return null
	}

	private fun loadClassLoader(jar: String, key: String): Boolean {
		if (classLoaders.containsKey(key)) {
			Log.i("JarLoader", "echo-loadClassLoader jar缓存: $key")
			return true
		}
		var success = false
		try {
			val cacheDir = File(App.instance.cacheDir.absolutePath + "/catvod_csp")
			if (!cacheDir.exists()) cacheDir.mkdirs()

			val jarFile = File(jar)
			jarFile.setReadable(true, false)
			jarFile.setWritable(false, false)

			val classLoader = DexClassLoader(jar, cacheDir.absolutePath, null, App.instance.classLoader)
			var count = 0
			do {
				try {
					val classInit = classLoader.loadClass("com.github.catvod.spider.Init")
					val initMethod = classInit.getMethod("init", Context::class.java)
					// 在子线程中调用 init 方法，避免网络请求在主线程中执行
					val initThread = Thread {
						try {
							initMethod.invoke(null, App.instance)
						} catch (e: Exception) {
							e.printStackTrace()
						}
					}
					initThread.start()
					initThread.join()
					Log.i("JarLoader", "echo-自定义爬虫代码加载成功!")
					success = true
					try {
						val proxy = classLoader.loadClass("com.github.catvod.spider.Proxy")
						val proxyMethod = proxy.getMethod("proxy", MutableMap::class.java)
						proxyMethods[key] = proxyMethod
					} catch (th: Throwable) {
						th.printStackTrace()
					}
					break
				} catch (th: Throwable) {
					th.printStackTrace()
				}
				count++
			} while (count < 2)

			if (success) {
				classLoaders[key] = classLoader
			}
		} catch (th: Throwable) {
			th.printStackTrace()
		}
		return success
	}

	private fun loadJarInternal(jar: String?, md5: String, key: String): DexClassLoader? {
		if (classLoaders.containsKey(key)) {
			Log.i("JarLoader", "echo-loadJarInternal jar缓存: $key")
			return classLoaders[key]
		}
		val cache = File(App.instance.filesDir.absolutePath + "/csp/" + key + ".jar")
		if (md5.isNotEmpty()) {
			if (cache.exists() && MD5.getFileMd5(cache).equals(md5, ignoreCase = true)) {
				return if (loadClassLoader(cache.absolutePath, key)) {
					classLoaders[key]
				} else {
					null
				}
			}
		} else {
			if (cache.exists() && !FileUtils.isWeekAgo(cache)) {
				if (loadClassLoader(cache.absolutePath, key)) {
					return classLoaders[key]
				}
			}
		}
		try {
			val jarUrl = jar ?: return null
			val response = OkGo.get<File>(jarUrl).execute()
			try {
				response.body.byteStream().use { input ->
					Files.newOutputStream(cache.toPath()).use { output ->
						val buffer = ByteArray(2048)
						var length: Int
						while ((input.read(buffer).also { length = it }) > 0) {
							output.write(buffer, 0, length)
						}
					}
				}
			} catch (e: Exception) {
				e.printStackTrace()
			}
			loadClassLoader(cache.absolutePath, key)
			return classLoaders[key]
		} catch (e: Throwable) {
			e.printStackTrace()
		}
		return null
	}
}
