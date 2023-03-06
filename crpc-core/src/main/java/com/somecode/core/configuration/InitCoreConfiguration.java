package com.somecode.core.configuration;

import com.somecode.common.util.Holder;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Map;

/**
 * 提供加载yml后的Map，防止多次加载
 * 具体的获取内容交由具体的服务供应商
 */
public class InitCoreConfiguration {

    /**
     * 日志
     */
//    private Logger log = LoggerFactory.getLogger(InitCoreConfiguration.class);

    /**
     * 配置文件路径
     */
    private static final String CONFIGURATION_PATH = "core-configure.yml";

    /**
     * 配置文件内容
     */
    private static Map<String, Object> coreConfigureMap = null;

    /**
     * 做个锁
     */
    private static Holder holder = new Holder();

    /**
     * 提供一个获取的出口
     */
    public static Object requireCoreConfigure(String configurationName) {
        // 判断是否有初始化
        if (coreConfigureMap == null) {
            // 创建一个该类对象去初始化
            synchronized (holder) {
                if (coreConfigureMap == null) {
                    try {
                        init();
                    } catch (Exception e) {
                        e.printStackTrace();
//                        initFile.log.info("core解析配置文件异常:" + e);
                    }
                }
            }
        }
        return coreConfigureMap.get(configurationName);
    }

    /**
     * 初始化
     * @throws IOException
     */
    private static void init() throws IOException {
        // 获取默认配置文件路径
//        URL url = InitFromConfigureFile.class.getClassLoader().getResource(STRATEGY_GROUP);
        URL url = InitCoreConfiguration.class.getClassLoader().getResource(CONFIGURATION_PATH);
        if(url == null) {
            throw new IOException(CONFIGURATION_PATH + "文件不存在classes路径下！");
        }
        // 开始解析文件
        Yaml yaml = new Yaml();
        coreConfigureMap = yaml.load(new BufferedReader(new InputStreamReader(url.openStream(), "utf-8")));
//        log.info("core-configuration解析完毕！");
    }

}
