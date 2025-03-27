package com.flux.collaboration.storage.record.plugins.hessian;

import com.alibaba.jvm.sandbox.api.event.Event;
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
 * @date 2025/1/28 15:54
 * @description hessian 服务录制和回放 功能描述
 */
@MetaInfServices(InvokePlugin.class)
public class HessianBaseProcessorPlugin extends AbstractInvokePluginAdapter {

    private static final String ENCHANCE_CLASS_NAME = "com.flux.scev6.rpc.hessian.DefaultHessianClient";
    private static final String ENCHANCE_METHOD_NAME = "doAction";

    @Override
    public InvokeType getType() {
        return InvokeType.DATAHUB_HESSIAN_CLIENT;
    }

    @Override
    public String identity() {
        return InvokeType.DATAHUB_HESSIAN_CLIENT.getInvokeName();
    }

    @Override
    public boolean isEntrance() {
        return false;
    }

    @Override
    protected List<EnhanceModel> getEnhanceModels() {

        EnhanceModel.MethodPattern mp = EnhanceModel.MethodPattern.builder()
                .methodName(ENCHANCE_METHOD_NAME)
                .parameterType( new String[] {
                        "java.lang.String",
                        "java.lang.String",
                        "java.lang.String",
                        "java.lang.String",
                        "java.lang.String",
                        "java.lang.String",
                        "java.lang.String",
                        "java.lang.String"
                }).build();

        EnhanceModel em = EnhanceModel.builder().classPattern(ENCHANCE_CLASS_NAME)
                .methodPatterns( new EnhanceModel.MethodPattern[]{ mp })
                .watchTypes(Event.Type.BEFORE, Event.Type.RETURN, Event.Type.THROWS).build();
        return Arrays.asList( em );
    }

    @Override
    protected InvocationProcessor getInvocationProcessor() {
        return new HessianBaseProcessorInvocationProcessor(getType());
    }
}
