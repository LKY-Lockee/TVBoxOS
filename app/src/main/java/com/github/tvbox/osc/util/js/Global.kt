package com.github.tvbox.osc.util.js

import androidx.annotation.Keep
import com.github.tvbox.osc.server.ControlManager.Companion.instance
import com.github.tvbox.osc.util.js.Connect.to
import com.github.tvbox.osc.util.js.Crypto.aes
import com.github.tvbox.osc.util.js.Crypto.rsa
import com.github.tvbox.osc.util.js.rsa.RSAEncrypt.decryptByPrivateKey
import com.github.tvbox.osc.util.js.rsa.RSAEncrypt.decryptByPublicKey
import com.github.tvbox.osc.util.js.rsa.RSAEncrypt.encryptByPrivateKey
import com.github.tvbox.osc.util.js.rsa.RSAEncrypt.encryptByPublicKey
import com.whl.quickjs.wrapper.JSArray
import com.whl.quickjs.wrapper.JSFunction
import com.whl.quickjs.wrapper.JSObject
import com.whl.quickjs.wrapper.QuickJSContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import java.net.URLEncoder
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.ExecutorService

class Global(val executor: ExecutorService) {
	private val timer: Timer = Timer()
	private var runtime: QuickJSContext? = null

	@Keep
	@Function
	fun getProxy(local: Boolean): String {
		return instance.getAddress(local) + "proxy?do=js"
	}

	@Keep
	@Function
	fun js2Proxy(dynamic: Boolean?, siteType: Int?, siteKey: String?, url: String?, headers: JSObject): String {
		return getProxy(true) + "&from=catvod" + "&siteType=" + (siteType ?: "") + "&siteKey=" + (siteKey ?: "") + "&header=" + URLEncoder.encode(headers.stringify()) + "&url=" + URLEncoder.encode(url ?: "")
	}

	@Keep
	@Function
	fun joinUrl(parent: String?, child: String?): String? {
		return HtmlParser.joinUrl(parent, child)
	}

	@Keep
	@Function
	fun pd(html: String?, rule: String?, add_url: String?): String? {
		return HtmlParser.parseDomForUrl(html ?: "", rule ?: "", add_url)
	}

	@Keep
	@Function
	fun pdfh(html: String?, rule: String?): String? {
		return HtmlParser.parseDomForUrl(html ?: "", rule ?: "", "")
	}

	@Keep
	@Function
	fun pdfa(html: String?, rule: String?): JSArray {
		val rt = runtime ?: return QuickJSContext.create().createNewJSArray()
		return JSUtils<String>().toArray(rt, HtmlParser.parseDomForArray(html ?: "", rule ?: ""))
	}

	@Keep
	@Function
	fun pdfla(html: String?, p1: String?, list_text: String?, list_url: String?, add_url: String?): JSArray {
		val rt = runtime ?: return QuickJSContext.create().createNewJSArray()
		return JSUtils<String>().toArray(rt, HtmlParser.parseDomForList(html ?: "", p1 ?: "", list_text ?: "", list_url ?: "", add_url))
	}

	@Keep
	@Function
	fun s2t(text: String?): String? {
		return try {
			Trans.s2t(false, text)
		} catch (e: Exception) {
			""
		}
	}

	@Keep
	@Function
	fun t2s(text: String?): String? {
		return try {
			Trans.t2s(false, text)
		} catch (e: Exception) {
			""
		}
	}

	@Keep
	@Function
	fun aesX(mode: String?, encrypt: Boolean, input: String, inBase64: Boolean, key: String, iv: String?, outBase64: Boolean): String {
		return aes(mode, encrypt, input, inBase64, key, iv, outBase64)
	}

	@Keep
	@Function
	fun rsaX(mode: String?, pub: Boolean, encrypt: Boolean, input: String, inBase64: Boolean, key: String, outBase64: Boolean): String {
		return rsa(pub, encrypt, input, inBase64, key, outBase64)
	}

	@Keep
	@Function
	fun rsaEncrypt(data: String, key: String): String? {
		return rsaEncrypt(data, key, null)
	}

	/**
	 * RSA 加密
	 * 
	 * @param data 要加密的数据
	 * @param key 密钥，type 为 1 则公钥，type 为 2 则私钥
	 * @param options 加密的选项，包含加密配置和类型：{ config: "RSA/ECB/PKCS1Padding", type: 1, long: 1 }
	 * - config 加密的配置，默认 RSA/ECB/PKCS1Padding （可选）
	 * - type 加密类型，1 公钥加密 私钥解密，2 私钥加密 公钥解密（可选，默认 1）
	 * - long 加密方式，1 普通，2 分段（可选，默认 1）
	 * - block 分段长度，false 固定117，true 自动（可选，默认 true）
	 * @return 返回加密结果
	 */
	@Keep
	@Function
	fun rsaEncrypt(data: String, key: String, options: JSObject?): String? {
		var mLong = 1
		var mType = 1
		var mBlock = true
		var mConfig: String? = null
		if (options != null) {
			val op = JSUtils.toJsonObject(options)
			if (op.has("config")) {
				try {
					mConfig = op.get("config") as String
				} catch (e: Exception) {
					e.printStackTrace()
				}
			}
			if (op.has("type")) {
				try {
					mType = (op.get("type") as Double).toInt()
				} catch (e: Exception) {
					e.printStackTrace()
				}
			}
			if (op.has("long")) {
				try {
					mLong = (op.get("long") as Double).toInt()
				} catch (e: Exception) {
					e.printStackTrace()
				}
			}
			if (op.has("block")) {
				try {
					mBlock = op.get("block") as Boolean
				} catch (e: Exception) {
					e.printStackTrace()
				}
			}
		}
		try {
			return when (mType) {
				1 -> if (mConfig != null) {
					encryptByPublicKey(data, key, mConfig, mLong, mBlock)
				} else {
					encryptByPublicKey(data, key, mLong, mBlock)
				}

				2 -> if (mConfig != null) {
					encryptByPrivateKey(data, key, mConfig, mLong, mBlock)
				} else {
					encryptByPrivateKey(data, key, mLong, mBlock)
				}

				else -> ""
			}
		} catch (e: Exception) {
			return ""
		}
	}

	@Keep
	@Function
	fun rsaDecrypt(encryptBase64Data: String, key: String): String? {
		return rsaDecrypt(encryptBase64Data, key, null)
	}

	/**
	 * RSA 解密
	 * 
	 * @param encryptBase64Data 加密后的 Base64 字符串
	 * @param key 密钥，type 为 1 则私钥，type 为 2 则公钥
	 * @param options 解密的选项，包含解密配置和类型：{ config: "RSA/ECB/PKCS1Padding", type: 1, long: 1 }
	 * - config 解密的配置，默认 RSA/ECB/PKCS1Padding （可选）
	 * - type 解密类型，1 公钥加密 私钥解密，2 私钥加密 公钥解密（可选，默认 1）
	 * - long 解密方式，1 普通，2 分段（可选，默认 1）
	 * - block 分段长度，false 固定128，true 自动（可选，默认 true）
	 * @return 返回解密结果
	 */
	@Keep
	@Function
	fun rsaDecrypt(encryptBase64Data: String, key: String, options: JSObject?): String? {
		var mLong = 1
		var mType = 1
		var mBlock = true
		var mConfig: String? = null
		if (options != null) {
			val op = JSUtils.toJsonObject(options)
			if (op.has("config")) {
				try {
					mConfig = op.get("config") as String
				} catch (e: Exception) {
					e.printStackTrace()
				}
			}
			if (op.has("type")) {
				try {
					mType = (op.get("type") as Double).toInt()
				} catch (e: Exception) {
					e.printStackTrace()
				}
			}
			if (op.has("long")) {
				try {
					mLong = (op.get("long") as Double).toInt()
				} catch (e: Exception) {
					e.printStackTrace()
				}
			}
			if (op.has("block")) {
				try {
					mBlock = op.get("block") as Boolean
				} catch (e: Exception) {
					e.printStackTrace()
				}
			}
		}
		try {
			return when (mType) {
				1 -> if (mConfig != null) {
					decryptByPrivateKey(encryptBase64Data, key, mConfig, mLong, mBlock)
				} else {
					decryptByPrivateKey(encryptBase64Data, key, mLong, mBlock)
				}

				2 -> if (mConfig != null) {
					decryptByPublicKey(encryptBase64Data, key, mConfig, mLong, mBlock)
				} else {
					decryptByPublicKey(encryptBase64Data, key, mLong, mBlock)
				}

				else -> ""
			}
		} catch (e: Exception) {
			return ""
		}
	}

	private fun req(url: String, options: JSObject): JSObject {
		val rt = runtime ?: throw IllegalStateException("QuickJSContext not initialized")
		try {
			val req = Req.objectFrom(JSUtils.toJsonObject(options).toString()) ?: return Connect.error(rt)
			val res = to(url, req).execute()
			return Connect.success(rt, req, res)
		} catch (e: Exception) {
			return Connect.error(rt)
		}
	}

	@Keep
	@Function
	fun _http(url: String, options: JSObject): JSObject? {
		val complete = options.getJSFunction("complete") ?: return req(url, options)
		val req = Req.objectFrom(JSUtils.toJsonObject(options).toString()) ?: return null
		to(url, req).enqueue(getCallback(complete, req))
		return null
	}

	@Keep
	@Function
	fun setTimeout(func: JSFunction, delay: Int) {
		func.hold()
		timer.schedule(object : TimerTask() {
			override fun run() {
				if (!executor.isShutdown) executor.submit {
					func.call()
				}
			}
		}, delay.toLong())
	}

	private fun getCallback(complete: JSFunction, req: Req): Callback {
		return object : Callback {
			override fun onResponse(call: Call, response: Response) {
				executor.submit {
					complete.call(Connect.success(runtime ?: return@submit, req, response))
				}
			}

			override fun onFailure(call: Call, e: IOException) {
				executor.submit {
					complete.call(Connect.error(runtime ?: return@submit))
				}
			}
		}
	}

	/**
	 * 声明用于依赖注入的 QuickJSContext
	 */
	@Keep
	@ContextSetter
	fun setJSContext(runtime: QuickJSContext) {
		this.runtime = runtime
	}
}