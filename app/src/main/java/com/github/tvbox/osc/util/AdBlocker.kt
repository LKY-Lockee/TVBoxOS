package com.github.tvbox.osc.util

import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream
import java.util.Locale

object AdBlocker {
	private val AD_HOSTS: MutableList<String> = ArrayList()

	fun clear() {
		AD_HOSTS.clear()
	}

	val isEmpty: Boolean
		get() = AD_HOSTS.isEmpty()

	fun addAdHost(host: String?) {
		host?.let { AD_HOSTS.add(it) }
	}

	fun hasHost(host: String?): Boolean {
		return AD_HOSTS.contains(host)
	}

	fun isAd(url: String): Boolean {
		val lowerUrl = url.lowercase(Locale.getDefault())
		for (adHost in AD_HOSTS) {
			if (lowerUrl.contains(adHost)) {
				return true
			}
		}
		return false
	}

	fun createEmptyResource(): WebResourceResponse {
		return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream("".toByteArray()))
	}
}
