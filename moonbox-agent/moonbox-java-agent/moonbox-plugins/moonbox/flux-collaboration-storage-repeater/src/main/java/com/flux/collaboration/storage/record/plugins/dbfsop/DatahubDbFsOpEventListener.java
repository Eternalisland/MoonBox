package com.flux.collaboration.storage.record.plugins.dbfsop;

import com.alibaba.jvm.sandbox.api.event.ReturnEvent;
import com.alibaba.jvm.sandbox.repeater.plugin.api.InvocationListener;
import com.alibaba.jvm.sandbox.repeater.plugin.api.InvocationProcessor;
import com.alibaba.jvm.sandbox.repeater.plugin.core.ContextResourceClear;
import com.alibaba.jvm.sandbox.repeater.plugin.core.cache.MoonboxRecordCache;
import com.alibaba.jvm.sandbox.repeater.plugin.core.cache.MoonboxRepeatCache;
import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.api.DefaultEventListener;
import com.alibaba.jvm.sandbox.repeater.plugin.core.trace.Tracer;
import com.vivo.internet.moonbox.common.api.model.Invocation;
import com.vivo.internet.moonbox.common.api.model.InvokeType;
import org.apache.commons.lang3.reflect.MethodUtils;

import java.util.ArrayList;
import java.util.List;

public class DatahubDbFsOpEventListener extends DefaultEventListener {
    /**
     *
     */
    private final ThreadLocal<Object> bizRef = new ThreadLocal<>();

    public DatahubDbFsOpEventListener(InvokeType invokeType, boolean entrance, InvocationListener listener, InvocationProcessor processor) {
        super(invokeType, entrance, listener, processor);
    }

    /**
     * handle return event
     *
     * @param event
     *            event
     */
    public void doReturn(ReturnEvent event) {
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
        if (InvokeType.JAVA_SHUFFLE_PLUGIN != invocation.getType()) {
//            invocation.setResponse(processor.assembleResponse(event));
            if(invocation.getIdentity().getEndpoint().startsWith("getDBRecordBind")) {
                try {
                    Object biz = bizRef.get();
                    List records = (List) MethodUtils.invokeMethod(biz, "getRecords");
                    List transferList = new ArrayList();
                    transferList.addAll(records) ;
                    invocation.setResponse(transferList);
                    bizRef.remove();
                } catch (Exception e) {
                    invocation.setResponse("反射执行 getRecords 方法失败") ;
                }
            }else {
                invocation.setResponse(processor.assembleResponse(event));
            }
            invocation.setResponseType(processor.assembleResponseType(event));
        }
        invocation.setEnd(System.currentTimeMillis());
        listener.onInvocation(invocation);
    }
}
