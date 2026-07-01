package com.github.tvbox.osc.bean

import com.thoughtworks.xstream.annotations.XStreamAlias
import com.thoughtworks.xstream.annotations.XStreamAsAttribute
import com.thoughtworks.xstream.annotations.XStreamConverter
import com.thoughtworks.xstream.annotations.XStreamImplicit
import com.thoughtworks.xstream.converters.extended.ToAttributedValueConverter
import java.io.Serializable

/**
 * @author pj567
 * @date 2020/12/18
 */
@XStreamAlias("list")
class Movie : Serializable {
	@XStreamAsAttribute
	var page: Int = 0

	/**
	 * 总页数
	 */
	@XStreamAsAttribute
	@XStreamAlias("pagecount")
	var pageCount: Int = 0

	@XStreamAsAttribute
	@XStreamAlias("pagesize")
	var pageSize: Int = 0

	/**
	 * 总条数
	 */
	@XStreamAsAttribute
	@XStreamAlias("recordcount")
	var recordCount: Int = 0

	@XStreamImplicit(itemFieldName = "video")
	var videoList: MutableList<Video>? = null

	@XStreamAlias("video")
	class Video : Serializable {
		/**
		 * 时间
		 */
		var last: String? = null

		/**
		 * 内容id
		 */
		var id: String? = null

		/**
		 * 父级id
		 */
		var tid: Int = 0

		/**
		 * 影片名称
		 */
		var name: String? = null // <![CDATA[老爸当家]]>

		/**
		 * 类型名称
		 */
		var type: String? = null

		/**
		 * 图片
		 */
		var pic: String? = null

		/**
		 * 语言
		 */
		var lang: String? = null

		/**
		 * 地区
		 */
		var area: String? = null

		/**
		 * 年份
		 */
		var year: Int = 0
		var state: String? = null

		/**
		 * 描述集数或者影片信息
		 */
		var note: String? = null // <![CDATA[共40集]]>

		/**
		 * 演员
		 */
		var actor: String? = null // <![CDATA[张国立,蒋欣,高鑫,曹艳艳,王维维,韩丹彤,孟秀,王新]]>

		/**
		 * 导演
		 */
		var director: String? = null // <![CDATA[陈国星]]>

		@XStreamAlias("dl")
		var urlBean: UrlBean? = null

		var des: String? = null // <![CDATA[权来]
		var sourceKey: String? = null
		var tag: String? = null

		@XStreamAlias("dl")
		class UrlBean : Serializable {
			@XStreamImplicit(itemFieldName = "dd")
			var infoList: MutableList<UrlInfo>? = null

			@XStreamAlias("dd")
			@XStreamConverter(value = ToAttributedValueConverter::class, strings = ["urls"])
			class UrlInfo : Serializable {
				@XStreamAsAttribute
				var flag: String? = null // zuidam3u8,zuidall(MP4)
				var urls: String? = null // <![CDATA[第01集$http://video.zuidajiexi.com/20170825/txpkmcnK/index.m3u8#第02集$http://video.zuidajiexi.com/20170825/YOApVCHc/index.m3u8]]
				var beanList: MutableList<InfoBean>? = null

				class InfoBean(var name: String, val url: String) : Serializable
			}
		}
	}
}
