package com.github.tvbox.osc.bean

import com.thoughtworks.xstream.annotations.XStreamAlias
import java.io.Serializable

/**
 * @author pj567
 * @date 2020/12/18
 */
@XStreamAlias("rss")
class AbsXml : Serializable {
	@XStreamAlias("list")
	var movie: Movie? = null
	var msg: String? = null
}
