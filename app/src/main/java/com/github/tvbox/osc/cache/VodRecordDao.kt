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
interface VodRecordDao {
	@Query("select count(*) from vodRecord")
	fun getCount(): Int

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	fun insert(record: VodRecord): Long

	@Delete
	fun delete(record: VodRecord): Int

	@Query("delete from vodRecord")
	fun deleteAll()

	@Query("select * from vodRecord where `sourceKey`=:sourceKey and `vodId`=:vodId")
	fun getVodRecord(sourceKey: String, vodId: String): VodRecord?

	@Query("select * from vodRecord order by updateTime desc limit :size")
	fun getAll(size: Int): List<VodRecord>

	/**
	 * 保留最新指定条数, 其他删除.
	 * 
	 * @param size 保留条数
	 */
	@Query("delete from vodRecord where id not in (select id from vodRecord order by updateTime desc limit :size)")
	fun reserver(size: Int): Int
}
