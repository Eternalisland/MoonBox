package com.flux.collaboration.storage.record.plugins.performance;

import com.alibaba.jvm.sandbox.api.event.BeforeEvent;
import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.event.ReturnEvent;
import com.alibaba.jvm.sandbox.api.event.ThrowsEvent;
import com.alibaba.jvm.sandbox.api.listener.EventListener;
import com.flux.collaboration.storage.record.plugins.performance.PerformanceMonitorAgentConfig.PerformanceMonitorAgentRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 不执行入参/返回值序列化的轻量性能事件监听器。
 *
 * @author yijiakang
 */
public class PerformanceMonitorEventListener implements EventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceMonitorEventListener.class);
    private static final String TRACE_MANAGER = "com.flux.collaboration.utils.trace.BusinessTraceManager";
    private final Map<Integer, InvocationState> states = new ConcurrentHashMap<>();
    private final PerformanceMonitorRuleIndex ruleIndex;

    public PerformanceMonitorEventListener(List<PerformanceMonitorAgentRule> rules) {
        this.ruleIndex = new PerformanceMonitorRuleIndex(rules == null ? Collections.emptyList() : rules);
    }

    @Override
    public void onEvent(Event event) {
        try {
            switch (event.type) {
                case BEFORE:
                    onBefore((BeforeEvent) event);
                    break;
                case RETURN:
                    onReturn((ReturnEvent) event);
                    break;
                case THROWS:
                    onThrows((ThrowsEvent) event);
                    break;
                default:
                    break;
            }
        } catch (Throwable throwable) {
            // 性能监控必须失败开放，不能影响业务方法。
            LOGGER.warn("性能监控 Agent 事件处理失败", throwable);
        }
    }

    private void onBefore(BeforeEvent event) {
        if (TRACE_MANAGER.equals(event.javaClassName)) {
            String traceId = getTraceId(event.argumentArray);
            if (traceId != null) {
                states.put(event.invokeId, InvocationState.lifecycle(event.javaMethodName, traceId));
            }
            return;
        }
        PerformanceMonitorAgentRule rule = findRule(event);
        if (rule == null) {
            return;
        }
        String signature = event.javaClassName + "#" + event.javaMethodName + event.javaMethodDesc;
        long slowThreshold = rule.getSlowMethodThresholdMs() == null ? 0L : rule.getSlowMethodThresholdMs();
        String token = PerformanceMonitorBridgeInvoker.beginSpan(event.javaClassName, event.javaMethodName, signature,
                rule.getMetricType(), slowThreshold);
        if (token != null) {
            states.put(event.invokeId, InvocationState.span(token));
        }
    }

    private void onReturn(ReturnEvent event) {
        InvocationState state = states.remove(event.invokeId);
        if (state == null) {
            return;
        }
        if (state.token != null) {
            PerformanceMonitorBridgeInvoker.endSpan(state.token, null);
        } else if ("startTrace".equals(state.lifecycleMethod)) {
            PerformanceMonitorBridgeInvoker.startTrace(state.traceId);
        } else if ("markDeletionStarted".equals(state.lifecycleMethod)) {
            PerformanceMonitorBridgeInvoker.flushTrace(state.traceId);
        } else if ("finishTrace".equals(state.lifecycleMethod)) {
            PerformanceMonitorBridgeInvoker.clearTrace(state.traceId);
        }
    }

    private void onThrows(ThrowsEvent event) {
        InvocationState state = states.remove(event.invokeId);
        if (state == null) {
            return;
        }
        if (state.token != null) {
            PerformanceMonitorBridgeInvoker.endSpan(state.token, getExceptionType(event));
        } else if ("markDeletionStarted".equals(state.lifecycleMethod)) {
            PerformanceMonitorBridgeInvoker.flushTrace(state.traceId, getExceptionType(event));
        } else if ("finishTrace".equals(state.lifecycleMethod)) {
            PerformanceMonitorBridgeInvoker.clearTrace(state.traceId);
        }
    }

    private String getExceptionType(ThrowsEvent event) {
        return event.throwable == null ? Throwable.class.getName() : event.throwable.getClass().getName();
    }

    private PerformanceMonitorAgentRule findRule(BeforeEvent event) {
        return ruleIndex.find(event.javaClassName, event.javaMethodName, event.javaMethodDesc,
                event.target == null ? null : event.target.getClass());
    }

    private String getTraceId(Object[] arguments) {
        return arguments != null && arguments.length > 0 && arguments[0] != null
                ? String.valueOf(arguments[0]) : null;
    }

    private static final class InvocationState {
        private final String token;
        private final String lifecycleMethod;
        private final String traceId;

        private InvocationState(String token, String lifecycleMethod, String traceId) {
            this.token = token;
            this.lifecycleMethod = lifecycleMethod;
            this.traceId = traceId;
        }

        private static InvocationState span(String token) {
            return new InvocationState(token, null, null);
        }

        private static InvocationState lifecycle(String method, String traceId) {
            return new InvocationState(null, method, traceId);
        }
    }
}
