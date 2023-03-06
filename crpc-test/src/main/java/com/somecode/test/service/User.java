package com.somecode.test.service;

import lombok.Data;

import java.io.Serializable;

/**
 * 模拟简单的实体
 */
@Data
public class User implements Serializable {

    /**
     * 姓名
     */
    private String name;

}
