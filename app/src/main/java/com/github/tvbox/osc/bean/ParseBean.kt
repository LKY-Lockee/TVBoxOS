package com.github.tvbox.osc.bean

import android.util.Base64
import com.github.tvbox.osc.util.DefaultConfig

/**
 * @author pj567
 * @date 2021/3/8
 */
class ParseBean {
	var name: String? = null

	/**
	 * 0 普通嗅探
	 * 
	 * 1 json
	 * 
	 * 2 Json扩展
	 * 
	 * 3 聚合
	 */
	var type: Int = 0
	var isDefault: Boolean = false
	var ext: String? = null

	var url: String? = null
		get() = DefaultConfig.checkReplaceProxy(field.orEmpty())

	fun mixUrl(): String? {
		val currentExt = ext ?: return null
		val currentUrl = url ?: return null

		if (currentExt.isEmpty()) return currentUrl

		val idx = currentUrl.indexOf("?")
		return if (idx > 0) {
			val encodedExt = Base64.encodeToString(
				currentExt.toByteArray(),
				Base64.DEFAULT or Base64.URL_SAFE or Base64.NO_WRAP
			)
			"${currentUrl.substring(0, idx + 1)}cat_ext=$encodedExt&${currentUrl.substring(idx + 1)}"
		} else {
			currentUrl
		}
	}
}
