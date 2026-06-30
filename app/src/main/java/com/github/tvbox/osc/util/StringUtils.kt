package com.github.tvbox.osc.util

object StringUtils {
	private val U2028 = String(byteArrayOf(0xE2.toByte(), 0x80.toByte(), 0xA8.toByte()))
	private val U2029 = String(byteArrayOf(0xE2.toByte(), 0x80.toByte(), 0xA9.toByte()))

	fun isEmpty(str: CharSequence?): Boolean {
		return str.isNullOrEmpty()
	}

	fun isNotEmpty(str: CharSequence?): Boolean {
		return !str.isNullOrEmpty()
	}

	fun isNull(obj: Any?): Boolean {
		return obj == null
	}

	fun isNotNull(obj: Any?): Boolean {
		return obj != null
	}

	fun isEmpty(obj: Any?): Boolean {
		return when (obj) {
			null -> true
			is CharSequence -> obj.isEmpty()
			is Collection<*> -> obj.isEmpty()
			is Map<*, *> -> obj.isEmpty()
			else -> if (obj.javaClass.isArray) java.lang.reflect.Array.getLength(obj) == 0 else false
		}
	}

	fun isNotEmpty(obj: Any?): Boolean {
		return !isEmpty(obj)
	}

	/**
	 * Escape JavaString string
	 * 
	 * @param line unescaped string
	 * @return escaped string
	 */
	fun escapeJavaScriptString(line: String): String {
		val sb = StringBuilder()
		for (i in line.indices) {
			when (val c = line[i]) {
				'"', '\'', '\\' -> {
					sb.append('\\')
					sb.append(c)
				}

				'\n' -> sb.append("\\n")
				'\r' -> sb.append("\\r")
				else -> sb.append(c)
			}
		}

		return sb.toString()
			.replace(U2028, "\u2028")
			.replace(U2029, "\u2029")
	}

	fun getBaseUrl(url: String?): String? {
		if (url.isNullOrEmpty()) {
			return url
		}
		val baseUrls = url.replace("http://", "").replace("https://", "")
		val baseUrl2 = baseUrls.split("/")[0]
		return if (url.startsWith("https")) {
			"https://$baseUrl2"
		} else {
			"http://$baseUrl2"
		}
	}

	fun arrayToString(list: Array<String>?, fromIndex: Int, cha: String?): String {
		return arrayToString(list, fromIndex, list?.size ?: 0, cha)
	}

	fun arrayToString(list: Array<String>?, fromIndex: Int, endIndex: Int, cha: String?): String {
		if (list == null || list.size <= fromIndex) {
			return ""
		}
		if (list.size <= 1) {
			return list[0]
		}
		val builder = StringBuilder()
		builder.append(list[fromIndex])
		var i = 1 + fromIndex
		while (i < list.size && i < endIndex) {
			builder.append(cha).append(list[i])
			i++
		}
		return builder.toString()
	}

	fun listToString(list: List<String>?, cha: String = "&&"): String {
		if (list.isNullOrEmpty()) {
			return ""
		}
		if (list.size == 1) {
			return list[0]
		}
		return list.joinToString(cha)
	}

	fun listToString(list: List<String>?, fromIndex: Int, cha: String?): String {
		if (list == null || list.size <= fromIndex) {
			return ""
		}
		if (list.size <= 1) {
			return list[0]
		}
		val builder = StringBuilder()
		builder.append(list[fromIndex])
		for (i in fromIndex + 1..<list.size) {
			builder.append(cha).append(list[i])
		}
		return builder.toString()
	}

	fun isBlank(text: String?): Boolean {
		return trim(text).isEmpty()
	}

	fun trimBlanks(str: String?): String? {
		if (str.isNullOrEmpty()) {
			return str
		}
		var len = str.length
		var st = 0

		while ((st < len) && (str[st] == '\n' || str[st] == '\r' || str[st] == '\u000c' || str[st] == '\t')) {
			st++
		}
		while ((st < len) && (str[len - 1] == '\n' || str[len - 1] == '\r' || str[len - 1] == '\u000c' || str[len - 1] == '\t')) {
			len--
		}
		return if ((st > 0) || (len < str.length)) str.substring(st, len) else str
	}

	fun trim(string: String?): String {
		if (string.isNullOrEmpty() || " " == string) return ""
		var start = 0
		val len = string.length
		var end = len - 1
		while ((start < end) && ((string[start] <= ' ') || (string[start] == '　'))) {
			++start
		}
		while ((start < end) && ((string[end] <= ' ') || (string[end] == '　'))) {
			--end
		}
		++end
		return if ((start > 0) || (end < len)) string.substring(start, end) else string
	}

	fun isJsonType(text: String?): Boolean {
		var content = text
		var result = false
		if (isNotEmpty(content)) {
			content = trim(content)
			if (content.startsWith("{") && content.endsWith("}")) {
				result = true
			} else if (content.startsWith("[") && content.endsWith("]")) {
				result = true
			}
		}
		return result
	}

	fun isJsonObject(text: String?): Boolean {
		var content = text
		var result = false
		if (isNotEmpty(content)) {
			content = trim(content)
			if (content.startsWith("{") && content.endsWith("}")) {
				result = true
			}
		}
		return result
	}

	fun isJsonArray(text: String?): Boolean {
		var content = text
		var result = false
		if (isNotEmpty(content)) {
			content = trim(content)
			if (content.startsWith("[") && content.endsWith("]")) {
				result = true
			}
		}
		return result
	}
}
