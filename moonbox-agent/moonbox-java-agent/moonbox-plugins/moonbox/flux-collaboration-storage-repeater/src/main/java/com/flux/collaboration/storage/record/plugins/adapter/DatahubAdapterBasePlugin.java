package com.flux.collaboration.storage.record.plugins.adapter;

import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.listener.EventListener;
import com.alibaba.jvm.sandbox.repeater.plugin.api.InvocationListener;
import com.alibaba.jvm.sandbox.repeater.plugin.api.InvocationProcessor;
import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.api.DefaultInvocationProcessor;
import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.spi.AbstractInvokePluginAdapter;
import com.alibaba.jvm.sandbox.repeater.plugin.core.model.EnhanceModel;
import com.alibaba.jvm.sandbox.repeater.plugin.spi.InvokePlugin;
import com.vivo.internet.moonbox.common.api.model.InvokeType;
import org.kohsuke.MetaInfServices;

import java.util.Arrays;
import java.util.List;

/**
 * @author Administrator
 * @date 2025/1/31 13:51
 * @description datahub adapter base处理类 功能描述
 */
@MetaInfServices(InvokePlugin.class)
public class DatahubAdapterBasePlugin extends AbstractInvokePluginAdapter {

    private static final String ENHANCE_CLASS_NAME = "com.flux.collaboration.storage.base.api.impl.DatahubAdapterBase";
    private static final String ENHANCE_METHOD_NAME = "messageProcessor";
    private static final String ENHANCE_CLASS_METHOD_PARAMETER_NAME_1 = "com.flux.collaboration.storage.message.AdapterMessageInfo";
    private static final String ENHANCE_CLASS_METHOD_PARAMETER_NAME_2 = "com.flux.collaboration.storage.common.flow.AdapterMessageProcessor";

    @Override
    protected List<EnhanceModel> getEnhanceModels() {

        // 执行 adapter 具体逻辑
        EnhanceModel.MethodPattern messageProcessor = EnhanceModel.MethodPattern.builder()
                .methodName(ENHANCE_METHOD_NAME)
                .parameterType(new String[]{ENHANCE_CLASS_METHOD_PARAMETER_NAME_1, ENHANCE_CLASS_METHOD_PARAMETER_NAME_2})
                .build();
        EnhanceModel em = EnhanceModel.builder().classPattern(ENHANCE_CLASS_NAME)
                .methodPatterns(new EnhanceModel.MethodPattern[]{messageProcessor}).watchTypes(Event.Type.BEFORE, Event.Type.RETURN, Event.Type.THROWS).build();
        return Arrays.asList(em);
    }

    @Override
    public InvokeType getType() {
        return InvokeType.DATAHUB_ADAPTER_BASE;
    }

    @Override
    public String identity() {
        return InvokeType.DATAHUB_ADAPTER_BASE.getInvokeName();
    }

    @Override
    public boolean isEntrance() {
        return false;
    }

    @Override
    protected InvocationProcessor getInvocationProcessor() {
        return new DatahubAdapterBaseInvocationProcessor(getType());
    }

    /**
     * @param listener 调用监听
     * @return
     */
    @Override
    protected EventListener getEventListener(InvocationListener listener) {
        return new DatahubAdapterBaseListener(getType(), isEntrance(), listener, getInvocationProcessor());
    }
}
