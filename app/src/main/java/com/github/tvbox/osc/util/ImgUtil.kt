package com.github.tvbox.osc.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Base64
import androidx.core.graphics.createBitmap
import com.github.tvbox.osc.base.App.Companion.instance
import me.jessyan.autosize.utils.AutoSizeUtils
import java.util.Random

/**
 * 图片工具
 * 
 * @version 1.0.0
 */
object ImgUtil {
	const val DEFAULT_WIDTH = 244
	const val DEFAULT_HEIGHT = 320
	private val drawableCache: MutableMap<String, Drawable> = HashMap()

	fun isBase64Image(picUrl: String): Boolean {
		return picUrl.startsWith("data:image")
	}

	fun decodeBase64ToBitmap(base64Str: String): Bitmap? {
		// 去掉 Base64 数据的头部前缀，例如 "data:image/png;base64,"
		val base64Data = base64Str.substring(base64Str.indexOf(",") + 1)
		val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
		return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
	}

	fun createTextDrawable(text: String): Drawable {
		val firstChar = text.ifEmpty { "TVBox" }.substring(0, 1)
		// 如果缓存中已存在，直接返回
		drawableCache[firstChar]?.let { return it }

		val width = 180
		val height = 240 // 设定图片大小
		val randomColor: Int = randomColor
		val cornerRadius = AutoSizeUtils.mm2px(instance, 5f).toFloat() // 圆角半径

		val bitmap = createBitmap(width, height)
		val canvas = Canvas(bitmap)
		// 画圆角背景
		val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
			color = randomColor
			style = Paint.Style.FILL
		}
		val rectF = RectF(0f, 0f, width.toFloat(), height.toFloat())
		canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, paint)
		paint.color = Color.WHITE // 文字颜色
		paint.textSize = 50f // 文字大小
		paint.textAlign = Paint.Align.CENTER
		val fontMetrics = paint.fontMetrics
		val x = width / 2f
		val y = (height - fontMetrics.bottom - fontMetrics.top) / 2f

		canvas.drawText(firstChar, x, y, paint)
		val drawable: Drawable = BitmapDrawable(bitmap)
		drawableCache[firstChar] = drawable
		return drawable
	}

	val randomColor: Int
		get() {
			val random = Random()
			return Color.argb(255, random.nextInt(256), random.nextInt(256), random.nextInt(256))
		}

	fun clearCache() {
		drawableCache.clear()
	}
}
