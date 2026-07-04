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
@XStreamAlias("class")
class MovieSort : Serializable {
	@XStreamImplicit(itemFieldName = "ty")
	var sortList: MutableList<SortData>? = null

	@XStreamAlias("ty")
	@XStreamConverter(value = ToAttributedValueConverter::class, strings = ["name"])
	class SortData : Serializable, Comparable<SortData> {
		val sort: Int = -1
		val select: Boolean = false
		val filterSelect: HashMap<String, String> = HashMap()

		@XStreamAsAttribute
		var id: String = ""
		var name: String = ""
		var filters: MutableList<SortFilter> = mutableListOf()

		/**
		 * 类型
		 */
		var flag: String = ""

		constructor()

		constructor(id: String, name: String) {
			this.id = id
			this.name = name
		}

		override fun compareTo(other: SortData): Int = 0

		override fun toString(): String {
			return "SortData{id='$id', name='$name', sort=$sort, select=$select, filters=$filters, filterSelect=$filterSelect, flag='$flag'}"
		}

		fun filterSelectCount(): Int {
			return filterSelect.values.count { it.isNotEmpty() }
		}
	}

	class SortFilter {
		var key: String? = null
		var name: String? = null
		var values: LinkedHashMap<String, String>? = null

		override fun toString(): String {
			return "SortFilter{key='$key', name='$name', values=$values}"
		}
	}
}
