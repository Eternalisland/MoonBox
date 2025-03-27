package com.flux.collaboration.storage.record.plugins.adapter;


import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.spi.AbstractReflectCompareStrategy;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.mock.MockRequest;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.mock.MockResponse;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.mock.SelectResult;
import com.alibaba.jvm.sandbox.repeater.plugin.spi.MockStrategy;
import com.vivo.internet.moonbox.common.api.model.InvokeType;
import org.kohsuke.MetaInfServices;

@MetaInfServices(MockStrategy.class)
public class DatahubAdapterBaseMockStrategy extends AbstractReflectCompareStrategy {
    @Override
    public String invokeType() {
        return InvokeType.DATAHUB_ADAPTER_BASE.name();
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
