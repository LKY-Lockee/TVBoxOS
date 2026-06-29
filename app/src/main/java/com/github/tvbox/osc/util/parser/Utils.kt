package com.github.tvbox.osc.util.parser

import android.webkit.ValueCallback
import android.webkit.WebView
import org.json.JSONObject
import java.util.regex.Pattern

object Utils {
	val RULE: Pattern = Pattern.compile("http((?!http).){12,}?\\.(m3u8|mp4|flv|avi|mkv|rm|wmv|mpg|m4a|mp3)\\?.*|http((?!http).){12,}\\.(m3u8|mp4|flv|avi|mkv|rm|wmv|mpg|m4a|mp3)|http((?!http).)*?video/tos*")
	const val UA_WIN_CHROME: String = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/94.0.4606.54 Safari/537.36"
	const val UA_MOBILE: String = "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.1 Mobile/15E148 Safari/604.1"

	fun isVip(url: String): Boolean {
		val hosts: List<String> = listOf("iqiyi.com", "v.qq.com", "youku.com", "le.com", "tudou.com", "mgtv.com", "sohu.com", "acfun.cn", "bilibili.com", "baofeng.com", "pptv.com")
		for (host in hosts) if (url.contains(host)) return true
		return false
	}

	fun isVideoFormat(url: String): Boolean {
		if (url.contains("url=http") || url.contains(".js") || url.contains(".css") || url.contains(".html")) return false
		return RULE.matcher(url).find()
	}

	fun substring(text: String?, num: Int = 1): String? {
		return if (text != null && text.length > num) {
			text.substring(0, text.length - num)
		} else {
			text
		}
	}

	fun loadUrl(webView: WebView, script: String, callback: ValueCallback<String?>? = null) {
		webView.evaluateJavascript(script, callback)
	}

	fun isBlackVodUrl(input: String?, url: String): Boolean {
		return url.contains("973973.xyz") || url.contains(".fit:")
	}

	fun fixJsonVodHeader(headers: JSONObject?, input: String, url: String): JSONObject {
		val result = headers ?: JSONObject()
		if (input.contains("www.mgtv.com")) {
			result.put("Referer", " ")
			result.put("User-Agent", " Mozilla/5.0")
		} else if (url.contains("titan.mgtv")) {
			result.put("Referer", " ")
			result.put("User-Agent", " Mozilla/5.0")
		} else if (input.contains("bilibili")) {
			result.put("Referer", " https://www.bilibili.com/")
			result.put("User-Agent", " $UA_WIN_CHROME")
		}
		return result
	}

	fun jsonParse(input: String, json: String): JSONObject? {
		val jsonPlayData = JSONObject(json)
		var url: String?
		url = if (jsonPlayData.has("data")) {
			jsonPlayData.getJSONObject("data").getString("url")
		} else {
			jsonPlayData.getString("url")
		}
		if (url.startsWith("//")) {
			url = "https:$url"
		}
		if (!url.startsWith("http")) {
			return null
		}
		if (url == input) {
			if (isVip(url) || !isVideoFormat(url)) {
				return null
			}
		}
		if (isBlackVodUrl(input, url)) {
			return null
		}
		var headers = JSONObject()
		val ua = jsonPlayData.optString("user-agent", "")
		if (ua.trim { it <= ' ' }.isNotEmpty()) {
			headers.put("User-Agent", " $ua")
		}
		val referer = jsonPlayData.optString("referer", "")
		if (referer.trim { it <= ' ' }.isNotEmpty()) {
			headers.put("Referer", " $referer")
		}

		headers = fixJsonVodHeader(headers, input, url)
		val taskResult = JSONObject()
		taskResult.put("header", headers)
		taskResult.put("url", url)
		return taskResult
	}
}
