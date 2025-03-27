package com.flux.collaboration.storage.record.plugins.ruleop;

import com.alibaba.jvm.sandbox.api.event.BeforeEvent;
import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.api.DefaultInvocationProcessor;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.RepeatContext;
import com.alibaba.jvm.sandbox.repeater.plugin.utils.ParameterTypesUtil;
import com.vivo.internet.moonbox.common.api.model.Identity;
import com.vivo.internet.moonbox.common.api.model.InvokeType;
import org.apache.commons.lang3.reflect.MethodUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Administrator
 * @date 2025/1/29 1:12
 * @description 基础rule处理类 功能描述
 */
public class RuleBaseProcessor extends DefaultInvocationProcessor {

    private ThreadLocal<Map<String,String>> extraLocal = new ThreadLocal<>();
    /**
     *
     * @param type
     */
    public RuleBaseProcessor(InvokeType type) {
        super(type);
    }


    /**
     * 记录规则id
     *
     * @param event 事件
     * @return
     */
    @Override
    public Identity assembleIdentity(BeforeEvent event) {
        try{
            Object ruleId = MethodUtils.invokeMethod(event.target, "getRULEID");
            getExtra().put("ruleId", (String)ruleId);
        } catch (Exception e) {
           // ignpre exception
        }
        return new Identity(getType().name(), event.target.getClass().getCanonicalName() ,
                event.javaMethodName + ParameterTypesUtil.getTypesStrByObjects(event.argumentArray), getExtra());
    }

    /**
     *  获取执行信息
     * @return
     */
    public Map<String, String> getExtra() {
        Map<String, String> extra = extraLocal.get();
        if(extra == null ) {
            extra = new LinkedHashMap<>();
            extraLocal.set(extra);
        }
        return extra;
    }

    /**
     * 是否需要跳过这次Mock;插件可以自己扩展;默认跳过入口的Mock
     *
     * @param event
     *            before事件
     * @param context
     *            回放上下文
     * @return 是否跳过
     */
    protected boolean skipMock(BeforeEvent event, Boolean entrance, RepeatContext context) {
        return false ;
    }
}
