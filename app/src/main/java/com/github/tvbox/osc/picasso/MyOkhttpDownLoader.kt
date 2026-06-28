/*
 * Copyright (C) 2013 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.tvbox.osc.picasso

import androidx.annotation.VisibleForTesting
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.squareup.picasso.Downloader
import okhttp3.Cache
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.net.URLDecoder
import java.util.Locale

/**
 * A [Downloader] which uses OkHttp to download images.
 */
class MyOkhttpDownLoader : Downloader {
	@VisibleForTesting
	val client: Call.Factory
	private val cache: Cache?

	/**
	 * Create a new downloader that uses the specified OkHttp instance. A response cache will not be automatically configured.
	 */
	constructor(client: OkHttpClient) {
		this.client = client
		this.cache = client.cache
	}

	/**
	 * Create a new downloader that uses the specified [Call.Factory] instance.
	 */
	constructor(client: Call.Factory) {
		this.client = client
		this.cache = null
	}

	override fun load(request: Request): Response {
		var url = request.url.toString()

		// 检查链接里面是否有自定义header
		val header = if (url.contains("@Headers=")) URLDecoder.decode(url.substringAfter("@Headers=").substringBefore("@"), "UTF-8") else null
		val cookie = if (url.contains("@Cookie=")) url.substringAfter("@Cookie=").substringBefore("@") else null
		val ua = if (url.contains("@User-Agent=")) url.substringAfter("@User-Agent=").substringBefore("@") else null
		val referer = if (url.contains("@Referer=")) url.substringAfter("@Referer=").substringBefore("@") else null

		url = url.substringBefore("@")
		val mRequestBuilder = request.newBuilder().url(url)
		if (header != null) {
			val jsonInfo = Gson().fromJson(header, JsonObject::class.java)
			for (key in jsonInfo.keySet()) {
				val value = jsonInfo.get(key).asString
				mRequestBuilder.addHeader(key.uppercase(Locale.getDefault()), removeDuplicateSlashes(value))
			}
		} else {
			if (!cookie.isNullOrEmpty()) {
				mRequestBuilder.addHeader("Cookie", cookie)
			}
			if (!ua.isNullOrEmpty()) {
				mRequestBuilder.addHeader("User-Agent", ua)
			} else {
				val mobileUA = "Dalvik/2.1.0 (Linux; U; Android 13; M2102J2SC Build/TKQ1.220829.002)"
				mRequestBuilder.addHeader("User-Agent", mobileUA)
			}
			if (!referer.isNullOrEmpty()) {
				mRequestBuilder.addHeader("Referer", referer)
			}
		}
		return client.newCall(mRequestBuilder.build()).execute()
	}

	override fun shutdown() {
	}

	companion object {
		private fun removeDuplicateSlashes(paramValue: String): String {
			return paramValue.replace("//", "/")
		}
	}
}
