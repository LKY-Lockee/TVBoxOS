package com.github.tvbox.osc.ui.compose.util

import android.content.Context
import coil.ImageLoader
import coil.intercept.Interceptor
import coil.request.ImageResult
import com.github.tvbox.osc.util.DefaultConfig
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.OkHttpClient
import java.net.URLDecoder
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * 解析 TVBox 图片地址中的自定义头后缀（@User-Agent=@Referer=@Cookie=@Headers=），
 * 返回清洗后的 URL 与请求头。逻辑与 picasso/MyOkhttpDownLoader 保持一致。
 */
internal fun parsePicUrl(raw: String): Pair<String, Map<String, String>> {
	val headers = LinkedHashMap<String, String>()

	val header = if (raw.contains("@Headers=")) {
		URLDecoder.decode(raw.substringAfter("@Headers=").substringBefore("@"), "UTF-8")
	} else null
	val cookie = if (raw.contains("@Cookie=")) raw.substringAfter("@Cookie=").substringBefore("@") else null
	val ua = if (raw.contains("@User-Agent=")) raw.substringAfter("@User-Agent=").substringBefore("@") else null
	val referer = if (raw.contains("@Referer=")) raw.substringAfter("@Referer=").substringBefore("@") else null

	if (header != null) {
		val jsonInfo = Gson().fromJson(header, JsonObject::class.java)
		for (key in jsonInfo.keySet()) {
			val value = jsonInfo.get(key).asString
			headers[key.uppercase(Locale.getDefault())] = removeDuplicateSlashes(value)
		}
	} else {
		cookie?.takeIf { it.isNotEmpty() }?.let { headers["Cookie"] = it }
		if (!ua.isNullOrEmpty()) {
			headers["User-Agent"] = ua
		} else {
			headers["User-Agent"] = "Dalvik/2.1.0 (Linux; U; Android 13; M2102J2SC Build/TKQ1.220829.002)"
		}
		referer?.takeIf { it.isNotEmpty() }?.let { headers["Referer"] = it }
	}

	val cleanUrl = DefaultConfig.checkReplaceProxy(raw.substringBefore("@"))
	return cleanUrl to headers
}

private fun removeDuplicateSlashes(paramValue: String): String = paramValue.replace("//", "/")

/**
 * 注入自定义头并清洗 URL 的 Coil 拦截器。
 */
class PicUrlInterceptor : Interceptor {
	override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
		val request = chain.request
		val data = request.data
		val raw = when (data) {
			is String -> data
			is android.net.Uri -> data.toString()
			else -> return chain.proceed(request)
		}
		if (!raw.contains("@")) return chain.proceed(request)
		val (cleanUrl, headers) = parsePicUrl(raw)
		val newRequest = request.newBuilder().data(cleanUrl).apply {
			headers.forEach { (k, v) -> addHeader(k, v) }
		}.build()
		return chain.proceed(newRequest)
	}
}

/** 构建主界面用的 Coil ImageLoader（带 PicUrlInterceptor）。 */
fun buildCoilImageLoader(context: Context): ImageLoader {
	val client = OkHttpClient.Builder()
		.connectTimeout(15, TimeUnit.SECONDS)
		.readTimeout(20, TimeUnit.SECONDS)
		.followRedirects(true)
		.build()
	return ImageLoader.Builder(context)
		.okHttpClient(client)
		.components { add(PicUrlInterceptor()) }
		.crossfade(true)
		.build()
}
