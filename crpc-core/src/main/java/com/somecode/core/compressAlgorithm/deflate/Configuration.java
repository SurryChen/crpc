package com.somecode.core.compressAlgorithm.deflate;

import com.somecode.core.configuration.InitCoreConfiguration;

import java.util.Map;

/**
 * 获取配置文件中的参数
 */
public class Configuration {

    public static Integer getDeflateLevel() {
        Integer level = null;
        try {
            Map compressAlgorithm = (Map) InitCoreConfiguration.requireCoreConfigure("CompressAlgorithm");
            Map deflateCompressAlgorithm = (Map) compressAlgorithm.get("DeflateCompressAlgorithm");
            level = Integer.parseInt(deflateCompressAlgorithm.get("level").toString());
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Deflate压缩算法格式错误！");
        }
        return level;
    }

}
