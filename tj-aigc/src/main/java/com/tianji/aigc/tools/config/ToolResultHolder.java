package com.tianji.aigc.tools.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ToolResultHolder {

    private static final Map<String, Map<String, Object>> HANDLER_MAP = new ConcurrentHashMap<>();

    /**
     * 工具类，禁止实例化
     */
    private ToolResultHolder() {
    }

    public static void put(String key, String field, Object result) {
        HANDLER_MAP
                .computeIfAbsent(key, k -> new HashMap<>())
                .put(field, result);
    }

    public static Map<String, Object> get(String key) {
        return key == null ? null : HANDLER_MAP.get(key);
    }

    public static Object get(String key, String field) {
        return Optional.ofNullable(HANDLER_MAP.get(key))
                .map(map -> map.get(field))
                .orElse(null);
    }

    public static void remove(String key) {
        HANDLER_MAP.remove(key);
    }

}
