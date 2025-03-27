package com.flux.collaboration.storage.record.plugins.monitor.message;

import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.listener.EventListener;
import com.alibaba.jvm.sandbox.repeater.plugin.api.InvocationListener;
import com.alibaba.jvm.sandbox.repeater.plugin.api.InvocationProcessor;
import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.spi.AbstractInvokePluginAdapter;
import com.alibaba.jvm.sandbox.repeater.plugin.core.model.EnhanceModel;
import com.alibaba.jvm.sandbox.repeater.plugin.spi.InvokePlugin;
import com.vivo.internet.moonbox.common.api.model.InvokeType;
import org.kohsuke.MetaInfServices;

import java.util.Arrays;
import java.util.List;

/**
 * @author yijiakang
 * @date 2025/1/24 17:54
 * @description 监控入口，下发和回传都会走这个方法 功能描述
 */

@MetaInfServices(InvokePlugin.class)
public class MessageMonitorFactoryPlugin extends AbstractInvokePluginAdapter {


    // com.flux.collaboration.storage.message.MonitorMessageFactory.putMessage

    @Override
    protected List<EnhanceModel> getEnhanceModels() {
        // 增强 netty 的 入口方法，后面回放也是这个方法
        // com.flux.collaboration.storage.message.MonitorMessageFactory.putMessage
        EnhanceModel.MethodPattern mp = EnhanceModel.MethodPattern.builder().methodName("putMessage")
//                .parameterType(new String[]{"com.flux.collaboration.storage.message.MonitorMessageExt",
//                        "com.flux.collaboration.storage.common.obj.TimerRunningInfo","java.lang.Boolean"})
                .build();

        EnhanceModel em = EnhanceModel.builder().classPattern("com.flux.collaboration.storage.message.MonitorMessageFactory").methodPatterns(new EnhanceModel.MethodPattern[]{mp}).watchTypes(Event.Type.BEFORE, Event.Type.RETURN, Event.Type.THROWS).build();

        return Arrays.asList(em);
    }

    @Override
    public InvokeType getType() {
        return InvokeType.DATAHUB_MONITOR_MESSAGE;
    }

    @Override
    public String identity() {
        return InvokeType.DATAHUB_MONITOR_MESSAGE.getInvokeName();
    }

    @Override
    public boolean isEntrance() {
        return false;
    }


    @Override
    protected InvocationProcessor getInvocationProcessor() {
        return new MessageMonitorFactoryInvocationProcessor(getType());
    }


    @Override
    protected EventListener getEventListener(InvocationListener listener) {
        return new MessageMonitorEventListener(getType(), isEntrance(), listener, getInvocationProcessor());
    }
}
