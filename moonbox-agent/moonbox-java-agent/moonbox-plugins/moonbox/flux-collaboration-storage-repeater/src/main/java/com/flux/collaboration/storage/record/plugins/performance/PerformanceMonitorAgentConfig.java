package com.flux.collaboration.storage.record.plugins.performance;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent ClassLoader 内使用的性能监控配置副本。
 *
 * @author yijiakang
 */
public class PerformanceMonitorAgentConfig {

    private boolean enabled;
    private List<PerformanceMonitorAgentRule> rules = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<PerformanceMonitorAgentRule> getRules() {
        return rules;
    }

    public void setRules(List<PerformanceMonitorAgentRule> rules) {
        this.rules = rules;
    }

    public static class PerformanceMonitorAgentRule {
        private boolean enabled = true;
        private String id;
        private String className;
        private String methodName;
        private List<String> parameterTypes = new ArrayList<>();
        private boolean includeSubClasses;
        private String metricType;
        private Long slowMethodThresholdMs;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        public String getMethodName() {
            return methodName;
        }

        public void setMethodName(String methodName) {
            this.methodName = methodName;
        }

        public List<String> getParameterTypes() {
            return parameterTypes;
        }

        public void setParameterTypes(List<String> parameterTypes) {
            this.parameterTypes = parameterTypes;
        }

        public boolean isIncludeSubClasses() {
            return includeSubClasses;
        }

        public void setIncludeSubClasses(boolean includeSubClasses) {
            this.includeSubClasses = includeSubClasses;
        }

        public String getMetricType() {
            return metricType;
        }

        public void setMetricType(String metricType) {
            this.metricType = metricType;
        }

        public Long getSlowMethodThresholdMs() {
            return slowMethodThresholdMs;
        }

        public void setSlowMethodThresholdMs(Long slowMethodThresholdMs) {
            this.slowMethodThresholdMs = slowMethodThresholdMs;
        }
    }
}
