package com.somecode.server.configuration;

import com.somecode.common.util.Holder;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Map;

public class InitServiceImplConfiguration {

    /**
     * 默认加载路径
     */
    private static final String SERVICE_PATH = "service-impl.yml";

    /**
     * 配置文件所有内容
     */
    private static Map serviceConfigurationMap = null;

    /**
     * 加锁
     */
    private static Holder holder = new Holder();

    /**
     * 私有
     */
    private InitServiceImplConfiguration() {}

    /**
     * 加载配置文件并初始化策略
     */
    private static void initServiceConfiguration() {
        try {
            // 获取默认配置文件路径
            URL url = InitServerConfiguration.class.getClassLoader().getResource(SERVICE_PATH);
            // 开始解析文件
            Yaml yaml = new Yaml();
            // 加载
            serviceConfigurationMap = yaml.load(new BufferedReader(new InputStreamReader(url.openStream(), "utf-8")));
            serviceConfigurationMap = (Map) serviceConfigurationMap.get("Impl");
            // 初始化完毕
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取配置文件中标注名对应的全类名
     */
    public static String getFullClassName(String className) {
        if (serviceConfigurationMap == null) {
            synchronized (holder) {
                if (serviceConfigurationMap == null) {
                    initServiceConfiguration();
                }
            }
        }
        System.out.println(className);
        return serviceConfigurationMap.get(className).toString();
    }

}
