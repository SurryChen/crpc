package com.somecode.common.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 策略分组
 * 目前支持的自定义策略组的策略有：序列化方案、注册中心、负载均衡策略、压缩算法
 * @author 落阳
 * @date 2023/2/27
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StrategyGroup implements Serializable {

    /**
     * 策略组名字
     */
    private String name;

    /**
     * 序列化策略
     */
    private String serialize;

    /**
     * 注册中心
     */
    private String registerCenter;

    /**
     * 负载均衡策略
     */
    private String loadBalance;

    /**
     * 压缩算法策略
     */
    private String compressAlgorithm;

}
