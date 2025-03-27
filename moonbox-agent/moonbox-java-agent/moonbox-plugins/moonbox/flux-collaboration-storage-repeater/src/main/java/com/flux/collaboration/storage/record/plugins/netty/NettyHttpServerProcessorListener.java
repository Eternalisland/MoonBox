package com.flux.collaboration.storage.record.plugins.netty;

import com.alibaba.jvm.sandbox.api.ProcessControlException;
import com.alibaba.jvm.sandbox.api.event.BeforeEvent;
import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.event.ReturnEvent;
import com.alibaba.jvm.sandbox.repeater.plugin.api.InvocationListener;
import com.alibaba.jvm.sandbox.repeater.plugin.api.InvocationProcessor;
import com.alibaba.jvm.sandbox.repeater.plugin.core.ContextResourceClear;
import com.alibaba.jvm.sandbox.repeater.plugin.core.bridge.ClassloaderBridge;
import com.alibaba.jvm.sandbox.repeater.plugin.core.cache.MoonboxRecordCache;
import com.alibaba.jvm.sandbox.repeater.plugin.core.cache.MoonboxRepeatCache;
import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.api.DefaultEventListener;
import com.alibaba.jvm.sandbox.repeater.plugin.core.model.MoonboxContext;
import com.alibaba.jvm.sandbox.repeater.plugin.core.serialize.SerializeException;
import com.alibaba.jvm.sandbox.repeater.plugin.core.trace.Tracer;
import com.alibaba.jvm.sandbox.repeater.plugin.core.utils.MoonboxStackTraceUtils;
import com.alibaba.jvm.sandbox.repeater.plugin.core.utils.SequenceGenerator;
import com.alibaba.jvm.sandbox.repeater.plugin.core.wrapper.SerializerWrapper;
import com.alibaba.jvm.sandbox.repeater.plugin.utils.ProtobufUtil;
import com.flux.collaboration.storage.record.util.SimpleRecordedUtils;
import com.vivo.internet.moonbox.common.api.model.HttpInvocation;
import com.vivo.internet.moonbox.common.api.model.Invocation;
import com.vivo.internet.moonbox.common.api.model.InvokeType;
import com.vivo.internet.moonbox.common.api.util.ParameterTypesUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author yijiakang
 * @date 2025/1/22 17:17
 * @description 监听 功能描述
 */
@Slf4j
public class NettyHttpServerProcessorListener extends DefaultEventListener {

    private final MoonboxContext MOONBOX_CONTEXT = MoonboxContext.getInstance();

    /**
     * @param invokeType
     * @param entrance
     * @param listener
     * @param processor
     */
    NettyHttpServerProcessorListener(InvokeType invokeType, boolean entrance, InvocationListener listener, InvocationProcessor processor) {
        super(invokeType, entrance, listener, processor);
    }


    /**
     * 重写initContext；对于http请求；before事件里面
     *
     * @param event 事件
     */
    @Override
    protected boolean sample(Event event) {
        // 如果是录制流量则进行采样率计算
        if (!MoonboxRepeatCache.isRepeatFlow(Tracer.getTraceId())
                && entrance && event instanceof BeforeEvent) {
            BeforeEvent beforeEvent = (BeforeEvent) event;
            try {
                // 拦截的 java 方法 和 java 类名
//                String javaClassName = beforeEvent.javaClassName;
//                String javaMethodName = beforeEvent.javaMethodName;
//                Object context = beforeEvent.argumentArray[0];
//                Object request = beforeEvent.argumentArray[1];
                return Tracer.getContext().inTimeSample(invokeType);
            } catch (Exception e) {
                log.error("error occurred when init dubbo invocation", e);
                ContextResourceClear.sampleFalse();
                return false;
            }
        } else {
            return super.sample(event);
        }
    }
    @Override
    protected void initContext(Event event) {
        // 兼容回放场景，只有上下文中traceId为空时，才进行初始化
        if(StringUtils.isBlank(Tracer.getTraceId())){
            super.initContext(event);
        }
    }
    /**
     * handle before event
     *
     * @param event event
     */
    protected void doBefore(BeforeEvent event) throws ProcessControlException {

        //如果是回放流量，直接doMock，并返回
        if (MoonboxRepeatCache.isRepeatFlow(Tracer.getTraceId())) {
            processor.doMock(event, entrance, invokeType, Boolean.TRUE);
            return;
        }
        //非回放的流量才会走到这里
        HttpInvocation invocation = (HttpInvocation) this.initInvocation(event);
        if (null == invocation.getStart()) {
            invocation.setStart(System.currentTimeMillis());
        }

        invocation.setTraceId(Tracer.getTraceId());
        invocation.setIndex(entrance ? 0 : SequenceGenerator.generate(Tracer.getTraceId()));
        invocation.setIdentity(processor.assembleIdentity(event));
        invocation.setEntrance(entrance);
        invocation.setType(invokeType);
        invocation.setProcessId(event.processId);
        invocation.setInvokeId(event.invokeId);
        invocation.setClassLoader(event.javaClassLoader);
        invocation.setHeaders((Map<String, String>) event.argumentArray[0]);
//        invocation.setP((Map<String, String>) event.argumentArray[0]);

        if (invocation.getRequest() == null) {
            Map<String,String> headers = (Map<String, String>) event.argumentArray[0];
            String requestUri = headers.get("REQUEST_URI");
            String url = "";
            String port = "" ;
            if(!StringUtils.isEmpty(requestUri)) {
                try { if(!requestUri.startsWith("http")) { requestUri = "http://" + requestUri; }
                    URL uRL1 = new URL(requestUri);
                    url = uRL1.getPath();
                    port = ""+ uRL1.getPort();
                } catch (Exception e) {
                }
            }
            Map<String,String> params = (Map<String, String>) event.argumentArray[1];
            String body = (String) event.argumentArray[2];
            // 翻译WrapperTransferModel参数
            Map<String, Object> httpParams = new HashMap<>(8);
            httpParams.put("requestURI", requestUri);
            httpParams.put("requestURL", url);
            httpParams.put("method", event.javaMethodName );
            httpParams.put("port", port );
            httpParams.put("contentType", headers.get("Content-Type"));
            httpParams.put("headers", headers);
            httpParams.put("paramsMap", params );
            httpParams.put("body", body );
            invocation.setRequest(new Object[] { httpParams });
//            invocation.setRequest(processor.assembleRequest(event));
            invocation.setParameterTypes(ParameterTypesUtil.getTypesArrayByObjects( event.argumentArray ));
        }

        if (ProtobufUtil.istRequestProtobufData(invocation.getIdentity().getUri(), invocation.getRequest(), event.javaClassLoader)) {
            invocation.setProtobufRequestFlag(true);
            invocation.setProtobufReqeustJsons(ProtobufUtil.getRequestProtobufData(invocation.getIdentity().getUri(), invocation.getRequest(), event.javaClassLoader));
        }
        invocation.setResponse(processor.assembleResponse(event));
        invocation.setResponseType(processor.assembleResponseType(event));

        if (ProtobufUtil.istResponseProtobufData(invocation.getIdentity().getUri(), invocation.getResponse(), event.javaClassLoader)) {
            invocation.setProtobufResponseFlag(true);
            invocation.setProtobufResponseJson(ProtobufUtil.getResponseProtobufData(invocation.getIdentity().getUri(), invocation.getResponse(), event.javaClassLoader));
        }
        invocation.setSerializeToken(ClassloaderBridge.instance().encode(event.javaClassLoader));

        if (InvokeType.isRecordStack(invokeType) && Tracer.getContext().isRecordStack()) {
            List<StackTraceElement> traces = MoonboxStackTraceUtils.retrieveStackTrace(null);
            invocation.setStackTraceElements(traces);
        }
        try {
            // fix issue#14 : useGeneratedKeys
            if (processor.inTimeSerializeRequest(invocation, event)) {
                SerializerWrapper.inTimeSerializeExcludeAnyTypes(invocation);
            }
        } catch (SerializeException e) {
            ContextResourceClear.sampleFalse();
            e.printStackTrace();
            return;
        }
        MoonboxRecordCache.cacheInvocation(event.invokeId, invocation);
        if (entrance) {
            MoonboxRecordCache.cacheEntranceInvocationCache(Tracer.getTraceId(), invocation);
        }
    }


    /**
     * 初始化invocation 放开给插件重写，可以初始化自定义的调用描述类型，模块不感知插件的类型
     *
     * @param beforeEvent
     *            before事件
     * @return 一次调用
     */
    protected Invocation initInvocation(BeforeEvent beforeEvent) {
        return new HttpInvocation();
    }


    /**
     * handle return event
     *
     * @param event
     *            event
     */
    protected void doReturn(ReturnEvent event) {
        //如果是回放流量，直接返回
        if (MoonboxRepeatCache.isRepeatFlow(Tracer.getTraceId())) {
            return;
        }

        Invocation invocation = MoonboxRecordCache.getInvocation(event.invokeId);
        if (null == invocation) {
            ContextResourceClear.sampleFalse();
            MoonboxRecordCache.removeInvocation(event.invokeId);
            return;
        }
        invocation.setResponse(processor.assembleResponse(event));
        invocation.setResponseType(processor.assembleResponseType(event));
        invocation.setEnd(System.currentTimeMillis());
        listener.onInvocation(invocation);
        // 释放数据
        SimpleRecordedUtils.releaseSimple(invocation.getOrganizationId(), invocation.getDatahubCustomerId(), invocation.getMessageId());
    }
}
