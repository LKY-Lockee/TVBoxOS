package com.github.tvbox.osc.util.js

import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.Key
import java.security.KeyFactory
import java.security.PublicKey
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.math.min

object Crypto {
	fun aes(mode: String?, encrypt: Boolean, input: String, inBase64: Boolean, key: String, iv: String?, outBase64: Boolean): String {
		try {
			var keyBuf = key.toByteArray()
			if (keyBuf.size < 16) keyBuf = keyBuf.copyOf(16)
			var ivBuf = iv?.toByteArray() ?: ByteArray(0)
			if (ivBuf.size < 16) ivBuf = ivBuf.copyOf(16)
			val cipher = Cipher.getInstance(mode + "Padding")
			val keySpec = SecretKeySpec(keyBuf, "AES")
			if (iv == null) cipher.init(if (encrypt) Cipher.ENCRYPT_MODE else Cipher.DECRYPT_MODE, keySpec)
			else cipher.init(if (encrypt) Cipher.ENCRYPT_MODE else Cipher.DECRYPT_MODE, keySpec, IvParameterSpec(ivBuf))
			val inBuf = if (inBase64) Base64.decode(input.replace("_".toRegex(), "/").replace("-".toRegex(), "+"), Base64.DEFAULT) else input.toByteArray(StandardCharsets.UTF_8)
			return if (outBase64) Base64.encodeToString(cipher.doFinal(inBuf), Base64.NO_WRAP) else String(cipher.doFinal(inBuf), StandardCharsets.UTF_8)
		} catch (e: Exception) {
			e.printStackTrace()
			return ""
		}
	}

	fun rsa(pub: Boolean, encrypt: Boolean, input: String, inBase64: Boolean, key: String, outBase64: Boolean): String {
		try {
			val rsaKey = generateKey(pub, key)
			val len = getModulusLength(rsaKey)
			var outBytes = ByteArray(0)
			val inBytes = if (inBase64) Base64.decode(input.replace("_".toRegex(), "/").replace("-".toRegex(), "+"), Base64.DEFAULT) else input.toByteArray(StandardCharsets.UTF_8)
			val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
			cipher.init(if (encrypt) Cipher.ENCRYPT_MODE else Cipher.DECRYPT_MODE, rsaKey)
			val blockLen = if (encrypt) len / 8 - 11 else len / 8
			var bufIdx = 0
			while (bufIdx < inBytes.size) {
				val bufEndIdx = min(bufIdx + blockLen, inBytes.size)
				val tmpInBytes = ByteArray(bufEndIdx - bufIdx)
				inBytes.copyInto(tmpInBytes, 0, bufIdx, bufEndIdx)
				val tmpBytes = cipher.doFinal(tmpInBytes)
				bufIdx = bufEndIdx
				outBytes = concatArrays(outBytes, tmpBytes)
			}
			return if (outBase64) Base64.encodeToString(outBytes, Base64.NO_WRAP) else String(outBytes, StandardCharsets.UTF_8)
		} catch (e: Exception) {
			e.printStackTrace()
			return ""
		}
	}

	private fun generateKey(pub: Boolean, key: String): Key {
		var processedKey = key
		processedKey = if (pub) processedKey.replace("\r\n".toRegex(), "").replace("\n".toRegex(), "").replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "")
		else processedKey.replace("\r\n".toRegex(), "").replace("\n".toRegex(), "").replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "")
		return if (pub) KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(Base64.decode(processedKey, Base64.DEFAULT))) else KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(Base64.decode(processedKey, Base64.DEFAULT)))
	}

	private fun getModulusLength(key: Key): Int {
		return if (key is PublicKey) (key as RSAPublicKey).modulus.bitLength()
		else (key as RSAPrivateKey).modulus.bitLength()
	}

	private fun concatArrays(a: ByteArray, b: ByteArray): ByteArray {
		val result = ByteArray(a.size + b.size)
		a.copyInto(result, 0, 0, a.size)
		b.copyInto(result, a.size, 0, b.size)
		return result
	}
}
