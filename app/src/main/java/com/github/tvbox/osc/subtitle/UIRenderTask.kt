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

import com.github.tvbox.osc.subtitle.SubtitleEngine.OnSubtitleChangeListener
import com.github.tvbox.osc.subtitle.model.Subtitle
import com.github.tvbox.osc.subtitle.runtime.AppTaskExecutor.Companion.mainThread

/**
 * @author AveryZhong.
 */
class UIRenderTask(private val mOnSubtitleChangeListener: OnSubtitleChangeListener) : Runnable {
	private var mSubtitle: Subtitle? = null

	override fun run() {
		mOnSubtitleChangeListener.onSubtitleChanged(mSubtitle)
	}

	fun execute(subtitle: Subtitle?) {
		mSubtitle = subtitle
		mainThread().execute(this)
	}
}
