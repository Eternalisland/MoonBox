package com.flux.collaboration.storage.record.plugins.hessianserv;

import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.api.DefaultInvocationProcessor;
import com.vivo.internet.moonbox.common.api.model.InvokeType;

public class HessianServiceProcessor extends DefaultInvocationProcessor {
    /**
     *
     * @param type
     */
    public HessianServiceProcessor(InvokeType type) {
        super(type);
    }
}
