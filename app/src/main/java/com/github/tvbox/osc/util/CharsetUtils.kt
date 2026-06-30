package com.github.tvbox.osc.util

import org.mozilla.universalchardet.UniversalDetector
import java.nio.charset.Charset
import java.util.regex.Pattern

/**
 * 字符集工具类，提供了检测字符集的工具方法
 * 首先当然是使用Mozilla的开源工具包UniversalCharset进行字符集检测，对于检测失败的，使用中文常用字进行再次检测
 */
object CharsetUtils {
	/**
	 * 中文常用字符集
	 */
	private val AVAILABLE_CHINESE_CHARSET_NAMES = arrayOf("GBK", "gb2312", "GB18030", "UTF-8", "Big5")

	/**
	 * 中文常用字
	 */
	private val CHINESE_COMMON_CHARACTER_PATTERN: Pattern = Pattern.compile("[的一是了我不人在他有这个上们来到时大地为子中你说生国年着就那和要]")

	fun detect(content: ByteArray): Charset {
		var charset: String? = universalDetect(content)
		if (!charset.isNullOrEmpty()) {
			return Charset.forName(charset)
		}

		var longestMatch = 0
		for (cs in AVAILABLE_CHINESE_CHARSET_NAMES) {
			val temp = String(content, Charset.forName(cs))
			val matcher = CHINESE_COMMON_CHARACTER_PATTERN.matcher(temp)

			var count = 0
			while (matcher.find()) {
				count += 1
			}
			if (count > longestMatch) {
				longestMatch = count
				charset = cs
			}
		}
		return if (charset == null) Charset.forName("GB18030") else Charset.forName(charset)
	}

	/**
	 * 使用Mozilla的开源工具包UniversalCharset进行字符集检测，不一定能完全检测中文字符集
	 */
	fun universalDetect(content: ByteArray): String? {
		val detector = UniversalDetector(null)
		detector.handleData(content, 0, content.size)
		detector.dataEnd()
		return detector.detectedCharset
	}
}
