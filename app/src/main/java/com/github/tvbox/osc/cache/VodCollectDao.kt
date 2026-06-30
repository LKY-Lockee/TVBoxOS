package com.github.tvbox.osc.cache

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * @author pj567
 * @date 2021/1/7
 */
@Dao
interface VodCollectDao {
	@Insert(onConflict = OnConflictStrategy.REPLACE)
	fun insert(record: VodCollect): Long

	@Delete
	fun delete(record: VodCollect): Int

	@Query("delete from vodCollect where `id`=:id")
	fun delete(id: Int)

	@Query("delete from vodCollect")
	fun deleteAll()

	@Query("select * from vodCollect where `id`=:id")
	fun getVodCollect(id: Int): VodCollect?

	@Query("select * from vodCollect where `sourceKey`=:sourceKey and `vodId`=:vodId")
	fun getVodCollect(sourceKey: String, vodId: String): VodCollect?

	@Query("select * from vodCollect order by updateTime desc")
	fun getAll(): List<VodCollect>
}
