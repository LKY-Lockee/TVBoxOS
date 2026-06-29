package com.github.tvbox.osc.util.parser

import android.util.Base64
import com.github.catvod.crawler.SpiderDebug.log
import com.github.tvbox.osc.util.LOG
import com.github.tvbox.osc.util.parser.JsonParallel.cancelTasks
import com.github.tvbox.osc.util.parser.JsonParallel.parse
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

object SuperParse {
	val flagWebJx: HashMap<String, MutableList<String>> = HashMap()
	var configs: HashMap<String, MutableList<String>>? = null
	var jsonJx: LinkedHashMap<String, String>? = null
	var webJx: MutableList<String>? = null

	fun parse(jx: LinkedHashMap<String, HashMap<String, String>>, flag: String, url: String): JSONObject {
		try {
			// 初始化全局配置（configs）一次
			if (configs == null) {
				val newConfigs = HashMap<String, MutableList<String>>()
				for ((key, parseBean) in jx) {
					val type = parseBean["type"] ?: continue
					if ("1" == type || "0" == type) {
						try {
							val ext = parseBean["ext"] ?: continue
							val flagsArray = JSONObject(ext).getJSONArray("flag")
							for (j in 0..<flagsArray.length()) {
								val flagKey = flagsArray.getString(j)
								val flagJx = newConfigs.getOrPut(flagKey) { ArrayList() }
								flagJx.add(key)
							}
						} catch (e: Exception) {
							log(e)
						}
					}
				}
				configs = newConfigs
			}

			// 根据配置构建 jsonJx 和 webJx
			val localJsonJx = LinkedHashMap<String, String>()
			val localWebJx = ArrayList<String>()
			val currentConfigs = configs ?: return JSONObject()
			val targetKeys = currentConfigs[flag]
			if (!targetKeys.isNullOrEmpty()) {
				for (key in targetKeys) {
					val parseBean = jx[key] ?: continue
					val type = parseBean["type"] ?: continue
					if ("1" == type) {
						val urlValue = parseBean["url"]
						val ext = parseBean["ext"]
						if (urlValue != null && ext != null) {
							localJsonJx[key] = mixUrl(urlValue, ext)
						}
					} else if ("0" == type) {
						val urlValue = parseBean["url"]
						if (urlValue != null) {
							localWebJx.add(urlValue)
						}
					}
				}
			} else {
				for ((key, parseBean) in jx) {
					val type = parseBean["type"] ?: continue
					if ("1" == type) {
						val urlValue = parseBean["url"]
						val ext = parseBean["ext"]
						if (urlValue != null && ext != null) {
							localJsonJx[key] = mixUrl(urlValue, ext)
						}
					} else if ("0" == type) {
						val urlValue = parseBean["url"]
						if (urlValue != null) {
							localWebJx.add(urlValue)
						}
					}
				}
			}
			jsonJx = localJsonJx
			webJx = localWebJx

			if (localWebJx.isNotEmpty()) {
				flagWebJx[flag] = localWebJx
			}

			if (localWebJx.isNotEmpty()) {
				val webResult = JSONObject()
				webResult.put("url", "proxy://go=SuperParse&flag=" + flag + "&url=" + Base64.encodeToString(url.toByteArray(), Base64.DEFAULT or Base64.URL_SAFE or Base64.NO_WRAP))
				webResult.put("parse", 1)
				webResult.put("ua", Utils.UA_WIN_CHROME)
				return webResult
			}
		} catch (e: Exception) {
			LOG.i("echo-result" + e.message)
		}
		return JSONObject()
	}

	fun doJsonJx(jsonJxs: LinkedHashMap<String, String>, url: String): JSONObject {
		LOG.i("echo-jsonJx1$jsonJxs")
		return parse(jsonJxs, url)
	}

	fun doJsonJx(url: String): JSONObject {
		LOG.i("echo-jsonJx2$jsonJx")
		return parse(jsonJx ?: return JSONObject(), url)
	}

	fun stopJsonJx() {
		cancelTasks()
	}

	private fun mixUrl(url: String, ext: String): String {
		if (ext.trim { it <= ' ' }.isNotEmpty()) {
			val idx = url.indexOf("?")
			if (idx > 0) {
				return url.substring(0, idx + 1) + "cat_ext=" + Base64.encodeToString(ext.toByteArray(), Base64.DEFAULT or Base64.URL_SAFE or Base64.NO_WRAP) + "&" + url.substring(idx + 1)
			}
		}
		return url
	}

	fun loadHtml(flag: String, url: String?): Array<Any?>? {
		val decodedUrl: String
		try {
			decodedUrl = String(Base64.decode(url, Base64.DEFAULT or Base64.URL_SAFE or Base64.NO_WRAP), StandardCharsets.UTF_8)
			var html = "\n" +
					"<!doctype html>\n" +
					"<html>\n" +
					"<head>\n" +
					"<title>解析</title>\n" +
					"<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />\n" +
					"<meta http-equiv=\"X-UA-Compatible\" content=\"IE=EmulateIE10\" />\n" +
					"<meta name=\"renderer\" content=\"webkit|ie-comp|ie-stand\">\n" +
					"<meta name=\"viewport\" content=\"width=device-width\">\n" +
					"</head>\n" +
					"<body>\n" +
					"<script>\n" +
					"var apiArray=[#jxs#];\n" +
					"var urlPs=\"#url#\";\n" +
					"var iframeHtml=\"\";\n" +
					"for(var i=0;i<apiArray.length;i++){\n" +
					"var URL=apiArray[i]+urlPs;\n" +
					"iframeHtml=iframeHtml+\"<iframe sandbox='allow-scripts allow-same-origin allow-forms' frameborder='0' allowfullscreen='true' webkitallowfullscreen='true' mozallowfullscreen='true' src=\"+URL+\"></iframe>\";\n" +
					"}\n" +
					"document.write(iframeHtml);\n" +
					"</script>\n" +
					"</body>\n" +
					"</html>"

			val jxs = StringBuilder()
			val jxUrls = flagWebJx[flag]
			if (jxUrls != null) {
				for (i in jxUrls.indices) {
					jxs.append("\"")
					jxs.append(jxUrls[i])
					jxs.append("\"")
					if (i < jxUrls.size - 1) {
						jxs.append(",")
					}
				}
			}
			html = html.replace("#url#", decodedUrl).replace("#jxs#", jxs.toString())
			val result = arrayOfNulls<Any>(3)
			result[0] = 200
			result[1] = "text/html; charset=\"UTF-8\""
			val baos = ByteArrayInputStream(html.toByteArray(StandardCharsets.UTF_8))
			result[2] = baos
			return result
		} catch (th: Throwable) {
			th.printStackTrace()
		}
		return null
	}
}
