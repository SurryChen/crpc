package com.somecode.common.spi.core;

import com.somecode.common.entity.NetworkNode;

import java.util.List;

/**
 * 注册中心的SPI
 */
public interface RegisterCenter {

    /**
     * 动态获取服务列表
     */
    public List<NetworkNode> requireNetworkNodeList(String serviceName);

}
