package com.github.tvbox.osc.cache

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vodCollect")
class VodCollect {
	@PrimaryKey(autoGenerate = true)
	var id: Int = 0

	@ColumnInfo(name = "vodId")
	var vodId: String = ""

	@ColumnInfo(name = "updateTime")
	var updateTime: Long = 0

	@ColumnInfo(name = "sourceKey")
	var sourceKey: String = ""

	@ColumnInfo(name = "name")
	var name: String = ""

	@ColumnInfo(name = "pic")
	var pic: String = ""
}
