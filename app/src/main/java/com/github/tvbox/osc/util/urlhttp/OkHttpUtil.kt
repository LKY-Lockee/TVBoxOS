package com.github.tvbox.osc.util.urlhttp

import com.github.tvbox.osc.util.OkGoHelper
import com.github.tvbox.osc.util.UA
import com.lzy.okgo.OkGo
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.IOException

object OkHttpUtil {
	const val METHOD_GET: String = "GET"
	const val METHOD_POST: String = "POST"

	fun string(client: OkHttpClient, url: String, tag: String?, paramsMap: Map<String, String>?, headerMap: Map<String, String>?, respHeaderMap: MutableMap<String, List<String>>?): String? {
		val stringCallback: OKCallBack<String> = object : OKCallBack<String>() {
			public override fun onParseResponse(call: Call?, response: Response?): String {
				try {
					if (respHeaderMap != null && response != null) {
						respHeaderMap.clear()
						respHeaderMap.putAll(response.headers.toMultimap())
					}
					return response?.body?.string().orEmpty()
				} catch (e: IOException) {
					return ""
				}
			}

			public override fun onFailure(call: Call?, e: Exception?) {
				result = ""
			}

			public override fun onResponse(response: String?) {
			}
		}
		val req = OKRequest(METHOD_GET, url, paramsMap, headerMap, stringCallback)
		req.setTag(tag)
		req.execute(client)
		return stringCallback.result
	}

	fun stringNoRedirect(url: String, headerMap: Map<String, String>?, respHeaderMap: MutableMap<String, List<String>>?): String? {
		return string(OkGoHelper.getNoRedirectClient(), url, null, null, headerMap, respHeaderMap)
	}

	fun string(url: String, headerMap: Map<String, String>?, respHeaderMap: MutableMap<String, List<String>>?): String? {
		return string(OkGoHelper.getDefaultClient(), url, null, null, headerMap, respHeaderMap)
	}

	fun string(url: String, headerMap: Map<String, String>?): String? {
		return string(OkGoHelper.getDefaultClient(), url, null, null, headerMap, null)
	}

	fun string(url: String, tag: String?, headerMap: Map<String, String>?): String? {
		return string(OkGoHelper.getDefaultClient(), url, tag, null, headerMap, null)
	}

	fun get(client: OkHttpClient, url: String, callBack: OKCallBack<*>?) {
		get(client, url, null, null, callBack)
	}

	fun get(client: OkHttpClient, url: String, paramsMap: Map<String, String>?, callBack: OKCallBack<*>?) {
		get(client, url, paramsMap, null, callBack)
	}

	fun get(client: OkHttpClient, url: String, paramsMap: Map<String, String>?, headerMap: Map<String, String>?, callBack: OKCallBack<*>?) {
		OKRequest(METHOD_GET, url, paramsMap, headerMap, callBack).execute(client)
	}

	fun post(client: OkHttpClient, url: String, callBack: OKCallBack<*>?) {
		post(client, url, null, callBack)
	}

	fun post(client: OkHttpClient, url: String, paramsMap: Map<String, String>?, callBack: OKCallBack<*>?) {
		post(client, url, paramsMap, null, callBack)
	}

	fun post(client: OkHttpClient, url: String, paramsMap: Map<String, String>?, headerMap: Map<String, String>?, callBack: OKCallBack<*>?) {
		OKRequest(METHOD_POST, url, paramsMap, headerMap, callBack).execute(client)
	}

	fun post(client: OkHttpClient, url: String, tag: String?, paramsMap: Map<String, String>?, headerMap: Map<String, String>?, callBack: OKCallBack<*>?) {
		val req = OKRequest(METHOD_POST, url, paramsMap, headerMap, callBack)
		req.setTag(tag)
		req.execute(client)
	}

	fun postJson(client: OkHttpClient, url: String, jsonStr: String?, callBack: OKCallBack<*>?) {
		postJson(client, url, jsonStr, null, callBack)
	}

	fun postJson(client: OkHttpClient, url: String, jsonStr: String?, headerMap: Map<String, String>?, callBack: OKCallBack<*>?) {
		OKRequest(METHOD_POST, url, jsonStr, headerMap, callBack).execute(client)
	}

	fun get(str: String?): String {
		return try {
			OkGo.get<String?>(str).headers("User-Agent", UA.random()).execute().body.string()
		} catch (e: IOException) {
			""
		}
	}

	/**
	 * 根据Tag取消请求
	 */
	fun cancel(client: OkHttpClient?, tag: Any?) {
		if (client == null || tag == null) return
		for (call in client.dispatcher.queuedCalls()) {
			if (tag == call.request().tag()) {
				call.cancel()
			}
		}
		for (call in client.dispatcher.runningCalls()) {
			if (tag == call.request().tag()) {
				call.cancel()
			}
		}
	}

	fun cancel(tag: Any?) {
		cancel(OkGoHelper.getDefaultClient(), tag)
	}

	/**
	 * 取消所有请求请求
	 */
	fun cancelAll(client: OkHttpClient? = OkGoHelper.getDefaultClient()) {
		if (client == null) return
		for (call in client.dispatcher.queuedCalls()) {
			call.cancel()
		}
		for (call in client.dispatcher.runningCalls()) {
			call.cancel()
		}
	}

	/**
	 * 获取重定向地址
	 * 
	 * @param headers
	 * @return
	 */
	fun getRedirectLocation(headers: Map<String, MutableList<String>>?): String? {
		if (headers == null) return null
		headers["location"]?.let { return it[0] }
		headers["Location"]?.let { return it[0] }
		return null
	}
}
