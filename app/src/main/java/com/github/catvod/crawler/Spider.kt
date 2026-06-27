package com.github.catvod.crawler

import android.content.Context
import com.github.tvbox.osc.util.OkGoHelper
import okhttp3.Dns
import org.json.JSONObject

open class Spider {
	protected var mContext: Context? = null

	open fun init(context: Context) {
		mContext = context
	}

	open fun init(context: Context, extend: String?) {
		init(context)
	}

	/**
	 * 首页数据内容
	 *
	 * @param filter 是否开启筛选
	 */
	open fun homeContent(filter: Boolean): String {
		return ""
	}

	/**
	 * 首页最近更新数据 如果上面的homeContent中不包含首页最近更新视频的数据 可以使用这个接口返回
	 */
	open fun homeVideoContent(): String {
		return ""
	}

	/**
	 * 分类数据
	 */
	open fun categoryContent(tid: String?, pg: String?, filter: Boolean, extend: Map<String, String>?): String {
		return ""
	}

	/**
	 * 详情数据
	 */
	open fun detailContent(ids: List<String>?): String {
		return ""
	}

	/**
	 * 搜索数据内容
	 */
	open fun searchContent(key: String?, quick: Boolean): String {
		return ""
	}

	/**
	 * 播放信息
	 */
	open fun playerContent(flag: String?, id: String?, vipFlags: List<String>?): String {
		return ""
	}

	/**
	 * 直播list
	 */
	open fun liveContent(url: String?): String {
		return ""
	}

	/**
	 * webview解析时使用 可自定义判断当前加载的 url 是否是视频
	 */
	open fun isVideoFormat(url: String?): Boolean {
		return false
	}

	/**
	 * 是否手动检测webview中加载的url
	 */
	open fun manualVideoCheck(): Boolean {
		return false
	}

	/**
	 * 取消请求tag
	 */
	open fun cancelByTag() {
	}

	/**
	 * 销毁
	 */
	open fun destroy() {
	}

	/**
	 * 爬虫代理
	 */
	open fun proxyLocal(params: Map<String, String>?): Array<Any?> {
		return emptyArray()
	}

	companion object {
		var empty: JSONObject = JSONObject()

		fun safeDns(): Dns? {
			return OkGoHelper.dnsOverHttps
		}
	}
}
