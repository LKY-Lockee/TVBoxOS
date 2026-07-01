package com.github.tvbox.osc.bean

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class EpgInfo(
	epgDate: Date,
	val title: String,
	val date: Date,
	val originStart: String,
	val originEnd: String,
	val index: Int
) {
	val startDateTime: Date
	val endDateTime: Date
	val dateStart: Int
	val dateEnd: Int
	val start: String
	val end: String
	val currentEpgDate: String

	init {
		val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
			timeZone = TimeZone.getTimeZone("GMT+8:00")
		}
		currentEpgDate = dateFormat.format(epgDate)

		val userSdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault()).apply {
			timeZone = TimeZone.getDefault()
		}
		val dateStr = dateFormat.format(date)
		startDateTime = userSdf.parse("$dateStr $originStart:00 GMT+8:00") ?: date
		endDateTime = userSdf.parse("$dateStr $originEnd:00 GMT+8:00") ?: date

		val zoneFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
		start = zoneFormat.format(startDateTime)
		end = zoneFormat.format(endDateTime)
		dateStart = start.replace(":", "").toInt()
		dateEnd = end.replace(":", "").toInt()
	}
}
