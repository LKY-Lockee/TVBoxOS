package com.github.tvbox.osc.bean

import java.util.Objects

/**
 * @author pj567
 * @date 2021/1/12
 */
class LiveChannelItem {
	var sourceIndex: Int = 0
	var includeBack: Boolean = false

	/**
	 * 频道索引号
	 */
	var channelIndex: Int = 0

	/**
	 * 频道名称
	 */
	var channelName: String? = null
	var channelNum: Int = 0

	/**
	 * 频道源名称
	 */
	var channelSourceNames: List<String>? = null

	/**
	 * 频道源地址
	 */
	var channelUrls: List<String>? = null
		set(value) {
			field = value
			sourceNum = value?.size ?: 0
		}

	/**
	 * 频道源总数
	 */
	var sourceNum: Int = 0
		private set

	val url: String?
		get() = channelUrls?.getOrNull(sourceIndex)

	val sourceName: String?
		get() = channelSourceNames?.getOrNull(sourceIndex)

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other == null || this::class.java != other::class.java) return false
		val that = other as LiveChannelItem
		return channelName == that.channelName && url == that.url
	}

	override fun hashCode(): Int {
		return Objects.hash(channelName, url)
	}

	fun preSource() {
		sourceIndex--
		if (sourceIndex < 0) sourceIndex = sourceNum - 1
	}

	fun nextSource() {
		sourceIndex++
		if (sourceIndex == sourceNum) sourceIndex = 0
	}
}
