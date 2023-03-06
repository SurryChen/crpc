package com.somecode.common.spi.core;

import com.somecode.common.entity.NetworkNode;

import java.util.List;

/**
 * 负载均衡算法的SPI
 */
public interface LoadBalance {

    /**
     * 传入所有结点，并从其中选出一个
     */
    public NetworkNode requireBetterNodeFromList(List<NetworkNode> networkNodeList);

}
