package com.github.tvbox.osc.util.js

import androidx.annotation.Keep
import com.github.tvbox.osc.data.PreferenceStore

class Local {
	@Keep
	@Function
	fun delete(str: String?, str2: String?) {
		try {
			PreferenceStore.delete("jsRuntime_${str}_$str2")
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}

	@Keep
	@Function
	fun get(str: String?, str2: String?): String? {
		try {
			return PreferenceStore.get("jsRuntime_${str}_$str2", "")
		} catch (e: Exception) {
			PreferenceStore.delete(str!!)
			return str2
		}
	}

	@Keep
	@Function
	fun set(str: String?, str2: String?, str3: String?) {
		try {
			PreferenceStore.put("jsRuntime_${str}_$str2", str3)
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}
}
