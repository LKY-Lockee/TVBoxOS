package com.github.tvbox.osc.bean

class LiveChannelGroup {
	/**
	 * 分组索引号
	 */
	var groupIndex: Int = 0

	/**
	 * 分组名称
	 */
	var groupName: String? = null

	/**
	 * 分组密码
	 */
	var groupPassword: String? = null
	var liveChannels: List<LiveChannelItem>? = null
}
