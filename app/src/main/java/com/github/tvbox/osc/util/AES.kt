package com.github.tvbox.osc.util

import org.json.JSONObject
import java.security.spec.AlgorithmParameterSpec
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object AES {
	fun rightPadding(key: String, replace: String, length: Int): String {
		val curLength = key.trim().length
		return when {
			curLength > length -> key.trim().substring(0, length)
			curLength == length -> key.trim()
			else -> key.trim() + replace.repeat(length - curLength)
		}
	}

	fun ecb(data: String, key: String): String {
		return try {
			val paddedKey = rightPadding(key, "0", 16)
			val data2 = toBytes(data)
			val keySpec = SecretKeySpec(paddedKey.toByteArray(), "AES")
			val cipher = Cipher.getInstance("AES/ECB/PKCS7Padding")
			cipher.init(Cipher.DECRYPT_MODE, keySpec)
			String(cipher.doFinal(data2))
		} catch (e: Exception) {
			e.printStackTrace()
			""
		}
	}

	fun cbc(data: String, key: String, iv: String): String {
		return try {
			val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
			val keySpec = SecretKeySpec(key.toByteArray(), "AES")
			val paramSpec: AlgorithmParameterSpec = IvParameterSpec(iv.toByteArray())
			cipher.init(Cipher.DECRYPT_MODE, keySpec, paramSpec)
			String(cipher.doFinal(toBytes(data)))
		} catch (e: Exception) {
			e.printStackTrace()
			""
		}
	}

	fun isJson(content: String): Boolean {
		return try {
			JSONObject(content)
			true
		} catch (e: Exception) {
			false
		}
	}

	fun toBytes(src: String): ByteArray {
		val l = src.length / 2
		val ret = ByteArray(l)
		for (i in 0..<l) {
			ret[i] = src.substring(i * 2, i * 2 + 2).toInt(16).toByte()
		}
		return ret
	}
}
