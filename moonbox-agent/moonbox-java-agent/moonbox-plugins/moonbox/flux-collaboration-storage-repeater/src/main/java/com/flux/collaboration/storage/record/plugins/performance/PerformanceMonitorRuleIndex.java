package com.flux.collaboration.storage.record.plugins.performance;

import com.flux.collaboration.storage.record.plugins.performance.PerformanceMonitorAgentConfig.PerformanceMonitorAgentRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent 热路径使用的性能增强规则索引。
 *
 * @author yijiakang
 */
class PerformanceMonitorRuleIndex {

    private static final String KEY_SEPARATOR = "\u0001";
    private final Map<String, PerformanceMonitorAgentRule> directSignatureRules = new HashMap<>();
    private final Map<String, PerformanceMonitorAgentRule> directWildcardRules = new HashMap<>();
    private final Map<String, List<PerformanceMonitorAgentRule>> subclassSignatureRules = new HashMap<>();
    private final Map<String, List<PerformanceMonitorAgentRule>> subclassWildcardRules = new HashMap<>();

    PerformanceMonitorRuleIndex(List<PerformanceMonitorAgentRule> rules) {
        if (rules == null) {
            return;
        }
        for (PerformanceMonitorAgentRule rule : rules) {
            if (!isValid(rule)) {
                continue;
            }
            String parameterDescriptor = toParameterDescriptor(rule.getParameterTypes());
            if (rule.isIncludeSubClasses()) {
                Map<String, List<PerformanceMonitorAgentRule>> index = parameterDescriptor == null
                        ? subclassWildcardRules : subclassSignatureRules;
                String indexKey = parameterDescriptor == null ? rule.getMethodName()
                        : signatureKey(rule.getMethodName(), parameterDescriptor);
                index.computeIfAbsent(indexKey, key -> new ArrayList<>()).add(rule);
            } else {
                Map<String, PerformanceMonitorAgentRule> index = parameterDescriptor == null
                        ? directWildcardRules : directSignatureRules;
                String indexKey = parameterDescriptor == null ? methodKey(rule.getClassName(), rule.getMethodName())
                        : signatureKey(rule.getClassName(), rule.getMethodName(), parameterDescriptor);
                index.putIfAbsent(indexKey, rule);
            }
        }
    }

    PerformanceMonitorAgentRule find(String className, String methodName, String methodDesc, Class<?> targetClass) {
        String parameterDescriptor = extractParameterDescriptor(methodDesc);
        PerformanceMonitorAgentRule directRule =
                directSignatureRules.get(signatureKey(className, methodName, parameterDescriptor));
        if (directRule != null) {
            return directRule;
        }
        directRule = directWildcardRules.get(methodKey(className, methodName));
        if (directRule != null) {
            return directRule;
        }
        PerformanceMonitorAgentRule subclassRule = findSubclassRule(
                subclassSignatureRules.getOrDefault(signatureKey(methodName, parameterDescriptor),
                        Collections.emptyList()),
                className, targetClass);
        if (subclassRule != null) {
            return subclassRule;
        }
        return findSubclassRule(subclassWildcardRules.getOrDefault(methodName, Collections.emptyList()),
                className, targetClass);
    }

    private boolean isValid(PerformanceMonitorAgentRule rule) {
        return rule != null && rule.isEnabled() && hasText(rule.getClassName()) && hasText(rule.getMethodName())
                && hasText(rule.getMetricType());
    }

    private String methodKey(String className, String methodName) {
        return className + KEY_SEPARATOR + methodName;
    }

    private String signatureKey(String className, String methodName, String parameterDescriptor) {
        return methodKey(className, methodName) + KEY_SEPARATOR + parameterDescriptor;
    }

    private String signatureKey(String methodName, String parameterDescriptor) {
        return methodName + KEY_SEPARATOR + parameterDescriptor;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private boolean containsType(Class<?> type, String expectedClassName) {
        if (type == null) {
            return false;
        }
        if (expectedClassName.equals(type.getName())) {
            return true;
        }
        for (Class<?> interfaceType : type.getInterfaces()) {
            if (containsType(interfaceType, expectedClassName)) {
                return true;
            }
        }
        return containsType(type.getSuperclass(), expectedClassName);
    }

    private PerformanceMonitorAgentRule findSubclassRule(List<PerformanceMonitorAgentRule> rules, String className,
            Class<?> targetClass) {
        for (PerformanceMonitorAgentRule rule : rules) {
            if (className.equals(rule.getClassName()) || containsType(targetClass, rule.getClassName())) {
                return rule;
            }
        }
        return null;
    }

    private String toParameterDescriptor(List<String> parameterTypes) {
        if (parameterTypes == null || parameterTypes.isEmpty()) {
            return null;
        }
        StringBuilder descriptor = new StringBuilder("(");
        for (String parameterType : parameterTypes) {
            descriptor.append(toDescriptor(parameterType));
        }
        return descriptor.append(')').toString();
    }

    private String extractParameterDescriptor(String methodDesc) {
        int endIndex = methodDesc == null ? -1 : methodDesc.indexOf(')');
        return endIndex < 0 ? "" : methodDesc.substring(0, endIndex + 1);
    }

    private String toDescriptor(String typeName) {
        if (typeName.endsWith("[]")) {
            return "[" + toDescriptor(typeName.substring(0, typeName.length() - 2));
        }
        switch (typeName) {
            case "boolean":
                return "Z";
            case "byte":
                return "B";
            case "char":
                return "C";
            case "short":
                return "S";
            case "int":
                return "I";
            case "long":
                return "J";
            case "float":
                return "F";
            case "double":
                return "D";
            default:
                return typeName.startsWith("[") ? typeName : "L" + typeName.replace('.', '/') + ";";
        }
    }
}
