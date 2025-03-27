package com.flux.collaboration.storage.record.plugins.hessian;

import com.alibaba.jvm.sandbox.api.event.BeforeEvent;
import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.api.DefaultInvocationProcessor;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.RepeatContext;
import com.alibaba.jvm.sandbox.repeater.plugin.utils.ParameterTypesUtil;
import com.vivo.internet.moonbox.common.api.model.Identity;
import com.vivo.internet.moonbox.common.api.model.InvokeType;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Administrator
 * @date 2025/2/8 17:04
 * @description 处理类 功能描述
 */
public class HessianBaseProcessorInvocationProcessor  extends DefaultInvocationProcessor {

    private ThreadLocal<Map<String,String>> extraLocal = new ThreadLocal<>();

    /**
     *
     * @param type
     */
    public HessianBaseProcessorInvocationProcessor(InvokeType type) {
        super(type);
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


    /**
     *
     * @param event 事件
     * @return
     */
    @Override
    public Identity assembleIdentity(BeforeEvent event) {
        Object arguments [] = event.argumentArray;
        String appId = (String) arguments[0];
        String clsId = (String) arguments[1];
        String method = (String) arguments[2];
        getExtra().put("appId", appId);
        getExtra().put("clsId", clsId);
        getExtra().put("method", method);
        Identity identity = new Identity(getType().name(), event.javaClassName,
                event.javaMethodName + ParameterTypesUtil.getTypesStrByObjects(event.argumentArray), getExtra());
        extraLocal.remove();
        return identity;
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

}
