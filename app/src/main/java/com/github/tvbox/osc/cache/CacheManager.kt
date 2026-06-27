package com.github.tvbox.osc.cache

import com.github.tvbox.osc.data.AppDataManager
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

/**
 * @author pj567
 * @since 2020/5/15
 */
object CacheManager {
	private fun toObject(data: ByteArray): Any? {
		try {
			ByteArrayInputStream(data).use { bais ->
				ObjectInputStream(bais).use { ois ->
					try {
						return ois.readObject()
					} catch (e: Exception) {
						e.printStackTrace()
					}
				}
			}
		} catch (e: Exception) {
			e.printStackTrace()
		}
		return null
	}

	private fun <T> toByteArray(body: T): ByteArray {
		try {
			ByteArrayOutputStream().use { baos ->
				ObjectOutputStream(baos).use { oos ->
					try {
						oos.writeObject(body)
						oos.flush()
						return baos.toByteArray()
					} catch (e: Exception) {
						e.printStackTrace()
					}
				}
			}
		} catch (e: Exception) {
			e.printStackTrace()
		}
		return ByteArray(0)
	}

	fun <T> delete(key: String, body: T) {
		val cache = Cache(key, toByteArray(body))
		AppDataManager.get().cacheDao.delete(cache)
	}

	fun <T> save(key: String, body: T) {
		val cache = Cache(key, toByteArray(body))
		AppDataManager.get().cacheDao.save(cache)
	}

	fun getCache(key: String): Any? {
		val cache = AppDataManager.get().cacheDao.getCache(key)
		if (cache != null && cache.data.isNotEmpty()) {
			return toObject(cache.data)
		}
		return null
	}
}
