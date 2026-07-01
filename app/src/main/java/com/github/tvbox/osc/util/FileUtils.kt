package com.github.tvbox.osc.util

import android.os.Environment
import android.text.TextUtils
import android.util.Base64
import com.github.tvbox.osc.base.App
import com.github.tvbox.osc.data.ConfigKey
import com.github.tvbox.osc.data.PreferenceStore
import com.github.tvbox.osc.server.ControlManager
import com.github.tvbox.osc.util.urlhttp.OkHttpUtil
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.regex.Pattern
import kotlin.math.max

object FileUtils {
	// JS 工具方法
	private val URL_JOIN: Pattern = Pattern.compile("^http.*\\.(js|txt|json)", Pattern.MULTILINE or Pattern.CASE_INSENSITIVE)
	private val cachedDirFiles: MutableMap<String, MutableSet<String>> = HashMap()

	fun writeSimple(data: ByteArray?, dst: File): Boolean {
		return try {
			if (dst.exists()) dst.delete()
			BufferedOutputStream(Files.newOutputStream(dst.toPath())).use { bos ->
				bos.write(data)
			}
			true
		} catch (e: IOException) {
			e.printStackTrace()
			false
		}
	}

	fun readSimple(src: File): ByteArray? {
		return try {
			BufferedInputStream(Files.newInputStream(src.toPath())).use { bis ->
				val len = bis.available()
				val data = ByteArray(len)
				bis.read(data)
				data
			}
		} catch (e: IOException) {
			e.printStackTrace()
			null
		}
	}

	fun copyFile(source: File, dest: File) {
		Files.newInputStream(source.toPath()).use { input ->
			Files.newOutputStream(dest.toPath()).use { output ->
				val buffer = ByteArray(1024)
				var length: Int
				while ((input.read(buffer).also { length = it }) > 0) {
					output.write(buffer, 0, length)
				}
			}
		}
	}

	fun recursiveDelete(file: File) {
		if (!file.exists()) return
		if (file.isDirectory) {
			file.listFiles()?.forEach { recursiveDelete(it) }
		}
		file.delete()
	}

	fun readFileToString(path: String, charsetName: String): String {
		// 定义返回结果
		val jsonString = StringBuilder()

		try {
			BufferedReader(InputStreamReader(Files.newInputStream(Paths.get(path)), charsetName)).use { reader ->
				// 读取文件
				var thisLine: String?
				while ((reader.readLine().also { thisLine = it }) != null) {
					jsonString.append(thisLine)
				}
			}
		} catch (e: IOException) {
			e.printStackTrace()
		}
		// 返回拼接好的JSON String
		return jsonString.toString()
	}

	val rootPath: String
		get() = Environment.getExternalStorageDirectory().absolutePath

	fun getLocal(path: String): File {
		return File(path.replace("file:/", rootPath))
	}

	val cacheDir: File?
		get() = App.instance.cacheDir

	val cachePath: String
		get() = cacheDir?.absolutePath.orEmpty()

	val filePath: String
		get() = App.instance.filesDir.absolutePath

	fun cleanDirectory(dir: File) {
		if (!dir.exists()) return
		dir.listFiles()?.forEach { one ->
			try {
				deleteFile(one)
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}
	}

	fun isWeekAgo(file: File): Boolean {
		val oneWeekMillis = 3L * 24 * 60 * 60 * 1000
		val timeDiff = System.currentTimeMillis() - file.lastModified()
		return timeDiff > oneWeekMillis
	}

	fun deleteFile(file: File) {
		if (!file.exists()) return
		if (file.isFile) {
			if (file.canWrite()) file.delete()
			return
		}
		if (file.isDirectory) {
			val files = file.listFiles()
			if (files == null || files.isEmpty()) {
				if (file.canWrite()) file.delete()
				return
			}
			for (one in files) {
				deleteFile(one)
			}
		}
	}

	fun cleanPlayerCache() {
		val ijkCachePath = "$cachePath/ijkcaches/"
		val thunderCachePath = "$cachePath/thunder/"
		val ijkCacheDir = File(ijkCachePath)
		val thunderCacheDir = File(thunderCachePath)
		try {
			if (ijkCacheDir.exists()) cleanDirectory(ijkCacheDir)
		} catch (e: Exception) {
			e.printStackTrace()
		}
		try {
			if (thunderCacheDir.exists()) cleanDirectory(thunderCacheDir)
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}

	fun read(path: String): String {
		return try {
			val br = BufferedReader(InputStreamReader(Files.newInputStream(getLocal(path).toPath())))
			val sb = StringBuilder()
			var text: String?
			while ((br.readLine().also { text = it }) != null) sb.append(text).append("\n")
			br.close()
			sb.toString()
		} catch (e: Exception) {
			""
		}
	}

	fun getFileName(filePath: String): String {
		if (TextUtils.isEmpty(filePath)) return ""
		var fileName = filePath
		val p = fileName.lastIndexOf(File.separatorChar)
		if (p != -1) {
			fileName = fileName.substring(p + 1)
		}
		return fileName
	}

	fun getFileNameWithoutExt(filePath: String): String {
		if (TextUtils.isEmpty(filePath)) return ""
		var fileName = filePath
		var p = fileName.lastIndexOf(File.separatorChar)
		if (p != -1) {
			fileName = fileName.substring(p + 1)
		}
		p = fileName.indexOf('.')
		if (p != -1) {
			fileName = fileName.substring(0, p)
		}
		return fileName
	}

	fun hasExtension(path: String): Boolean {
		val lastDotIndex = path.lastIndexOf(".")
		val lastSlashIndex = max(path.lastIndexOf("/"), path.lastIndexOf("\\"))
		// 如果路径中有点号，并且点号在最后一个斜杠之后，认为有后缀
		return lastDotIndex > lastSlashIndex && lastDotIndex < path.length - 1
	}

	fun saveCache(cache: File, json: String) {
		try {
			val cacheDir = cache.parentFile ?: return
			if (!cacheDir.exists()) cacheDir.mkdirs()
			if (cache.exists()) cache.delete()
			FileOutputStream(cache).use { fos ->
				fos.write(json.toByteArray(StandardCharsets.UTF_8))
				fos.flush()
			}
		} catch (th: Throwable) {
			th.printStackTrace()
		}
	}

	fun loadModule(name: String): String? {
		var resolvedName = name
		var rel: String? = null
		try {
			resolvedName = when {
				resolvedName.contains("gbk.js") -> "gbk.js"
				resolvedName.contains("模板.js") -> "模板.js"
				resolvedName.contains("cat.js") -> "cat.js"
				else -> resolvedName
			}
			TVBoxRuntimeLog.i("echo-loadModule $resolvedName")
			val m = URL_JOIN.matcher(resolvedName)
			if (m.find()) {
				if (!PreferenceStore.get(ConfigKey.DEBUG_OPEN, false)) {
					val cache = getCache(MD5.encode(resolvedName))
					rel = cache
					if (StringUtils.isEmpty(cache)) {
						val netStr = get(resolvedName)
						if (!TextUtils.isEmpty(netStr)) {
							setCache(604800, MD5.encode(resolvedName), netStr)
						}
						rel = netStr
					}
				} else {
					rel = get(resolvedName)
				}
			} else if (resolvedName.startsWith("assets://")) {
				rel = getAsOpen(resolvedName.substring(9))
			} else if (isAsFile(resolvedName, "js/lib")) {
				rel = getAsOpen("js/lib/$resolvedName")
			} else if (resolvedName.startsWith("file://")) {
				rel = get(
					ControlManager.instance
						.getAddress(true) + "file/" + resolvedName.replace("file:///", "")
						.replace("file://", "")
				)
			} else if (resolvedName.startsWith("clan://localhost/")) {
				rel = get(
					ControlManager.instance
						.getAddress(true) + "file/" + resolvedName.replace("clan://localhost/", "")
				)
			} else if (resolvedName.startsWith("clan://")) {
				val substring = resolvedName.substring(7)
				val indexOf = substring.indexOf('/')
				rel = get("http://" + substring.substring(0, indexOf) + "/file/" + substring.substring(indexOf + 1))
			}
		} catch (e: Exception) {
			e.printStackTrace()
		}
		return rel
	}

	fun isAsFile(name: String, dir: String): Boolean {
		// 1. 先从缓存里取目录列表
		var files = cachedDirFiles[dir]
		if (files == null) {
			TVBoxRuntimeLog.i("echo-读取AssetsList")
			try {
				val list = App.instance.assets.list(dir)
				files = if (list != null) HashSet(listOf(*list)) else mutableSetOf()
			} catch (e: IOException) {
				files = mutableSetOf()
			}
			cachedDirFiles[dir] = files
		}
		// 2. 内存查找
		return files.contains(name.trim())
	}

	fun getAsOpen(name: String): String {
		return try {
			App.instance.assets.open(name).use { input ->
				val data = ByteArray(input.available())
				input.read(data)
				String(data, StandardCharsets.UTF_8)
			}
		} catch (e: Exception) {
			e.printStackTrace()
			""
		}
	}

	fun getCache(name: String?): String? {
		return try {
			var code = ""
			val file = open(name)
			if (file.exists()) {
				code = String(readSimple(file) ?: return null)
			}
			if (TextUtils.isEmpty(code)) {
				return ""
			}
			val asJsonObject = Gson().fromJson(code, JsonObject::class.java).asJsonObject
			if ((asJsonObject.get("expires").asInt.toLong()) <= System.currentTimeMillis() / 1000) {
				recursiveDelete(open(name))
			}
			asJsonObject.get("data").asString
		} catch (e4: Exception) {
			""
		}
	}

	fun setCache(time: Int, name: String?, data: String?) {
		try {
			val jSONObject = JSONObject()
			jSONObject.put("expires", (time + (System.currentTimeMillis() / 1000)).toInt())
			jSONObject.put("data", data)
			writeSimple(jSONObject.toString().toByteArray(), open(name))
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}

	fun setCacheByte(name: String?, data: ByteArray?) {
		try {
			writeSimple(byteMerger("//DRPY".toByteArray(), Base64.encode(data, Base64.URL_SAFE)), open("B_$name"))
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}

	fun byteMerger(bt1: ByteArray, bt2: ByteArray): ByteArray {
		val bt3 = ByteArray(bt1.size + bt2.size)
		System.arraycopy(bt1, 0, bt3, 0, bt1.size)
		System.arraycopy(bt2, 0, bt3, bt1.size, bt2.size)
		return bt3
	}

	fun get(str: String): String? {
		return get(str, null)
	}

	fun get(str: String, headerMap: MutableMap<String, String>?): String? {
		val headers = headerMap ?: HashMap<String, String>().also {
			it["User-Agent"] = if (str.startsWith("https://gitcode.net/")) UA.random() else "okhttp/3.15"
		}
		return OkHttpUtil.string(str, headers)
	}

	fun open(str: String?): File {
		return File("$externalCachePath/qjscache_$str.js")
	}

	val externalCachePath: String
		get() {
			return App.instance.externalCacheDir?.absolutePath ?: cachePath
		}
}
