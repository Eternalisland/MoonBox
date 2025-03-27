package com.flux.collaboration.storage.record.plugins.singleprocess;

import com.alibaba.jvm.sandbox.api.event.BeforeEvent;
import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.event.ReturnEvent;
import com.alibaba.jvm.sandbox.repeater.plugin.api.InvocationListener;
import com.alibaba.jvm.sandbox.repeater.plugin.api.InvocationProcessor;
import com.alibaba.jvm.sandbox.repeater.plugin.core.ContextResourceClear;
import com.alibaba.jvm.sandbox.repeater.plugin.core.cache.MoonboxRecordCache;
import com.alibaba.jvm.sandbox.repeater.plugin.core.cache.MoonboxRepeatCache;
import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.api.DefaultEventListener;
import com.alibaba.jvm.sandbox.repeater.plugin.core.trace.Tracer;
import com.flux.collaboration.storage.record.util.SimpleRecordedUtils;
import com.vivo.internet.moonbox.common.api.model.Invocation;
import com.vivo.internet.moonbox.common.api.model.InvokeType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

import java.lang.reflect.InvocationTargetException;

/**
 * @author Administrator
 * @date 2025/2/4 23:01
 * @description DefaultEventListener 功能描述
 */
@Slf4j
public class BrokerSingleProcessListener extends DefaultEventListener {


    public BrokerSingleProcessListener(InvokeType invokeType, boolean entrance, InvocationListener listener, InvocationProcessor processor) {
        super(invokeType, entrance, listener, processor);
    }

    @Override
    public Invocation initInvocation(BeforeEvent beforeEvent) {
        Invocation invocation = new Invocation();
        Object p1 = beforeEvent.argumentArray[0];
        try {
            if (Tracer.getContext() != null && !StringUtils.isEmpty(Tracer.getContext().getExtra("organizationId"))) {
                invocation.setOrganizationId(Tracer.getContext().getExtra("organizationId"));
                invocation.setDatahubCustomerId(Tracer.getContext().getExtra("datahubCustomerId"));
                invocation.setMessageId(Tracer.getContext().getExtra("messageId"));  ;
            }else {
                String organizationId = (String) MethodUtils.invokeMethod(p1, "getOrganizationId");
                String datahubCustomerId = (String) MethodUtils.invokeMethod(p1, "getDatahubCustomerId");
                String messageId = (String) MethodUtils.invokeMethod(p1, "getMessageId");
                invocation.setOrganizationId(organizationId);
                invocation.setDatahubCustomerId(datahubCustomerId);
                invocation.setMessageId(messageId);
            }
        } catch (Exception e) {}
        return invocation;
    }

    @Override
    protected void initContext(Event event) {
        // 兼容回放场景，只有上下文中traceId为空时，才进行初始化
        if(StringUtils.isBlank(Tracer.getTraceId())){
            super.initContext(event);
        }
    }

    protected boolean sample(Event event) {
        // 如果是非回放流量
        if (!MoonboxRepeatCache.isRepeatFlow(Tracer.getTraceId())
                && entrance && event instanceof BeforeEvent ) {
            if (Tracer.getContext() != null && !StringUtils.isEmpty(Tracer.getContext().getExtra("organizationId"))) {
                if (!SimpleRecordedUtils.matchSimple(Tracer.getContext().getExtra("organizationId"), Tracer.getContext().getExtra("datahubCustomerId"), Tracer.getContext().getExtra("messageId"))) {
                    ContextResourceClear.sampleFalse();
                    return false;
                }
            } else {
                try {
                    BeforeEvent beforeEvent = (BeforeEvent) event;
                    Object p1 = beforeEvent.argumentArray[0];
                    String organizationId = (String) MethodUtils.invokeMethod(p1, "getOrganizationId");
                    String datahubCustomerId = (String) MethodUtils.invokeMethod(p1, "getDatahubCustomerId");
                    String messageId = (String) MethodUtils.invokeMethod(p1, "getMessageId");
                    if (!SimpleRecordedUtils.matchSimple(organizationId, datahubCustomerId, messageId)) {
                        ContextResourceClear.sampleFalse();
                        return false;
                    }

                    if (Tracer.getContext() != null) {
                        Tracer.getContext().putExtra("organizationId", organizationId);
                        Tracer.getContext().putExtra("datahubCustomerId", datahubCustomerId);
                        Tracer.getContext().putExtra("messageId", messageId);
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
            return Tracer.getContext().inTimeSample(invokeType);
        }else {
            return super.sample(event);
        }
    }

    @Override
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
        super.doReturn(event);
        // 释放数据
        SimpleRecordedUtils.releaseSimple(invocation.getOrganizationId(), invocation.getDatahubCustomerId(), invocation.getMessageId());
    }
}
