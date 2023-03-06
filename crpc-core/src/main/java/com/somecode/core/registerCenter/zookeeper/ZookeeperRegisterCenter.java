package com.somecode.core.registerCenter.zookeeper;

import com.somecode.common.entity.NetworkNode;
import com.somecode.common.spi.core.RegisterCenter;
import com.somecode.common.util.StringUtils;

import java.util.List;

/**
 * 注册中心
 */
public class ZookeeperRegisterCenter implements RegisterCenter {

    @Override
    public List<NetworkNode> requireNetworkNodeList(String serviceName) {
        System.out.println("服务名:" + serviceName);
        if (!StringUtils.isEmpty(serviceName)) {
            return ZookeeperClient.getNetworkNodes(serviceName);
        }
        throw new RuntimeException("[Zookeeper]：服务名不能为空！");
    }

}
