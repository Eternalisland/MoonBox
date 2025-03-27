package com.flux.collaboration.storage.record.plugins.netty;

import com.alibaba.jvm.sandbox.repeater.plugin.core.bridge.ClassloaderBridge;
import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.AbstractRepeater;
import com.alibaba.jvm.sandbox.repeater.plugin.core.spring.SpringContextAdapter;
import com.alibaba.jvm.sandbox.repeater.plugin.core.trace.Tracer;
import com.alibaba.jvm.sandbox.repeater.plugin.core.utils.MethodSignatureParser;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.RepeatContext;
import com.alibaba.jvm.sandbox.repeater.plugin.exception.RepeatException;
import com.alibaba.jvm.sandbox.repeater.plugin.spi.Repeater;
import com.vivo.internet.moonbox.common.api.model.HttpInvocation;
import com.vivo.internet.moonbox.common.api.model.Identity;
import com.vivo.internet.moonbox.common.api.model.Invocation;
import com.vivo.internet.moonbox.common.api.model.InvokeType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.kohsuke.MetaInfServices;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Administrator
 * @date 2025/2/6 14:10
 * @description netty 服务回放器 功能描述
 */
@Slf4j
@MetaInfServices(Repeater.class)
public class NettyHttpBaseRepeater extends AbstractRepeater {


    @Override
    public InvokeType getType() {
        return InvokeType.NETTY_HTTP;
    }

    @Override
    public String identity() {
        return InvokeType.NETTY_HTTP.getInvokeName();
    }

    @Override
    protected Object executeRepeat(RepeatContext context) throws Exception {

        Invocation invocation = context.getRecordModel().getEntranceInvocation();
        if (!getType().equals(invocation.getType())) {
            throw new RepeatException("invoke type miss match, required invoke type is: " + invocation.getType());
        }
        Identity identity = invocation.getIdentity();
        String endpoint = identity.getEndpoint();
        // array[0]=/methodName
        String methodName = getTargetMethod(endpoint);

        String location = identity.getLocation();

        Object collaborationServicesFactory = SpringContextAdapter.getBeanByType("com.flux.collaboration.services.base.impl.CollaborationServicesFactory");
        Class<?> collaborationServicesFactoryCls = collaborationServicesFactory.getClass();
        Class<?> AopUtilsClass = ClassloaderBridge.instance().findClassInstance("org.springframework.aop.support.AopUtils");;
        boolean isAopPoxy = (boolean) MethodUtils.invokeStaticMethod(AopUtilsClass, "isAopProxy", collaborationServicesFactory);
        if(isAopPoxy) {
            collaborationServicesFactoryCls = (Class<?>) MethodUtils.invokeStaticMethod(AopUtilsClass, "getTargetClass", collaborationServicesFactory);
        }
        Field field = (collaborationServicesFactoryCls).getDeclaredField("serversTypesMap");
        field.setAccessible(true);
        Map<String,Map<String,Map<String,Object>>> serversTypesMap = ( Map<String,Map<String,Map<String,Object>>>) field.get(null);
        List<Object> allObjects = serversTypesMap.values().stream()
                 // 第一层 Map 的 Value 流（第二层 Map）
                .map( sv -> {
                    Iterator<Map.Entry<String, Map<String, Object>>> iterator = sv.entrySet().iterator();
                    while (iterator.hasNext()) {
                        Map.Entry<String, Map<String, Object>> next = iterator.next();
                        String key = next.getKey();
                        if(!key.equals("48") && !key.equals("49")  && !key.equals("50")) {
                            iterator.remove();
                        }
                    }
                    return sv;
                })
                .flatMap(secondMap -> secondMap != null ? secondMap.values().stream() : Stream.empty()) // 展开第二层 Map 的 Value 流（第三层 Map）
                .flatMap(thirdMap -> thirdMap != null ? thirdMap.values().stream() : Stream.empty()) // 展开第三层 Map 的 Value 流（Object）
                .collect(Collectors.toList());

        Map<String,Object> serverHandler = new HashMap<>();
        for (Object serviceObject : allObjects) {

            isAopPoxy = (boolean) MethodUtils.invokeStaticMethod(AopUtilsClass, "isAopProxy", serviceObject);
            if(isAopPoxy) {
                serviceObject = MethodUtils.invokeMethod(serviceObject,"getTargetSource");
                serviceObject = MethodUtils.invokeMethod(serviceObject,"getTarget");
            }
            Field servicesHandlerField = serviceObject.getClass().getDeclaredField("servicesHandler");
            servicesHandlerField.setAccessible(true);
            Map<String,Map<String, Object>> servicesHandler = (Map<String, Map<String, Object>>) servicesHandlerField.get(serviceObject);
            List<Object> serverLists = servicesHandler.values().stream()
                    .flatMap(secondMap -> secondMap != null ? secondMap.values().stream() : Stream.empty())
                    .collect(Collectors.toList());
            for (Object serverHandle : serverLists) {

                Field nettyChannelInitializerField = serverHandle.getClass().getDeclaredField("nettyChannelInitializer");
                nettyChannelInitializerField.setAccessible(true);
                Object nettyChannelInitializer = nettyChannelInitializerField.get(serverHandle);

                Field httpServerHandlerField = nettyChannelInitializer.getClass().getDeclaredField("httpServerHandler");
                httpServerHandlerField.setAccessible(true);
                Object httpServerHandler = httpServerHandlerField.get(nettyChannelInitializer);

                Field  httpProcessorField = httpServerHandler.getClass().getDeclaredField("httpProcessor");
                httpProcessorField.setAccessible(true);
                Map<String,Object > httpProcessor = (Map<String, Object>) httpProcessorField.get(httpServerHandler);
                serverHandler.putAll(httpProcessor);
            }
        }
       String requestURI =  (String)((HttpInvocation)context.getRecordModel().getEntranceInvocation()).getHeaders().get("REQUEST_URI");
        if(!requestURI.startsWith("http")) {
            requestURI = "http://" + requestURI;
        }
        String uriStr = new URI(requestURI).getPath();

        String questServiceUri = uriStr;
        String [] uri = uriStr.split("/");

        Object bean = null;
        if (serverHandler.containsKey(uriStr)){
            //全url 匹配
            questServiceUri = uriStr;
            bean = serverHandler.get(questServiceUri);
        }else if (serverHandler.containsKey(uriStr.substring(0, uriStr.lastIndexOf("/")))){
            //匹配最后一个 url / 之前
            questServiceUri = uriStr.substring(0, uriStr.lastIndexOf("/"));
            bean = serverHandler.get(questServiceUri);
        }
        if (bean == null) {
            bean = JavaInstanceCache.getInstance(identity.getLocation());
        }
        if (bean == null) {
            throw new RepeatException("no bean found in context, className=" + identity.getLocation());
        }
        ClassLoader classLoader = ClassloaderBridge.instance().decode(invocation.getSerializeToken());
        if (classLoader == null) {
            classLoader = ClassLoader.getSystemClassLoader();
        }

        // 这里需要兼容 没有参数的方法 的情况，例如 public void nonArgsMethod(){}，此时 array[1] 会抛出数组越界的情况
        Method method;

        Class<?>[] parameterTypes = new Class[] {
                classLoader.loadClass("java.util.Map"),
                classLoader.loadClass("java.util.Map"),
                classLoader.loadClass("java.lang.String"),
        };
        method =  findTargetMethod(bean, methodName, parameterTypes);
        if( method == null ) {
            throw new RepeatException(identity.getLocation() + "has no method =" + methodName);
        }
        // 这里没法办像HTTP、DUBBO透传traceId，因此在执行前先执行Trace.start()，根据traceId创建好TraceContext，避免在
        Tracer.start(context.getTraceId());

        Object[] httpRequest = invocation.getRequest();

        Map<String,Object> httpParamMap = (Map<String, Object>) httpRequest[0];

        Object headers = httpParamMap.get("headers");
        Object paramsMap = httpParamMap.get("paramsMap");
        Object body = httpParamMap.get("body");
        // 开始invoke
        Object result = method.invoke(bean, headers, paramsMap, body);
        try {
            String responseBody = (String) MethodUtils.invokeMethod(result, "getResponseBody");
            Object httpStatus = (Object) MethodUtils.invokeMethod(result, "getHttpStatus");
            int statusCode = (int) MethodUtils.invokeMethod(httpStatus, "code");
            if(statusCode >= 400) {
                responseBody = "http response status is: " + statusCode + ", " + responseBody ;
            }
            return responseBody ;
        } catch (Exception e) {
            log.error(e.getMessage(),e);
        }
        return result;
    }

    private Method findTargetMethod (Object bean, String methodName, Class<?>... parameterType) {
        // 获取对象的类
        Class<?> clazz = bean.getClass();
        // 递归查找方法
        while (clazz != null) {
            try {
                // 尝试获取当前类的声明的方法
                if(parameterType == null ){
                    return clazz.getDeclaredMethod(methodName);
                }else {
                    return clazz.getDeclaredMethod(methodName,parameterType);
                }
            } catch (NoSuchMethodException e) {
                // 如果当前类没有该方法，则继续查找父类
                clazz = clazz.getSuperclass();
            }
        }
        // 如果未找到，返回null
        return null;
    }


    /**
     * 获得即将回放的java方法
     */
    private String getTargetMethod(String methodName) {
        String method = methodName.substring(1);
        // 这里 method 可能为 method()，不知道是不是录制的地方有地方冗余了，这里先简单处理下
        int leftBracketIndex = method.indexOf("(");
        if (leftBracketIndex != -1) {
            method = method.substring(0, leftBracketIndex);
        }
        return method;
    }
}
