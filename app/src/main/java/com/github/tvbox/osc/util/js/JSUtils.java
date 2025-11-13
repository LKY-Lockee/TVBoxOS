package com.github.tvbox.osc.util.js;

import com.whl.quickjs.wrapper.JSArray;
import com.whl.quickjs.wrapper.JSFunction;
import com.whl.quickjs.wrapper.JSObject;
import com.whl.quickjs.wrapper.QuickJSContext;
import com.whl.quickjs.wrapper.QuickJSException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class JSUtils<T> {
    public static boolean isEmpty(Object obj) {
        if (obj == null) return true;
        else if (obj instanceof CharSequence) return ((CharSequence) obj).length() == 0;
        else if (obj instanceof Collection) return ((Collection<?>) obj).isEmpty();
        else if (obj instanceof Map) return ((Map<?, ?>) obj).isEmpty();
        else if (obj.getClass().isArray()) return Array.getLength(obj) == 0;

        return false;
    }

    public static boolean isNotEmpty(CharSequence str) {
        return !isEmpty(str);
    }

    public static boolean isNotEmpty(Object obj) {
        return !isEmpty(obj);
    }

    public static void checkRefCountIsZero(JSObject obj) {
        if (obj.isRefCountZero()) {
            throw new QuickJSException("The call threw an exception, the reference count of the current object has already reached zero.");
        }
    }

    public static JSONArray toJsonArray(JSArray arr) {
        JSONArray jsonArray = new JSONArray();
        for (int i = 0; i < arr.length(); i++) {
            Object obj = arr.get(i);
            if (obj == null || obj instanceof JSFunction) {
                continue;
            }
            if (obj instanceof Number || obj instanceof String || obj instanceof Boolean) {
                jsonArray.put(obj);
            } else if (obj instanceof JSArray) {
                jsonArray.put(toJsonArray((JSArray) obj));
            } else if (obj instanceof JSObject) {
                jsonArray.put(toJsonObject((JSObject) obj));
            }
        }
        return jsonArray;
    }

    public static String toJsonString(JSObject obj) {
        return obj.getContext().stringify(obj);
    }

    public static JSONObject toJsonObject(JSObject obj) {
        checkRefCountIsZero(obj);

        JSONObject jsonObject = new JSONObject();
        JSONArray json = toJsonArray(obj.getNames());
        for (int i = 0; i < json.length(); i++) {
            String key = json.optString(i);
            Object o = obj.getProperty(key);
            if (o == null || o instanceof JSFunction) {
                continue;
            }
            if (o instanceof Number || o instanceof String || o instanceof Boolean) {
                try {
                    jsonObject.put(key, o);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else if (o instanceof JSArray) {
                try {
                    jsonObject.put(key, toJsonArray((JSArray) o));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else if (o instanceof JSObject) {
                try {
                    jsonObject.put(key, toJsonObject((JSObject) o));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return jsonObject;
    }

    public JSArray toArray(QuickJSContext ctx, List<T> items) {
        JSArray array = ctx.createNewJSArray();
        if (items == null || items.isEmpty()) return array;
        for (int i = 0; i < items.size(); i++) {
            array.set(items.get(i), i);
        }
        return array;
    }

    public JSArray toArray(QuickJSContext ctx, byte[] bytes) {
        JSArray array = ctx.createNewJSArray();
        if (bytes == null) return array;
        for (int i = 0; i < bytes.length; i++) {
            array.set((int) bytes[i], i);
        }
        return array;
    }

    public JSArray toArray(QuickJSContext ctx, T[] arrays) {
        JSArray array = ctx.createNewJSArray();
        if (arrays == null) return array;
        for (int i = 0; i < arrays.length; i++) {
            array.set(arrays[i], i);
        }
        return array;
    }

    public JSObject toObj(QuickJSContext ctx, Map<String, T> map) {
        JSObject obj = ctx.createNewJSObject();
        if (map == null || map.isEmpty()) return obj;
        for (String s : map.keySet()) {
            ctx.setProperty(obj, s, map.get(s));
        }
        return obj;
    }
}
