package com.github.tvbox.osc.util

import android.app.Activity
import android.os.Process
import java.util.Stack
import kotlin.system.exitProcess

/**
 * @author pj567
 * @date 2020/12/23
 */
class AppManager private constructor() {
	/**
	 * 添加Activity到堆栈
	 */
	fun addActivity(activity: Activity) {
		activityStack.add(activity)
	}

	/**
	 * 是否有activity
	 */
	val isActivity: Boolean
		get() = activityStack.isNotEmpty()

	/**
	 * 获取当前Activity（堆栈中最后一个压入的）
	 */
	fun currentActivity(): Activity? {
		return if (activityStack.isEmpty()) null else activityStack.lastElement()
	}

	/**
	 * 结束当前Activity（堆栈中最后一个压入的）
	 */
	fun finishActivity() {
		if (activityStack.isEmpty()) return
		val activity = activityStack.lastElement()
		if (!activity.isFinishing) {
			activity.finish()
		}
	}

	fun finishActivity(activity: Activity) {
		activityStack.remove(activity)
	}

	/**
	 * 结束指定类名的Activity
	 */
	fun finishActivity(cls: Class<*>) {
		for (activity in activityStack) {
			if (activity.javaClass == cls) {
				if (!activity.isFinishing) {
					activity.finish()
				}
				break
			}
		}
	}

	fun backActivity(cls: Class<*>) {
		while (activityStack.isNotEmpty()) {
			val activity = activityStack.pop()
			if (activity.javaClass == cls) {
				activityStack.push(activity)
				break
			} else {
				activity.finish()
			}
		}
	}

	/**
	 * 结束所有Activity
	 */
	fun finishAllActivity() {
		for (activity in activityStack) {
			if (!activity.isFinishing) {
				activity.finish()
			}
		}
		activityStack.clear()
	}

	/**
	 * 获取指定的Activity
	 */
	fun getActivity(cls: Class<*>): Activity? {
		for (activity in activityStack) {
			if (activity.javaClass == cls) {
				return activity
			}
		}
		return null
	}

	fun appExit(code: Int) {
		try {
			finishAllActivity()
			Process.killProcess(Process.myPid())
			exitProcess(code)
		} catch (e: Exception) {
			activityStack.clear()
			e.printStackTrace()
		}
	}

	companion object {
		private val activityStack = Stack<Activity>()

		val instance: AppManager by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
			AppManager()
		}
	}
}
