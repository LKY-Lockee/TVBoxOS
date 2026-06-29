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

import java.util.concurrent.Executor

/**
 * @author AveryZhong.
 */
class AppTaskExecutor private constructor() : TaskExecutor() {
	private val mDefaultTaskExecutor = DefaultTaskExecutor()
	private var mDelegate: TaskExecutor = mDefaultTaskExecutor

	override val isMainThread: Boolean
		get() = mDelegate.isMainThread

	fun setDelegate(taskExecutor: TaskExecutor?) {
		mDelegate = taskExecutor ?: mDefaultTaskExecutor
	}

	override fun executeOnDeskIO(task: Runnable) {
		mDelegate.executeOnDeskIO(task)
	}

	override fun executeOnMainThread(task: Runnable) {
		mDelegate.executeOnMainThread(task)
	}

	override fun postToMainThread(task: Runnable) {
		mDelegate.postToMainThread(task)
	}

	companion object {
		private val sDeskIO = Executor { command -> instance.executeOnDeskIO(command) }
		private val sMainThread = Executor { command -> instance.executeOnMainThread(command) }

		val instance: AppTaskExecutor by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
			AppTaskExecutor()
		}

		fun deskIO(): Executor {
			return sDeskIO
		}

		fun mainThread(): Executor {
			return sMainThread
		}
	}
}
