package com.somecode.client.configure;

import com.somecode.common.entity.NetworkNode;
import com.somecode.common.entity.StrategyGroup;
import com.somecode.common.entity.ServiceInfo;
import com.somecode.common.util.Holder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

/**
 * 根据配置文件初始化RPC策略分组
 *
 * @author 落阳
 * @date 2023/2/27
 */
public class InitFromConfigureFile {

    /**
     * 加载日志
     */
    private static Logger log = LoggerFactory.getLogger(InitFromConfigureFile.class);

    /**
     * 策略组配置文件名
     */
    private static final String STRATEGY_GROUP = "strategy-group.yml";

    /**
     * 服务组配置文件名
     */
    private static final String SERVICE_GROUP = "service-group.yml";

    /**
     * 策略组缓存
     */
    private static Map<String, StrategyGroup> strategyGroupMap = new HashMap<>();

    /**
     * 服务组缓存
     */
    private static Map<String, ServiceInfo> serviceInfoMap = null;

    /**
     * 加个锁
     */
    private static Holder holder = new Holder();

    /**
     * 不允许创建对象
     */
    private InitFromConfigureFile() { }

    /**
     * 提供获取缓存的出口，因为缓存不允许被其他类修改
     */
    public static ServiceInfo getServiceInfo(String serviceName) {
        // 只加载一次配置文件，单例加载
        if (serviceInfoMap == null) {
            synchronized (holder) {
                if (serviceInfoMap == null) {
                    try {
                        serviceInfoMap = new HashMap<>();
                        init();
                    } catch (Exception e) {
                        log.info(SERVICE_GROUP + "和" + STRATEGY_GROUP + "初始化失败！");
                    }
                }
            }
        }
        ServiceInfo serviceInfo = serviceInfoMap.get(serviceName);
        if (serviceInfo == null) {
            log.info(serviceName + "服务未配置！");
        }
        return serviceInfo;
    }

    /**
     * 初始化策略组和服务组，并配对
     */
    public static void init() throws IOException {
        log.info("开始初始化----------------------");
        // 初始化策略组
        initStrategyConfigure();
        // 初始化服务组
        initServiceConfigure();
        log.info("结束初始化----------------------");
    }

    /**
     * 读取策略组文件并初始化
     */
    private static void initStrategyConfigure() throws IOException {
        // 清空缓存
        strategyGroupMap.clear();
        // 获取默认配置文件路径
        ClassLoader classLoader = InitFromConfigureFile.class.getClassLoader();
        URL url = classLoader.getResource(STRATEGY_GROUP);
        if(url == null) {
            throw new IOException(STRATEGY_GROUP + "文件不存在classes路径下！");
        }
        // 开始解析文件
        Yaml yaml = new Yaml();
        Map<String, Object> strategyConfigureMap = yaml.load(new BufferedReader(new InputStreamReader(url.openStream(), "utf-8")));
        // 将内容填充到策略组实体
        StrategyGroup strategyGroup = null;
        List<LinkedHashMap> strategyList = (List<LinkedHashMap>) strategyConfigureMap.get("Strategy");
        for (LinkedHashMap map: strategyList) {
            strategyGroup = new StrategyGroup();
            strategyGroup.setName(map.get("name").toString());
            strategyGroup.setLoadBalance(map.get("loadBalance").toString());
            strategyGroup.setCompressAlgorithm(map.get("compressAlgorithm").toString());
            strategyGroup.setSerialize(map.get("serialize").toString());
            strategyGroup.setRegisterCenter(map.get("registerCenter").toString());
            strategyGroupMap.put(strategyGroup.getName(), strategyGroup);
        }
        // 装载完毕
        log.info("策略组装载完毕！");
    }

    /**
     * 装载服务配置分组
     */
    private static void initServiceConfigure() throws IOException {
        // 获取默认配置文件路径
        URL url = InitFromConfigureFile.class.getClassLoader().getResource(SERVICE_GROUP);
        if(url == null) {
            throw new IOException(STRATEGY_GROUP + "文件不存在classes路径下！");
        }
        // 开始解析文件
        Yaml yaml = new Yaml();
        Map<String, Object> serviceConfigureMap = yaml.load(new BufferedReader(new InputStreamReader(url.openStream(), "utf-8")));
        // 服务载体
        ServiceInfo serviceInfo = null;
        // 节点载体
        NetworkNode networkNode = null;
        // 节点列表载体
        LinkedList<NetworkNode> networkNodeList = null;
        // 读取文件
        List<LinkedHashMap> serviceList = (List<LinkedHashMap>) serviceConfigureMap.get("Service");
        for (LinkedHashMap serviceMap: serviceList) {
            serviceInfo = new ServiceInfo();
            serviceInfo.setName(serviceMap.get("name").toString());
            serviceInfo.setTimeout(new Integer(serviceMap.get("timeout").toString()));
            // 装载节点信息
            networkNodeList = new LinkedList();
            // 读取配置文件中
            List<LinkedHashMap> networkNodeConfigureList = (List<LinkedHashMap>) serviceMap.get("netWorkNode");
            for (LinkedHashMap networkMap: networkNodeConfigureList) {
                networkNode = new NetworkNode();
                networkNode.setName(networkMap.get("name").toString());
                networkNode.setHost(networkMap.get("host").toString());
                networkNode.setPort(new Integer(networkMap.get("port").toString()));
                networkNodeList.add(networkNode);
            }
            serviceInfo.setNetworkNode(networkNodeList);
            serviceInfo.setStrategyGroup(strategyGroupMap.get(serviceMap.get("strategy").toString()));
            serviceInfoMap.put(serviceInfo.getName(), serviceInfo);
        }
        // 装载完毕
        log.info("服务组装载完毕！");
    }

}
