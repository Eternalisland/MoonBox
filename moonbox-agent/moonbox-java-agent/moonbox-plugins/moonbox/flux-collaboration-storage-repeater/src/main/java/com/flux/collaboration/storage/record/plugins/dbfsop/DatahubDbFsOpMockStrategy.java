package com.flux.collaboration.storage.record.plugins.dbfsop;

import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.spi.AbstractReflectCompareStrategy;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.mock.MockRequest;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.mock.SelectResult;
import com.alibaba.jvm.sandbox.repeater.plugin.spi.MockStrategy;
import com.vivo.internet.moonbox.common.api.model.InvokeType;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.MetaInfServices;


@Slf4j
@MetaInfServices(MockStrategy.class)
public class DatahubDbFsOpMockStrategy  extends AbstractReflectCompareStrategy {

    /**
     * 执行类型
     * @return
     */
    @Override
    public String invokeType() {
        return InvokeType.DATAHUB_DB_FS_OP.name();
    }

    /**
     *
     * @param request mock request
     * @return
     */
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
