package com.github.tvbox.osc.util

import com.github.catvod.crawler.SpiderDebug.log
import com.github.tvbox.osc.server.ControlManager.Companion.instance
import com.github.tvbox.osc.util.parser.SuperParse.loadHtml
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder

object Proxy {
	fun proxy(params: Map<String, List<String>>): Array<Any?>? {
		return try {
			val what = params["go"]?.getOrNull(0) ?: return null
			when (what) {
				"live" -> itv(params)
				"bom" -> removeBOMFromM3U8(params)
				"ad" -> null // TODO
				"SuperParse" -> {
					val flag = params["flag"]?.getOrNull(0) ?: return null
					val url = params["url"]?.getOrNull(0) ?: return null
					loadHtml(flag, url)
				}

				else -> null
			}
		} catch (ignored: Throwable) {
			null
		}
	}

	private fun itv(params: Map<String, List<String>>): Array<Any?>? {
		return try {
			val result = arrayOfNulls<Any>(3)
			var url = params["url"]?.getOrNull(0) ?: return null
			val type = params["type"]?.getOrNull(0) ?: return null
			url = URLDecoder.decode(url, "UTF-8")

			val client = OkGoHelper.ItvClient ?: return null
			when (type) {
				"m3u8" -> {
					val redirectUrl = getRedirectedUrl(url)

					val request = Request.Builder().url(redirectUrl).build()
					executeRequest(client, request).use { response ->
						if (response.isSuccessful) {
							val respContent = checkNotNull(response.body).string()
							val m3u8Content = processM3u8Content(respContent, redirectUrl)
							result[0] = 200
							result[1] = "application/vnd.apple.mpegurl"
							result[2] = ByteArrayInputStream(m3u8Content.toByteArray())
						} else {
							throw IOException("M3U8 Request failed with code: " + response.code)
						}
					}
				}

				"ts" -> {
					val request = Request.Builder().url(url).build()
					executeRequest(client, request).use { response ->
						if (response.isSuccessful) {
							result[0] = 200
							result[1] = "video/mp2t"
							result[2] = ByteArrayInputStream(checkNotNull(response.body).bytes())
						} else {
							throw IOException("TS Request failed with code: " + response.code)
						}
					}
				}

				else -> {
					throw IllegalArgumentException("Invalid type: $type")
				}
			}
			result
		} catch (e: Exception) {
			log(e)
			null
		}
	}

	private fun removeBOMFromM3U8(params: Map<String, List<String>>): Array<Any?>? {
		return try {
			val result = arrayOfNulls<Any>(3)
			var url = params["url"]?.getOrNull(0) ?: return null
			url = URLDecoder.decode(url, "UTF-8")

			val client = OkGoHelper.ItvClient ?: return null
			val redirectUrl = getRedirectedUrl(url)

			val request = Request.Builder().url(redirectUrl).build()
			executeRequest(client, request).use { response ->
				if (response.isSuccessful) {
					var m3u8Content = checkNotNull(response.body).string()
					// 检查并去除 UTF-8 BOM 头（BOM 为 \uFEFF）
					if (m3u8Content.startsWith('\ufeff')) {
						m3u8Content = m3u8Content.substring(1)
					}
					result[0] = 200
					result[1] = "application/vnd.apple.mpegurl"
					result[2] = ByteArrayInputStream(m3u8Content.toByteArray())
				} else {
					throw IOException("M3U8 Request failed with code: " + response.code)
				}
			}
			result
		} catch (e: Exception) {
			log(e)
			null
		}
	}

	private fun executeRequest(client: OkHttpClient, request: Request): Response {
		return try {
			client.newCall(request).execute()
		} catch (e: IOException) {
			System.err.println("网络请求异常：" + e.message)
			throw e // 重新抛出异常，让外层处理
		}
	}

	private fun processM3u8Content(m3u8Content: String, m3u8Url: String): String {
		val m3u8Lines = m3u8Content.trim().split("\n")
		val processedM3u8 = StringBuilder()

		for (line in m3u8Lines) {
			if (line.startsWith("#")) {
				processedM3u8.append(line).append("\n")
			} else {
				processedM3u8.append(joinUrl(m3u8Url, line)).append("\n")
			}
		}
		return processedM3u8.toString().replace("\\n\\n", "\n")
	}

	private fun joinUrl(base: String?, url: String?): String? {
		if (base == null && url == null) return ""
		val safeBase = base?.trim().orEmpty()
		val safeUrl = url?.trim().orEmpty()
		return try {
			val baseUri = URI(safeBase)
			val urlUri = URI(safeUrl)
			val proxyUrl = instance.getAddress(true) + "proxy?go=live&type=ts&url="
			when {
				safeUrl.startsWith("http://") || safeUrl.startsWith("https://") ->
					proxyUrl + URLEncoder.encode(urlUri.toString(), "UTF-8")

				safeUrl.startsWith("://") ->
					proxyUrl + URLEncoder.encode(URI(baseUri.scheme + safeUrl).toString(), "UTF-8")

				safeUrl.startsWith("//") ->
					proxyUrl + URLEncoder.encode(URI(baseUri.scheme + ":" + safeUrl).toString(), "UTF-8")

				else -> {
					val resolvedUri = baseUri.resolve(safeUrl)
					proxyUrl + URLEncoder.encode(resolvedUri.toString(), "UTF-8")
				}
			}
		} catch (e: Exception) {
			e.printStackTrace()
			null
		}
	}

	fun getRedirectedUrl(url: String): String {
		val client = OkHttpClient.Builder()
			.followRedirects(false) // 不自动跟随重定向
			.build()

		val request = Request.Builder()
			.url(url)
			.build()

		client.newCall(request).execute().use { response ->
			if (response.isRedirect) { // 判断是否为重定向
				return response.header("Location") ?: url // 获取重定向后的地址
			}
			return url // 如果没有重定向，返回原 URL
		}
	}

	fun getM3U8Content(url: String): String {
		val request = Request.Builder()
			.url(url)
			.build()

		val client = OkGoHelper.ItvClient ?: throw IOException("请求失败")
		client.newCall(request).execute().use { response ->
			if (response.isSuccessful) {
				return response.body.string() // 获取 m3u8 文件内容
			} else {
				throw IOException("请求失败，HTTP 状态码: " + response.code)
			}
		}
	}
}
