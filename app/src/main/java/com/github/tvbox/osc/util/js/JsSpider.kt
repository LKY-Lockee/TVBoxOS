package com.github.tvbox.osc.util.js

import android.content.Context
import android.text.TextUtils
import android.util.Base64
import com.github.catvod.crawler.Spider
import com.github.tvbox.osc.util.FileUtils
import com.github.tvbox.osc.util.MD5
import com.github.tvbox.osc.util.TVBoxRuntimeLog
import com.github.tvbox.osc.util.js.Connect.cancelByTag
import com.github.tvbox.osc.util.js.Json.invalid
import com.github.tvbox.osc.util.js.Json.valid
import com.whl.quickjs.wrapper.JSArray
import com.whl.quickjs.wrapper.JSObject
import com.whl.quickjs.wrapper.QuickJSContext
import com.whl.quickjs.wrapper.QuickJSContext.BytecodeModuleLoader
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.lang.reflect.Method
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

class JsSpider(key: String, private val api: String, private val dex: Class<*>?) : Spider() {
	private val executor: ExecutorService = Executors.newSingleThreadExecutor()
	private val key: String = "J${MD5.encode(key)}"
	private var ctx: QuickJSContext = QuickJSContext.create()
	private lateinit var jsObject: JSObject
	private var cat = false

	init {
		initializeJS()
	}

	override fun cancelByTag() {
		cancelByTag("js_okhttp_tag")
	}

	private fun submit(runnable: Runnable?) {
		executor.submit(runnable)
	}

	private fun <T> submit(callable: Callable<T?>?): Future<T?>? {
		return executor.submit<T?>(callable)
	}

	private fun call(func: String?, vararg args: Any?): Any? {
		try {
			val callArgs = arrayOf(*args)
			return submit<Any?> { Async.run(jsObject, func, callArgs).get() }?.get() // 等待 executor 线程完成 JS 调用
		} catch (e: InterruptedException) {
			TVBoxRuntimeLog.i("Executor 提交或等待失败$e")
			return null
		} catch (e: ExecutionException) {
			TVBoxRuntimeLog.i("Executor 提交或等待失败$e")
			return null
		}
	}

	private fun cfg(ext: String): JSObject {
		val cfg = ctx.createNewJSObject()
		cfg.setProperty("stype", 3)
		cfg.setProperty("skey", key)
		if (invalid(ext)) cfg.setProperty("ext", ext)
		else ctx.setProperty(cfg, "ext", ctx.parse(ext))
		return cfg
	}

	override fun init(context: Context, extend: String?) {
		try {
			if (cat) call("init", (submit<JSObject?> { cfg(extend ?: return@submit null) } ?: return).get())
			else call("init", if (extend != null && valid(extend)) ctx.parse(extend) else extend)
		} catch (ignored: Exception) {
		}
	}

	override fun homeContent(filter: Boolean): String {
		return try {
			call("home", filter) as String
		} catch (e: Exception) {
			""
		}
	}

	override fun homeVideoContent(): String {
		return try {
			call("homeVod") as String
		} catch (e: Exception) {
			""
		}
	}

	override fun categoryContent(tid: String, pg: String, filter: Boolean, extend: HashMap<String, String>?): String {
		try {
			val obj = (submit<JSObject> { JSUtils<String>().toObj(ctx, extend) } ?: return "").get()
			return call("category", tid, pg, filter, obj) as String
		} catch (e: Exception) {
			return ""
		}
	}

	override fun detailContent(ids: List<String>?): String {
		return try {
			call("detail", ids?.get(0)) as String
		} catch (e: Exception) {
			""
		}
	}

	override fun searchContent(key: String?, quick: Boolean): String {
		return try {
			call("search", key, quick) as String
		} catch (e: Exception) {
			""
		}
	}

	override fun playerContent(flag: String?, id: String, vipFlags: List<String>): String {
		try {
			val c = ctx
			val future = submit<JSArray?> { JSUtils<String>().toArray(c, vipFlags) } ?: return ""
			val array = future.get()
			return call("play", flag, id, array) as String
		} catch (e: Exception) {
			return ""
		}
	}

	override fun manualVideoCheck(): Boolean {
		return try {
			(call("sniffer") as? Boolean) ?: false
		} catch (e: Exception) {
			false
		}
	}

	override fun isVideoFormat(url: String): Boolean {
		return try {
			(call("isVideo", url) as? Boolean) ?: false
		} catch (e: Exception) {
			false
		}
	}

	override fun proxyLocal(params: Map<String, List<String>>): Array<Any?> {
		return try {
			(submit<Array<Any?>?> { proxy1(params) } ?: return arrayOfNulls(0)).get() ?: arrayOfNulls(0)
		} catch (e: Exception) {
			arrayOfNulls(0)
		}
	}

	override fun destroy() {
		submit {
			executor.shutdownNow()
			ctx.destroy()
		}
	}

	private fun initializeJS() {
		(submit<Any?> {
			createCtx()
			val c = ctx
			if (dex != null) createDex()

			var content: String? = FileUtils.loadModule(api)
			if (TextUtils.isEmpty(content)) {
				return@submit null
			}
			val code = content ?: return@submit null

			if (code.startsWith("//bb")) {
				cat = true
				val b: ByteArray = Base64.decode(code.replace("//bb", ""), 0)
				c.execute(byteFF(b))
				c.evaluateModule("${String.format(SPIDER_STRING_CODE, "$key.js")}globalThis.$key = __JS_SPIDER__;", "tv_box_root.js")
			} else {
				if (code.contains("__JS_SPIDER__")) {
					content = code.replace("__JS_SPIDER__\\s*=".toRegex(), "export default ")
				}
				if (code.contains("__jsEvalReturn") && !code.contains("export default")) {
					cat = true
				}
				c.evaluateModule(content, api)
				c.evaluateModule("${String.format(SPIDER_STRING_CODE, api)}globalThis.$key = __JS_SPIDER__;", "tv_box_root.js")
			}
			jsObject = c.getGlobalObject().getProperty(key) as JSObject
			null
		} ?: return).get()
	}

	private fun createCtx() {
		ctx.setModuleLoader(object : BytecodeModuleLoader() {
			override fun getModuleBytecode(moduleName: String): ByteArray? {
				val ss = FileUtils.loadModule(moduleName)
				if (TextUtils.isEmpty(ss)) {
					TVBoxRuntimeLog.i("echo-getModuleBytecode empty :$moduleName")
					return ctx.compileModule("", moduleName)
				}
				if ((ss ?: return null).startsWith("//DRPY")) {
					return Base64.decode(ss.replace("//DRPY", ""), Base64.URL_SAFE)
				} else if (ss.startsWith("//bb")) {
					val b = Base64.decode(ss.replace("//bb", ""), 0)
					return byteFF(b)
				} else {
					if (moduleName.contains("cheerio.min.js")) {
						FileUtils.setCacheByte("cheerio.min", ctx.compileModule(ss, "cheerio.min.js"))
					} else if (moduleName.contains("crypto-js.js")) {
						FileUtils.setCacheByte("crypto-js", ctx.compileModule(ss, "crypto-js.js"))
					}
					return ctx.compileModule(ss, moduleName)
				}
			}

			override fun moduleNormalizeName(baseModuleName: String?, moduleName: String?): String {
				return UriUtil.resolve(baseModuleName, moduleName)
			}
		})
		ctx.setConsole(object : QuickJSContext.Console {
			override fun log(info: String?) {
				TVBoxRuntimeLog.i("QuJs$info")
			}

			override fun info(info: String?) {
				TVBoxRuntimeLog.i("QuJs$info")
			}

			override fun warn(info: String?) {
				TVBoxRuntimeLog.i("QuJs$info")
			}

			override fun error(info: String?) {
				TVBoxRuntimeLog.e("QuJs$info")
			}
		})

		QuickJSBinder.bind(ctx.getGlobalObject(), Global(executor))

		val local = ctx.createNewJSObject()
		ctx.getGlobalObject().setProperty("local", local)
		QuickJSBinder.bind(local, Local())

		ctx.getGlobalObject().context.evaluate(FileUtils.loadModule("net.js"))
	}

	private fun createDex() {
		try {
			val obj = ctx.createNewJSObject()
			val clz = dex
			val classes = (clz ?: return).declaredClasses
			ctx.getGlobalObject().setProperty("jsapi", obj)
			if (classes.size == 0) invokeSingle(clz, obj)
			if (classes.size >= 1) invokeMultiple(clz, obj)
		} catch (e: Throwable) {
			e.printStackTrace()
		}
	}

	private fun invokeSingle(clz: Class<*>, jsObj: JSObject) {
		invoke(clz, jsObj, clz.getDeclaredConstructor(QuickJSContext::class.java).newInstance(ctx))
	}

	private fun invokeMultiple(clz: Class<*>, jsObj: JSObject) {
		for (subClz in clz.declaredClasses) {
			val javaObj: Any = subClz.getDeclaredConstructor(clz).newInstance(clz.getDeclaredConstructor(QuickJSContext::class.java).newInstance(ctx))
			val subObj: JSObject = ctx.createNewJSArray()
			invoke(subClz, subObj, javaObj)
			jsObj.setProperty(subClz.simpleName, subObj)
		}
	}

	private fun invoke(clz: Class<*>, jsObj: JSObject, javaObj: Any?) {
		for (method in clz.methods) {
			if (!method.isAnnotationPresent(Function::class.java)) continue
			invoke(jsObj, method, javaObj)
		}
	}

	private fun invoke(jsObj: JSObject, method: Method, javaObj: Any?) {
		jsObj.setProperty(method.name) { objects: Array<Any?> ->
			try {
				return@setProperty method.invoke(javaObj, *objects)
			} catch (e: Throwable) {
				return@setProperty null
			}
		}
	}

	private val content: String?
		get() {
			val global = "globalThis.$key"
			val code = FileUtils.loadModule(api) ?: return null
			if (code.isEmpty()) return null
			val c = ctx
			return if (code.contains("__jsEvalReturn")) {
				c.evaluate("req = http")
				"$code$global = __jsEvalReturn()"
			} else if (code.contains("__JS_SPIDER__")) {
				code.replace("__JS_SPIDER__", global)
			} else {
				code.replace("export default.*?[{]".toRegex(), "$global = {")
			}
		}

	private fun proxy1(params: Map<String, List<String>>?): Array<Any?> {
		val flatParams = params?.mapValues { it.value.firstOrNull() ?: "" }
		val c = ctx
		val `object` = JSUtils<String>().toObj(c, flatParams)
		val jsObj = jsObject
		val rawResult = jsObj.getJSFunction("proxy").call(`object`)
		val array = JSUtils.toJsonArray(rawResult as? JSArray ?: return arrayOfNulls(0))
		val headerAvailable = array.length() > 3 && array.opt(3) != null
		val result = arrayOfNulls<Any>(4)
		result[0] = array.opt(0)
		result[1] = array.opt(1)
		result[2] = getStream(array.opt(2))
		result[3] = if (headerAvailable) getHeader(array.opt(3)) else null
		if (array.length() > 4) {
			try {
				if (array.optInt(4) == 1) {
					var content = array.optString(2)
					if (content.contains("base64,")) content = content.substring(content.indexOf("base64,") + 7)
					result[2] = ByteArrayInputStream(Base64.decode(content, Base64.DEFAULT))
				}
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}
		return result
	}

	private fun getHeader(headerRaw: Any?): MutableMap<String, String> {
		val headers: MutableMap<String, String> = HashMap()
		when (headerRaw) {
			is JSONObject -> {
				val keys = headerRaw.keys()
				while (keys.hasNext()) {
					val key = keys.next()
					headers[key] = headerRaw.optString(key)
				}
			}

			is String -> {
				try {
					val json = JSONObject(headerRaw)
					val keys = json.keys()
					while (keys.hasNext()) {
						val key = keys.next()
						headers[key] = json.optString(key)
					}
				} catch (e: JSONException) {
					TVBoxRuntimeLog.i("getHeader: 无法解析 String 为 JSON$e")
				}
			}

			is MutableMap<*, *> -> {
				for (entry in headerRaw.entries) {
					headers[entry.key.toString()] = entry.value.toString()
				}
			}
		}
		return headers
	}

	private fun proxy2(params: Map<String, String>): Array<Any?> {
		val url = params["url"] ?: return arrayOfNulls(0)
		val header = params["header"] ?: return arrayOfNulls(0)
		val c = ctx
		val array = submit<JSArray?> { JSUtils<String>().toArray(c, listOf(*url.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())) }?.get() ?: return arrayOfNulls(0)
		val `object` = submit<Any?> { c.parse(header) }?.get() ?: return arrayOfNulls(0)
		val json = call("proxy", array, `object`) as String?
		val res = Res.objectFrom(json) ?: return arrayOfNulls(0)
		var contentType = res.contentType
		if (TextUtils.isEmpty(contentType)) contentType = "application/octet-stream"
		val result = arrayOfNulls<Any>(3)
		result[0] = 200
		result[1] = contentType
		if (res.buffer == 2) {
			result[2] = ByteArrayInputStream(Base64.decode(res.content, Base64.DEFAULT))
		} else {
			result[2] = ByteArrayInputStream(res.content?.toByteArray())
		}
		return result
	}

	private fun getStream(o: Any): ByteArrayInputStream {
		if (o is JSONArray) {
			val bytes = ByteArray(o.length())
			for (i in 0..<o.length()) bytes[i] = o.optInt(i).toByte()
			return ByteArrayInputStream(bytes)
		} else {
			return ByteArrayInputStream(o.toString().toByteArray())
		}
	}

	companion object {
		private const val SPIDER_STRING_CODE = "import * as spider from '%s'\n\n" +
				"if (!globalThis.__JS_SPIDER__) {\n" +
				"    if (spider.__jsEvalReturn) {\n" +
				"        globalThis.req = http\n" +
				"        globalThis.__JS_SPIDER__ = spider.__jsEvalReturn()\n" +
				"        globalThis.__JS_SPIDER__.is_cat = true\n" +
				"    } else if (spider.default) {\n" +
				"        globalThis.__JS_SPIDER__ = typeof spider.default === 'function' ? spider.default() : spider.default\n" +
				"    }\n" +
				"}"

		fun byteFF(bytes: ByteArray): ByteArray {
			val newBt = ByteArray(bytes.size - 4)
			newBt[0] = 1
			System.arraycopy(bytes, 5, newBt, 1, bytes.size - 5)
			return newBt
		}
	}
}