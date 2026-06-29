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

package com.github.tvbox.osc.subtitle.runtime

import android.os.Handler
import android.os.Looper
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * @author AveryZhong.
 */
class DefaultTaskExecutor : TaskExecutor() {
	private val mLock = Any()
	private val mDeskIO: ExecutorService = Executors.newFixedThreadPool(3)
	private var mMainHandler: Handler? = null

	override val isMainThread: Boolean
		get() = Thread.currentThread() === Looper.getMainLooper().thread

	override fun executeOnDeskIO(task: Runnable) {
		mDeskIO.execute(task)
	}

	override fun postToMainThread(task: Runnable) {
		val handler = mMainHandler ?: synchronized(mLock) {
			mMainHandler ?: Handler(Looper.getMainLooper()).also { mMainHandler = it }
		}
		handler.post(task)
	}
}
