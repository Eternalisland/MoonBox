package com.flux.collaboration.storage.record.plugins.hessian;

import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.spi.AbstractReflectCompareStrategy;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.mock.MockRequest;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.mock.SelectResult;
import com.alibaba.jvm.sandbox.repeater.plugin.spi.MockStrategy;
import com.vivo.internet.moonbox.common.api.model.InvokeType;
import org.kohsuke.MetaInfServices;

/**
 * @author Administrator
 * @date 2025/2/8 17:05
 * @description mock 策略 功能描述
 */
@MetaInfServices(MockStrategy.class)
public class HessianBaseProcessorMockStrategy extends AbstractReflectCompareStrategy {

    @Override
    public String invokeType() {
        return InvokeType.DATAHUB_HESSIAN_SERVICE.name();
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
}
