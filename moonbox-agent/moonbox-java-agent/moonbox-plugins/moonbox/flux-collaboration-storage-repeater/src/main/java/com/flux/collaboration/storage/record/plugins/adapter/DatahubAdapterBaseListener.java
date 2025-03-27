package com.flux.collaboration.storage.record.plugins.adapter;

import com.alibaba.jvm.sandbox.api.ProcessControlException;
import com.alibaba.jvm.sandbox.api.event.BeforeEvent;
import com.alibaba.jvm.sandbox.repeater.plugin.api.InvocationListener;
import com.alibaba.jvm.sandbox.repeater.plugin.api.InvocationProcessor;
import com.alibaba.jvm.sandbox.repeater.plugin.core.ContextResourceClear;
import com.alibaba.jvm.sandbox.repeater.plugin.core.bridge.ClassloaderBridge;
import com.alibaba.jvm.sandbox.repeater.plugin.core.cache.MoonboxRecordCache;
import com.alibaba.jvm.sandbox.repeater.plugin.core.cache.MoonboxRepeatCache;
import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.api.DefaultEventListener;
import com.alibaba.jvm.sandbox.repeater.plugin.core.serialize.SerializeException;
import com.alibaba.jvm.sandbox.repeater.plugin.core.trace.Tracer;
import com.alibaba.jvm.sandbox.repeater.plugin.core.utils.MoonboxStackTraceUtils;
import com.alibaba.jvm.sandbox.repeater.plugin.core.utils.SequenceGenerator;
import com.alibaba.jvm.sandbox.repeater.plugin.core.wrapper.SerializerWrapper;
import com.alibaba.jvm.sandbox.repeater.plugin.utils.ProtobufUtil;
import com.vivo.internet.moonbox.common.api.model.Invocation;
import com.vivo.internet.moonbox.common.api.model.InvokeType;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author Administrator
 * @date 2025/1/31 13:56
 * @description Datahub adapter Base监听器 功能描述
 */
@Slf4j
public class DatahubAdapterBaseListener extends DefaultEventListener {
    /**
     *
     * @param invokeType
     * @param entrance
     * @param listener
     * @param processor
     */
    public DatahubAdapterBaseListener(InvokeType invokeType, boolean entrance, InvocationListener listener, InvocationProcessor processor) {
        super(invokeType, entrance, listener, processor);
    }


    /**
     * handle before event
     *
     * @param event
     *            event
     */
    protected void doBefore(BeforeEvent event) throws ProcessControlException {

        //如果是回放流量，直接doMock，并返回
        if (MoonboxRepeatCache.isRepeatFlow(Tracer.getTraceId())) {
            processor.doMock(event, entrance, invokeType, Boolean.TRUE);
            return;
        }

        //非回放的流量才会走到这里
        Invocation invocation = this.initInvocation(event);

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

        if (invocation.getRequest() == null) {
            invocation.setRequest(processor.assembleRequest(event));
        }

        if (ProtobufUtil.istRequestProtobufData(invocation.getIdentity().getUri(), invocation.getRequest(),
                event.javaClassLoader)) {
            invocation.setProtobufRequestFlag(true);
            invocation.setProtobufReqeustJsons(ProtobufUtil.getRequestProtobufData(invocation.getIdentity().getUri(),
                    invocation.getRequest(), event.javaClassLoader));
        }

        invocation.setResponse(processor.assembleResponse(event));
        invocation.setResponseType(processor.assembleResponseType(event));
        if (ProtobufUtil.istResponseProtobufData(invocation.getIdentity().getUri(), invocation.getResponse(),
                event.javaClassLoader)) {
            invocation.setProtobufResponseFlag(true);
            invocation.setProtobufResponseJson(ProtobufUtil.getResponseProtobufData(invocation.getIdentity().getUri(),
                    invocation.getResponse(), event.javaClassLoader));
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
            log.error("Error occurred serialize", e);
            return;
        }
        MoonboxRecordCache.cacheInvocation(event.invokeId, invocation);
    }
}
