package com.github.tvbox.osc.util.js

import com.whl.quickjs.wrapper.JSArray
import com.whl.quickjs.wrapper.JSFunction
import com.whl.quickjs.wrapper.JSObject
import com.whl.quickjs.wrapper.QuickJSContext
import com.whl.quickjs.wrapper.QuickJSException
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.lang.reflect.Array as ReflectArray

class JSUtils<T> {
	fun toArray(ctx: QuickJSContext, items: List<T>?): JSArray {
		val array = ctx.createNewJSArray()
		if (items.isNullOrEmpty()) return array
		for (i in items.indices) {
			array.set(items[i], i)
		}
		return array
	}

	fun toArray(ctx: QuickJSContext, bytes: ByteArray?): JSArray {
		val array = ctx.createNewJSArray()
		if (bytes == null) return array
		for (i in bytes.indices) {
			array.set(bytes[i].toInt(), i)
		}
		return array
	}

	fun toArray(ctx: QuickJSContext, arrays: Array<T?>?): JSArray {
		val array = ctx.createNewJSArray()
		if (arrays == null) return array
		for (i in arrays.indices) {
			array.set(arrays[i], i)
		}
		return array
	}

	fun toObj(ctx: QuickJSContext, map: Map<String, T>?): JSObject {
		val obj = ctx.createNewJSObject()
		if (map.isNullOrEmpty()) return obj
		for ((key, value) in map) {
			ctx.setProperty(obj, key, value)
		}
		return obj
	}

	companion object {
		fun isEmpty(obj: Any?): Boolean {
			if (obj == null) return true
			else if (obj is CharSequence) return obj.isEmpty()
			else if (obj is Collection<*>) return obj.isEmpty()
			else if (obj is Map<*, *>) return obj.isEmpty()
			else if (obj.javaClass.isArray) return ReflectArray.getLength(obj) == 0

			return false
		}

		fun isNotEmpty(str: CharSequence?): Boolean {
			return !isEmpty(str)
		}

		fun isNotEmpty(obj: Any?): Boolean {
			return !isEmpty(obj)
		}

		fun checkRefCountIsZero(obj: JSObject) {
			if (obj.isRefCountZero) {
				throw QuickJSException("The call threw an exception, the reference count of the current object has already reached zero.")
			}
		}

		fun toJsonArray(arr: JSArray): JSONArray {
			val jsonArray = JSONArray()
			for (i in 0..<arr.length()) {
				val obj = arr.get(i)
				if (obj == null || obj is JSFunction) {
					continue
				}
				when (obj) {
					is Number, is String, is Boolean -> {
						jsonArray.put(obj)
					}

					is JSArray -> {
						jsonArray.put(toJsonArray(obj))
					}

					is JSObject -> {
						jsonArray.put(toJsonObject(obj))
					}
				}
			}
			return jsonArray
		}

		fun toJsonString(obj: JSObject): String? {
			return obj.context.stringify(obj)
		}

		fun toJsonObject(obj: JSObject): JSONObject {
			checkRefCountIsZero(obj)

			val jsonObject = JSONObject()
			val json: JSONArray = toJsonArray(obj.names)
			for (i in 0..<json.length()) {
				val key = json.optString(i)
				val o = obj.getProperty(key)
				if (o == null || o is JSFunction) {
					continue
				}
				when (o) {
					is Number, is String, is Boolean -> {
						try {
							jsonObject.put(key, o)
						} catch (e: JSONException) {
							e.printStackTrace()
						}
					}

					is JSArray -> {
						try {
							jsonObject.put(key, toJsonArray(o))
						} catch (e: JSONException) {
							e.printStackTrace()
						}
					}

					is JSObject -> {
						try {
							jsonObject.put(key, toJsonObject(o))
						} catch (e: JSONException) {
							e.printStackTrace()
						}
					}
				}
			}
			return jsonObject
		}
	}
}
