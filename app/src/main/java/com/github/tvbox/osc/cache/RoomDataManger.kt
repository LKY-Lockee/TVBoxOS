package com.github.tvbox.osc.cache

import com.github.tvbox.osc.bean.VodInfo
import com.github.tvbox.osc.data.AppDataManager
import com.github.tvbox.osc.util.HawkConfig
import com.github.tvbox.osc.util.HistoryHelper
import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.orhanobut.hawk.Hawk

/**
 * @author pj567
 * @date :2021/1/7
 */
object RoomDataManger {
	private val vodInfoStrategy = object : ExclusionStrategy {
		override fun shouldSkipField(field: FieldAttributes): Boolean {
			return field.declaringClass == VodInfo::class.java && field.name in setOf("seriesFlags", "seriesMap")
		}

		override fun shouldSkipClass(clazz: Class<*>): Boolean {
			return false
		}
	}

	private val vodInfoGson: Gson by lazy {
		GsonBuilder().addSerializationExclusionStrategy(vodInfoStrategy).create()
	}

	fun insertVodRecord(sourceKey: String, vodInfo: VodInfo) {
		var record = AppDataManager.get().getVodRecordDao().getVodRecord(sourceKey, vodInfo.id)
		if (record == null) {
			record = VodRecord()
		}
		record.sourceKey = sourceKey
		record.vodId = vodInfo.id
		record.updateTime = System.currentTimeMillis()
		record.dataJson = vodInfoGson.toJson(vodInfo)
		AppDataManager.get().getVodRecordDao().insert(record)
	}

	fun insertVodCollect(sourceKey: String, vodInfo: VodInfo) {
		val existing = AppDataManager.get().getVodCollectDao().getVodCollect(sourceKey, vodInfo.id)
		if (existing != null) {
			return
		}
		val record = VodCollect()
		record.sourceKey = sourceKey
		record.vodId = vodInfo.id
		record.updateTime = System.currentTimeMillis()
		record.name = vodInfo.name.orEmpty()
		record.pic = vodInfo.pic.orEmpty()
		AppDataManager.get().getVodCollectDao().insert(record)
	}

	fun deleteVodRecord(sourceKey: String, vodInfo: VodInfo) {
		val record = AppDataManager.get().getVodRecordDao().getVodRecord(sourceKey, vodInfo.id)
		if (record != null) {
			AppDataManager.get().getVodRecordDao().delete(record)
		}
	}

	fun deleteVodCollect(id: Int) {
		AppDataManager.get().getVodCollectDao().delete(id)
	}

	fun deleteVodCollect(sourceKey: String, vodInfo: VodInfo) {
		val record = AppDataManager.get().getVodCollectDao().getVodCollect(sourceKey, vodInfo.id)
		if (record != null) {
			AppDataManager.get().getVodCollectDao().delete(record)
		}
	}

	fun deleteVodRecordAll() {
		AppDataManager.get().getVodRecordDao().deleteAll()
	}

	fun deleteVodCollectAll() {
		AppDataManager.get().getVodCollectDao().deleteAll()
	}

	fun getVodInfo(sourceKey: String, vodId: String): VodInfo? {
		val record = AppDataManager.get().getVodRecordDao().getVodRecord(sourceKey, vodId)
		try {
			if (record != null && record.dataJson.isNotEmpty()) {
				val vodInfo = vodInfoGson.fromJson<VodInfo>(record.dataJson, object : TypeToken<VodInfo>() {}.type)
				if (vodInfo.name == null) return null
				return vodInfo
			}
		} catch (e: Exception) {
			e.printStackTrace()
		}
		return null
	}

	fun getAllVodRecord(limit: Int): List<VodInfo> {
		val count = AppDataManager.get().getVodRecordDao().getCount()
		val index = Hawk.get(HawkConfig.HISTORY_NUM, 0)
		val hisNum = HistoryHelper.getHisNum(index)
		if (count > hisNum) {
			AppDataManager.get().getVodRecordDao().reserver(hisNum)
		}
		val recordList = AppDataManager.get().getVodRecordDao().getAll(limit)
		val vodInfoList = mutableListOf<VodInfo>()
		for (record in recordList) {
			try {
				if (record.dataJson.isNotEmpty()) {
					val info = vodInfoGson.fromJson<VodInfo>(record.dataJson, object : TypeToken<VodInfo>() {}.type)
					info.sourceKey = record.sourceKey
					if (info.name != null) {
						vodInfoList.add(info)
					}
				}
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}
		return vodInfoList
	}

	fun getAllVodCollect(): List<VodCollect> {
		return AppDataManager.get().getVodCollectDao().getAll()
	}

	fun isVodCollect(sourceKey: String, vodId: String): Boolean {
		val record = AppDataManager.get().getVodCollectDao().getVodCollect(sourceKey, vodId)
		return record != null
	}
}
