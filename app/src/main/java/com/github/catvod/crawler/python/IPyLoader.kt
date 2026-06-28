package com.github.catvod.crawler.python

import com.github.catvod.crawler.Spider

interface IPyLoader {
	fun clear()

	fun setConfig(jsonStr: String?)

	fun setRecentPyKey(pyApi: String?)

	fun getSpider(key: String, cls: String, ext: String): Spider

	fun proxyInvoke(params: Map<String, List<String>>): Array<Any?>?
}
