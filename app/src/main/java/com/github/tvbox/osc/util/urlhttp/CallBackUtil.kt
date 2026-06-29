package com.github.tvbox.osc.util.urlhttp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import kotlin.math.floor
import kotlin.math.max

/**
 * Created by fighting on 2017/4/7.
 */
abstract class CallBackUtil<T> {
	fun onProgress(progress: Float, total: Long) {
	}

	fun onError(response: RealResponse) {
		val errorMessage: String = when {
			response.inputStream != null -> getRetString(response.inputStream) ?: ""
			response.errorStream != null -> getRetString(response.errorStream) ?: ""
			response.exception != null -> response.exception?.message ?: ""
			else -> ""
		}
		mMainHandler.post { onFailure(response.code, errorMessage) }
	}

	fun onSuccess(response: RealResponse?) {
		val obj = onParseResponse(response)
		mMainHandler.post { onResponse(obj) }
	}

	/**
	 * 解析response，执行在子线程
	 */
	abstract fun onParseResponse(response: RealResponse?): T?

	/**
	 * 访问网络失败后被调用，执行在UI线程
	 */
	abstract fun onFailure(code: Int, errorMessage: String?)

	/**
	 * 访问网络成功后被调用，执行在UI线程
	 */
	abstract fun onResponse(response: T?)

	abstract class CallBackDefault : CallBackUtil<RealResponse>() {
		override fun onParseResponse(response: RealResponse?): RealResponse? {
			return response
		}
	}

	abstract class CallBackString : CallBackUtil<String>() {
		override fun onParseResponse(response: RealResponse?): String? {
			try {
				val inputStream = response?.inputStream ?: return null
				return getRetString(inputStream)
			} catch (e: Exception) {
				throw RuntimeException("failure")
			}
		}
	}

	abstract class CallBackBitmap : CallBackUtil<Bitmap?> {
		private var mTargetWidth = 0
		private var mTargetHeight = 0

		constructor()

		constructor(targetWidth: Int, targetHeight: Int) {
			mTargetWidth = targetWidth
			mTargetHeight = targetHeight
		}

		constructor(imageView: ImageView) {
			val width = imageView.width
			val height = imageView.height
			if (width <= 0 || height <= 0) {
				throw RuntimeException("无法获取ImageView的width或height")
			}
			mTargetWidth = width
			mTargetHeight = height
		}

		override fun onParseResponse(response: RealResponse?): Bitmap? {
			return if (mTargetWidth == 0 || mTargetHeight == 0) {
				BitmapFactory.decodeStream(response?.inputStream)
			} else {
				val inputStream = response?.inputStream ?: return null
				getZoomBitmap(inputStream)
			}
		}

		/**
		 * 压缩图片，避免OOM异常
		 */
		private fun getZoomBitmap(inputStream: InputStream): Bitmap {
			var data: ByteArray? = null
			try {
				data = input2byte(inputStream)
			} catch (e: IOException) {
				e.printStackTrace()
			}
			val options = BitmapFactory.Options()
			options.inJustDecodeBounds = true

			BitmapFactory.decodeByteArray(data, 0, data?.size ?: 0, options)
			val picWidth = options.outWidth
			val picHeight = options.outHeight
			var sampleSize = 1
			val heightRatio = floor((picWidth.toFloat() / mTargetWidth.toFloat()).toDouble()).toInt()
			val widthRatio = floor((picHeight.toFloat() / mTargetHeight.toFloat()).toDouble()).toInt()
			if (heightRatio > 1 || widthRatio > 1) {
				sampleSize = max(heightRatio, widthRatio)
			}
			options.inSampleSize = sampleSize
			options.inJustDecodeBounds = false
			return BitmapFactory.decodeByteArray(data, 0, data?.size ?: 0, options) ?: throw RuntimeException("Failed to decode stream.")
		}
	}

	/**
	 * 下载文件时的回调类
	 * 
	 * @property destFileDir:文件目录
	 * @property destFileName：文件名
	 */
	abstract class CallBackFile(private val destFileDir: String, private val destFileName: String) : CallBackUtil<File?>() {
		override fun onParseResponse(response: RealResponse?): File? {
			var inputStream: InputStream? = null
			val buf = ByteArray(1024 * 8)
			var len: Int
			var fos: FileOutputStream? = null
			try {
				inputStream = response?.inputStream ?: return null
				val total = response.contentLength

				var sum: Long = 0

				val dir = File(destFileDir)
				if (!dir.exists()) {
					dir.mkdirs()
				}
				val file = File(dir, destFileName)
				fos = FileOutputStream(file)
				while ((inputStream.read(buf).also { len = it }) != -1) {
					sum += len.toLong()
					fos.write(buf, 0, len)
					val finalSum = sum
					mMainHandler.post { onProgress(finalSum * 100.0f / total, total) }
				}
				fos.flush()

				return file
			} catch (e: Exception) {
				e.printStackTrace()
			} finally {
				try {
					fos?.close()
				} catch (ignored: IOException) {
				}
				try {
					inputStream?.close()
				} catch (ignored: IOException) {
				}
			}
			return null
		}
	}

	companion object {
		val mMainHandler: Handler = Handler(Looper.getMainLooper())

		fun input2byte(inStream: InputStream): ByteArray {
			val swapStream = ByteArrayOutputStream()
			val buff = ByteArray(100)
			var rc: Int
			while ((inStream.read(buff, 0, 100).also { rc = it }) > 0) {
				swapStream.write(buff, 0, rc)
			}
			return swapStream.toByteArray()
		}

		private fun getRetString(inputStream: InputStream?): String? {
			try {
				val safeStream = inputStream ?: return null
				val reader = BufferedReader(InputStreamReader(safeStream, StandardCharsets.UTF_8))
				val sb = StringBuilder()
				var line: String?
				while ((reader.readLine().also { line = it }) != null) {
					sb.append(line).append("\n")
				}
				safeStream.close()
				return sb.toString()
			} catch (e: Exception) {
				return null
			}
		}
	}
}
