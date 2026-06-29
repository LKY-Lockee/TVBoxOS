package com.github.tvbox.osc.util.urlhttp

import com.github.tvbox.osc.util.urlhttp.internal.BrotliSource.create
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.asResponseBody
import okio.GzipSource
import okio.buffer

class BrotliInterceptor : Interceptor {
	override fun intercept(chain: Interceptor.Chain): Response {
		var userRequest = chain.request()
		if (chain.request().header("Accept-Encoding") == null) {
			userRequest = chain.request().newBuilder()
				.header("Accept-Encoding", "br,gzip")
				.build()
			return uncompress(chain.proceed(userRequest))
		}
		return chain.proceed(userRequest)
	}

	fun uncompress(response: Response): Response {
		val body = response.body
		val encoding = response.header("Content-Encoding")
		if (!encoding.isNullOrEmpty()) {
			val brotliSource = when (encoding) {
				"br" -> {
					create(body.source())
				}

				"gzip" -> {
					GzipSource(body.source())
				}

				else -> {
					return response
				}
			}
			return response.newBuilder()
				.removeHeader("Content-Encoding")
				.removeHeader("Content-Length")
				.body(brotliSource.buffer().asResponseBody(body.contentType(), -1L))
				.build()
		} else {
			return response
		}
	}
}
