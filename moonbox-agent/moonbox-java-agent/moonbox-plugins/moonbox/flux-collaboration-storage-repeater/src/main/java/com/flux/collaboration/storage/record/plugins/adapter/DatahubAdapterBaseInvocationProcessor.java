package com.flux.collaboration.storage.record.plugins.adapter;

import com.alibaba.jvm.sandbox.api.event.BeforeEvent;
import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.api.DefaultInvocationProcessor;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.RepeatContext;
import com.alibaba.jvm.sandbox.repeater.plugin.utils.ParameterTypesUtil;
import com.vivo.internet.moonbox.common.api.model.Identity;
import com.vivo.internet.moonbox.common.api.model.InvokeType;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

public class DatahubAdapterBaseInvocationProcessor extends DefaultInvocationProcessor {

    private  Logger logger = LoggerFactory.getLogger(DatahubAdapterBaseInvocationProcessor.class);
    /*
     *  V101(20260519) yijiakang 【Sonar漏洞修复】修复 SonarQube 扫描问题，任务号:20260509145520000648
     */
    @SuppressWarnings("java:S1068")
    private static final String VERNUM = "V101(20260519)" ;

    private ThreadLocal<Map<String, String>> extraLocal = new ThreadLocal<>();


    /**
     * @param type
     */
    public DatahubAdapterBaseInvocationProcessor(InvokeType type) {
        super(type);
    }

    public Identity assembleIdentity(BeforeEvent event) {
        Object[] argumentArray = event.argumentArray;
        Object adapterInfo = argumentArray[0];
        try {
            String organizationId = (String) MethodUtils.invokeMethod(adapterInfo, "getOrganizationId");
            String datahubCustomerId = (String) MethodUtils.invokeMethod(adapterInfo, "getDatahubCustomerId");
            String messageId = (String) MethodUtils.invokeMethod(adapterInfo, "getMessageId");
            getExtra().put("organizationId", organizationId);
            getExtra().put("datahubCustomerId", datahubCustomerId);
            getExtra().put("messageId", messageId);
        } catch (Exception e) {
            // 20260519 yijiakang 【功能完善】 SONARQUBE 漏洞修复 (20260509145520000648)
            logger.error(e.getMessage(),e);
        }
        Identity identity = new Identity(getType().name(), event.target.getClass().getCanonicalName(), event.javaMethodName + ParameterTypesUtil.getTypesStrByObjects(event.argumentArray), getExtra());
        extraLocal.remove();
        return identity;
    }


    /**
     * 获取执行信息
     *
     * @return
     */
    public Map<String, String> getExtra() {
        Map<String, String> extra = extraLocal.get();
        if (extra == null) {
            extra = new LinkedHashMap<>();
            extraLocal.set(extra);
        }
        return extra;
    }

    /**
     * @param event before事件
     * @return
     */
    @Override
    public Object[] assembleRequest(BeforeEvent event) {

        if (event.argumentArray == null || event.argumentArray.length < 1) {
            return null;
        }
        return new Object[]{event.argumentArray[0]};
    }

    /**
     * 是否需要跳过这次Mock;插件可以自己扩展;默认跳过入口的Mock
     *
     * @param event   before事件
     * @param context 回放上下文
     * @return 是否跳过
     */
    protected boolean skipMock(BeforeEvent event, Boolean entrance, RepeatContext context) {
        return false;
    }
}
