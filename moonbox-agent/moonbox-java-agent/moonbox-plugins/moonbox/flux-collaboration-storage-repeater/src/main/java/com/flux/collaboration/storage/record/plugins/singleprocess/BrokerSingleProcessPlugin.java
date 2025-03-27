package com.flux.collaboration.storage.record.plugins.singleprocess;

import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.listener.EventListener;
import com.alibaba.jvm.sandbox.repeater.plugin.api.InvocationListener;
import com.alibaba.jvm.sandbox.repeater.plugin.api.InvocationProcessor;
import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.api.DefaultInvocationProcessor;
import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.spi.AbstractInvokePluginAdapter;
import com.alibaba.jvm.sandbox.repeater.plugin.core.model.EnhanceModel;
import com.alibaba.jvm.sandbox.repeater.plugin.core.trace.Tracer;
import com.alibaba.jvm.sandbox.repeater.plugin.spi.InvokePlugin;
import com.vivo.internet.moonbox.common.api.model.InvokeType;
import org.kohsuke.MetaInfServices;

import java.util.Arrays;
import java.util.List;

import static com.vivo.internet.moonbox.common.api.model.InvokeType.DATAHUB_BROKER_SINGLE;

/**
 * @author Administrator
 * @date 2025/2/4 22:44
 * @description 定时器执行逻辑 功能描述
 */
@MetaInfServices(InvokePlugin.class)
public class BrokerSingleProcessPlugin extends AbstractInvokePluginAdapter {


    private static final String ENHANCE_FLOW_CLASS = "com.flux.collaboration.broker.singleprocess.FlowProcAdapterTimer";
    private static final String ENHANCE_CLASS = "com.flux.collaboration.broker.singleprocess.SingleProcAdapterTimer";
    private static final String ENHANCE_METHOD = "start";

    @Override
    protected List<EnhanceModel> getEnhanceModels() {
        EnhanceModel.MethodPattern tmp= EnhanceModel.MethodPattern.builder()
                .methodName(ENHANCE_METHOD)
                .parameterType(new String[] {"com.flux.collaboration.storage.common.obj.ExecutorJobArg"})
                .build();
        EnhanceModel tem = EnhanceModel.builder()
                .classPattern(ENHANCE_FLOW_CLASS)
                .methodPatterns(new EnhanceModel.MethodPattern[]{tmp})
                .watchTypes(Event.Type.BEFORE, Event.Type.RETURN, Event.Type.THROWS)
                .build();
        EnhanceModel mem = EnhanceModel.builder()
                .classPattern(ENHANCE_CLASS)
                .methodPatterns(new EnhanceModel.MethodPattern[]{tmp})
                .watchTypes(Event.Type.BEFORE, Event.Type.RETURN, Event.Type.THROWS)
                .build();
        return Arrays.asList(tem,mem);
    }

    @Override
    public InvokeType getType() {
        return DATAHUB_BROKER_SINGLE;
    }

    @Override
    public String identity() {
        return DATAHUB_BROKER_SINGLE.getInvokeName();
    }

    @Override
    public boolean isEntrance() {
        return true;
    }


    @Override
    protected EventListener getEventListener(InvocationListener listener) {
        return new BrokerSingleProcessListener(getType(), isEntrance(), listener, getInvocationProcessor());
    }

    @Override
    protected InvocationProcessor getInvocationProcessor() {
        return new BrokerSingleProcessor(getType());
    }
}
