package com.flux.collaboration.storage.record.plugins.ruleop;

import com.alibaba.jvm.sandbox.api.ProcessControlException;
import com.alibaba.jvm.sandbox.api.event.BeforeEvent;
import com.alibaba.jvm.sandbox.repeater.plugin.api.InvocationListener;
import com.alibaba.jvm.sandbox.repeater.plugin.api.InvocationProcessor;
import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.api.DefaultEventListener;
import com.vivo.internet.moonbox.common.api.model.InvokeType;

/**
 * @author Administrator
 * @date 2025/1/28 23:13
 * @description 规则执行结果比较 功能描述
 */
public class RuleBaseProcessorListener extends DefaultEventListener {


    /**
     *
     * @param invokeType
     * @param entrance
     * @param listener
     * @param processor
     */
    public RuleBaseProcessorListener(InvokeType invokeType, boolean entrance, InvocationListener listener, InvocationProcessor processor) {
        super(invokeType, entrance, listener, processor);
    }

    /**
     * handle before event
     *
     * @param event
     *            event
     */
    protected void doBefore(BeforeEvent event) throws ProcessControlException {
        super.doBefore(event);
    }
}
