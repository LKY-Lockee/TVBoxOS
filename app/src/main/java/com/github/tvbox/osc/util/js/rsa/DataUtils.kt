package com.github.tvbox.osc.util.js.rsa

import android.util.Base64
import java.nio.ByteBuffer

/**
 * 数据工具类
 */
object DataUtils {
	/**
	 * 将 Base64 字符串解码成字节数组
	 */
	fun base64Decode(data: String): ByteArray {
		return Base64.decode(data, Base64.NO_WRAP)
	}

	/**
	 * 将字节数组转换成 Base64 编码
	 */
	fun base64Encode(data: ByteArray?): String {
		return Base64.encodeToString(data, Base64.NO_WRAP)
	}

	/**
	 * 将字节数组转换成 int 类型
	 */
	fun byte2Int(bytes: ByteArray): Int {
		val buffer = ByteBuffer.wrap(bytes)
		return buffer.getInt()
	}

	/**
	 * 将 int 转换成字节数组
	 */
	fun int2byte(data: Int): ByteArray {
		val buffer = ByteBuffer.allocate(4)
		buffer.putInt(data)
		return buffer.array()
	}
}
