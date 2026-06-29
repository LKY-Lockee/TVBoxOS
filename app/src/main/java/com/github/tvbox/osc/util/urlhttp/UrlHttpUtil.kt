package com.github.tvbox.osc.util.urlhttp

import com.github.tvbox.osc.util.urlhttp.CallBackUtil.CallBackBitmap
import com.github.tvbox.osc.util.urlhttp.CallBackUtil.CallBackFile
import java.io.File

/**
 * Created by fighting on 2017/4/24.
 */
object UrlHttpUtil {
	const val FILE_TYPE_FILE: String = "file/*"
	const val FILE_TYPE_IMAGE: String = "image/*"
	const val FILE_TYPE_AUDIO: String = "audio/*"
	const val FILE_TYPE_VIDEO: String = "video/*"
	private const val METHOD_GET = "GET"
	private const val METHOD_POST = "POST"

	/**
	 * get请求
	 * 
	 * @param url：url
	 * @param callBack：回调接口，onFailure方法在请求失败时调用，onResponse方法在请求成功后调用，这两个方法都执行在UI线程。
	 */
	fun get(url: String, callBack: CallBackUtil<*>) {
		get(url, null, null, callBack)
	}

	/**
	 * get请求，可以传递参数
	 * 
	 * @param url：url
	 * @param paramsMap：map集合，封装键值对参数
	 * @param callBack：回调接口，onFailure方法在请求失败时调用，onResponse方法在请求成功后调用，这两个方法都执行在UI线程。
	 */
	fun get(url: String, paramsMap: Map<String, String>?, callBack: CallBackUtil<*>) {
		get(url, paramsMap, null, callBack)
	}

	/**
	 * get请求，可以传递参数
	 * 
	 * @param url：url
	 * @param paramsMap：map集合，封装键值对参数
	 * @param headerMap：map集合，封装请求头键值对
	 * @param callBack：回调接口，onFailure方法在请求失败时调用，onResponse方法在请求成功后调用，这两个方法都执行在UI线程。
	 */
	fun get(url: String, paramsMap: Map<String, String>?, headerMap: Map<String, String>?, callBack: CallBackUtil<*>) {
		RequestUtil(METHOD_GET, url, paramsMap, headerMap, callBack).execute()
	}

	/**
	 * post请求
	 * 
	 * @param url：url
	 * @param callBack：回调接口，onFailure方法在请求失败时调用，onResponse方法在请求成功后调用，这两个方法都执行在UI线程。
	 */
	fun post(url: String, callBack: CallBackUtil<*>) {
		post(url, null, callBack)
	}

	/**
	 * post请求，可以传递参数
	 * 
	 * @param url：url
	 * @param paramsMap：map集合，封装键值对参数
	 * @param callBack：回调接口，onFailure方法在请求失败时调用，onResponse方法在请求成功后调用，这两个方法都执行在UI线程。
	 */
	fun post(url: String, paramsMap: Map<String, String>?, callBack: CallBackUtil<*>) {
		post(url, paramsMap, null, callBack)
	}

	/**
	 * post请求，可以传递参数
	 * 
	 * @param url：url
	 * @param paramsMap：map集合，封装键值对参数
	 * @param headerMap：map集合，封装请求头键值对
	 * @param callBack：回调接口，onFailure方法在请求失败时调用，onResponse方法在请求成功后调用，这两个方法都执行在UI线程。
	 */
	fun post(url: String, paramsMap: Map<String, String>?, headerMap: Map<String, String>?, callBack: CallBackUtil<*>) {
		RequestUtil(METHOD_POST, url, paramsMap, headerMap, callBack).execute()
	}

	/**
	 * post请求，可以传递参数
	 * 
	 * @param url：url
	 * @param jsonStr：json格式的键值对参数
	 * @param callBack：回调接口，onFailure方法在请求失败时调用，onResponse方法在请求成功后调用，这两个方法都执行在UI线程。
	 */
	fun postJson(url: String, jsonStr: String?, callBack: CallBackUtil<*>) {
		postJson(url, jsonStr, null, callBack)
	}

	/**
	 * post请求，可以传递参数
	 * 
	 * @param url：url
	 * @param jsonStr：json格式的键值对参数
	 * @param headerMap：map集合，封装请求头键值对
	 * @param callBack：回调接口，onFailure方法在请求失败时调用，onResponse方法在请求成功后调用，这两个方法都执行在UI线程。
	 */
	fun postJson(url: String, jsonStr: String?, headerMap: Map<String, String>?, callBack: CallBackUtil<*>) {
		RequestUtil(url, jsonStr, headerMap, callBack).execute()
	}

	/**
	 * post请求，上传单个文件
	 * 
	 * @param url：url
	 * @param file：File对象
	 * @param fileKey：上传参数时file对应的键
	 * @param fileType：File类型，是image，video，audio，file
	 * @param callBack：回调接口，onFailure方法在请求失败时调用，onResponse方法在请求成功后调用，这两个方法都执行在UI线程。还可以重写onProgress方法，得到上传进度
	 */
	fun uploadFile(url: String, file: File?, fileKey: String?, fileType: String?, callBack: CallBackUtil<*>) {
		uploadFile(url, file, fileKey, fileType, null, callBack)
	}

	/**
	 * post请求，上传单个文件
	 * 
	 * @param url：url
	 * @param file：File对象
	 * @param fileKey：上传参数时file对应的键
	 * @param fileType：File类型，是image，video，audio，file
	 * @param paramsMap：map集合，封装键值对参数
	 * @param callBack：回调接口，onFailure方法在请求失败时调用，onResponse方法在请求成功后调用，这两个方法都执行在UI线程。还可以重写onProgress方法，得到上传进度
	 */
	fun uploadFile(url: String, file: File?, fileKey: String?, fileType: String?, paramsMap: Map<String, String>?, callBack: CallBackUtil<*>) {
		uploadFile(url, file, fileKey, fileType, paramsMap, null, callBack)
	}

	/**
	 * post请求，上传单个文件
	 * 
	 * @param url：url
	 * @param file：File对象
	 * @param fileKey：上传参数时file对应的键
	 * @param fileType：File类型，是image，video，audio，file
	 * @param paramsMap：map集合，封装键值对参数
	 * @param headerMap：map集合，封装请求头键值对
	 * @param callBack：回调接口，onFailure方法在请求失败时调用，onResponse方法在请求成功后调用，这两个方法都执行在UI线程。还可以重写onProgress方法，得到上传进度
	 */
	fun uploadFile(url: String, file: File?, fileKey: String?, fileType: String?, paramsMap: Map<String, String>?, headerMap: Map<String, String>?, callBack: CallBackUtil<*>) {
		RequestUtil(url, file, null, null, fileKey, fileType, paramsMap, headerMap, callBack).execute()
	}

	/**
	 * post请求，上传多个文件，以list集合的形式
	 * 
	 * @param url：url
	 * @param fileList：集合元素是File对象
	 * @param fileKey：上传参数时fileList对应的键
	 * @param fileType：File类型，是image，video，audio，file
	 * @param callBack：回调接口，onFailure方法在请求失败时调用，onResponse方法在请求成功后调用，这两个方法都执行在UI线程。
	 */
	fun uploadListFile(url: String, fileList: List<File>?, fileKey: String?, fileType: String?, callBack: CallBackUtil<*>) {
		uploadListFile(url, fileList, fileKey, fileType, null, callBack)
	}

	/**
	 * post请求，上传多个文件，以list集合的形式
	 * 
	 * @param url：url
	 * @param fileList：集合元素是File对象
	 * @param fileKey：上传参数时fileList对应的键
	 * @param fileType：File类型，是image，video，audio，file
	 * @param paramsMap：map集合，封装键值对参数
	 * @param callBack：回调接口，onFailure方法在请求失败时调用，onResponse方法在请求成功后调用，这两个方法都执行在UI线程。
	 */
	fun uploadListFile(url: String, fileList: List<File>?, fileKey: String?, fileType: String?, paramsMap: Map<String, String>?, callBack: CallBackUtil<*>) {
		uploadListFile(url, fileList, fileKey, fileType, paramsMap, null, callBack)
	}

	/**
	 * post请求，上传多个文件，以list集合的形式
	 * 
	 * @param url：url
	 * @param fileList：集合元素是File对象
	 * @param fileKey：上传参数时fileList对应的键
	 * @param fileType：File类型，是image，video，audio，file
	 * @param paramsMap：map集合，封装键值对参数
	 * @param headerMap：map集合，封装请求头键值对
	 * @param callBack：回调接口，onFailure方法在请求失败时调用，onResponse方法在请求成功后调用，这两个方法都执行在UI线程。
	 */
	fun uploadListFile(url: String, fileList: List<File>?, fileKey: String?, fileType: String?, paramsMap: Map<String, String>?, headerMap: Map<String, String>?, callBack: CallBackUtil<*>) {
		RequestUtil(url, null, fileList, null, fileKey, fileType, paramsMap, headerMap, callBack).execute()
	}

	/**
	 * post请求，上传多个文件，以map集合的形式
	 * 
	 * @param url：url
	 * @param fileMap：集合key是File对象对应的键，集合value是File对象
	 * @param fileType：File类型，是image，video，audio，file
	 * @param callBack：回调接口，onFailure方法在请求失败时调用，onResponse方法在请求成功后调用，这两个方法都执行在UI线程。
	 */
	fun uploadMapFile(url: String, fileMap: Map<String, File>?, fileType: String?, callBack: CallBackUtil<*>) {
		uploadMapFile(url, fileMap, fileType, null, callBack)
	}

	/**
	 * post请求，上传多个文件，以map集合的形式
	 * 
	 * @param url：url
	 * @param fileMap：集合key是File对象对应的键，集合value是File对象
	 * @param fileType：File类型，是image，video，audio，file
	 * @param paramsMap：map集合，封装键值对参数
	 * @param callBack：回调接口，onFailure方法在请求失败时调用，onResponse方法在请求成功后调用，这两个方法都执行在UI线程。
	 */
	fun uploadMapFile(url: String, fileMap: Map<String, File>?, fileType: String?, paramsMap: Map<String, String>?, callBack: CallBackUtil<*>) {
		uploadMapFile(url, fileMap, fileType, paramsMap, null, callBack)
	}

	/**
	 * post请求，上传多个文件，以map集合的形式
	 * 
	 * @param url：url
	 * @param fileMap：集合key是File对象对应的键，集合value是File对象
	 * @param fileType：File类型，是image，video，audio，file
	 * @param paramsMap：map集合，封装键值对参数
	 * @param headerMap：map集合，封装请求头键值对
	 * @param callBack：回调接口，onFailure方法在请求失败时调用，onResponse方法在请求成功后调用，这两个方法都执行在UI线程。
	 */
	fun uploadMapFile(url: String, fileMap: Map<String, File>?, fileType: String?, paramsMap: Map<String, String>?, headerMap: Map<String, String>?, callBack: CallBackUtil<*>) {
		RequestUtil(url, null, null, fileMap, null, fileType, paramsMap, headerMap, callBack).execute()
	}

	/**
	 * 加载图片
	 */
	fun getBitmap(url: String, callBack: CallBackBitmap) {
		getBitmap(url, null, callBack)
	}

	/**
	 * 加载图片，带参数
	 */
	fun getBitmap(url: String, paramsMap: Map<String, String>?, callBack: CallBackBitmap) {
		get(url, paramsMap, null, callBack)
	}

	/**
	 * 下载文件,不带参数
	 */
	fun downloadFile(url: String, callBack: CallBackFile) {
		downloadFile(url, null, callBack)
	}

	/**
	 * 下载文件,带参数
	 */
	fun downloadFile(url: String, paramsMap: Map<String, String>?, callBack: CallBackFile) {
		downloadFile(url, paramsMap, null, callBack)
	}

	/**
	 * 下载文件,带参数,带请求头
	 */
	fun downloadFile(url: String, paramsMap: Map<String, String>?, headerMap: Map<String, String>?, callBack: CallBackFile) {
		get(url, paramsMap, headerMap, callBack)
	}
}
