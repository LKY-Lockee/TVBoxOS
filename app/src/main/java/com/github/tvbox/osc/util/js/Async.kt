package com.github.tvbox.osc.util.js

import com.google.common.util.concurrent.SettableFuture
import com.whl.quickjs.wrapper.JSCallFunction
import com.whl.quickjs.wrapper.JSObject

class Async private constructor() {
	private val future: SettableFuture<Any?> = SettableFuture.create()
	private val callback: JSCallFunction = JSCallFunction { args -> // args[0] holds the resolved value from the JS promise
		future.set(if (args.size > 0) args[0] else null)
		null
	}

	private fun call(`object`: JSObject, name: String?, args: Array<Any?>): SettableFuture<Any?> {
		try {
			val function = `object`.getJSFunction(name)
			if (function == null) {
				future.set(null)
				return future
			}
			val result = function.call(*args)
			if (result is JSObject) {
				then(result)
			} else {
				future.set(result)
			}
		} catch (t: Throwable) {
			future.setException(t)
		}
		return future
	}

	private fun then(result: Any?) {
		val promise = result as JSObject
		val thenFn = promise.getJSFunction("then")
		if (thenFn != null) {
			thenFn.call(callback)
		} else {
			// If there's no then, complete immediately
			future.set(result)
		}
	}

	companion object {
		fun run(`object`: JSObject, name: String?, args: Array<Any?>): SettableFuture<Any?> {
			return Async().call(`object`, name, args)
		}
	}
}
