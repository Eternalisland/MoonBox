package com.flux.collaboration.storage.record.plugins.dbfsop;

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


@MetaInfServices(InvokePlugin.class)
public class DatahubDbFsOpPlugin extends AbstractInvokePluginAdapter {

    /**
     *
     */
    private static final String DATAHUB_DBFS_OP_ENCHANCE_CLASS = "com.flux.collaboration.storage.common.dbstorage.impl.DatahubFileOperateServiceManager";

    // 查询文件
    private static final String DATAHUB_DBFS_OP_ENCHANCE_METHOD = "queryFile";


    /**
     * @return
     */
    @Override
    protected List<EnhanceModel> getEnhanceModels() {
        EnhanceModel.MethodPattern mp = EnhanceModel.MethodPattern.builder().methodName(DATAHUB_DBFS_OP_ENCHANCE_METHOD)
               .build();
        EnhanceModel dbfsop = EnhanceModel.builder()
                .classPattern(DATAHUB_DBFS_OP_ENCHANCE_CLASS)
                .methodPatterns(new EnhanceModel.MethodPattern[]{mp})
                .watchTypes(Event.Type.BEFORE, Event.Type.RETURN, Event.Type.THROWS)
                .build();
        return Arrays.asList(dbfsop);
    }

    @Override
    protected InvocationProcessor getInvocationProcessor() {
        return new DatahubDbFsOpProcessor(getType());
    }

    @Override
    public InvokeType getType() {
        return InvokeType.DATAHUB_DB_FS_OP;
    }

    @Override
    public String identity() {
        return InvokeType.DATAHUB_DB_FS_OP.getInvokeName();
    }

    @Override
    public boolean isEntrance() {
        return false;
    }

    /**
     * @param listener 调用监听
     * @return
     */
    protected EventListener getEventListener(InvocationListener listener) {
        return new DatahubDbFsOpEventListener(getType(), isEntrance(), listener, getInvocationProcessor());
    }
}
