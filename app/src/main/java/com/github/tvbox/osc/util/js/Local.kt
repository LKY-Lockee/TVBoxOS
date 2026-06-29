package com.github.tvbox.osc.util.js

import androidx.annotation.Keep
import com.orhanobut.hawk.Hawk

class Local {
	@Keep
	@Function
	fun delete(str: String?, str2: String?) {
		try {
			Hawk.delete("jsRuntime_${str}_$str2")
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}

	@Keep
	@Function
	fun get(str: String?, str2: String?): String? {
		try {
			return Hawk.get("jsRuntime_${str}_$str2", "")
		} catch (e: Exception) {
			Hawk.delete(str)
			return str2
		}
	}

	@Keep
	@Function
	fun set(str: String?, str2: String?, str3: String?) {
		try {
			Hawk.put<String?>("jsRuntime_${str}_$str2", str3)
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}
}
