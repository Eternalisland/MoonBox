package com.vivo.internet.moonbox.common.api;

/**
 * @author yijiakang
 * @date 2025/2/2 11:45
 * @description 策略 功能描述
 */
public interface SimpleLimitStrategy {


    /**
     *  是否满足 记录条件，所有的 插件应该实现这个方法
     * @return
     */
    boolean matchSimple(String organizationId, String datahubCustomerId, String messageId);


    /**
     *
     * @param organizationId
     * @param datahubCustomerId
     * @param messageId
     */
    void releaseSimple(String organizationId, String datahubCustomerId, String messageId);

}
