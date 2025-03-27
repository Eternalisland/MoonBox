package com.flux.collaboration.storage.record.plugins.ruleop;

import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.listener.EventListener;
import com.alibaba.jvm.sandbox.repeater.plugin.api.InvocationListener;
import com.alibaba.jvm.sandbox.repeater.plugin.api.InvocationProcessor;
import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.api.DefaultEventListener;
import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.api.DefaultInvocationProcessor;
import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.spi.AbstractInvokePluginAdapter;
import com.alibaba.jvm.sandbox.repeater.plugin.core.model.EnhanceModel;
import com.alibaba.jvm.sandbox.repeater.plugin.spi.InvokePlugin;
import com.vivo.internet.moonbox.common.api.model.InvokeType;
import org.kohsuke.MetaInfServices;

import java.util.Arrays;
import java.util.List;

/**
 * @author yijiakang
 * @date 2025/1/28 15:42
 * @description 规则执行相关录制和回放 功能描述
 */
@MetaInfServices(InvokePlugin.class)
public class RuleBaseProcessorPlugin extends AbstractInvokePluginAdapter {

    private static final String ENHANCE_TRANSFORM_CLASS = "com.flux.collaboration.storage.base.api.impl.BaseUserTransform";
    private static final String ENHANCE_MAPPING_CLASS = "com.flux.collaboration.storage.base.api.impl.BaseUserMapping";
    private static final String ENHANCE_METHOD = "process";

    @Override
    public InvokeType getType() {
        return InvokeType.DATAHUB_RULE_OPERATOR;
    }

    @Override
    public String identity() {
        return InvokeType.DATAHUB_RULE_OPERATOR.getInvokeName();
    }

    @Override
    public boolean isEntrance() {
        return false;
    }

    @Override
    protected List<EnhanceModel> getEnhanceModels() {

        //  String organizationId,
        //	        String datahubCustomerId,
        //	        String messageId,
        //            String messageGroupSysId,
        //            String stdNo,
        //            BasJndiInfo destJdbc,
        //            RuleCustomerMsgWeb  ruleParamInfo,
        //            String stdTableHeader,
        //            String webTableHeader
        EnhanceModel.MethodPattern tmp= EnhanceModel.MethodPattern.builder()
                .methodName(ENHANCE_METHOD)
                .parameterType(new String[] {"java.lang.String",
                        "java.lang.String",
                        "java.lang.String",
                        "java.lang.String",
                        "java.lang.String",
                        "com.flux.collaboration.storage.model.BasJndiInfo",
                        "java.lang.String",
                        "java.lang.String"})
                .build();
        //  String organizationId,
        //			String datahubCustomerId,
        //			String messageId,
        //			String messageGroupSysId,
        //			String stdNo,
        //			RuleCustomerMsgWeb  ruleParamInfo,
        //            String stdTableHeader
        EnhanceModel.MethodPattern mmp= EnhanceModel.MethodPattern.builder()
                .methodName(ENHANCE_METHOD)
                .parameterType(new String[] {"java.lang.String",
                        "java.lang.String",
                        "java.lang.String",
                        "java.lang.String",
                        "java.lang.String",
                        "java.lang.String"})
                .build();
        EnhanceModel tem = EnhanceModel.builder()
                .classPattern(ENHANCE_TRANSFORM_CLASS)
                .methodPatterns(new EnhanceModel.MethodPattern[]{tmp})
                .watchTypes(Event.Type.BEFORE, Event.Type.RETURN, Event.Type.THROWS)
                .build();
        EnhanceModel mem = EnhanceModel.builder()
                .classPattern(ENHANCE_MAPPING_CLASS)
                .methodPatterns(new EnhanceModel.MethodPattern[]{mmp})
                .watchTypes(Event.Type.BEFORE, Event.Type.RETURN, Event.Type.THROWS)
                .build();
        return Arrays.asList(tem,mem);
    }

    @Override
    protected InvocationProcessor getInvocationProcessor() {
        return new RuleBaseProcessor(getType());
    }


    /**
     * 返回事件监听器 - 子类若参数的组装方式不适配，可以重写改方法
     *
     * @param listener 调用监听
     * @return 事件监听器
     */
    protected EventListener getEventListener(InvocationListener listener) {
        return new RuleBaseProcessorListener(getType(), isEntrance(), listener, getInvocationProcessor());
    }
}
