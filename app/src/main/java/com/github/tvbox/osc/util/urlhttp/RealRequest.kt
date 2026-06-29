package com.github.tvbox.osc.util.urlhttp

import android.text.TextUtils
import java.io.BufferedWriter
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * Created by fighting on 2017/4/24.
 */
internal class RealRequest {
	/**
	 * get请求
	 */
	fun getData(requestURL: String, headerMap: Map<String, String>?): RealResponse {
		var conn: HttpURLConnection? = null
		try {
			conn = getHttpURLConnection(requestURL, "GET")
			conn.setDoInput(true)
			if (headerMap != null) {
				setHeader(conn, headerMap)
			}
			conn.connect()
			return getRealResponse(conn)
		} catch (e: Exception) {
			return getExceptionResponse(conn, e)
		}
	}

	/**
	 * post请求
	 */
	fun postData(requestURL: String, body: String?, bodyType: String?, headerMap: Map<String, String>?): RealResponse {
		var conn: HttpURLConnection? = null
		try {
			conn = getHttpURLConnection(requestURL, "POST")
			conn.setDoOutput(true) //可写出
			conn.setDoInput(true) //可读入
			conn.setUseCaches(false) //不是有缓存
			if (!TextUtils.isEmpty(bodyType)) {
				conn.setRequestProperty("Content-Type", bodyType)
			}
			if (headerMap != null) {
				setHeader(conn, headerMap) //请求头必须放在conn.connect()之前
			}
			conn.connect() // 连接，以上所有的请求配置必须在这个API调用之前
			if (!TextUtils.isEmpty(body)) {
				val writer = BufferedWriter(OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8))
				writer.write(body)
				writer.close()
			}
			return getRealResponse(conn)
		} catch (e: Exception) {
			return getExceptionResponse(conn, e)
		}
	}

	/**
	 * 上传文件
	 */
	fun uploadFile(requestURL: String, file: File?, fileList: List<File>?, fileMap: Map<String, File>?, fileKey: String?, fileType: String?, paramsMap: Map<String, String>?, headerMap: Map<String, String>?, callBack: CallBackUtil<*>?): RealResponse {
		var conn: HttpURLConnection? = null
		try {
			conn = getHttpURLConnection(requestURL, "POST")
			setConnection(conn)
			if (headerMap != null) {
				setHeader(conn, headerMap)
			}
			conn.connect()
			val outputStream = DataOutputStream(conn.getOutputStream())
			if (paramsMap != null) {
				outputStream.write(getParamsString(paramsMap).toByteArray()) //上传参数
				outputStream.flush()
			}
			if (file != null) {
				writeFile(file, fileKey, fileType, outputStream, callBack) //上传文件
			} else if (fileList != null) {
				for (f in fileList) {
					writeFile(f, fileKey, fileType, outputStream, null)
				}
			} else if (fileMap != null) {
				for ((key, value) in fileMap) {
					writeFile(value, key, fileType, outputStream, null)
				}
			}
			val endData: ByteArray = (LINE_END + TWO_HYPHENS + BOUNDARY + TWO_HYPHENS + LINE_END).toByteArray() //写结束标记位
			outputStream.write(endData)
			outputStream.flush()
			return getRealResponse(conn)
		} catch (e: Exception) {
			return getExceptionResponse(conn, e)
		}
	}

	/**
	 * 得到Connection对象，并进行一些设置
	 */
	private fun getHttpURLConnection(requestURL: String, requestMethod: String): HttpURLConnection {
		val url = URL(requestURL)
		val conn = url.openConnection() as HttpURLConnection
		conn.connectTimeout = 10 * 1000
		conn.readTimeout = 15 * 1000
		conn.requestMethod = requestMethod
		return conn
	}

	/**
	 * 设置请求头
	 */
	private fun setHeader(conn: HttpURLConnection, headerMap: Map<String, String>) {
		for ((key, value) in headerMap) {
			conn.setRequestProperty(key, value)
		}
	}

	/**
	 * 上传文件时设置Connection参数
	 */
	private fun setConnection(conn: HttpURLConnection) {
		conn.setDoOutput(true)
		conn.setDoInput(true)
		conn.setUseCaches(false)
		conn.setRequestProperty("Connection", "Keep-Alive")
		conn.setRequestProperty("Charset", "UTF-8")
		conn.setRequestProperty("Content-Type", "multipart/form-data; BOUNDARY=$BOUNDARY")
	}

	/**
	 * 上传文件时得到拼接的参数字符串
	 */
	private fun getParamsString(paramsMap: Map<String, String>): String {
		val strBuf = StringBuilder()
		for ((key, value) in paramsMap) {
			strBuf.append(TWO_HYPHENS)
			strBuf.append(BOUNDARY)
			strBuf.append(LINE_END)
			strBuf.append("Content-Disposition: form-data; name=\"").append(key).append("\"")
			strBuf.append(LINE_END)

			strBuf.append("Content-Type: " + "text/plain")
			strBuf.append(LINE_END)
			strBuf.append("Content-Length: ").append(value.length)
			strBuf.append(LINE_END)
			strBuf.append(LINE_END)
			strBuf.append(value)
			strBuf.append(LINE_END)
		}
		return strBuf.toString()
	}

	/**
	 * 上传文件时写文件
	 */
	private fun writeFile(file: File, fileKey: String?, fileType: String?, outputStream: DataOutputStream, callBack: CallBackUtil<*>?) {
		outputStream.write(getFileParamsString(file, fileKey, fileType).toByteArray())
		outputStream.flush()

		val inputStream = FileInputStream(file)
		val total = file.length()
		var sum: Long = 0
		val buffer = ByteArray(1024 * 2)
		var length: Int
		while ((inputStream.read(buffer).also { length = it }) != -1) {
			outputStream.write(buffer, 0, length)
			sum += length
			if (callBack != null) {
				val finalSum = sum
				CallBackUtil.mMainHandler.post { callBack.onProgress(finalSum * 100.0f / total, total) }
			}
		}
		outputStream.flush()
		inputStream.close()
	}

	/**
	 * 上传文件时得到一定格式的拼接字符串
	 */
	private fun getFileParamsString(file: File, fileKey: String?, fileType: String?): String {
		return LINE_END +
				TWO_HYPHENS +
				BOUNDARY +
				LINE_END +
				"Content-Disposition: form-data; name=\"" + fileKey + "\"; filename=\"" + file.name + "\"" +
				LINE_END +
				"Content-Type: " + fileType +
				LINE_END +
				"Content-Length: " + file.length() +
				LINE_END +
				LINE_END
	}

	/**
	 * 当正常返回时，得到Response对象
	 */
	private fun getRealResponse(conn: HttpURLConnection): RealResponse {
		val response = RealResponse()
		response.code = conn.getResponseCode()
		response.contentLength = conn.contentLength.toLong()
		response.inputStream = conn.getInputStream()
		response.errorStream = conn.errorStream
		return response
	}

	/**
	 * 当发生异常时，得到Response对象
	 */
	private fun getExceptionResponse(conn: HttpURLConnection?, e: Exception): RealResponse {
		conn?.disconnect()
		e.printStackTrace()
		val response = RealResponse()
		response.exception = e
		return response
	}

	companion object {
		private val BOUNDARY = UUID.randomUUID().toString()
		private const val TWO_HYPHENS = "--"
		private const val LINE_END = "\r\n"
	}
}
