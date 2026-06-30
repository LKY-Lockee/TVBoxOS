package com.github.tvbox.osc.util

import android.content.Context
import android.content.pm.PackageManager
import android.text.TextUtils
import androidx.core.net.toUri
import com.github.tvbox.osc.api.ApiConfig
import com.github.tvbox.osc.bean.MovieSort.SortData
import com.github.tvbox.osc.server.ControlManager
import com.google.gson.JsonObject
import java.util.regex.Pattern

/**
 * @author pj567
 * @date 2020/12/21
 */
object DefaultConfig {
	private val snifferMatch: Pattern = Pattern.compile(
		"http((?!http).){12,}?\\.(m3u8|mp4|flv|avi|mkv|rm|wmv|mpg|m4a)\\?.*|" +
				"http((?!http).){12,}\\.(m3u8|mp4|flv|avi|mkv|rm|wmv|mpg|m4a)|" +
				"http((?!http).)*?video/tos*|" +
				"http((?!http).){20,}?/m3u8\\?pt=m3u8.*|" +
				"http((?!http).)*?default\\.ixigua\\.com/.*|" +
				"http((?!http).)*?dycdn-tos\\.pstatp[^?]*|" +
				"http.*?/player/m3u8play\\.php\\?url=.*|" +
				"http.*?/player/.*?[pP]lay\\.php\\?url=.*|" +
				"http.*?/playlist/m3u8/\\?vid=.*|" +
				"http.*?\\.php\\?type=m3u8&.*|" +
				"http.*?/download.aspx\\?.*|" +
				"http.*?/api/up_api.php\\?.*|" +
				"https.*?\\.66yk\\.cn.*|" +
				"http((?!http).)*?netease\\.com/file/.*"
	)
	private val NO_AD_KEYWORDS: List<String> = listOf(
		"tx", "youku", "qq", "qiyi", "letv", "leshi", "sohu", "mgtv", "bilibili", "imgo", "优酷", "芒果", "腾讯", "奇艺"
	)

	fun adjustSort(sourceKey: String?, list: MutableList<SortData>, withMy: Boolean): MutableList<SortData> {
		val data: MutableList<SortData> = ArrayList()
		if (sourceKey != null) {
			val sb = ApiConfig.instance.getSource(sourceKey) ?: return ArrayList()
			val categories = sb.categories
			if (!categories.isNullOrEmpty()) {
				for (cate in categories) {
					for (sortData in list) {
						if (sortData.name == cate) {
							data.add(sortData)
						}
					}
				}
			} else {
				data.addAll(list)
			}
		}
		if (withMy) data.add(0, SortData("my0", "主页"))
		data.sort()
		return data
	}

	fun getAppVersionCode(mContext: Context): Int {
		// 包管理操作管理类
		return try {
			val packageInfo = mContext.packageManager.getPackageInfo(mContext.packageName, 0)
			packageInfo.versionCode
		} catch (e: PackageManager.NameNotFoundException) {
			e.printStackTrace()
			-1
		}
	}

	fun getAppVersionName(mContext: Context): String {
		// 包管理操作管理类
		return try {
			val packageInfo = mContext.packageManager.getPackageInfo(mContext.packageName, 0)
			packageInfo.versionName ?: ""
		} catch (e: PackageManager.NameNotFoundException) {
			e.printStackTrace()
			""
		}
	}

	/**
	 * 后缀
	 */
	fun getFileSuffix(name: String): String {
		if (TextUtils.isEmpty(name)) {
			return ""
		}
		val endP = name.lastIndexOf(".")
		return if (endP > -1) name.substring(endP) else ""
	}

	/**
	 * 获取文件的前缀
	 */
	fun getFilePrefixName(fileName: String): String {
		if (TextUtils.isEmpty(fileName)) {
			return ""
		}
		val start = fileName.lastIndexOf(".")
		return if (start > -1) fileName.substring(0, start) else fileName
	}

	fun isVideoFormat(url: String): Boolean {
		val path = url.toUri().path
		if (TextUtils.isEmpty(path)) {
			return false
		}
		return snifferMatch.matcher(url).find()
	}

	fun safeJsonString(obj: JsonObject, key: String, defaultVal: String): String {
		return try {
			if (obj.has(key)) {
				if (obj.get(key).isJsonObject || obj.get(key).isJsonArray)
					obj.get(key).toString().trim()
				else
					obj.getAsJsonPrimitive(key).asString.trim()
			} else defaultVal
		} catch (ignored: Throwable) {
			defaultVal
		}
	}

	fun safeJsonInt(obj: JsonObject, key: String, defaultVal: Int): Int {
		return try {
			if (obj.has(key)) obj.getAsJsonPrimitive(key).asInt else defaultVal
		} catch (ignored: Throwable) {
			defaultVal
		}
	}

	fun safeJsonStringList(obj: JsonObject, key: String): ArrayList<String> {
		val result = ArrayList<String>()
		try {
			if (obj.has(key)) {
				if (obj.get(key).isJsonObject) {
					result.add(obj.get(key).asString)
				} else {
					for (opt in obj.getAsJsonArray(key)) {
						result.add(opt.asString)
					}
				}
			}
		} catch (ignored: Throwable) {
		}
		return result
	}

	fun checkReplaceProxy(urlOri: String): String {
		if (urlOri.startsWith("proxy://")) return urlOri.replace("proxy://", ControlManager.instance.getAddress(true) + "proxy?")
		return urlOri
	}

	fun noAd(flag: String?): Boolean {
		if (flag.isNullOrEmpty()) return false
		for (keyword in NO_AD_KEYWORDS) {
			if (flag == keyword || flag.contains(keyword)) {
				return true
			}
		}
		return false
	}
}
