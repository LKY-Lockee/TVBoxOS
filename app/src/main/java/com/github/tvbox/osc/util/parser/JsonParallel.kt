package com.github.tvbox.osc.util.parser

import android.util.Base64
import com.github.catvod.crawler.SpiderDebug.log
import okhttp3.Headers.Companion.toHeaders
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.CompletionService
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * 并发解析，直到获得第一个结果
 */
object JsonParallel {
	private val futures: MutableList<Future<JSONObject?>> = ArrayList()
	private var client: OkHttpClient? = null
	private var executorService: ExecutorService? = null

	fun parse(jx: LinkedHashMap<String, String>, url: String): JSONObject {
		try {
			if (jx.isNotEmpty()) {
				val localClient = OkHttpClient().also { client = it }
				// 使用线程池并发处理任务
				val localExecutor = Executors.newFixedThreadPool(5).also { executorService = it }
				val completionService: CompletionService<JSONObject?> = ExecutorCompletionService(localExecutor)
				futures.clear()

				// 遍历所有的解析配置
				for ((jxName, parseUrl) in jx) {
					futures.add(completionService.submit {
						try {
							// 获取请求头，并从中取出实际url
							val reqHeaders = getReqHeader(parseUrl)
							val realUrl = reqHeaders["url"] ?: return@submit null
							reqHeaders.remove("url")
							val headers = reqHeaders.toHeaders()
							val request = Request.Builder()
								.url(realUrl + url)
								.headers(headers)
								.tag("ParseTag")
								.build()

							val call = localClient.newCall(request)
							val response = call.execute()
							val json = response.body.string()

							val taskResult = Utils.jsonParse(url, json) ?: return@submit null
							taskResult.put("jxFrom", jxName)
							return@submit taskResult
						} catch (th: Throwable) {
							// 输出日志
							return@submit null
						}
					})
				}

				var pTaskResult: JSONObject? = null
				for (i in futures.indices) {
					val completed = completionService.take()
					try {
						pTaskResult = completed.get()
						if (pTaskResult != null) {
							localClient.dispatcher.cancelAll()
							for (future in futures) {
								try {
									future.cancel(true)
								} catch (t: Throwable) {
									log(t)
								}
							}
							futures.clear()
							break
						}
					} catch (th: Throwable) {
						log(th)
					}
				}
				localExecutor.shutdownNow()
				if (pTaskResult != null) return pTaskResult
			}
		} catch (th: Throwable) {
			log(th)
		}
		return JSONObject()
	}

	fun cancelTasks() {
		client?.dispatcher?.cancelAll()
		for (future in futures) {
			try {
				future.cancel(true)
			} catch (ignored: Throwable) {
			}
		}
		futures.clear()
		executorService?.shutdownNow()
	}

	fun getReqHeader(url: String): HashMap<String, String> {
		val reqHeaders = HashMap<String, String>()
		reqHeaders["url"] = url
		if (url.contains("cat_ext")) {
			try {
				val start = url.indexOf("cat_ext=")
				val end = url.indexOf("&", start)
				var ext = url.substring(start + 8, end)
				ext = String(Base64.decode(ext, Base64.DEFAULT or Base64.URL_SAFE or Base64.NO_WRAP))
				val newUrl = url.substring(0, start) + url.substring(end + 1)
				val jsonObject = JSONObject(ext)
				if (jsonObject.has("header")) {
					val headerJson = jsonObject.optJSONObject("header")
					if (headerJson != null) {
						val keys = headerJson.keys()
						while (keys.hasNext()) {
							val key = keys.next()
							reqHeaders[key] = headerJson.optString(key, "")
						}
					}
				}
				reqHeaders["url"] = newUrl
			} catch (ignored: Throwable) {
			}
		}
		return reqHeaders
	}
}
