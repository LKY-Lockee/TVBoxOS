package com.github.tvbox.osc.bean

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class EpgInfo(
	epgDate: Date,
	val title: String?,
	val date: Date,
	originStart: String?,
	originEnd: String?,
	val index: Int
) {
	val startDateTime: Date?
	val endDateTime: Date?
	val dateStart: Int
	val dateEnd: Int
	val start: String
	val end: String
	val currentEpgDate: String

	init {
		val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
			timeZone = TimeZone.getTimeZone("GMT+8:00")
		}
		currentEpgDate = dateFormat.format(epgDate)

		val userSdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US).apply {
			timeZone = TimeZone.getDefault()
		}
		val dateStr = dateFormat.format(date)
		startDateTime = userSdf.parse("$dateStr ${originStart ?: "00:00"}:00 GMT+8:00")
		endDateTime = userSdf.parse("$dateStr ${originEnd ?: "00:00"}:00 GMT+8:00")

		val zoneFormat = SimpleDateFormat("HH:mm", Locale.US)
		start = if (startDateTime != null) zoneFormat.format(startDateTime) else "00:00"
		end = if (endDateTime != null) zoneFormat.format(endDateTime) else "00:00"
		dateStart = start.replace(":", "").toInt()
		dateEnd = end.replace(":", "").toInt()
	}
}
