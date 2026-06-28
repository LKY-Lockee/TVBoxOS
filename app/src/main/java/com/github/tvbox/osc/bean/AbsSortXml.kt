package com.github.tvbox.osc.bean

import com.thoughtworks.xstream.annotations.XStreamAlias
import java.io.Serializable

/**
 * @author pj567
 * @date 2020/12/18
 */
@XStreamAlias("rss")
class AbsSortXml : Serializable {
	@XStreamAlias("class")
	var classes: MovieSort? = null
	var list: Movie? = null
	var videoList: MutableList<Movie.Video>? = null
}
