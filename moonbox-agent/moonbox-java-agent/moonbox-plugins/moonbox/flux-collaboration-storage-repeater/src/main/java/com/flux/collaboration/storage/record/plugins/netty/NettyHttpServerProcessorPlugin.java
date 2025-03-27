package com.flux.collaboration.storage.record.plugins.netty;


import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.listener.EventListener;
import com.alibaba.jvm.sandbox.repeater.plugin.api.InvocationListener;
import com.alibaba.jvm.sandbox.repeater.plugin.api.InvocationProcessor;
import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.spi.AbstractInvokePluginAdapter;
import com.alibaba.jvm.sandbox.repeater.plugin.core.model.EnhanceModel;
import com.alibaba.jvm.sandbox.repeater.plugin.spi.InvokePlugin;
import com.google.common.collect.Lists;
import com.vivo.internet.moonbox.common.api.model.InvokeType;
import org.kohsuke.MetaInfServices;

import java.util.List;


/**
 * @author yijiakang
 * 2025/1/22 16:20
 * netty 服务入口流量录制类 功能描述
 */

@MetaInfServices(InvokePlugin.class)
public class NettyHttpServerProcessorPlugin extends AbstractInvokePluginAdapter {

    private static final String CLASS_REST_PATTERN = "com.flux.collaboration.services.base.DatahubBaseRESTfulService";
    private static final String CLASS_RS_PATTERN = "com.flux.collaboration.services.base.DatahubBaseRSService";
    private static final String CLASS_WS_PATTERN = "com.flux.collaboration.services.base.DatahubBaseWSService";
    private static final String CLASS_WEBHOOK_PATTERN = "com.flux.collaboration.services.base.DatahubBaseWebHookService";


    @Override
    protected List<EnhanceModel> getEnhanceModels() {
        // 增强 netty 的 入口方法，后面回放也是这个方法
        // com.flux.collaboration.storage.common.http.netty.BaseHttpServerHandler.handleHttpRequest
        EnhanceModel.MethodPattern mp_post = EnhanceModel.MethodPattern.builder()
                .methodName("doPost")
                .parameterType(new String[]{"java.util.Map",
                        "java.util.Map","java.lang.String"})
                .build();

        EnhanceModel.MethodPattern mp_put = EnhanceModel.MethodPattern.builder()
                .methodName("doPut")
                .parameterType(new String[]{"java.util.Map",
                        "java.util.Map","java.lang.String"})
                .build();


        EnhanceModel.MethodPattern mp_get = EnhanceModel.MethodPattern.builder()
                .methodName("doGet")
                .parameterType(new String[]{"java.util.Map",
                        "java.util.Map","java.lang.String"})
                .build();


        EnhanceModel.MethodPattern mp_delete = EnhanceModel.MethodPattern.builder()
                .methodName("doDelete")
                .parameterType(new String[]{"java.util.Map",
                        "java.util.Map","java.lang.String"})
                .build();


        EnhanceModel em_rest = EnhanceModel.builder()
                .classPattern(CLASS_REST_PATTERN)
                .methodPatterns(new EnhanceModel.MethodPattern[]{mp_post, mp_get, mp_put, mp_delete})
                .watchTypes(Event.Type.BEFORE, Event.Type.RETURN, Event.Type.THROWS)
                .build();

        EnhanceModel em_rs = EnhanceModel.builder()
                .classPattern(CLASS_RS_PATTERN)
                .methodPatterns(new EnhanceModel.MethodPattern[]{mp_post, mp_get, mp_put, mp_delete})
                .watchTypes(Event.Type.BEFORE, Event.Type.RETURN, Event.Type.THROWS)
                .build();

        EnhanceModel em_ws = EnhanceModel.builder()
                .classPattern(CLASS_WS_PATTERN)
                .methodPatterns(new EnhanceModel.MethodPattern[]{mp_post, mp_get, mp_put, mp_delete})
                .watchTypes(Event.Type.BEFORE, Event.Type.RETURN, Event.Type.THROWS)
                .build();

        EnhanceModel em_webhook = EnhanceModel.builder()
                .classPattern(CLASS_WEBHOOK_PATTERN)
                .methodPatterns(new EnhanceModel.MethodPattern[]{mp_post, mp_get, mp_put, mp_delete})
                .watchTypes(Event.Type.BEFORE, Event.Type.RETURN, Event.Type.THROWS)
                .build();
        return Lists.newArrayList(em_rest,em_rs, em_ws,em_webhook);
    }

    @Override
    protected InvocationProcessor getInvocationProcessor() {
        return new NettytHttpServerInvocationProcessor(getType());
    }

    @Override
    protected EventListener getEventListener(InvocationListener listener) {
        return new NettyHttpServerProcessorListener(getType(), isEntrance(), listener, getInvocationProcessor());
    }

    @Override
    public InvokeType getType() {
        return InvokeType.NETTY_HTTP;
    }

    @Override
    public String identity() {
        return InvokeType.NETTY_HTTP.getInvokeName();
    }

    /**
     * 入口 逻辑，需要记录
     * @return
     */
    @Override
    public boolean isEntrance() {
        return true;
    }
}
