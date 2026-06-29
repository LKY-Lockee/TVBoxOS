package com.github.tvbox.osc.util.urlhttp

import android.text.TextUtils
import java.io.File
import java.io.UnsupportedEncodingException
import java.net.HttpURLConnection
import java.net.URLEncoder

/**
 * Created by fighting on 2017/4/24.
 */
internal class RequestUtil {
	private var mThread: Thread? = null

	/**
	 * 一般的get请求或post请求
	 */
	constructor(method: String, url: String, paramsMap: Map<String, String>?, headerMap: Map<String, String>?, callBack: CallBackUtil<*>) {
		when (method) {
			"GET" -> urlHttpGet(url, paramsMap, headerMap, callBack)
			"POST" -> urlHttpPost(url, paramsMap, null, headerMap, callBack)
		}
	}

	/**
	 * post请求，传递json格式数据。
	 */
	constructor(url: String, jsonStr: String?, headerMap: Map<String, String>?, callBack: CallBackUtil<*>) {
		urlHttpPost(url, null, jsonStr, headerMap, callBack)
	}

	/**
	 * 上传文件
	 */
	constructor(url: String, file: File?, fileList: List<File>?, fileMap: Map<String, File>?, fileKey: String?, fileType: String?, paramsMap: Map<String, String>?, headerMap: Map<String, String>?, callBack: CallBackUtil<*>) {
		urlHttpUploadFile(url, file, fileList, fileMap, fileKey, fileType, paramsMap, headerMap, callBack)
	}

	/**
	 * get请求
	 */
	private fun urlHttpGet(url: String, paramsMap: Map<String, String>?, headerMap: Map<String, String>?, callBack: CallBackUtil<*>) {
		mThread = Thread {
			val response = RealRequest().getData(getUrl(url, paramsMap), headerMap)
			if (response.code == HttpURLConnection.HTTP_OK) {
				callBack.onSuccess(response)
			} else {
				callBack.onError(response)
			}
		}
	}

	/**
	 * post请求
	 */
	private fun urlHttpPost(url: String, paramsMap: Map<String, String>?, jsonStr: String?, headerMap: Map<String, String>?, callBack: CallBackUtil<*>) {
		mThread = Thread {
			val response = RealRequest().postData(url, getPostBody(paramsMap, jsonStr), getPostBodyType(paramsMap, jsonStr), headerMap)
			if (response.code == HttpURLConnection.HTTP_OK) {
				callBack.onSuccess(response)
			} else {
				callBack.onError(response)
			}
		}
	}

	/**
	 * 上传文件
	 */
	private fun urlHttpUploadFile(url: String, file: File?, fileList: List<File>?, fileMap: Map<String, File>?, fileKey: String?, fileType: String?, paramsMap: Map<String, String>?, headerMap: Map<String, String>?, callBack: CallBackUtil<*>) {
		mThread = Thread {
			val response = RealRequest().uploadFile(url, file, fileList, fileMap, fileKey, fileType, paramsMap, headerMap, callBack)
			if (response.code == HttpURLConnection.HTTP_OK) {
				callBack.onSuccess(response)
			} else {
				callBack.onError(response)
			}
		}
	}

	/**
	 * get请求，将键值对凭接到url上
	 */
	private fun getUrl(path: String, paramsMap: Map<String, String>?): String {
		var result = path
		if (paramsMap != null) {
			val pathBuilder = StringBuilder("$path?")
			for ((key, value) in paramsMap) {
				pathBuilder.append(key).append("=").append(value).append("&")
			}
			result = pathBuilder.toString()
			result = result.substring(0, result.length - 1)
		}
		return result
	}

	/**
	 * 得到post请求的body
	 */
	private fun getPostBody(params: Map<String, String>?, jsonStr: String?): String? { //throws UnsupportedEncodingException {
		if (params != null) {
			return getPostBodyFormParamMap(params)
		} else if (!TextUtils.isEmpty(jsonStr)) {
			return jsonStr
		}
		return null
	}

	/**
	 * 根据键值对参数得到body
	 */
	private fun getPostBodyFormParamMap(params: Map<String, String>): String? { //throws UnsupportedEncodingException {
		val result = StringBuilder()
		var first = true
		try {
			for ((key, value) in params) {
				if (first) first = false
				else result.append("&")
				result.append(URLEncoder.encode(key, "UTF-8"))
				result.append("=")
				result.append(URLEncoder.encode(value, "UTF-8"))
			}
			return result.toString()
		} catch (e: UnsupportedEncodingException) {
			return null
		}
	}

	/**
	 * 得到bodyType
	 */
	private fun getPostBodyType(paramsMap: Map<String, String>?, jsonStr: String?): String? {
		if (paramsMap != null) {
			//return "text/plain";不知为什么这儿总是报错。目前暂不设置(20170424)
			return null
		} else if (!TextUtils.isEmpty(jsonStr)) {
			return "application/json;charset=utf-8"
		}
		return null
	}

	/**
	 * 开启子线程，调用run方法
	 */
	fun execute() {
		mThread?.start()
	}
}
