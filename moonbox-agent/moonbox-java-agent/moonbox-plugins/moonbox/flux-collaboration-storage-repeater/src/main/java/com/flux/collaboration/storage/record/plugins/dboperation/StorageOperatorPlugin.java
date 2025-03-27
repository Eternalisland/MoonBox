package com.flux.collaboration.storage.record.plugins.dboperation;

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

/**
 * @author yijiakang
 * @date 2025/1/27 23:50
 * @description 存储操作逻辑 功能描述
 */

@MetaInfServices(InvokePlugin.class)
public class StorageOperatorPlugin extends AbstractInvokePluginAdapter {

    private static final String ENHANCE_CLASS_NAME = "com.flux.collaboration.storage.base.dao.impl.StorageOperatorImpl";

    @Override
    public InvokeType getType() {
        return InvokeType.DATAHUB_STORAGE_OPERATOR;
    }

    @Override
    public String identity() {
        return InvokeType.DATAHUB_STORAGE_OPERATOR.getInvokeName();
    }

    @Override
    public boolean isEntrance() {
        return false;
    }

    /**
     * 数据库操作，数据库操作需要记录执行结果，回放的时候 mock
     *
     * @return
     */
    @Override
    protected List<EnhanceModel> getEnhanceModels() {

        // 执行存储过程
        EnhanceModel.MethodPattern excuteDBProcedure = EnhanceModel.MethodPattern.builder()
                .methodName("excuteDBProcedure")
//                .parameterType(new String[]{"com.flux.collaboration.storage.base.dao.helper.SqlParamUtils",
//                        "java.lang.String","java.util.List","java.util.List"})
                .build();

        EnhanceModel.MethodPattern getDBRecordBind = EnhanceModel.MethodPattern.builder()
                .methodName("getDBRecordBind")
                .parameterType(new String[]{"com.flux.collaboration.storage.base.dao.helper.SqlParamUtils",
                        "java.lang.String", "com.flux.collaboration.storage.base.dao.BizComponent"})
                .build();


        EnhanceModel.MethodPattern excuteDBSqlBind = EnhanceModel.MethodPattern.builder()
                .methodName("excuteDBSqlBind")
//                .parameterType(new String[]{"com.flux.collaboration.storage.base.dao.helper.SqlParamUtils",
//                        "java.lang.String"})
                .build();

        EnhanceModel.MethodPattern excuteDBSameSqlBind = EnhanceModel.MethodPattern.builder()
                .methodName("excuteDBSameSqlBind")
//                .parameterType(new String[]{"com.flux.collaboration.storage.base.dao.helper.SqlParamUtils",
//                        "java.lang.String","java.util.List","java.util.List"})
                .build();

        EnhanceModel.MethodPattern excuteDBSameListSqlBind = EnhanceModel.MethodPattern.builder()
                .methodName("excuteDBSameListSqlBind")
//                .parameterType(new String[]{"com.flux.collaboration.storage.base.dao.helper.SqlParamUtils",
//                        "java.lang.String","java.util.List","java.util.List"})
                .build();

        EnhanceModel.MethodPattern excuteDBDiffSqlBind = EnhanceModel.MethodPattern.builder()
                .methodName("excuteDBDiffSqlBind")
                .parameterType(new String[]{"com.flux.collaboration.storage.base.dao.helper.SqlParamUtils",
                        List.class.getCanonicalName(),
                        List.class.getCanonicalName(),
                        List.class.getCanonicalName(),
                        List.class.getCanonicalName()})
                .build();

        EnhanceModel em = EnhanceModel.builder()
                .classPattern(ENHANCE_CLASS_NAME)
                .methodPatterns(new EnhanceModel.MethodPattern[]{excuteDBProcedure, getDBRecordBind, excuteDBSameSqlBind, excuteDBSqlBind, excuteDBSameListSqlBind, excuteDBDiffSqlBind})
                .watchTypes(Event.Type.BEFORE, Event.Type.RETURN, Event.Type.THROWS)
                .build();

        return Arrays.asList(em);
    }

    @Override
    protected InvocationProcessor getInvocationProcessor() {
        return new StorageOperatorDataProcessor(getType());
    }


    /**
     * @param listener 调用监听
     * @return
     */
    protected EventListener getEventListener(InvocationListener listener) {
        return new StorageOperatorListener(getType(), isEntrance(), listener, getInvocationProcessor());
    }
}
