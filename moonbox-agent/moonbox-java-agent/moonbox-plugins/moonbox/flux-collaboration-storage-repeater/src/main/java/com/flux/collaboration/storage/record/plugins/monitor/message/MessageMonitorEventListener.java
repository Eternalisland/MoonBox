package com.flux.collaboration.storage.record.plugins.monitor.message;

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
import com.flux.collaboration.storage.record.util.SimpleRecordedUtils;
import com.vivo.internet.moonbox.common.api.model.Invocation;
import com.vivo.internet.moonbox.common.api.model.InvokeType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.MethodUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * @author yijiakang
 * @date 2025/1/27 14:37
 * @description 监听器 功能描述
 */
@Slf4j
public class MessageMonitorEventListener extends DefaultEventListener {


    /**
     *
     * @param invokeType
     * @param entrance
     * @param listener
     * @param processor
     */
    public MessageMonitorEventListener(InvokeType invokeType, boolean entrance, InvocationListener listener, InvocationProcessor processor) {
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
        Object argv = event.argumentArray[0];
        try {
            Object adapterSourceInfo = MethodUtils.invokeMethod(argv, "getAdapterSourceInfo") ;

            if(adapterSourceInfo != null) {
                String organizationId = (String) MethodUtils.invokeMethod(adapterSourceInfo, "getOrganizationId");
                String datahubCustomerId = (String) MethodUtils.invokeMethod(adapterSourceInfo, "getDatahubCustomerId");
                String messageId = (String) MethodUtils.invokeMethod(adapterSourceInfo, "getMessageId");
                if (SimpleRecordedUtils.matchSimple(organizationId, datahubCustomerId, messageId)) {
                    invocation.setOrganizationId(organizationId);
                    invocation.setDatahubCustomerId(datahubCustomerId);
                    invocation.setMessageId(messageId);
                }else {
                    // 不在继续往下执行
                    ContextResourceClear.sampleFalse();
                    return ;
                }
//                invocation.setOrganizationId((String) MethodUtils.invokeMethod(adapterSourceInfo, "getOrganizationId"));
//                invocation.setDatahubCustomerId((String) MethodUtils.invokeMethod(adapterSourceInfo, "getDatahubCustomerId"));
//                invocation.setMessageId((String) MethodUtils.invokeMethod(adapterSourceInfo, "getMessageId"));
            }
        } catch (Exception e) {
            log.error("log getAdapterSourceInfo", e);
        }
        if (null == invocation.getStart()) {
            invocation.setStart(System.currentTimeMillis());
        }
        // 设置了消息集消息编码的流量才需要被记录录制
        Invocation entranceInvocation = MoonboxRecordCache.getEntranceInvocationCacheIfPresent(Tracer.getTraceId());
        if(entranceInvocation != null) {
            entranceInvocation.setOrganizationId(invocation.getOrganizationId());
            entranceInvocation.setDatahubCustomerId(invocation.getDatahubCustomerId());
            entranceInvocation.setMessageId(invocation.getMessageId());
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
