package com.github.tvbox.osc.bean

import com.github.tvbox.osc.bean.AbsJson.AbsJsonVod
import com.github.tvbox.osc.bean.MovieSort.SortData
import com.google.gson.annotations.SerializedName
import java.io.Serializable

class AbsSortJson : Serializable {
	@SerializedName(value = "class")
	var classes: List<AbsJsonClass>? = null
	var list: List<AbsJsonVod>? = null

	fun toAbsSortXml(): AbsSortXml {
		val absSortXml = AbsSortXml()
		val movieSort = MovieSort()
		movieSort.sortList = classes.orEmpty().map { cls ->
			SortData().apply {
				id = cls.typeId
				name = cls.typeName
				flag = cls.typeFlag
			}
		}.toMutableList()

		absSortXml.list = list?.takeIf { it.isNotEmpty() }?.let { vods ->
			Movie().apply {
				videoList = vods.map { it.toXmlVideo() }.toMutableList()
			}
		}

		absSortXml.classes = movieSort
		return absSortXml
	}

	class AbsJsonClass : Serializable {
		@SerializedName("type_id")
		var typeId: String? = null

		@SerializedName("type_name")
		var typeName: String? = null

		@SerializedName("type_flag")
		var typeFlag: String? = null
	}
}
