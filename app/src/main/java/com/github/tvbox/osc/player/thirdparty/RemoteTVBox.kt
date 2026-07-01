package com.github.tvbox.osc.player.thirdparty

import android.app.Activity
import com.github.tvbox.osc.base.App.Companion.instance
import com.github.tvbox.osc.data.ConfigKey
import com.github.tvbox.osc.data.PreferenceStore
import com.github.tvbox.osc.server.RemoteServer.Companion.getLocalIPAddress
import com.github.tvbox.osc.util.IpScanning
import okhttp3.Call
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.URLEncoder
import java.util.Objects
import java.util.concurrent.TimeUnit

object RemoteTVBox {
	private var availableFailNum = 0
	private var availableSuccessNum = 0
	private var availableIpNum = 0

	fun run(activity: Activity, url: String?, title: String?, subtitle: String?, headers: HashMap<String, String>?): Boolean {
		var resolvedUrl = url
		val actionUrl: String = availableActionUrl
		if (actionUrl.isEmpty()) {
			return false
		}
		try {
			if (!headers.isNullOrEmpty()) {
				resolvedUrl = "$resolvedUrl|"
				val urlBuilder = StringBuilder(resolvedUrl)
				for ((idx, hk) in headers.keys.withIndex()) {
					urlBuilder.append(hk).append("=").append(URLEncoder.encode(headers[hk], "UTF-8"))
					if (idx < headers.size - 1) {
						urlBuilder.append("&")
					}
				}
				resolvedUrl = urlBuilder.toString()
			}
			val params = HashMap<String, String>()
			params["do"] = "push"
			params["url"] = resolvedUrl.orEmpty()
			post(actionUrl, params, object : okhttp3.Callback {
				override fun onFailure(call: Call, e: IOException) {
					e.printStackTrace()
				}

				override fun onResponse(call: Call, response: Response) {
				}
			})
		} catch (e: Exception) {
			e.printStackTrace()
		}

		return true
	}

	fun searchAvailable(callback: Callback) {
		availableFailNum = 0
		availableSuccessNum = 0
		val localIp = getLocalIPAddress(instance)
		val searchList = IpScanning().search(localIp, false)
		availableIpNum = searchList.size
		val port = 9978
		for (one in searchList) {
			val ip = one.ip
			if (ip == localIp) {
				availableIpNum--
				continue
			}
			val actionUrl = "http://$ip:$port/action"
			val viewHost = "$ip:$port"
			try {
				post(actionUrl, null, object : okhttp3.Callback {
					override fun onFailure(call: Call, e: IOException) {
						availableFailNum++
						callback.fail(availableFailNum == availableIpNum, (availableSuccessNum + availableFailNum) == availableIpNum)
					}

					override fun onResponse(call: Call, response: Response) {
						availableSuccessNum++
						val result = Objects.requireNonNull(response.body).string()
						if (result == "ok") {
							callback.found(viewHost, (availableSuccessNum + availableFailNum) == availableIpNum)
						}
					}
				})
			} catch (ignored: Exception) {
			}
		}
	}

	var available: String?
		get() = PreferenceStore.getObj(ConfigKey.REMOTE_TVBOX, null as String?)
		set(viewHost) {
			PreferenceStore.put(ConfigKey.REMOTE_TVBOX, viewHost)
		}

	val availableActionUrl: String
		get() {
			val availableHost = available ?: return ""
			return "http://$availableHost/action"
		}

	fun post(url: String, params: Map<String, String>?, callback: okhttp3.Callback) {
		val builder = OkHttpClient.Builder()
		builder.readTimeout(1000, TimeUnit.MILLISECONDS)
		builder.writeTimeout(1000, TimeUnit.MILLISECONDS)
		builder.connectTimeout(1000, TimeUnit.MILLISECONDS)
		val client = builder.build()
		val formBodyBuilder = FormBody.Builder()
		if (!params.isNullOrEmpty()) {
			for (entry in params.entries) {
				formBodyBuilder.add(entry.key, entry.value)
			}
		}
		val formBody = formBodyBuilder.build()
		client.newCall(Request.Builder().url(url).post(formBody).build()).enqueue(callback)
	}

	abstract class Callback {
		abstract fun found(viewHost: String?, end: Boolean)

		abstract fun fail(all: Boolean, end: Boolean)
	}
}
