package com.flux.collaboration.storage.record.plugins.monitor.message;

import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.spi.AbstractReflectCompareStrategy;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.mock.MockRequest;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.mock.MockResponse;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.mock.SelectResult;
import com.alibaba.jvm.sandbox.repeater.plugin.spi.MockStrategy;
import com.vivo.internet.moonbox.common.api.model.InvokeType;
import org.kohsuke.MetaInfServices;

/**
 * @author Administrator
 * @date 2025/2/8 9:53
 * @description 监控处理模块 mock 逻辑 功能描述
 */
@MetaInfServices(MockStrategy.class)
public class MessageMonitorMockStrategy extends AbstractReflectCompareStrategy {
    /**
     *
     * @return
     */
    @Override
    public String invokeType() {
        return InvokeType.DATAHUB_MONITOR_MESSAGE.name();
    }

    @Override
    protected SelectResult select(MockRequest request) {
        SelectResult select = super.select(request);
        SelectResult selectResult = SelectResult.builder()
                .match(true)
                .invocation(select.getInvocation())
                .cost(select.getCost())
                .build();
        return selectResult;
    }


    /**
     *
     * @param request mock请求对象
     * @return
     */
    @Override
    public MockResponse execute(final MockRequest request) {
        MockResponse mockResponse = super.execute(request);
        mockResponse.action =  MockResponse.Action.SKIP_IMMEDIATELY;
        return mockResponse;
    };

}
