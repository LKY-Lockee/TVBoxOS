package com.github.tvbox.osc.cache

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * @author pj567
 * @since 2020/5/15
 */
@Entity(tableName = "cache")
class Cache(
	@PrimaryKey val key: String,
	val data: ByteArray
)
