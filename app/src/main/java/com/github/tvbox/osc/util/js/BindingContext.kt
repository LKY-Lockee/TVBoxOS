package com.github.tvbox.osc.util.js

import java.lang.reflect.Method

class BindingContext {
	val functionMap: MutableMap<String, Method> = HashMap()
	var contextSetter: Method? = null
}
