package com.github.tvbox.osc.bean

import java.util.Date

data class LiveEpgDate(
	var index: Int = 0,
	var datePresented: String? = null,
	var dateParamVal: Date? = null
)
