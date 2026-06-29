package com.github.tvbox.osc.util.js

import com.whl.quickjs.wrapper.JSObject
import com.whl.quickjs.wrapper.QuickJSException
import java.util.concurrent.ConcurrentHashMap

/**
 * QuickJS 绑定工具类
 * 用于将 Java 对象的方法自动注册到 JavaScript 环境
 * 替代原来的 JSObject.bind() 方法
 */
object QuickJSBinder {
	private val bindingContextMap = ConcurrentHashMap<Class<*>, BindingContext>()

	/**
	 * 获取已缓存的绑定上下文数量
	 */
	val cacheSize: Int
		get() = bindingContextMap.size

	/**
	 * 将 Java 对象绑定到 JSObject
	 * 
	 * 自动扫描并注册带 @Function 注解的方法
	 * 
	 * @param jsObject JavaScript 对象
	 * @param callbackReceiver Java 对象实例
	 */
	fun bind(jsObject: JSObject, callbackReceiver: Any) {
		JSUtils.checkRefCountIsZero(jsObject)

		val bindingContext = getBindingContext(callbackReceiver.javaClass)
		val functionMap = bindingContext.functionMap

		// 1. 处理 @ContextSetter 注解 - 注入 QuickJSContext
		val contextSetter = bindingContext.contextSetter
		if (contextSetter != null) {
			try {
				contextSetter.invoke(callbackReceiver, jsObject.context)
			} catch (e: Exception) {
				throw QuickJSException("Failed to invoke context setter: " + e.message)
			}
		}

		// 2. 处理 @Function 注解 - 注册方法到 JS 环境
		if (functionMap.isNotEmpty()) {
			for (entry in functionMap.entries) {
				val functionName = entry.key
				val functionMethod = entry.value
				try {
					jsObject.setProperty(functionName) { args: Array<Any?> ->
						try {
							return@setProperty functionMethod.invoke(callbackReceiver, *args)
						} catch (e: Exception) {
							throw QuickJSException(e.message)
						}
					}
				} catch (e: Exception) {
					throw QuickJSException(e.message)
				}
			}
		}
	}

	/**
	 * 获取类的绑定上下文（带缓存）
	 * 
	 * @param callbackReceiverClass Java 类
	 * @return 绑定上下文
	 */
	private fun getBindingContext(callbackReceiverClass: Class<*>): BindingContext {
		var bindingContext = bindingContextMap[callbackReceiverClass]
		if (bindingContext == null) {
			bindingContext = BindingContext()
			val functionMap = bindingContext.functionMap

			// 扫描所有公共方法
			for (method in callbackReceiverClass.methods) {
				var methodHandled = false

				// 处理 @Function 注解
				val functionAnnotation = method.getAnnotation(Function::class.java)
				if (functionAnnotation != null) {
					var functionName = functionAnnotation.name
					if (functionName.isEmpty()) {
						functionName = method.name
					}
					if (!functionMap.containsKey(functionName)) {
						functionMap[functionName] = method
						methodHandled = true
					}
				}

				// 处理 @ContextSetter 注解
				if (!methodHandled) {
					val contextSetterAnnotation = method.getAnnotation(ContextSetter::class.java)
					if (contextSetterAnnotation != null) {
						bindingContext.contextSetter = method
					}
				}
			}

			bindingContextMap[callbackReceiverClass] = bindingContext
		}
		return bindingContext
	}

	/**
	 * 清除绑定上下文缓存
	 * 
	 * 在类重新加载或修改后可调用此方法
	 * 
	 * @param clazz 要清除的类，如果为 null 则清除所有
	 */
	fun clearCache(clazz: Class<*>?) {
		if (clazz == null) {
			bindingContextMap.clear()
		} else {
			bindingContextMap.remove(clazz)
		}
	}
}
