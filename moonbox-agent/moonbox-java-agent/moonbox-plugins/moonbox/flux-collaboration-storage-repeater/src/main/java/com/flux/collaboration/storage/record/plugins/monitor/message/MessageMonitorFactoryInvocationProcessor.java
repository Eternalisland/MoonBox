package com.flux.collaboration.storage.record.plugins.monitor.message;

import com.alibaba.jvm.sandbox.api.event.BeforeEvent;
import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.api.DefaultInvocationProcessor;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.RepeatContext;
import com.vivo.internet.moonbox.common.api.model.InvokeType;

/**
 * @author Administrator
 * @date 2025/2/7 11:32
 * @description 处理逻辑 功能描述
 */
public class MessageMonitorFactoryInvocationProcessor extends DefaultInvocationProcessor {

    /**
     *
     * @param type
     */
    public MessageMonitorFactoryInvocationProcessor(InvokeType type) {
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
}
