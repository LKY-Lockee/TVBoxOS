package com.github.tvbox.osc.util.js

import android.text.TextUtils
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

class Req {
	@SerializedName("buffer")
	val buffer: Int? = null
		get() = field ?: 0

	@SerializedName("redirect")
	val redirect: Int? = null
		get() = field ?: 1

	@SerializedName("timeout")
	val timeout: Int? = null
		get() = field ?: 10000

	@SerializedName("postType")
	val postType: String? = null
		get() = if (TextUtils.isEmpty(field)) "json" else field

	@SerializedName("method")
	val method: String? = null
		get() = if (TextUtils.isEmpty(field)) "get" else field

	@SerializedName("body")
	val body: String? = null

	@SerializedName("data")
	val data: JsonElement? = null

	@SerializedName("headers")
	private val headers: JsonElement? = null

	val isRedirect: Boolean
		get() {
			return this.redirect == 1
		}

	val header: MutableMap<String, String>
		get() = Json.toMap(this.headers ?: return HashMap())

	val charset: String
		get() {
			val header = this.header
			for (key in listOf("Content-Type", "content-type")) {
				val value = header[key]
				if (value != null) return getCharset(value)
			}
			return "UTF-8"
		}

	private fun getCharset(value: String): String {
		for (text in value.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) if (text.contains("charset=")) return text.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
		return "UTF-8"
	}

	companion object {
		fun objectFrom(json: String?): Req? {
			return Gson().fromJson(json, Req::class.java)
		}
	}
}
