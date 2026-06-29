package com.github.tvbox.osc.util.js

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.json.JSONObject

object Json {
	fun valid(text: String): Boolean {
		try {
			JSONObject(text)
			return true
		} catch (e: Exception) {
			return false
		}
	}

	fun invalid(text: String): Boolean {
		return !valid(text)
	}

	fun safeString(obj: JsonObject, key: String?): String {
		return try {
			obj.getAsJsonPrimitive(key).asString.trim { it <= ' ' }
		} catch (e: Exception) {
			""
		}
	}

	fun safeListString(obj: JsonObject, key: String?): List<String> {
		val result: MutableList<String> = ArrayList()
		if (!obj.has(key)) return result
		if (obj.get(key).isJsonObject) result.add(safeString(obj, key))
		else for (opt in obj.getAsJsonArray(key)) result.add(opt.asString)
		return result
	}

	fun safeListElement(obj: JsonObject, key: String?): List<JsonElement> {
		val result: MutableList<JsonElement> = ArrayList()
		if (!obj.has(key)) return result
		if (obj.get(key).isJsonObject) result.add(obj.get(key).getAsJsonObject())
		for (opt in obj.getAsJsonArray(key)) result.add(opt.getAsJsonObject())
		return result
	}

	fun safeObject(element: JsonElement): JsonObject {
		var element = element
		try {
			if (element.isJsonPrimitive) element = JsonParser.parseString(element.getAsJsonPrimitive().asString)
			return element.getAsJsonObject()
		} catch (e: Exception) {
			return JsonObject()
		}
	}

	fun toMap(element: JsonElement): MutableMap<String, String> {
		val map: MutableMap<String, String> = HashMap()
		val `object` = safeObject(element)
		for (key in `object`.keySet()) map[key] = safeString(`object`, key)
		return map
	}
}
