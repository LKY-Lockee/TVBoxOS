package com.github.tvbox.osc.cache

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * @author pj567
 * @date 2021/1/7
 */
@Entity(tableName = "vodRecord")
class VodRecord {
	@PrimaryKey(autoGenerate = true)
	var id: Int = 0

	@ColumnInfo(name = "vodId")
	var vodId: String = ""

	@ColumnInfo(name = "updateTime")
	var updateTime: Long = 0

	@ColumnInfo(name = "sourceKey")
	var sourceKey: String = ""

	var dataJson: String = ""
}
