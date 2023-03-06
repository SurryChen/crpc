package com.somecode.core.registerCenter.zookeeper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Zookeeper连接参数信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ZookeeperInfo implements Serializable {

    private String ip;

    private Integer port;

    /**
     * 会话允许超时时间
     */
    private Integer sessionTimeout;

    /**
     * 连接允许超时时间
     */
    private Integer connectionTimeout;

}
