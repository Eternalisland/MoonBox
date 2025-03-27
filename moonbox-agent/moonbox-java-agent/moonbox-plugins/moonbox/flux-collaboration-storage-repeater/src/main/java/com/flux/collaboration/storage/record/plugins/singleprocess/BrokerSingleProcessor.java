package com.flux.collaboration.storage.record.plugins.singleprocess;

import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.api.DefaultInvocationProcessor;
import com.vivo.internet.moonbox.common.api.model.InvokeType;

/**
 * @author Administrator
 * @date 2025/2/4 22:44
 * @description 定时器执行逻辑 功能描述
 */
public class BrokerSingleProcessor  extends DefaultInvocationProcessor {
    public BrokerSingleProcessor(InvokeType type) {
        super(type);
    }
}
