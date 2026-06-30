package com.github.tvbox.osc.util.js

import android.util.Base64
import com.github.tvbox.osc.util.OkGoHelper
import com.github.tvbox.osc.util.urlhttp.OkHttpUtil
import com.google.common.net.HttpHeaders
import com.google.gson.JsonElement
import com.lzy.okgo.OkGo
import com.whl.quickjs.wrapper.JSObject
import com.whl.quickjs.wrapper.QuickJSContext
import okhttp3.Call
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.util.Random

object Connect {
	var client: OkHttpClient = OkGoHelper.defaultClient

	fun to(url: String, req: Req): Call {
		return client.newCall(getRequest(url, req, req.header.toHeaders()))
	}

	fun success(ctx: QuickJSContext, req: Req, res: Response): JSObject {
		try {
			val jsObject = ctx.createNewJSObject()
			val jsHeader = ctx.createNewJSObject()
			setHeader(ctx, res, jsHeader)
			jsObject.setProperty("headers", jsHeader)
			if (req.buffer == 0) jsObject.setProperty("content", String(res.body.bytes(), charset(req.charset)))
			if (req.buffer == 1) {
				val array = ctx.createNewJSArray()
				val bytes = res.body.bytes()
				for (i in bytes.indices) {
					array.set(bytes[i].toInt(), i)
				}
				jsObject.setProperty("content", array)
			}
			if (req.buffer == 2) jsObject.setProperty("content", Base64.encodeToString(res.body.bytes(), Base64.DEFAULT or Base64.NO_WRAP))
			return jsObject
		} catch (e: Exception) {
			return error(ctx)
		}
	}

	fun error(ctx: QuickJSContext): JSObject {
		val jsObject = ctx.createNewJSObject()
		val jsHeader = ctx.createNewJSObject()
		jsObject.setProperty("headers", jsHeader)
		jsObject.setProperty("content", "")
		return jsObject
	}

	private fun getRequest(url: String, req: Req, headers: Headers): Request {
		return if (req.method.equals("post", ignoreCase = true)) {
			Request.Builder().url(url).tag("js_okhttp_tag").headers(headers).post(getPostBody(req, headers[HttpHeaders.CONTENT_TYPE])).build()
		} else if (req.method.equals("header", ignoreCase = true)) {
			Request.Builder().url(url).tag("js_okhttp_tag").headers(headers).head().build()
		} else {
			Request.Builder().url(url).tag("js_okhttp_tag").headers(headers).get().build()
		}
	}

	private fun getPostBody(req: Req, contentType: String?): RequestBody {
		val data = req.data
		if (data != null && req.postType == "json") return getJsonBody(data)
		if (data != null && req.postType == "form") return getFormBody(data)
		if (data != null && req.postType == "form-data") return getFormDataBody(data)
		if (req.body != null && contentType != null) return req.body.toRequestBody(contentType.toMediaType())
		return "".toRequestBody(null)
	}

	private fun getJsonBody(data: JsonElement): RequestBody {
		return data.toString().toRequestBody("application/json".toMediaType())
	}

	private fun getFormBody(data: JsonElement): RequestBody {
		val formBody = FormBody.Builder()
		val params = Json.toMap(data)
		for ((key, value) in params) formBody.add(key, value)
		return formBody.build()
	}

	private fun getFormDataBody(data: JsonElement): RequestBody {
		val boundary = "--dio-boundary-" + Random().nextInt(42949) + Random().nextInt(67296)
		val builder = MultipartBody.Builder(boundary).setType(MultipartBody.FORM)
		val params = Json.toMap(data)
		for ((key, value) in params) builder.addFormDataPart(key, value)
		return builder.build()
	}

	private fun setHeader(ctx: QuickJSContext, res: Response, `object`: JSObject) {
		for (entry in res.headers.toMultimap().entries) {
			if (entry.value.size == 1) `object`.setProperty(entry.key, entry.value[0])
			if (entry.value.size >= 2) `object`.setProperty(entry.key, JSUtils<String>().toArray(ctx, entry.value))
		}
	}

	fun cancelByTag(tag: Any) {
		try {
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
			OkGo.getInstance().cancelTag(tag)
			OkHttpUtil.cancel(tag)
		} catch (ignored: Exception) {
		}
	}
}
