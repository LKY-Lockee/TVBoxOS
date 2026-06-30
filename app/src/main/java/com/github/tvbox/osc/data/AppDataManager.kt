package com.github.tvbox.osc.data

import androidx.room.Room.databaseBuilder
import androidx.room.RoomDatabase.JournalMode
import com.github.tvbox.osc.base.App
import com.github.tvbox.osc.util.FileUtils
import java.io.File

/**
 * @author pj567
 * @since 2020/5/15
 */
object AppDataManager {
	private var dbInstance: AppDataBase? = null
	private const val DB_FILE_VERSION = 3
	private const val DB_NAME = "tvbox"

	fun dbPath(): String = "$DB_NAME.v$DB_FILE_VERSION.db"

	fun get(): AppDataBase {
		return dbInstance ?: databaseBuilder(App.instance, AppDataBase::class.java, dbPath())
			.setJournalMode(JournalMode.TRUNCATE)
			.allowMainThreadQueries() //可以在主线程操作
			.build()
			.also { dbInstance = it }
	}

	fun backup(path: File): Boolean {
		dbInstance?.let { db ->
			if (db.isOpen) {
				db.close()
			}
		}
		val db: File = App.instance.getDatabasePath(dbPath())
		return if (db.exists()) {
			FileUtils.copyFile(db, path)
			true
		} else {
			false
		}
	}

	fun restore(path: File): Boolean {
		dbInstance?.let { db ->
			if (db.isOpen) {
				db.close()
			}
		}
		val db: File = App.instance.getDatabasePath(dbPath())
		if (db.exists()) {
			db.delete()
		}
		db.parentFile?.let { parent ->
			if (!parent.exists()) parent.mkdirs()
		}
		FileUtils.copyFile(path, db)
		return true
	}
}
