package com.github.tvbox.osc.util

import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.UriUtil
import java.math.BigDecimal
import java.util.regex.Pattern

/**
 * @author asdfgh、FongMi
 * 参考 FongMi/TV 的代码
 * https://github.com/FongMi/TV
 */
object M3U8 {
	private const val TAG_DISCONTINUITY = "#EXT-X-DISCONTINUITY"
	private const val TAG_MEDIA_DURATION = "#EXTINF"
	private const val TAG_END_LIST = "#EXT-X-ENDLIST"
	private const val TAG_KEY = "#EXT-X-KEY"

	private val REGEX_X_DISCONTINUITY: Pattern = Pattern.compile("#EXT-X-DISCONTINUITY[\\s\\S]*?(?=#EXT-X-DISCONTINUITY|$)")
	private val REGEX_MEDIA_DURATION: Pattern = Pattern.compile("$TAG_MEDIA_DURATION:([\\d.]+)\\b")
	private val REGEX_URI: Pattern = Pattern.compile("URI=\"(.+?)\"")
	var currentAdCount: Int = 0

	fun isAd(regex: String): Boolean {
		return regex.contains(TAG_DISCONTINUITY) || regex.contains(TAG_MEDIA_DURATION) || regex.contains(TAG_END_LIST) || regex.contains(TAG_KEY) || isDouble(regex)
	}

	@UnstableApi
	fun purify(tsUrlPre: String, m3u8content: String?): String? {
		val start = System.currentTimeMillis()
		currentAdCount = 0
		if (m3u8content.isNullOrEmpty()) return null
		if (!m3u8content.startsWith("#EXTM3U")) return null
		var result = removeMinorityUrl(tsUrlPre, m3u8content)
		if (result != null && currentAdCount > 0) return result
		result = get(tsUrlPre, m3u8content)
		val cost = System.currentTimeMillis() - start
		TVBoxRuntimeLog.i("echo-fixAdM3u8Ai 耗时：" + cost + "ms")
		return result
	}

	private fun maxPercent(preUrlMap: HashMap<String, Int>): Double {
		var maxTimes = 0
		var totalTimes = 0
		for (entry in preUrlMap.entries) {
			if (entry.value > maxTimes) {
				maxTimes = entry.value
			}
			totalTimes += entry.value
		}
		return maxTimes * 1.0 / (totalTimes * 1.0)
	}

	private fun removeMinorityUrl(tsUrlPre: String, m3u8content: String): String? {
		var lineSplit = "\n"
		if (m3u8content.contains("\r\n")) lineSplit = "\r\n"
		val lines = m3u8content.split(lineSplit).filter { it.isNotEmpty() }.toTypedArray()

		// 第一阶段：按去掉文件后缀后统计各前缀出现次数
		val preUrlMap = HashMap<String, Int>()
		for (line in lines) {
			if (line.isEmpty() || line[0] == '#') {
				continue
			}
			val iLast = line.lastIndexOf('.')
			if (iLast <= 4) {
				continue
			}
			val preUrl = line.substring(0, iLast - 4)
			preUrlMap.merge(preUrl, 1, Integer::sum)
		}
		if (preUrlMap.size <= 1) return null
		var domainFiltering = false
		if (maxPercent(preUrlMap) < 0.8) {
			// 尝试判断域名，取同域名最多的链接，其它域名当作广告去除
			preUrlMap.clear()
			for (line in lines) {
				if (line.isEmpty() || line[0] == '#') {
					continue
				}
				if (!line.startsWith("http://") && !line.startsWith("https://")) {
					return null
				}
				val iFirst = line.indexOf('/', 9) // skip http:// 或 https://
				if (iFirst <= 0) {
					continue
				}
				val preUrl = line.substring(0, iFirst)
				preUrlMap.merge(preUrl, 1, Integer::sum)
			}
			if (preUrlMap.size <= 1) return null
			if (maxPercent(preUrlMap) < 0.8) {
				return null // 视频非广告片断占比不够大
			}
			var allDomainsExceedThreshold = true
			for (count in preUrlMap.values) {
				if (count <= 15) {
					allDomainsExceedThreshold = false
					break
				}
			}
			if (allDomainsExceedThreshold) return null
			domainFiltering = true
		}

		// 找出出现次数最多的 key（文件前缀或域名均适用）
		var maxTimes = 0
		var maxTimesPreUrl = ""
		for (entry in preUrlMap.entries) {
			if (entry.value > maxTimes) {
				maxTimesPreUrl = entry.key
				maxTimes = entry.value
			}
		}
		if (maxTimes == 0) return null

		var dealtExtXKey = false
		for (i in lines.indices) {
			// 处理解密KEY的绝对路径拼接
			if (!dealtExtXKey && lines[i].startsWith("#EXT-X-KEY")) {
				var keyUrl = ""
				var start = lines[i].indexOf("URI=\"")
				if (start != -1) {
					start += "URI=\"".length
					val end = lines[i].indexOf("\"", start)
					if (end != -1) {
						keyUrl = lines[i].substring(start, end)
					}
					if (!keyUrl.startsWith("http://") && !keyUrl.startsWith("https://")) {
						val newKeyUrl: String
						if (keyUrl.isNotEmpty() && keyUrl[0] == '/') {
							val iFirst = tsUrlPre.indexOf('/', 9) // skip https://, http://
							newKeyUrl = tsUrlPre.substring(0, iFirst) + keyUrl
						} else newKeyUrl = tsUrlPre + keyUrl
						lines[i] = lines[i].replace("URI=\"$keyUrl\"", "URI=\"$newKeyUrl\"")
					}
					dealtExtXKey = true
				}
			}
			if (lines[i].isEmpty() || lines[i][0] == '#') {
				continue
			}
			// 根据判断方式过滤
			if (!domainFiltering) {
				if (lines[i].startsWith(maxTimesPreUrl)) {
					if (!lines[i].startsWith("http://") && !lines[i].startsWith("https://")) {
						if (lines[i][0] == '/') {
							val iFirst = tsUrlPre.indexOf('/', 9) // skip https://, http://
							lines[i] = tsUrlPre.substring(0, iFirst) + lines[i]
						} else lines[i] = tsUrlPre + lines[i]
					}
				} else {
					if (i > 0 && lines[i - 1].isNotEmpty() && lines[i - 1][0] == '#') {
						lines[i - 1] = ""
					}
					lines[i] = ""
					currentAdCount += 1
				}
			} else {
				// 域名过滤模式：先转换为绝对 URL
				var absoluteUrl = lines[i]
				if (!absoluteUrl.startsWith("http://") && !absoluteUrl.startsWith("https://")) {
					if (absoluteUrl[0] == '/') {
						val iFirst = tsUrlPre.indexOf('/', 9)
						absoluteUrl = tsUrlPre.substring(0, iFirst) + absoluteUrl
					} else {
						absoluteUrl = tsUrlPre + absoluteUrl
					}
				}
				// 提取域名部分（http://xxx或https://xxx）
				val iFirst = absoluteUrl.indexOf('/', 9)
				val domain = if (iFirst > 0) absoluteUrl.substring(0, iFirst) else absoluteUrl
				// 保留条件：域名等于出现次数最多的，或者该域名出现次数超过timesNoAd次
				val cnt = preUrlMap[domain]

				/**
				 * @author asdfgh
				 * [asdfgh](https://github.com/asdfgh)
				 */
				// 出现超过多少次的域名不认为是广告
				val timesNoAd = 15
				if (domain == maxTimesPreUrl || (cnt != null && cnt > timesNoAd)) {
					lines[i] = absoluteUrl
				} else {
					if (i > 0 && lines[i - 1].isNotEmpty() && lines[i - 1][0] == '#') {
						lines[i - 1] = ""
					}
					lines[i] = ""
					currentAdCount += 1
				}
			}
		}
		return java.lang.String.join(lineSplit, *lines)
	}

	@UnstableApi
	private fun get(tsUrlPre: String, m3u8Content: String): String? {
		val content = m3u8Content.replace("\r\n", "\n")
		val sb = StringBuilder()
		for (line in content.split("\n").filter { it.isNotEmpty() }) {
			sb.append(if (shouldResolve(line)) resolve(tsUrlPre, line) else line).append("\n")
		}
		val ads = getRegex(tsUrlPre)
		if (ads.isNullOrEmpty()) return null
		return clean(sb.toString(), ads)
	}

	private fun getRegex(tsUrlPre: String): List<String>? {
		val hostsRegex = VideoParseRuler.hostsRegex
		var list: List<String>? = ArrayList()
		for (host in hostsRegex.keys) {
			if (!tsUrlPre.contains(host)) continue
			if (hostsRegex[host] == null) continue
			list = hostsRegex[host]
			break
		}
		return list
	}

	private fun clean(line: String, ads: List<String>): String {
		var result = line
		var scan = false
		for (ad in ads) {
			if (ad.contains(TAG_DISCONTINUITY) || ad.contains(TAG_MEDIA_DURATION))
				result = scanAd(result, ad)
			else if (isDouble(ad)) scan = true
		}
		return if (scan) scan(result, ads) else result
	}

	private fun scanAd(line: String, tagAd: String): String {
		var result = line
		val m1 = RegexUtils.getPattern(tagAd).matcher(result)
		val needRemoveAd: MutableList<String> = ArrayList()
		while (m1.find()) {
			val group = m1.group()
			val groupCleaned = group.replace(TAG_END_LIST, "")
			val m2 = REGEX_MEDIA_DURATION.matcher(group)
			var tCount = 0
			while (m2.find()) {
				tCount += 1
			}
			needRemoveAd.add(groupCleaned)
			currentAdCount += tCount
		}
		for (rem in needRemoveAd) {
			result = result.replace(rem, "")
		}
		return result
	}

	private fun scan(line: String, ads: List<String>): String {
		var result = line
		val m1 = REGEX_X_DISCONTINUITY.matcher(result)
		val needRemoveAd: MutableList<String> = ArrayList()
		while (m1.find()) {
			val group = m1.group()
			val groupCleaned = group.replace(TAG_END_LIST, "")
			val m2 = REGEX_MEDIA_DURATION.matcher(group)
			var ft = BigDecimal.ZERO
			var lt = BigDecimal.ZERO
			var t = BigDecimal.ZERO
			var tCount = 0
			while (m2.find()) {
				if (ft == BigDecimal.ZERO) ft = BigDecimal(m2.group(1))
				lt = BigDecimal(m2.group(1))
				t = t.add(lt)
				tCount += 1
			}

			val ftStr = ft.toString()
			val ltStr = lt.toString()
			val tStr = t.toString()
			for (ad in ads) {
				if (ad.startsWith("-")) {
					val adClean = ad.substring(1)
					// 匹配最后一条切片
					if (ltStr.startsWith(adClean)) {
						needRemoveAd.add(groupCleaned)
						currentAdCount += tCount
						break
					}
				} else {
					// 匹配第一条切片或广告切片总时长
					if (ftStr.startsWith(ad) || tStr.startsWith(ad)) {
						needRemoveAd.add(groupCleaned)
						currentAdCount += tCount
						break
					}
				}
			}
		}
		for (rem in needRemoveAd) {
			result = result.replace(rem, "")
		}
		return result
	}

	private fun isDouble(ad: String): Boolean {
		return try {
			ad.toDouble() != 0.0
		} catch (e: Exception) {
			false
		}
	}

	private fun shouldResolve(line: String): Boolean {
		return (!line.startsWith("#") && !line.startsWith("http")) || line.startsWith(TAG_KEY)
	}

	@UnstableApi
	private fun resolve(base: String?, line: String): String {
		if (line.startsWith(TAG_KEY)) {
			val matcher = REGEX_URI.matcher(line)
			val value = if (matcher.find()) matcher.group(1) else null
			return if (value == null) line else line.replace(value, UriUtil.resolve(base, value))
		} else {
			return UriUtil.resolve(base, line)
		}
	}
}
