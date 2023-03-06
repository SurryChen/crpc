package com.somecode.common.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 节点信息
 * @author 落阳
 * @date 2023/2/27
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class NetworkNode implements Serializable {

    /**
     * 节点名称
     * 实际上并不会用到，只是节点出错的时候，便于管理员去找到哪一个结点
     */
    private String name;

    /**
     * 节点ip
     */
    private String host;

    /**
     * 节点端口
     */
    private Integer port;

}
