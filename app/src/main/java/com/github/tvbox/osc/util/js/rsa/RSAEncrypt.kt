package com.github.tvbox.osc.util.js.rsa

import android.util.Log
import com.github.tvbox.osc.util.js.rsa.DataUtils.base64Decode
import com.github.tvbox.osc.util.js.rsa.DataUtils.base64Encode
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.NoSuchAlgorithmException
import java.security.PrivateKey
import java.security.PublicKey
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.InvalidKeySpecException
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher

/**
 * RSA 非对称加密算法，加解密工具类，
 * 加密长度 不能超过 128 个字节。
 */
object RSAEncrypt {
	val TAG: String = RSAEncrypt::class.java.simpleName + " --> "

	/**
	 * 标准 jdk 加密填充方式，加解密算法/工作模式/填充方式
	 */
	const val ECB_PKCS1_PADDING: String = "RSA/ECB/PKCS1Padding"

	/**
	 * RSA 加密算法
	 */
	const val KEY_ALGORITHM: String = "RSA"

	/**
	 * RSA 最大加密明文大小
	 */
	private const val MAX_ENCRYPT_BLOCK = 117

	/**
	 * RSA最大解密密文大小
	 */
	private const val MAX_DECRYPT_BLOCK = 128

	/**
	 * 随机生成 RSA 密钥对
	 * 
	 * @param keyLength 密钥长度，范围：512～2048，一般：1024
	 */
	fun getKeyPair(keyLength: Int): KeyPair? {
		try {
			val generator = KeyPairGenerator.getInstance(KEY_ALGORITHM)
			generator.initialize(keyLength)
			return generator.genKeyPair()
		} catch (e: Exception) {
			handleException(e)
		}
		return null
	}

	/**
	 * 获取公钥 Base64 编码
	 * 
	 * @param publicKey 公钥
	 */
	fun getPublicKeyBase64(publicKey: PublicKey): String {
		return base64Encode(publicKey.encoded)
	}

	/**
	 * 获取私钥 Base64 编码
	 * 
	 * @param privateKey 公钥
	 */
	fun getPrivateKeyBase64(privateKey: PrivateKey): String {
		return base64Encode(privateKey.encoded)
	}

	/**
	 * 获取 PublicKey 对象
	 * 
	 * @param pubKey 公钥，X509 格式de
	 */
	fun getPublicKey(pubKey: String): PublicKey? {
		try {
			// 将公钥进行 Base64 解码  创建 PublicKey 对象并返回
			return KeyFactory.getInstance(KEY_ALGORITHM).generatePublic(X509EncodedKeySpec(base64Decode(pubKey)))
		} catch (unused: NoSuchAlgorithmException) {
			handleException(Exception("无此算法"))
		} catch (unused2: InvalidKeySpecException) {
			handleException(Exception("公钥非法"))
		} catch (unused3: NullPointerException) {
			handleException(Exception("公钥数据为空"))
		}
		return null
	}

	/**
	 * 获取 PrivateKey 对象
	 * 
	 * @param prvKey 私钥，PKCS8 格式
	 */
	fun getPrivateKey(prvKey: String): PrivateKey? {
		try {
			// 将私钥进行 Base64 解码  创建 PrivateKey 对象并返回
			return KeyFactory.getInstance(KEY_ALGORITHM).generatePrivate(PKCS8EncodedKeySpec(base64Decode(prvKey)))
		} catch (unused: NoSuchAlgorithmException) {
			handleException(Exception("无此算法"))
		} catch (unused2: InvalidKeySpecException) {
			handleException(Exception("私钥非法"))
		} catch (unused3: NullPointerException) {
			handleException(Exception("私钥数据为空"))
		}
		return null
	}

	// --------------------- 1 公钥加密，私钥解密 ---------------------
	/**
	 * 使用公钥将数据进行分段加密
	 * 
	 * @param data 要加密的数据
	 * @param pubKey 公钥 Base64 字符串，X509 格式
	 * @return 加密后的 Base64 编码数据，加密失败返回 null
	 */
	fun encryptByPublicKey(data: String, pubKey: String, mlong: Int, block: Boolean): String? {
		return encryptByPublicKey(data, pubKey, ECB_PKCS1_PADDING, mlong, block)
	}

	fun encryptByPublicKey(data: String, pubKey: String, config: String?, mlong: Int, block: Boolean): String? {
		try {
			val bytes = data.toByteArray(StandardCharsets.UTF_8)
			// 创建 Cipher 对象
			val cipher = Cipher.getInstance(config)
			// 初始化 Cipher 对象，加密模式
			val rSAPublicKey = getPublicKey(pubKey) as RSAPublicKey?
			cipher.init(Cipher.ENCRYPT_MODE, rSAPublicKey)
			if (mlong == 1) {
				return base64Encode(cipher.doFinal(bytes))
			}
			var bitLength = MAX_ENCRYPT_BLOCK
			if (block) {
				bitLength = (rSAPublicKey ?: return null).modulus.bitLength() / 8 - 11
			}
			val inputLen = bytes.size
			// 保存加密的数据
			val out = ByteArrayOutputStream()
			var offSet = 0
			var i = 0
			var cache: ByteArray
			// 使用 RSA 对数据分段加密
			while (inputLen - offSet > 0) {
				if (inputLen - offSet > bitLength) {
					cache = cipher.doFinal(bytes, offSet, bitLength)
				} else {
					cache = cipher.doFinal(bytes, offSet, inputLen - offSet)
				}
				// 将加密以后的数据保存到内存
				out.write(cache, 0, cache.size)
				i++
				offSet = i * bitLength
			}
			val encryptedData = out.toByteArray()
			out.close()
			// 将加密后的数据转换成 Base64 字符串
			return base64Encode(encryptedData)
		} catch (e: Exception) {
			handleException(e)
		}
		return null
	}

	/**
	 * 使用私钥将加密后的 Base64 字符串进行分段解密
	 * 
	 * @param encryptBase64Data 加密后的 Base64 字符串
	 * @param prvKey 私钥 Base64 字符串，PKCS8 格式
	 * @return 解密后的明文，解密失败返回 null
	 */
	fun decryptByPrivateKey(encryptBase64Data: String, prvKey: String, mlong: Int, block: Boolean): String? {
		return decryptByPrivateKey(encryptBase64Data, prvKey, ECB_PKCS1_PADDING, mlong, block)
	}

	fun decryptByPrivateKey(encryptBase64Data: String, prvKey: String, config: String?, mlong: Int, block: Boolean): String? {
		try {
			// 将要解密的数据，进行 Base64 解码
			val encryptedData = base64Decode(encryptBase64Data)
			// 创建 Cipher 对象，用来解密
			val cipher = Cipher.getInstance(config)
			// 初始化 Cipher 对象，解密模式
			val rSAPrivateKey = getPrivateKey(prvKey) as RSAPrivateKey?
			cipher.init(Cipher.DECRYPT_MODE, rSAPrivateKey)
			if (mlong == 1) {
				return String(cipher.doFinal(encryptedData))
			}
			var bitLength = MAX_DECRYPT_BLOCK
			if (block) {
				bitLength = (rSAPrivateKey ?: return null).modulus.bitLength() / 8
			}
			val inputLen = encryptedData.size
			// 保存解密的数据
			val out = ByteArrayOutputStream()
			var offSet = 0
			var i = 0
			var cache: ByteArray
			// 对数据分段解密
			while (inputLen - offSet > 0) {
				if (inputLen - offSet > bitLength) {
					cache = cipher.doFinal(encryptedData, offSet, bitLength)
				} else {
					cache = cipher.doFinal(encryptedData, offSet, inputLen - offSet)
				}
				// 将解密后的数据保存到内存
				out.write(cache, 0, cache.size)
				i++
				offSet = i * bitLength
			}
			out.close()
			return out.toString("UTF-8")
		} catch (e: Exception) {
			handleException(e)
		}
		return null
	}

	// --------------------- 2 私钥加密，公钥解密 ---------------------
	/**
	 * 使用 私钥 将数据进行分段加密
	 * 
	 * @param data 要加密的数据
	 * @param prvKey 私钥 Base64 字符串，PKCS8 格式
	 * @return 加密后的 Base64 编码数据，加密失败返回 null
	 */
	fun encryptByPrivateKey(data: String, prvKey: String, mlong: Int, block: Boolean): String? {
		return encryptByPrivateKey(data, prvKey, ECB_PKCS1_PADDING, mlong, block)
	}

	fun encryptByPrivateKey(data: String, prvKey: String, config: String?, mlong: Int, block: Boolean): String? {
		try {
			val bytes = data.toByteArray(StandardCharsets.UTF_8)
			// 创建 Cipher 对象
			val cipher = Cipher.getInstance(config)
			// 初始化 Cipher 对象，加密模式
			val rSAPrivateKey = getPrivateKey(prvKey) as RSAPrivateKey?
			cipher.init(Cipher.ENCRYPT_MODE, rSAPrivateKey)
			if (mlong == 1) {
				return base64Encode(cipher.doFinal(bytes))
			}
			var bitLength = MAX_ENCRYPT_BLOCK
			if (block) {
				bitLength = (rSAPrivateKey ?: return null).modulus.bitLength() / 8 - 11
			}
			val inputLen = bytes.size
			// 保存加密的数据
			val out = ByteArrayOutputStream()
			var offSet = 0
			var i = 0
			var cache: ByteArray
			// 使用 RSA 对数据分段加密
			while (inputLen - offSet > 0) {
				if (inputLen - offSet > bitLength) {
					cache = cipher.doFinal(bytes, offSet, bitLength)
				} else {
					cache = cipher.doFinal(bytes, offSet, inputLen - offSet)
				}
				// 将加密以后的数据保存到内存
				out.write(cache, 0, cache.size)
				i++
				offSet = i * bitLength
			}
			val encryptedData = out.toByteArray()
			out.close()
			// 将加密后的数据转换成 Base64 字符串
			return base64Encode(encryptedData)
		} catch (e: Exception) {
			handleException(e)
		}
		return null
	}

	/**
	 * 使用 公钥 将加密后的 Base64 字符串进行分段解密
	 * 
	 * @param encryptBase64Data 加密后的 Base64 字符串
	 * @param pubKey 公钥 Base64 字符串，X509 格式
	 * @return 解密后的明文，解密失败返回 null
	 */
	fun decryptByPublicKey(encryptBase64Data: String, pubKey: String, mlong: Int, block: Boolean): String? {
		return decryptByPublicKey(encryptBase64Data, pubKey, ECB_PKCS1_PADDING, mlong, block)
	}

	fun decryptByPublicKey(encryptBase64Data: String, pubKey: String, config: String?, mlong: Int, block: Boolean): String? {
		try {
			// 将要解密的数据，进行 Base64 解码
			val encryptedData = base64Decode(encryptBase64Data)
			// 创建 Cipher 对象，用来解密
			val cipher = Cipher.getInstance(config)
			// 初始化 Cipher 对象，解密模式
			val rSAPublicKey = getPublicKey(pubKey) as RSAPublicKey?
			cipher.init(Cipher.DECRYPT_MODE, rSAPublicKey)
			if (mlong == 1) {
				return String(cipher.doFinal(encryptedData))
			}
			var bitLength = MAX_DECRYPT_BLOCK
			if (block) {
				bitLength = (rSAPublicKey ?: return null).modulus.bitLength() / 8
			}
			val inputLen = encryptedData.size
			// 保存解密的数据
			val out = ByteArrayOutputStream()
			var offSet = 0
			var i = 0
			var cache: ByteArray
			// 对数据分段解密
			while (inputLen - offSet > 0) {
				if (inputLen - offSet > bitLength) {
					cache = cipher.doFinal(encryptedData, offSet, bitLength)
				} else {
					cache = cipher.doFinal(encryptedData, offSet, inputLen - offSet)
				}
				// 将解密后的数据保存到内存
				out.write(cache, 0, cache.size)
				i++
				offSet = i * bitLength
			}
			out.close()
			return out.toString("UTF-8")
		} catch (e: Exception) {
			handleException(e)
		}
		return null
	}

	/**
	 * 处理异常
	 */
	private fun handleException(e: Exception) {
		e.printStackTrace()
		Log.e(TAG, TAG + e)
	}
}
