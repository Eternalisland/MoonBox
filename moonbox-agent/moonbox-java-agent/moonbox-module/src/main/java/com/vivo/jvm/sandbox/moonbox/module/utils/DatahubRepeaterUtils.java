package com.vivo.jvm.sandbox.moonbox.module.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.jvm.sandbox.repeater.plugin.common.Constants;
import com.alibaba.jvm.sandbox.repeater.plugin.core.bridge.ClassloaderBridge;
import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.api.DefaultBroadcaster;
import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.api.DefaultFlowDispatcher;
import com.alibaba.jvm.sandbox.repeater.plugin.core.model.MoonboxContext;
import com.alibaba.jvm.sandbox.repeater.plugin.core.serialize.SerializeException;
import com.alibaba.jvm.sandbox.repeater.plugin.core.trace.TraceGenerator;
import com.alibaba.jvm.sandbox.repeater.plugin.core.utils.HttpUtil;
import com.alibaba.jvm.sandbox.repeater.plugin.core.utils.MoonboxThreadPool;
import com.alibaba.jvm.sandbox.repeater.plugin.core.utils.SignUtils;
import com.alibaba.jvm.sandbox.repeater.plugin.core.wrapper.SerializerWrapper;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.RepeatMeta;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.RepeaterResult;
import com.alibaba.jvm.sandbox.repeater.plugin.spi.MockStrategy;
import com.google.common.collect.Lists;
import com.vivo.internet.moonbox.common.api.model.Invocation;
import com.vivo.internet.moonbox.common.api.model.RecordPullModel;
import com.vivo.internet.moonbox.common.api.model.RecordPullRequest;
import com.vivo.internet.moonbox.common.api.model.RecordWrapper;
import com.vivo.internet.moonbox.common.api.util.RetryAction;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author Administrator
 * @date 2025/2/6 18:12
 * @description 回放辅助类 功能描述
 */
@Slf4j
public class DatahubRepeaterUtils {


    private static final MoonboxContext MOONBOX_CONTEXT = MoonboxContext.getInstance();

    private static final String PULL_RECORD_URL = MOONBOX_CONTEXT.getHttpUrl() + Constants.RECORD_PULL_URL_PATH;
    /**
     * 拉取流量并进行流量分发
     *
     * @param recordPullRequest
     *            recordPullRequest
     * @return
     */
    public static void pullAndDispatch(RecordPullRequest recordPullRequest) {
        RecordPullModel recordPullModel;
        do {
            Thread.currentThread().setContextClassLoader(DefaultBroadcaster.class.getClassLoader());
            recordPullModel = pull(recordPullRequest);
            if (recordPullModel == null || CollectionUtils.isEmpty(recordPullModel.getRecords())) {
                log.info("get repeat data is empty param: {}", JSON.toJSONString(recordPullRequest));
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return;
            }
            int currentPageIndex = recordPullRequest.getPageIndex();
            // 保留历史逻辑兼容
            recordPullRequest.setPageIndex( currentPageIndex + 1);
            recordPullRequest.setScrollId(recordPullModel.getScrollId());
            List<CompletableFuture<Void>> futures = Lists.newArrayList();
            for (String recordWrapperStr : recordPullModel.getRecords()) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        log.info("replay record executing start...");
//                        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
//                        ClassLoader parent = contextClassLoader.getParent();
                        Class<?> classInstance = ClassloaderBridge.instance().findClassInstance("com.flux.collaboration.storage.base.dao.RecordMap");
                        ClassLoader classLoader = classInstance.getClassLoader();
//                        FieldUtils.writeField( FieldUtils.getDeclaredField(ClassLoader.class, "parent",true),contextClassLoader, classLoader, true);
                        RecordWrapper recordWrapper = SerializerWrapper.hessianDeserialize(recordWrapperStr,
                                RecordWrapper.class);
//                        FieldUtils.writeField(FieldUtils.getDeclaredField(ClassLoader.class, "parent",true) ,contextClassLoader, parent, true);
                        // 转换recordWrapper（流量数据）并且获取repeaterMeta（回放配置元数据）
                        RepeatMeta meta = convertWrapperAndMeta(recordWrapper);
                        // 进行流量的回放分发
                        DefaultFlowDispatcher.instance().dispatch(meta, recordWrapper.reTransform());
                        log.info("replay record executing end");
                    } catch (SerializeException e) {
                        log.info("deserialize repeat data failed:{}", JSON.toJSONString(recordPullRequest), e);
                    } catch (Throwable t) {
                        log.error("[Error-0000]-uncaught exception occurred when , params={}",
                                JSON.toJSONString(recordPullRequest), t);
                    }
                }, MoonboxThreadPool.MOONBOX_THREAD_POOL);
                futures.add(future);
            }

            CompletableFuture<?>[] strArray = futures.toArray(new CompletableFuture[0]);
            try {
                CompletableFuture<?> allFuture = CompletableFuture.allOf(strArray);
                allFuture.get(20, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        } while (recordPullModel.getHasNext());
    }


    /**
     * 拉取录制的数据
     *
     * @param recordPullRequest
     *            recordPullRequest
     * @return {@link RecordPullModel}
     */
    public static RecordPullModel pull(RecordPullRequest recordPullRequest) {
        Map<String, String> headers = SignUtils.getHeaders();
        headers.put("content-type", "application/json");
        RetryAction<String> retryAction = new RetryAction<String>("pull-record-data") {
            @Override
            protected String execute() {
                HttpUtil.Resp resp = HttpUtil.invokePostBody(PULL_RECORD_URL, headers,
                        JSON.toJSONString(recordPullRequest));
                if (!resp.isSuccess() || StringUtils.isEmpty(resp.getBody())) {
                    log.info("get repeat data failed param: {}, response:{}", JSON.toJSONString(recordPullRequest),
                            resp);
                    return null;
                }
                return resp.getBody();
            }
        };
        // 错误就重试5次
        String body = retryAction.retry(5, 3000);
        if (StringUtils.isBlank(body)) {
            return null;
        }
        RepeaterResult<RecordPullModel> recordPullModelResult = JSON.parseObject(body,
                new TypeReference<RepeaterResult<RecordPullModel>>() {
                });
        return recordPullModelResult.getData();
    }


    /**
     *
     * @param recordWrapper
     * @return
     * @throws SerializeException
     */
    public static RepeatMeta convertWrapperAndMeta(RecordWrapper recordWrapper) throws SerializeException {
        SerializerWrapper.inTimeDeserialize(recordWrapper.getEntranceInvocation());
        RepeatMeta meta = new RepeatMeta();
        meta.setAppName(MOONBOX_CONTEXT.getAppName());
        meta.setMock(MOONBOX_CONTEXT.getConfig().isRepeaterMock());
        meta.setTraceId(recordWrapper.getTraceId());
        MockStrategy.StrategyType strategyType = MockStrategy.StrategyType
                .getStrategyType(MOONBOX_CONTEXT.getConfig().getStrategyType());
        if (strategyType == null) {
            strategyType = MockStrategy.StrategyType.OBJECT_DFF;
        }
        meta.setStrategyType(strategyType);
        meta.setRepeatId(TraceGenerator.generate());
        if (meta.isMock() && CollectionUtils.isNotEmpty(recordWrapper.getSubInvocations())) {
            for (Invocation invocation : recordWrapper.getSubInvocations()) {
                SerializerWrapper.inTimeDeserialize(invocation);
            }
        }
        return meta;
    }
}
