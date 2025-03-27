package com.flux.collaboration.storage.record.util;

import com.alibaba.jvm.sandbox.repeater.plugin.core.spring.SpringContextAdapter;
import com.vivo.internet.moonbox.common.api.SimpleLimitStrategy;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.slf4j.Logger;

/**
 * 是否录制该条数据
 */
public class SimpleRecordedUtils /*implements SimpleLimitStrategy*/ {

    private static Logger logger = org.slf4j.LoggerFactory.getLogger(SimpleRecordedUtils.class);

    private static final String SIMPLE_IMPL_CLASS = "com.flux.collaboration.services.api.impl.agent.util.RecordSimpleUtils";
    /**
     * @param organizationId
     * @param datahubCustomerId
     * @param messageId
     * @return
     */
    public static boolean matchSimple(String organizationId, String datahubCustomerId, String messageId)  {
        /**
         *
         */
        Object bean = SpringContextAdapter.getBeanByName("recordSimpleUtils");
        if (bean != null) {
            try {
                return (boolean) MethodUtils.invokeMethod(bean, "matchSimple", organizationId, datahubCustomerId, messageId);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
        return false;
    }

    /**
     *
     * @param organizationId
     * @param datahubCustomerId
     * @param messageId
     * @throws NoSuchMethodException
     */
    public static void releaseSimple(String organizationId, String datahubCustomerId, String messageId) {
        /**
         *
         */
        Object bean = SpringContextAdapter.getBeanByName("recordSimpleUtils");
        if (bean != null) {
            try {
                 MethodUtils.invokeMethod(bean, "releaseSimple",organizationId, datahubCustomerId, messageId);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }
}
