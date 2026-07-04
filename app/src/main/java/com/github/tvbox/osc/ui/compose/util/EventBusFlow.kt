package com.github.tvbox.osc.ui.compose.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * 将 EventBus 事件转为 Compose 可观察的 [StateFlow]。
 * 使用 catch-all 订阅者接收所有事件并按类型 [T] 过滤，生命周期内自动注册/注销。
 */
@Composable
inline fun <reified T : Any> rememberEventBusEvents(
	noinline filter: (T) -> Boolean = { true }
): StateFlow<T?> {
	val flow = remember { MutableStateFlow<T?>(null) }
	DisposableEffect(T::class) {
		val subscriber = CatchAllSubscriber { event ->
			if (event is T && filter(event)) flow.value = event
		}
		EventBus.getDefault().register(subscriber)
		onDispose { EventBus.getDefault().unregister(subscriber) }
	}
	return flow
}

/**
 * 订阅 [T] 类型事件并以回调形式通知；生命周期内自动注册/注销。
 */
@Composable
inline fun <reified T : Any> rememberEventBusCallback(
	noinline onEvent: (T) -> Unit
) {
	DisposableEffect(T::class) {
		val subscriber = CatchAllSubscriber { event -> if (event is T) onEvent(event) }
		EventBus.getDefault().register(subscriber)
		onDispose { EventBus.getDefault().unregister(subscriber) }
	}
}

/**
 * 通用 EventBus 订阅者：接收所有事件并转发给 [handle]。
 */
class CatchAllSubscriber(private val handle: (Any) -> Unit) {
	@Subscribe(threadMode = ThreadMode.MAIN)
	fun onAnyEvent(event: Any) {
		handle(event)
	}
}
