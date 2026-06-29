package com.github.tvbox.osc.util.urlhttp

import okhttp3.Call
import okhttp3.Response
import java.io.IOException

abstract class OKCallBack<T> {
	var result: T? = null
		protected set

	open fun onError(call: Call?, e: Exception?) {
		onFailure(call, e)
	}

	fun onSuccess(call: Call?, response: Response?) {
		val obj = onParseResponse(call, response)
		this.result = obj
		onResponse(obj)
	}

	protected abstract fun onParseResponse(call: Call?, response: Response?): T?

	protected abstract fun onFailure(call: Call?, e: Exception?)

	protected abstract fun onResponse(response: T?)

	abstract class OKCallBackDefault : OKCallBack<Response>() {
		public override fun onParseResponse(call: Call?, response: Response?): Response? {
			return response
		}
	}

	abstract class OKCallBackString : OKCallBack<String>() {
		override fun onError(call: Call?, e: Exception?) {
			this.result = ""
			super.onError(call, e)
		}

		public override fun onParseResponse(call: Call?, response: Response?): String {
			return try {
				response?.body?.string().orEmpty()
			} catch (e: IOException) {
				""
			}
		}
	}
}
