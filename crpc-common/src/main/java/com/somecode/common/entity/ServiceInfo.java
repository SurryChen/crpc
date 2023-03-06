package com.somecode.common.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 服务信息，包括服务对应的多个结点
 * @author 落阳
 * @date 2023/2/27
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServiceInfo implements Serializable {

    /**
     * 服务名称
     */
    private String name;

    /**
     * 服务对应的节点列表
     */
    private List<NetworkNode> networkNode;

    /**
     * 策略组
     */
    private StrategyGroup strategyGroup;

    /**
     * 超时设置
     * 默认是ms（毫秒）
     */
    private Integer timeout;

}
