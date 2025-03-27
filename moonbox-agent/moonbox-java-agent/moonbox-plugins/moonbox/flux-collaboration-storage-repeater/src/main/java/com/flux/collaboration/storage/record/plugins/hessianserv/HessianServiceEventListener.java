package com.flux.collaboration.storage.record.plugins.hessianserv;

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
import com.alibaba.jvm.sandbox.repeater.plugin.core.spring.SpringContextAdapter;
import com.alibaba.jvm.sandbox.repeater.plugin.core.trace.Tracer;
import com.flux.collaboration.storage.record.util.SimpleRecordedUtils;
import com.vivo.internet.moonbox.common.api.model.Invocation;
import com.vivo.internet.moonbox.common.api.model.InvokeType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

import java.util.Map;

@Slf4j
public class HessianServiceEventListener extends DefaultEventListener {

    /**
     *
     * @param invokeType
     * @param entrance
     * @param listener
     * @param processor
     */
    public HessianServiceEventListener(InvokeType invokeType, boolean entrance, InvocationListener listener, InvocationProcessor processor) {
        super(invokeType, entrance, listener, processor);
    }

    @Override
    public Invocation initInvocation(BeforeEvent beforeEvent) {
        Invocation invocation = new Invocation();
        String loginInfo = (String) beforeEvent.argumentArray[0];
        String bizDataInfo = (String) beforeEvent.argumentArray[1];

        try {
            if (Tracer.getContext() != null && !StringUtils.isEmpty(Tracer.getContext().getExtra("organizationId"))) {
                invocation.setOrganizationId(Tracer.getContext().getExtra("organizationId"));
                invocation.setDatahubCustomerId(Tracer.getContext().getExtra("datahubCustomerId"));
                invocation.setMessageId(Tracer.getContext().getExtra("messageId"));  ;
            }else {

                // 获取 AppSecretService 这个bean
                Object appSecret = SpringContextAdapter.getBeanByType("com.flux.scev6.appsecret.AppSecretService");
                // 反射获取 getAppSecret , 类加载器不一样，无法直接获取
                String appsecinfo = (String) MethodUtils.invokeMethod(appSecret, "getAppSecret");

                Class<?> classLoginInfo = ClassloaderBridge.instance().findClassInstance("com.flux.scev6.login.LoginInfoUtil");
                Class<?> classBizInfo = ClassloaderBridge.instance().findClassInstance("com.flux.scev6.utils.BizDataUtil");
                Object loginObject = MethodUtils.invokeStaticMethod(classLoginInfo, "fromJSONString", loginInfo, appsecinfo);
                Map<String, String>  bizDataObject = (Map<String, String>) MethodUtils.invokeStaticMethod(classBizInfo, "fromJSONString", bizDataInfo, appsecinfo);
                String organizationId = (String) MethodUtils.invokeMethod(loginObject, "getBizOrgId");
                String datahubCustomerId = bizDataObject.get("interfaceType"); //wms 配置界面中的 【接口类型】
                String  messageId = bizDataObject.get("messageId");   //wms 配置界面中的 【消息ID】

                if (null == datahubCustomerId
                        || datahubCustomerId.length()<1){
                    datahubCustomerId = bizDataObject.get("INTERFACEID"); //datahub中 消息集编码 wms 配置界面中的 【接口编号】
                }
                if (null == messageId
                        || messageId.length()<1){
                    messageId = bizDataObject.get("INTERFACESUBID"); //datahub中 消息集编码  wms 配置界面中的 【接口子编码】
                }
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
                if (!SimpleRecordedUtils.matchSimple(Tracer.getContext().getExtra("organizationId"),
                        Tracer.getContext().getExtra("datahubCustomerId"),
                        Tracer.getContext().getExtra("messageId"))) {
                    ContextResourceClear.sampleFalse();
                    return false;
                }
            } else {
                try {
                    String loginInfo = (String) ((BeforeEvent)event).argumentArray[0];
                    String bizDataInfo = (String) ((BeforeEvent)event).argumentArray[1];
                    // 获取 AppSecretService 这个bean
                    Object appSecret = SpringContextAdapter.getBeanByType("com.flux.scev6.appsecret.AppSecretService");
                    // 反射获取 getAppSecret , 类加载器不一样，无法直接获取
                    String appsecinfo = (String) MethodUtils.invokeMethod(appSecret, "getAppSecret");

                    Class<?> classLoginInfo = ClassloaderBridge.instance().findClassInstance("com.flux.scev6.login.LoginInfoUtil");
                    Class<?> classBizInfo = ClassloaderBridge.instance().findClassInstance("com.flux.scev6.utils.BizDataUtil");
                    Object loginObject = MethodUtils.invokeStaticMethod(classLoginInfo, "fromJSONString", loginInfo, appsecinfo);
                    Map<String, String>  bizDataObject = (Map<String, String>) MethodUtils.invokeStaticMethod(classBizInfo, "fromJSONString", bizDataInfo, appsecinfo);
                    String organizationId = (String) MethodUtils.invokeMethod(loginObject, "getBizOrgId");
                    String datahubCustomerId = bizDataObject.get("interfaceType"); //wms 配置界面中的 【接口类型】
                    String  messageId = bizDataObject.get("messageId");   //wms 配置界面中的 【消息ID】

                    if (null == datahubCustomerId
                            || datahubCustomerId.length()<1){
                        datahubCustomerId = bizDataObject.get("INTERFACEID"); //datahub中 消息集编码 wms 配置界面中的 【接口编号】
                    }
                    if (null == messageId
                            || messageId.length()<1){
                        messageId = bizDataObject.get("INTERFACESUBID"); //datahub中 消息集编码  wms 配置界面中的 【接口子编码】
                    }

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
