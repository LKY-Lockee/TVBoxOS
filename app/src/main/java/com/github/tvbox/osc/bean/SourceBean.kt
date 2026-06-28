package com.github.tvbox.osc.bean

class SourceBean {
	var key: String? = null
	var name: String? = null
	var api: String? = null

	/**
	 * 0 xml
	 * 
	 * 1 json
	 * 
	 * 3 Spider
	 */
	var type: Int = 0

	/**
	 * 是否可以站点选择
	 */
	var filterable: Int = 0

	/**
	 * 站点解析Url
	 */
	var playerUrl: String? = null

	/**
	 * 扩展数据
	 */
	var ext: String? = null

	/**
	 * 自定义jar
	 */
	var jar: String? = null

	/**
	 * 分类&排序
	 */
	var categories: List<String>? = null

	/**
	 * 0 system
	 * 
	 * 1 ikj
	 * 
	 * 2 exo
	 * 
	 * 10 mxplayer
	 * 
	 * -1 以参数设置页面的为准
	 */
	var playerType: Int = 0

	/**
	 * 需要点击播放的嗅探站点selector
	 * 
	 * ddrk.me;#id
	 */
	var clickSelector: String? = null

	/**
	 * 展示风格
	 */
	var style: String? = null

	/**
	 * 是否可搜索
	 */
	var searchable: Int = 0

	/**
	 * 是否可以快速搜索
	 */
	var quickSearch: Int = 0

	val isSearchable: Boolean
		get() = searchable != 0

	val isQuickSearch: Boolean
		get() = quickSearch != 0
}
