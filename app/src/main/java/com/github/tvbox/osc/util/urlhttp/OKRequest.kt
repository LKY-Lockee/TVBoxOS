package com.github.tvbox.osc.util.urlhttp

import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException

internal class OKRequest private constructor(
	private val mMethodType: String,
	private var mUrl: String,
	private val mJsonStr: String?,
	private val mParamsMap: Map<String, String>?,
	private val mHeaderMap: Map<String, String>?,
	private val mCallBack: OKCallBack<*>?
) {
	private var mTag: Any? = null

	private val mOkHttpRequest: Request by lazy {
		val requestBuilder = Request.Builder()
		when (mMethodType) {
			OkHttpUtil.METHOD_GET -> setGetParams()
			OkHttpUtil.METHOD_POST -> requestBuilder.post(requestBody)
		}
		requestBuilder.url(mUrl)
		if (mTag != null) requestBuilder.tag(mTag)
		mHeaderMap?.let { setHeader(requestBuilder) }
		requestBuilder.build()
	}

	private val requestBody: RequestBody
		get() {
			mJsonStr?.let { json ->
				if (json.isNotEmpty()) {
					val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
					return json.toRequestBody(mediaType)
				}
			}
			val formBody = FormBody.Builder()
			mParamsMap?.let { params ->
				for ((key, value) in params) {
					formBody.add(key, value)
				}
			}
			return formBody.build()
		}

	constructor(methodType: String, url: String, paramsMap: Map<String, String>?, headerMap: Map<String, String>?, callBack: OKCallBack<*>?) : this(methodType, url, null, paramsMap, headerMap, callBack)

	constructor(methodType: String, url: String, jsonStr: String?, headerMap: Map<String, String>?, callBack: OKCallBack<*>?) : this(methodType, url, jsonStr, null, headerMap, callBack)

	fun setTag(tag: Any?) {
		mTag = tag
	}

	private fun setGetParams() {
		mParamsMap?.let { params ->
			mUrl = "$mUrl?"
			for ((key, value) in params) {
				mUrl = "$mUrl$key=$value&"
			}
			mUrl = mUrl.substring(0, mUrl.length - 1)
		}
	}

	private fun setHeader(requestBuilder: Request.Builder) {
		mHeaderMap?.let { headers ->
			for ((key, value) in headers) {
				requestBuilder.addHeader(key, value)
			}
		}
	}

	fun execute(client: OkHttpClient) {
		val call = client.newCall(mOkHttpRequest)
		try {
			val response = call.execute()
			mCallBack?.onSuccess(call, response)
		} catch (e: IOException) {
			mCallBack?.onError(call, e)
		}
	}

	fun call(client: OkHttpClient) {
		client.newCall(mOkHttpRequest).enqueue(object : Callback {
			override fun onFailure(call: Call, e: okio.IOException) {
				mCallBack?.onError(call, e)
			}

			override fun onResponse(call: Call, response: Response) {
				mCallBack?.onSuccess(call, response)
			}
		})
	}
}
