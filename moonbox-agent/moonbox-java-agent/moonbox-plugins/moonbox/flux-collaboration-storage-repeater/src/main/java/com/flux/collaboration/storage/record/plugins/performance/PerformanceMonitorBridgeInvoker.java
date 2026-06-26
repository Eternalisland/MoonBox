package com.flux.collaboration.storage.record.plugins.performance;

import com.alibaba.fastjson.JSON;
import com.alibaba.jvm.sandbox.repeater.plugin.core.spring.SpringContextAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通过 Spring 反射桥调用业务 ClassLoader 中的性能监控实现。
 *
 * @author yijiakang
 */
public final class PerformanceMonitorBridgeInvoker {

    /**
     * V100(20260626) yijiakang 【功能完善】流程定时器绑定性能链路最终监控流水号，增加本地缓存与 Rust JNI 性能统计字段 本地缓存之后 redis和mongodb操作耗时变得无意义，需要对齐本地缓存性能统计，任务号:20260522102904000460
     */
    @SuppressWarnings("unused")
    private static final String verNum = "V100(20260626)";

    private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceMonitorBridgeInvoker.class);
    private static final String BRIDGE_BEAN_NAME = "datahubPerformanceMonitorBridge";
    private static final Map<String, Method> METHOD_CACHE = new ConcurrentHashMap<>();
    private static volatile Object bridge;
    private static volatile Class<?> bridgeClass;

    private PerformanceMonitorBridgeInvoker() {
    }

    public static PerformanceMonitorAgentConfig getConfig() {
        try {
            Object json = invoke("getConfigJson", new Class<?>[0]);
            return json == null ? null : JSON.parseObject(String.valueOf(json), PerformanceMonitorAgentConfig.class);
        } catch (Exception e) {
            LOGGER.warn("读取性能监控配置失败", e);
            return null;
        }
    }

    public static void startTrace(String traceId) {
        invokeQuietly("startTrace", new Class<?>[]{String.class}, traceId);
    }

    public static void bindMessageGroupSysId(String traceId, String messageGroupSysId) {
        invokeQuietly("bindMessageGroupSysId", new Class<?>[]{String.class, String.class}, traceId, messageGroupSysId);
    }

    public static String beginSpan(String className, String methodName, String signature, String metricType,
            long slowMethodThresholdMs) {
        try {
            Object token = invoke("beginSpan",
                    new Class<?>[]{String.class, String.class, String.class, String.class, long.class},
                    className, methodName, signature, metricType, slowMethodThresholdMs);
            return token == null ? null : String.valueOf(token);
        } catch (Exception e) {
            LOGGER.warn("创建性能监控 Span 失败: {}", signature, e);
            return null;
        }
    }

    public static void endSpan(String token, String exceptionType) {
        invokeQuietly("endSpan", new Class<?>[]{String.class, String.class}, token, exceptionType);
    }

    public static void flushTrace(String traceId) {
        invokeQuietly("flushTrace", new Class<?>[]{String.class}, traceId);
    }

    public static void flushTrace(String traceId, String lifecycleExceptionType) {
        invokeQuietly("flushTrace", new Class<?>[]{String.class, String.class}, traceId, lifecycleExceptionType);
    }

    public static void clearTrace(String traceId) {
        invokeQuietly("clearTrace", new Class<?>[]{String.class}, traceId);
    }

    private static Object getBridge() {
        Object currentBridge = bridge;
        if (currentBridge == null) {
            currentBridge = SpringContextAdapter.getBeanByName(BRIDGE_BEAN_NAME);
            if (currentBridge != null) {
                synchronized (PerformanceMonitorBridgeInvoker.class) {
                    if (bridgeClass != currentBridge.getClass()) {
                        METHOD_CACHE.clear();
                        bridgeClass = currentBridge.getClass();
                    }
                    bridge = currentBridge;
                }
            }
        }
        return currentBridge;
    }

    private static Object invoke(String methodName, Class<?>[] parameterTypes, Object... arguments) throws Exception {
        Object currentBridge = getBridge();
        if (currentBridge == null) {
            return null;
        }
        String cacheKey = methodName + "#" + parameterTypes.length;
        Method method = METHOD_CACHE.get(cacheKey);
        if (method == null) {
            method = currentBridge.getClass().getMethod(methodName, parameterTypes);
            method.setAccessible(true);
            METHOD_CACHE.put(cacheKey, method);
        }
        return method.invoke(currentBridge, arguments);
    }

    private static void invokeQuietly(String methodName, Class<?>[] parameterTypes, Object... arguments) {
        try {
            invoke(methodName, parameterTypes, arguments);
        } catch (Exception e) {
            LOGGER.warn("调用性能监控桥失败: {}", methodName, e);
        }
    }
}
