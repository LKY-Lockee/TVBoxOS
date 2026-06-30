package com.github.tvbox.osc.util

import java.util.regex.Pattern

object RegexUtils {
	private val patternCache: MutableMap<String, Pattern> = HashMap()

	fun getPattern(regex: String): Pattern {
		return patternCache.getOrPut(regex) { Pattern.compile(regex) }
	}

	fun getPattern(regex: String, flag: Int): Pattern {
		val key = "$regex|$flag"
		return patternCache.getOrPut(key) { Pattern.compile(regex, flag) }
	}
}
