package com.somecode.server.configuration;

import com.somecode.common.spi.core.CompressAlgorithm;
import com.somecode.common.spi.core.Serialize;
import com.somecode.common.spi.extension.ExtensionLoader;
import com.somecode.common.util.Holder;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 根据配置文件返回指定
 */
public class InitServerConfiguration {

    /**
     * 默认加载路径
     */
    private static final String SERVER_PATH = "server-configuration.yml";

    /**
     * 指定的序列化方案
     */
    private static Serialize serialize;

    /**
     * 指定的压缩方案
     */
    private static CompressAlgorithm compressAlgorithm;

    /**
     * 配置文件所有内容
     */
    private static Map serverConfigurationMap = null;

    /**
     * 做个锁用于加载配置文件
     */
    private static Holder holder = new Holder<>();

    /**
     * 不允许创建对象
     */
    private InitServerConfiguration() {
    }

    /**
     * 加载配置文件并初始化策略
     */
    private static void initServerConfiguration() {
        try {
            // 获取默认配置文件路径
            URL url = InitServerConfiguration.class.getClassLoader().getResource(SERVER_PATH);
            // 开始解析文件
            Yaml yaml = new Yaml();
            // 加载
            serverConfigurationMap = yaml.load(new BufferedReader(new InputStreamReader(url.openStream(), "utf-8")));
            // 还不够
            serverConfigurationMap = (LinkedHashMap) serverConfigurationMap.get("Server");
            ExtensionLoader<Serialize> serializeExtensionLoader = ExtensionLoader.load(Serialize.class);
            serialize = serializeExtensionLoader.getExtension(serverConfigurationMap.get("serialize").toString());
            ExtensionLoader<CompressAlgorithm> compressAlgorithmExtensionLoader = ExtensionLoader.load(CompressAlgorithm.class);
            compressAlgorithm = compressAlgorithmExtensionLoader.getExtension(serverConfigurationMap.get("compressAlgorithm").toString());
            // 初始化完毕
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取序列化方案对象
     */
    public static Serialize getSerialize() {
        if (serialize == null) {
            synchronized (holder) {
                if (serialize == null) {
                    initServerConfiguration();
                }
            }
        }
        return serialize;
    }

    /**
     * 获取压缩方案
     */
    public static CompressAlgorithm getCompressAlgorithm() {
        if (compressAlgorithm == null) {
            synchronized (holder) {
                if (compressAlgorithm == null) {
                    initServerConfiguration();
                }
            }
        }
        return compressAlgorithm;
    }

}
