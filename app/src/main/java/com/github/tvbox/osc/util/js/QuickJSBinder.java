package com.github.tvbox.osc.util.js;

import com.whl.quickjs.wrapper.JSObject;
import com.whl.quickjs.wrapper.QuickJSException;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * QuickJS 绑定工具类
 * 用于将 Java 对象的方法自动注册到 JavaScript 环境
 * 替代原来的 JSObject.bind() 方法
 */
public class QuickJSBinder {
    private static final ConcurrentHashMap<Class<?>, BindingContext> bindingContextMap = new ConcurrentHashMap<>();

    /**
     * 将 Java 对象绑定到 JSObject
     * 自动扫描并注册带 @Function 注解的方法
     *
     * @param jsObject         JavaScript 对象
     * @param callbackReceiver Java 对象实例
     * @throws QuickJSException 绑定失败时抛出
     */
    public static void bind(JSObject jsObject, Object callbackReceiver) throws QuickJSException {
        Objects.requireNonNull(jsObject, "jsObject cannot be null");
        Objects.requireNonNull(callbackReceiver, "callbackReceiver cannot be null");

        JSUtils.checkRefCountIsZero(jsObject);

        BindingContext bindingContext = getBindingContext(callbackReceiver.getClass());
        Map<String, Method> functionMap = bindingContext.getFunctionMap();

        // 1. 处理 @ContextSetter 注解 - 注入 QuickJSContext
        Method contextSetter = bindingContext.getContextSetter();
        if (contextSetter != null) {
            try {
                contextSetter.invoke(callbackReceiver, jsObject.getContext());
            } catch (Exception e) {
                throw new QuickJSException("Failed to invoke context setter: " + e.getMessage());
            }
        }

        // 2. 处理 @Function 注解 - 注册方法到 JS 环境
        if (!functionMap.isEmpty()) {
            for (Map.Entry<String, Method> entry : functionMap.entrySet()) {
                String functionName = entry.getKey();
                final Method functionMethod = entry.getValue();
                try {
                    jsObject.setProperty(functionName, args -> {
                        try {
                            return functionMethod.invoke(callbackReceiver, args);
                        } catch (Exception e) {
                            throw new QuickJSException(e.getMessage());
                        }
                    });
                } catch (Exception e) {
                    throw new QuickJSException(e.getMessage());
                }
            }
        }
    }

    /**
     * 获取类的绑定上下文（带缓存）
     *
     * @param callbackReceiverClass Java 类
     * @return 绑定上下文
     * @throws QuickJSException 解析失败时抛出
     */
    private static BindingContext getBindingContext(Class<?> callbackReceiverClass) throws QuickJSException {
        Objects.requireNonNull(callbackReceiverClass, "callbackReceiverClass cannot be null");

        BindingContext bindingContext = bindingContextMap.get(callbackReceiverClass);
        if (bindingContext == null) {
            bindingContext = new BindingContext();
            Map<String, Method> functionMap = bindingContext.getFunctionMap();

            // 扫描所有公共方法
            for (Method method : callbackReceiverClass.getMethods()) {
                boolean methodHandled = false;

                // 处理 @Function 注解
                Function functionAnnotation = method.getAnnotation(Function.class);
                if (functionAnnotation != null) {
                    String functionName = functionAnnotation.name();
                    if (functionName == null || functionName.isEmpty()) {
                        functionName = method.getName();
                    }
                    if (!functionMap.containsKey(functionName)) {
                        functionMap.put(functionName, method);
                        methodHandled = true;
                    }
                }

                // 处理 @ContextSetter 注解
                if (!methodHandled) {
                    ContextSetter contextSetterAnnotation = method.getAnnotation(ContextSetter.class);
                    if (contextSetterAnnotation != null) {
                        bindingContext.setContextSetter(method);
                    }
                }
            }

            bindingContextMap.put(callbackReceiverClass, bindingContext);
        }
        return bindingContext;
    }

    /**
     * 清除绑定上下文缓存
     * 在类重新加载或修改后可调用此方法
     *
     * @param clazz 要清除的类，如果为 null 则清除所有
     */
    public static void clearCache(Class<?> clazz) {
        if (clazz == null) {
            bindingContextMap.clear();
        } else {
            bindingContextMap.remove(clazz);
        }
    }

    /**
     * 获取已缓存的绑定上下文数量
     *
     * @return 缓存数量
     */
    public static int getCacheSize() {
        return bindingContextMap.size();
    }
}

