package com.flux.collaboration.storage.record.plugins.performance;

import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.listener.EventListener;
import com.alibaba.jvm.sandbox.repeater.plugin.api.InvocationListener;
import com.alibaba.jvm.sandbox.repeater.plugin.api.InvocationProcessor;
import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.api.DefaultInvocationProcessor;
import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.spi.AbstractInvokePluginAdapter;
import com.alibaba.jvm.sandbox.repeater.plugin.core.model.EnhanceModel;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.RepeaterConfig;
import com.alibaba.jvm.sandbox.repeater.plugin.spi.InvokePlugin;
import com.flux.collaboration.storage.record.plugins.performance.PerformanceMonitorAgentConfig.PerformanceMonitorAgentRule;
import com.vivo.internet.moonbox.common.api.model.InvokeType;
import org.kohsuke.MetaInfServices;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Sandbox Agent 性能监控插件。
 *
 * @author yijiakang
 */
@MetaInfServices(InvokePlugin.class)
public class PerformanceMonitorPlugin extends AbstractInvokePluginAdapter {

    private static final String TRACE_MANAGER = "com.flux.collaboration.utils.trace.BusinessTraceManager";
    private volatile PerformanceMonitorAgentConfig performanceConfig;

    @Override
    public boolean enable(RepeaterConfig config) {
        performanceConfig = PerformanceMonitorBridgeInvoker.getConfig();
        return super.enable(config) && performanceConfig != null && performanceConfig.isEnabled();
    }

    @Override
    protected List<EnhanceModel> getEnhanceModels() {
        PerformanceMonitorAgentConfig currentConfig = performanceConfig;
        if (currentConfig == null) {
            currentConfig = PerformanceMonitorBridgeInvoker.getConfig();
        }
        if (currentConfig == null || !currentConfig.isEnabled()) {
            return Collections.emptyList();
        }
        List<EnhanceModel> models = new ArrayList<>();
        models.add(model(TRACE_MANAGER, "startTrace", Collections.singletonList("java.lang.String"), false));
        models.add(model(TRACE_MANAGER, "markDeletionStarted", Collections.singletonList("java.lang.String"), false));
        models.add(model(TRACE_MANAGER, "finishTrace", Collections.singletonList("java.lang.String"), false));
        if (currentConfig.getRules() != null) {
            for (PerformanceMonitorAgentRule rule : currentConfig.getRules()) {
                if (!isValidRule(rule)) {
                    continue;
                }
                models.add(model(rule.getClassName(), rule.getMethodName(), rule.getParameterTypes(),
                        rule.isIncludeSubClasses()));
            }
        }
        return models;
    }

    private boolean isValidRule(PerformanceMonitorAgentRule rule) {
        return rule != null && rule.isEnabled() && hasText(rule.getClassName()) && hasText(rule.getMethodName())
                && hasText(rule.getMetricType());
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private EnhanceModel model(String className, String methodName, List<String> parameterTypes,
            boolean includeSubClasses) {
        EnhanceModel.MethodPattern.MethodPatternBuilder methodBuilder = EnhanceModel.MethodPattern.builder()
                .methodName(methodName);
        if (parameterTypes != null && !parameterTypes.isEmpty()) {
            methodBuilder.parameterType(parameterTypes.toArray(new String[0]));
        }
        return EnhanceModel.builder()
                .classPattern(className)
                .methodPatterns(new EnhanceModel.MethodPattern[]{methodBuilder.build()})
                .includeSubClasses(includeSubClasses)
                .watchTypes(Event.Type.BEFORE, Event.Type.RETURN, Event.Type.THROWS)
                .build();
    }

    @Override
    protected InvocationProcessor getInvocationProcessor() {
        return new DefaultInvocationProcessor(getType());
    }

    @Override
    protected EventListener getEventListener(InvocationListener listener) {
        PerformanceMonitorAgentConfig currentConfig = performanceConfig;
        return new PerformanceMonitorEventListener(currentConfig == null || currentConfig.getRules() == null
                ? Collections.emptyList() : currentConfig.getRules());
    }

    @Override
    public InvokeType getType() {
        return InvokeType.DATAHUB_PERFORMANCE_MONITOR;
    }

    @Override
    public String identity() {
        return getType().getInvokeName();
    }

    @Override
    public boolean isEntrance() {
        return false;
    }
}
