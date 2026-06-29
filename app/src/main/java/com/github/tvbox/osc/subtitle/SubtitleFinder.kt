/*
 *                       Copyright (C) of Avery
 *
 *                              _ooOoo_
 *                             o8888888o
 *                             88" . "88
 *                             (| -_- |)
 *                             O\  =  /O
 *                          ____/`- -'\____
 *                        .'  \\|     |//  `.
 *                       /  \\|||  :  |||//  \
 *                      /  _||||| -:- |||||-  \
 *                      |   | \\\  -  /// |   |
 *                      | \_|  ''\- -/''  |   |
 *                      \  .-\__  `-`  ___/-. /
 *                    ___`. .' /- -.- -\  `. . __
 *                 ."" '<  `.___\_<|>_/___.'  >'"".
 *                | | :  `- \`.;`\ _ /`;.`/ - ` : | |
 *                \  \ `-.   \_ __\ /__ _/   .-` /  /
 *           ======`-.____`-.___\_____/___.-`____.-'======
 *                              `=- -='
 *           ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
 *              Buddha bless, there will never be bug!!!
 */

package com.github.tvbox.osc.subtitle

import com.github.tvbox.osc.subtitle.model.Subtitle

/**
 * @author AveryZhong.
 */
object SubtitleFinder {
	fun find(position: Long, subtitles: List<Subtitle>?): Subtitle? {
		if (subtitles.isNullOrEmpty()) {
			return null
		}
		var start = 0
		var end = subtitles.size - 1
		while (start <= end) {
			val middle = (start + end) / 2
			val middleSubtitle = subtitles[middle]
			val startTime = middleSubtitle.start ?: return null
			val endTime = middleSubtitle.end ?: return null
			when {
				position < startTime.mSeconds -> {
					if (position > endTime.mSeconds) {
						return middleSubtitle
					}
					end = middle - 1
				}

				position > endTime.mSeconds -> {
					if (position < startTime.mSeconds) {
						return middleSubtitle
					}
					start = middle + 1
				}

				position in startTime.mSeconds..endTime.mSeconds -> {
					return middleSubtitle
				}
			}
		}
		return null
	}
}
