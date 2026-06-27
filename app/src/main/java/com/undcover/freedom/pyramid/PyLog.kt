package com.undcover.freedom.pyramid

import android.util.Log

/**
 * Created by UndCover on 16/12/15.
 */
class PyLog {
	fun setLogLevel(logLevel: Int): PyLog {
		Companion.logLevel = logLevel
		return instance
	}

	/**
	 * 设置内设Log的过滤，添加需要输出的Log
	 * 
	 * FILTER_LC 生命周期
	 * 
	 * FILTER_NW 网络请求
	 * 
	 * FILTER_AM AtyManager
	 * 
	 * FILTER_FW 框架
	 */
	fun setFilter(filter: Int): PyLog {
		isLifeCycleEnable = (filter and FILTER_LC) / FILTER_LC == 1
		isNetWorkEnable = (filter and FILTER_NW) / FILTER_NW == 1
		isFrameWorkEnable = (filter and FILTER_FW) / FILTER_FW == 1
		isAtyManagerEnable = (filter and FILTER_AM) / FILTER_AM == 1
		return instance
	}

	object TagConstant {
		const val TAG_APP: String = "PythonLoader"
		const val TAG_LC: String = "-----LifeCycle-----"
		const val TAG_AM: String = "-----AtyManager-----"
		const val TAG_NW: String = "-----NetWork-----"
		const val TAG_FW: String = "-----FrameWork-----"
		const val TAG_DEF: String = ""
		const val TAG_REQ: String = "Request\n"
		const val TAG_RSP: String = "Response\n"
	}

	companion object {
		const val LEVEL_V: Int = 5
		const val LEVEL_D: Int = 4
		const val LEVEL_I: Int = 3
		const val LEVEL_W: Int = 2
		const val LEVEL_E: Int = 1
		const val LEVEL_RELEASE: Int = 0

		/**
		 * 用于生命周期
		 */
		const val FILTER_LC: Int = 0x01

		/**
		 * 用于网络请求 默认为 LEVEL_I
		 */
		const val FILTER_NW: Int = 0x02

		/**
		 * ActivityManager内置Log
		 */
		const val FILTER_AM: Int = 0x04

		/**
		 * 用于FrameWork内置log
		 */
		const val FILTER_FW: Int = 0x08

		private var logLevel: Int = LEVEL_RELEASE
		private var isLifeCycleEnable = false
		private var isNetWorkEnable = false
		private var isFrameWorkEnable = false
		private var isAtyManagerEnable = false
		private const val SEGMENTATION_SIZE = 3 * 1024

		val instance: PyLog by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
			PyLog()
		}

		private fun longV(tag: String, msg: String) {
			var remaining = msg
			if (logLevel < LEVEL_V) return
			while (remaining.length > SEGMENTATION_SIZE) {
				val logContent = remaining.substring(0, SEGMENTATION_SIZE)
				remaining = remaining.replace(logContent, "\t\t")
				Log.v(tag, logContent)
			}
			Log.v(tag, remaining)
		}

		private fun longD(tag: String, msg: String) {
			var remaining = msg
			if (logLevel < LEVEL_D) return
			while (remaining.length > SEGMENTATION_SIZE) {
				val logContent = remaining.substring(0, SEGMENTATION_SIZE)
				remaining = remaining.replace(logContent, "\t\t")
				Log.d(tag, logContent)
			}
			Log.d(tag, remaining)
		}

		private fun longI(tag: String, msg: String) {
			var remaining = msg
			if (logLevel < LEVEL_I) return
			while (remaining.length > SEGMENTATION_SIZE) {
				val logContent = remaining.substring(0, SEGMENTATION_SIZE)
				remaining = remaining.replace(logContent, "\t\t")
				Log.i(tag, logContent)
			}
			Log.i(tag, remaining)
		}

		private fun longW(tag: String, msg: String) {
			var remaining = msg
			if (logLevel < LEVEL_W) return
			while (remaining.length > SEGMENTATION_SIZE) {
				val logContent = remaining.substring(0, SEGMENTATION_SIZE)
				remaining = remaining.replace(logContent, "\t\t")
				Log.w(tag, logContent)
			}
			Log.w(tag, remaining)
		}

		private fun longE(tag: String, msg: String) {
			var remaining = msg
			if (logLevel < LEVEL_E) return
			while (remaining.length > SEGMENTATION_SIZE) {
				val logContent = remaining.substring(0, SEGMENTATION_SIZE)
				remaining = remaining.replace(logContent, "\t\t")
				Log.e(tag, logContent)
			}
			Log.e(tag, remaining)
		}

		/**
		 * 默认Tag
		 */
		fun v(msg: String?) {
			v(TagConstant.TAG_DEF, msg)
		}

		/**
		 * 默认Tag
		 */
		fun d(msg: String?) {
			d(TagConstant.TAG_DEF, msg)
		}

		/**
		 * 默认Tag
		 */
		fun i(msg: String?) {
			i(TagConstant.TAG_DEF, msg)
		}

		/**
		 * 默认Tag
		 */
		fun w(msg: String?) {
			w(TagConstant.TAG_DEF, msg)
		}

		/**
		 * 默认Tag
		 */
		fun e(msg: String?) {
			e(TagConstant.TAG_DEF, msg)
		}

		/**
		 * 添加AppTag
		 */
		fun v(tag: String, msg: String?) {
			val msgStr = "$tag $msg"
			if (msgStr.length > SEGMENTATION_SIZE) {
				longV(TagConstant.TAG_APP, msgStr)
			} else {
				if (logLevel < LEVEL_V) return
				Log.v(TagConstant.TAG_APP, msgStr)
			}
		}

		/**
		 * 添加AppTag
		 */
		fun d(tag: String, msg: String?) {
			val msgStr = "$tag $msg"
			if (msgStr.length > SEGMENTATION_SIZE) {
				longD(TagConstant.TAG_APP, msgStr)
			} else {
				if (logLevel < LEVEL_D) return
				Log.d(TagConstant.TAG_APP, msgStr)
			}
		}

		/**
		 * 添加AppTag
		 */
		fun i(tag: String, msg: String?) {
			val msgStr = "$tag $msg"
			if (msgStr.length > SEGMENTATION_SIZE) {
				longI(TagConstant.TAG_APP, msgStr)
			} else {
				if (logLevel < LEVEL_I) return
				Log.i(TagConstant.TAG_APP, msgStr)
			}
		}

		/**
		 * 添加AppTag
		 */
		fun w(tag: String, msg: String?) {
			val msgStr = "$tag $msg"
			if (msgStr.length > SEGMENTATION_SIZE) {
				longW(TagConstant.TAG_APP, msgStr)
			} else {
				if (logLevel < LEVEL_W) return
				Log.w(TagConstant.TAG_APP, msgStr)
			}
		}

		/**
		 * 添加AppTag
		 */
		fun e(tag: String, msg: String?) {
			val msgStr = "$tag $msg"
			if (msgStr.length > SEGMENTATION_SIZE) {
				longE(TagConstant.TAG_APP, msgStr)
			} else {
				if (logLevel < LEVEL_E) return
				Log.e(TagConstant.TAG_APP, msgStr)
			}
		}

		/**
		 * 多参数,使用默认Tag
		 */
		fun v(vararg args: String?) {
			val msg = getArgsStr(*args)
			v(TagConstant.TAG_DEF, msg)
		}

		/**
		 * 多参数,使用默认Tag
		 */
		fun d(vararg args: String?) {
			val msg = getArgsStr(*args)
			d(msg)
		}

		/**
		 * 多参数,使用默认Tag
		 */
		fun i(vararg args: String?) {
			val msg = getArgsStr(*args)
			i(msg)
		}

		/**
		 * 多参数,使用默认Tag
		 */
		fun w(vararg args: String?) {
			val msg = getArgsStr(*args)
			w(msg)
		}

		/**
		 * 多参数,使用默认Tag
		 */
		fun e(vararg args: String?) {
			val msg = getArgsStr(*args)
			e(msg)
		}

		/**
		 * 打印生命周期
		 */
		fun lc(tag: String, msg: String?) {
			if (isLifeCycleEnable) {
				d(tag, TagConstant.TAG_LC, msg)
			}
		}

		/**
		 * 打印ActivityManager管理
		 */
		fun am(tag: String, msg: String?) {
			if (isAtyManagerEnable) {
				d(tag, TagConstant.TAG_AM, msg)
			}
		}

		/**
		 * 打印框架信息
		 */
		fun fw(tag: String, msg: String?) {
			if (isFrameWorkEnable) {
				d(tag, TagConstant.TAG_FW, msg)
			}
		}

		/**
		 * 打印网络请求
		 */
		fun nw(tag: String, msg: String?) {
			if (isNetWorkEnable) {
				nw(tag, msg, false)
			}
		}

		fun nw(tag: String, msg: String?, isError: Boolean) {
			if (isNetWorkEnable) {
				if (isError) {
					e(tag, TagConstant.TAG_NW, msg)
				} else {
					i(tag, TagConstant.TAG_NW, msg)
				}
			}
		}

		private fun getArgsStr(vararg args: String?): String {
			return args.filterNotNull().joinToString(separator = " ", postfix = if (args.isNotEmpty()) " " else "")
		}
	}
}
