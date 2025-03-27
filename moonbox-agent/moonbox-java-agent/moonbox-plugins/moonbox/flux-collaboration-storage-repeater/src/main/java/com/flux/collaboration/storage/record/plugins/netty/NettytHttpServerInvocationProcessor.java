package com.flux.collaboration.storage.record.plugins.netty;

import com.alibaba.jvm.sandbox.api.event.BeforeEvent;
import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.event.ReturnEvent;
import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.api.DefaultInvocationProcessor;
import com.alibaba.jvm.sandbox.repeater.plugin.utils.ParameterTypesUtil;
import com.vivo.internet.moonbox.common.api.model.Identity;
import com.vivo.internet.moonbox.common.api.model.InvokeType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;

/**
 * @author yijiakang
 * @date 2025/1/26 15:05
 * @description netty http 服务类 功能描述
 */
@Slf4j
public class NettytHttpServerInvocationProcessor extends DefaultInvocationProcessor {

    /**
     *
     * @param type
     */
    public NettytHttpServerInvocationProcessor(InvokeType type) {
        super(type);
    }

    @Override
    public Identity assembleIdentity(BeforeEvent event) {
        try {
            JavaInstanceCache.cacheInstance(event.target);
        } catch (Exception e) {
            // ignore
        }
        // 记录执行具体类名字的
        return new Identity(getType().name(), event.target.getClass().getCanonicalName(),
                event.javaMethodName + ParameterTypesUtil.getTypesStrByObjects(event.argumentArray), getExtra());
    }
    public Object assembleResponse(Event event) {
        Object HttpProcessResult = null ;
        if (event.type == Event.Type.RETURN) {
            HttpProcessResult =  ((ReturnEvent) event).object;
            try {
                String responseBody = (String) MethodUtils.invokeMethod(HttpProcessResult,
                        "getResponseBody");
                Object httpStatus = (Object) MethodUtils.invokeMethod(HttpProcessResult,
                        "getHttpStatus");
                int statusCode = (int) MethodUtils.invokeMethod(httpStatus,
                        "code");
                if(statusCode >= 400) {
                    responseBody = "http response status is: " + statusCode + ", " + responseBody ;
                }
                return responseBody ;
            } catch (Exception e) {
                log.error(e.getMessage(),e);
            }
        }
        return null;
    };
}
