package com.github.tvbox.osc.util.js

import android.text.TextUtils
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import java.io.ByteArrayInputStream

class Res {
	@SerializedName("code")
	val code: Int? = null
		get() = field ?: 200

	@SerializedName("buffer")
	val buffer: Int? = null
		get() = field ?: 0

	@SerializedName("content")
	val content: String? = null
		get() = if (TextUtils.isEmpty(field)) "" else field

	@SerializedName("headers")
	private val headers: JsonElement? = null

	val header: MutableMap<String, String>
		get() {
			val h = this.headers ?: return HashMap()
			return Json.toMap(h)
		}

	val contentType: String
		get() {
			val header = this.header
			for (key in listOf("Content-Type", "content-type")) {
				val value = header[key]
				if (value != null) return value
			}
			return "application/octet-stream"
		}

	val stream: ByteArrayInputStream
		get() {
			if (this.buffer == 2) return ByteArrayInputStream(Base64.decode(this.content, Base64.DEFAULT))
			return ByteArrayInputStream((this.content.orEmpty()).toByteArray())
		}

	companion object {
		fun objectFrom(json: String?): Res? {
			return Gson().fromJson(json, Res::class.java)
		}
	}
}
