package com.github.tvbox.osc.bean

import com.github.tvbox.osc.util.RegexUtils
import java.io.Serializable
import kotlin.math.min

/**
 * @author pj567
 * @date 2020/12/22
 */
class VodInfo : Serializable {
	/**
	 * 时间
	 */
	var last: String? = null

	/**
	 * 内容id
	 */
	var id: String = ""

	/**
	 * 父级id
	 */
	var tid: Int = 0

	/**
	 * 影片名称
	 */
	var name: String? = null // <![CDATA[老爸当家]]>

	/**
	 * 类型名称
	 */
	var type: String? = null

	/**
	 * 视频分类
	 */
	var dt: String? = null // zuidam3u8,zuidall

	/**
	 * 图片
	 */
	var pic: String? = null

	/**
	 * 语言
	 */
	var lang: String? = null

	/**
	 * 地区
	 */
	var area: String? = null

	/**
	 * 年份
	 */
	var year: Int = 0
	var state: String? = null

	/**
	 * 描述集数或者影片信息
	 */
	var note: String? = null // <![CDATA[共40集]]>

	/**
	 * 演员
	 */
	var actor: String? = null // <![CDATA[张国立,蒋欣,高鑫,曹艳艳,王维维,韩丹彤,孟秀,王新]]>

	/**
	 * 导演
	 */
	var director: String? = null // <![CDATA[陈国星]]>
	var seriesFlags: List<VodSeriesFlag>? = null
	var seriesMap: LinkedHashMap<String, MutableList<VodSeries>>? = null
	var des: String = "" // <![CDATA[权来]
	var playFlag: String = ""
	var playIndex: Int = 0
	var playNote: String = ""
	var sourceKey: String = ""
	var playerCfg: String = ""
	var reverseSort: Boolean = false

	fun setVideo(video: Movie.Video) {
		last = video.last
		id = video.id
		tid = video.tid
		name = video.name
		type = video.type
		pic = video.pic
		lang = video.lang
		area = video.area
		year = video.year
		state = video.state
		note = video.note
		actor = video.actor
		director = video.director
		des = video.des

		val infoList = video.urlBean?.infoList ?: return
		if (infoList.isEmpty()) return

		val tempSeriesMap = LinkedHashMap<String, MutableList<VodSeries>>()
		val flags = mutableListOf<VodSeriesFlag>()

		for (urlInfo in infoList) {
			val beanList = urlInfo.beanList ?: continue
			if (beanList.isEmpty()) continue

			val seriesList = beanList.map { infoBean ->
				VodSeries(infoBean.name, infoBean.url)
			}.toMutableList()

			urlInfo.flag?.let { flag ->
				tempSeriesMap[flag] = seriesList
				flags.add(VodSeriesFlag(flag))
			}
		}

		seriesFlags = flags
		seriesMap = LinkedHashMap()
		for (flag in flags) {
			val flagName = flag.name ?: continue
			val list = tempSeriesMap[flagName] ?: continue
			if (flags.size <= 5) {
				if (isReverse(list)) list.reverse()
			}
			seriesMap?.put(flagName, list)
		}
	}

	private fun extractNumber(name: String): Int {
		val matcher = RegexUtils.getPattern("\\d+").matcher(name)
		return if (matcher.find()) {
			matcher.group().toInt()
		} else {
			0
		}
	}

	private fun isReverse(list: List<VodSeries>): Boolean {
		var ascCount = 0
		var descCount = 0
		// 比较最多前 6 个相邻元素对
		val limit = min(list.size - 1, 6)
		for (i in 0..<limit) {
			val currentName = list[i].name
			val nextName = list[i + 1].name
			val current = extractNumber(currentName)
			val next = extractNumber(nextName)
			if (current < next) {
				ascCount++
				if (ascCount == 2) return false
			} else if (current > next) {
				descCount++
				if (descCount == 2) return true
			}
		}
		return false
	}

	fun reverse() {
		seriesMap?.values?.forEach { it.reverse() }
	}

	class VodSeriesFlag : Serializable {
		var name: String = ""
		var selected: Boolean = false

		constructor()

		constructor(name: String) {
			this.name = name
		}
	}

	data class VodSeries(var name: String, var url: String) : Serializable {
		var selected: Boolean = false
	}
}
