package com.github.tvbox.osc.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File

private val Context.dataStore by preferencesDataStore(name = "tvbox_prefs")

object PreferenceStore {
	private var appContext: Context? = null
	private val scope = CoroutineScope(Dispatchers.IO)

	@PublishedApi
	internal val gson = Gson()

	@PublishedApi
	internal val context: Context
		get() = appContext ?: throw IllegalStateException("PreferenceStore not initialized. Call PreferenceStore.init(context) first.")

	fun init(context: Context) {
		appContext = context.applicationContext
	}

	fun get(key: String, default: Int): Int {
		return runBlocking {
			context.dataStore.data.first()[intPreferencesKey(key)] ?: default
		}
	}

	fun put(key: String, value: Int) {
		scope.launch {
			context.dataStore.edit { prefs ->
				prefs[intPreferencesKey(key)] = value
			}
		}
	}

	fun get(key: String, default: Boolean): Boolean {
		return runBlocking {
			context.dataStore.data.first()[booleanPreferencesKey(key)] ?: default
		}
	}

	fun put(key: String, value: Boolean) {
		scope.launch {
			context.dataStore.edit { prefs ->
				prefs[booleanPreferencesKey(key)] = value
			}
		}
	}

	fun get(key: String, default: String): String {
		return runBlocking {
			context.dataStore.data.first()[stringPreferencesKey(key)] ?: default
		}
	}

	fun put(key: String, value: String?) {
		if (value == null) {
			delete(key)
			return
		}
		scope.launch {
			context.dataStore.edit { prefs ->
				prefs[stringPreferencesKey(key)] = value
			}
		}
	}

	fun get(key: String, default: Float): Float {
		return runBlocking {
			context.dataStore.data.first()[floatPreferencesKey(key)] ?: default
		}
	}

	fun put(key: String, value: Float) {
		scope.launch {
			context.dataStore.edit { prefs ->
				prefs[floatPreferencesKey(key)] = value
			}
		}
	}

	fun get(key: String, default: Long): Long {
		return runBlocking {
			context.dataStore.data.first()[longPreferencesKey(key)] ?: default
		}
	}

	fun put(key: String, value: Long) {
		scope.launch {
			context.dataStore.edit { prefs ->
				prefs[longPreferencesKey(key)] = value
			}
		}
	}

	fun get(key: String, default: Set<String>): Set<String> {
		return runBlocking {
			context.dataStore.data.first()[stringSetPreferencesKey(key)] ?: default
		}
	}

	fun put(key: String, value: Set<String>) {
		scope.launch {
			context.dataStore.edit { prefs ->
				prefs[stringSetPreferencesKey(key)] = value
			}
		}
	}

	inline fun <reified T> getObj(key: String, default: T): T {
		val json = get(key, "")
		if (json.isEmpty()) return default
		return try {
			gson.fromJson(json, T::class.java)
		} catch (e: Exception) {
			default
		}
	}

	inline fun <reified T> putObj(key: String, value: T?) {
		if (value == null) {
			delete(key)
			return
		}
		put(key, gson.toJson(value))
	}

	fun delete(key: String) {
		scope.launch {
			context.dataStore.edit { prefs ->
				prefs.remove(stringPreferencesKey(key))
				prefs.remove(intPreferencesKey(key))
				prefs.remove(booleanPreferencesKey(key))
				prefs.remove(floatPreferencesKey(key))
				prefs.remove(longPreferencesKey(key))
				prefs.remove(stringSetPreferencesKey(key))
			}
		}
	}

	fun contains(key: String): Boolean {
		return runBlocking {
			val prefs = context.dataStore.data.first()
			prefs.contains(stringPreferencesKey(key)) ||
					prefs.contains(intPreferencesKey(key)) ||
					prefs.contains(booleanPreferencesKey(key)) ||
					prefs.contains(floatPreferencesKey(key)) ||
					prefs.contains(longPreferencesKey(key))
		}
	}

	/**
	 * Flush pending writes and copy the DataStore file to [dest].
	 */
	fun exportFile(dest: File) {
		runBlocking {
			context.dataStore.edit { /* no-op to flush pending writes */ }
		}
		val prefsFile = File(context.filesDir, "datastore/tvbox_prefs.preferences_pb")
		if (prefsFile.exists()) {
			prefsFile.copyTo(dest, overwrite = true)
		}
	}

	/**
	 * Copy a backed-up DataStore file back to the app's datastore directory.
	 * 
	 * App should restart afterward for the new file to take effect.
	 */
	fun importFile(src: File) {
		if (src.exists()) {
			val prefsFile = File(context.filesDir, "datastore/tvbox_prefs.preferences_pb")
			prefsFile.parentFile?.mkdirs()
			src.copyTo(prefsFile, overwrite = true)
		}
	}
}
