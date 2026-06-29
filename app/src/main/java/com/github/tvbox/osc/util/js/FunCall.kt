package com.github.tvbox.osc.util.js

import com.whl.quickjs.wrapper.JSCallFunction
import com.whl.quickjs.wrapper.JSObject
import java.util.concurrent.Callable

class FunCall private constructor(private val jsObject: JSObject, private val name: String?, private vararg val args: Any?) : Callable<Any?> {
	private var result: Any? = null
	private val jsCallFunction: JSCallFunction = JSCallFunction { args -> args[0].also { result = it } }

	override fun call(): Any? {
		val func = jsObject.getJSFunction(name) ?: return null
		result = func.call(*args)
		if (result !is JSObject) return result
		val promise = result as JSObject
		val then = promise.getJSFunction("then")
		if (then != null) then.call(jsCallFunction)
		return result
	}

	companion object {
		fun call(jsObject: JSObject, name: String?, vararg args: Any?): FunCall {
			return FunCall(jsObject, name, *args)
		}
	}
}
