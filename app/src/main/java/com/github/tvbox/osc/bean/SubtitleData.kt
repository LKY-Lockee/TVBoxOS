package com.github.tvbox.osc.bean

data class SubtitleData(
	var isNew: Boolean = false,
	var subtitleList: List<Subtitle>? = null,
	var isZip: Boolean = false
)
