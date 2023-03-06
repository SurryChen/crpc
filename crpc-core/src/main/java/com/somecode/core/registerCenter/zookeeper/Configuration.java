package com.somecode.core.registerCenter.zookeeper;

import com.somecode.common.util.StringUtils;
import com.somecode.core.configuration.InitCoreConfiguration;

import java.util.Map;

public class Configuration {

    /**
     * 默认超时时间
     */
    private static final Integer SESSION_TIMEOUT = 3000;
    private static final Integer CONNECTION_TIMEOUT = 3000;

    /**
     * 获取配置文件中信息
     */
    public static ZookeeperInfo getZookeeperInfo() {
        // 获取配置内容
        Map zookeeperInfoMap = (Map) InitCoreConfiguration.requireCoreConfigure("Zookeeper");
        // 存放配置信息
        ZookeeperInfo zookeeperInfo = new ZookeeperInfo();
        // 获取具体信息
        String ip = zookeeperInfoMap.get("ip").toString();
        if (StringUtils.isEmpty(ip)) {
            return null;
        }
        Integer port = null;
        Integer sessionTimeout = null;
        Integer connectionTimeout = null;
        try {
            port = new Integer(zookeeperInfoMap.get("port").toString());
            sessionTimeout = new Integer(zookeeperInfoMap.get("sessionTimeout").toString());
            connectionTimeout = new Integer(zookeeperInfoMap.get("connectionTimeout").toString());
        }catch (Exception e) {
            throw new IllegalArgumentException("Zookeeper配置出错！");
        } finally {
            // 说明有端口
            if (port != null) {
                if (sessionTimeout == null) {
                    sessionTimeout = SESSION_TIMEOUT;
                }
                if (connectionTimeout == null) {
                    connectionTimeout = CONNECTION_TIMEOUT;
                }
            } else {
                return null;
            }
        }
        // 读取正常，开始填充
        zookeeperInfo.setIp(ip);
        zookeeperInfo.setPort(port);
        zookeeperInfo.setSessionTimeout(sessionTimeout);
        zookeeperInfo.setConnectionTimeout(connectionTimeout);
        return zookeeperInfo;
    }

}
