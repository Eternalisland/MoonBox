package com.flux.collaboration.storage.record.plugins.hessianserv;

import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.listener.EventListener;
import com.alibaba.jvm.sandbox.repeater.plugin.api.InvocationListener;
import com.alibaba.jvm.sandbox.repeater.plugin.api.InvocationProcessor;
import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.spi.AbstractInvokePluginAdapter;
import com.alibaba.jvm.sandbox.repeater.plugin.core.model.EnhanceModel;
import com.alibaba.jvm.sandbox.repeater.plugin.spi.InvokePlugin;
import com.vivo.internet.moonbox.common.api.model.InvokeType;
import org.kohsuke.MetaInfServices;

import java.util.Arrays;
import java.util.List;

import static com.vivo.internet.moonbox.common.api.model.InvokeType.DATAHUB_HESSIAN_SERVICE;


@MetaInfServices(InvokePlugin.class)
public class HessianServiceProcessPlugin extends AbstractInvokePluginAdapter {


    private static final String HESSIAN_SERVICE_WMS_CLASS = "com.flux.collaboration.services.api.impl.DatahubExternalServiceImpl";
    private static final String HESSIAN_SERVICE_TMS_CLASS = "com.flux.collaboration.services.api.impl.DatahubTmsExternalServiceImpl";
    private static final String HESSIAN_SERVICE_OCP_CLASS = "com.flux.collaboration.services.api.impl.DatahubOcpExternalServiceImpl";

    private static final String HESSIAN_SERVICE_METHOD = "invokeInterface";

    /**
     * @return
     */
    @Override
    protected List<EnhanceModel> getEnhanceModels() {
        EnhanceModel.MethodPattern mp = EnhanceModel.MethodPattern.builder().methodName(HESSIAN_SERVICE_METHOD).parameterType(new String[]{"java.lang.String", "java.lang.String"}).build();
        EnhanceModel wms = EnhanceModel.builder()
                .classPattern(HESSIAN_SERVICE_WMS_CLASS)
                .methodPatterns(new EnhanceModel.MethodPattern[]{mp})
                .watchTypes(Event.Type.BEFORE, Event.Type.RETURN, Event.Type.THROWS)
                .build();
        EnhanceModel tms = EnhanceModel.builder()
                .classPattern(HESSIAN_SERVICE_TMS_CLASS)
                .methodPatterns(new EnhanceModel.MethodPattern[]{mp})
                .watchTypes(Event.Type.BEFORE, Event.Type.RETURN, Event.Type.THROWS)
                .build();
        EnhanceModel ocp = EnhanceModel.builder()
                .classPattern(HESSIAN_SERVICE_OCP_CLASS)
                .methodPatterns(new EnhanceModel.MethodPattern[]{mp})
                .watchTypes(Event.Type.BEFORE, Event.Type.RETURN, Event.Type.THROWS)
                .build();
        return Arrays.asList(wms, tms, ocp);
    }

    @Override
    protected InvocationProcessor getInvocationProcessor() {
        return new HessianServiceProcessor(getType());
    }

    @Override
    public InvokeType getType() {
        return DATAHUB_HESSIAN_SERVICE;
    }

    @Override
    public String identity() {
        return DATAHUB_HESSIAN_SERVICE.getInvokeName();
    }

    @Override
    public boolean isEntrance() {
        return true;
    }

    @Override
    protected EventListener getEventListener(InvocationListener listener) {
        return new HessianServiceEventListener(getType(), isEntrance(), listener, getInvocationProcessor());
    }
}
