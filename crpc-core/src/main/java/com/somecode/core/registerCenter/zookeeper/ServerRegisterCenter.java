package com.somecode.core.registerCenter.zookeeper;

import com.somecode.core.configuration.InitCoreConfiguration;
import org.I0Itec.zkclient.ZkClient;
import org.apache.zookeeper.CreateMode;

import java.util.Map;

/**
 * 注册中心处理
 */
public class ServerRegisterCenter {

    /**
     * Zookeeper路径中的信息
     */
    private static final String CRPC = "/crpc";

    private static ZkClient zc;

    public static void register() {
        try {
            // 获取配置文件信息
            Map zookeeperInfo = (Map) InitCoreConfiguration.requireCoreConfigure("Zookeeper");
            // 获取连接
            zc = new ZkClient(zookeeperInfo.get("ip") + ":" + zookeeperInfo.get("port"), (Integer) zookeeperInfo.get("sessionTimeout"), (Integer) zookeeperInfo.get("connectionTimeout"));
            StringBuffer stringBuffer = new StringBuffer(CRPC);
            if (!zc.exists(stringBuffer.toString())) {
                // 创建节点
                zc.create(stringBuffer.toString(), "", CreateMode.PERSISTENT);
            }
            // 通过配置文件获取注册的服务名
            Map serviceInfo = (Map) InitCoreConfiguration.requireCoreConfigure("Service");
            stringBuffer.append("/").append(serviceInfo.get("name"));
            // 创建服务目录
            if (!zc.exists(stringBuffer.toString())) {
                zc.create(stringBuffer.toString(), "", CreateMode.PERSISTENT);
            }
            stringBuffer.append("/").append(serviceInfo.get("ip") + ":" + serviceInfo.get("port"));
            // 再次创建
            // 该目录设置了服务数据
            zc.create(stringBuffer.toString(), serviceInfo.get("name"), CreateMode.EPHEMERAL);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
