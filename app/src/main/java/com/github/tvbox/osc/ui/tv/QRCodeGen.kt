package com.github.tvbox.osc.ui.tv

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import java.util.EnumMap

/**
 * @author pj567
 * @date 2021/1/5
 */
object QRCodeGen {
	fun generateBitmap(content: String, width: Int, height: Int, padding: Int = 0): Bitmap? {
		val qrCodeWriter = QRCodeWriter()
		val hints: MutableMap<EncodeHintType?, String?> = EnumMap(EncodeHintType::class.java)
		hints[EncodeHintType.CHARACTER_SET] = "utf-8"
		hints[EncodeHintType.MARGIN] = padding.toString() + ""
		try {
			val encode = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height, hints)
			val pixels = IntArray(width * height)
			for (i in 0..<height) {
				for (j in 0..<width) {
					if (encode.get(j, i)) {
						pixels[i * width + j] = 0x00000000
					} else {
						pixels[i * width + j] = -0x1
					}
				}
			}
			return Bitmap.createBitmap(pixels, 0, width, width, height, Bitmap.Config.RGB_565)
		} catch (e: WriterException) {
			e.printStackTrace()
		}
		return null
	}
}
