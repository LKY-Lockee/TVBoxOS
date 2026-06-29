package com.github.tvbox.osc.subtitle

import com.github.tvbox.osc.subtitle.model.TimedTextObject

class SubtitleLoadSuccessResult {
	var fileName: String = ""
	var content: String = ""
	var timedTextObject: TimedTextObject? = null
	var subtitlePath: String = ""
}
