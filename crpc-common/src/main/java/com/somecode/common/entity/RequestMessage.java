package com.somecode.common.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 请求实体
 * @author 落阳
 * @date 2023/3/3
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestMessage implements Serializable {

    /**
     * 类名
     */
    private String className;

    /**
     * 方法名
     */
    private String methodName;

    /**
     * 传参，要按照对应的参数类型
     */
    private List<Object> paramObjectList;

    /**
     * 参数类型，因为可能会被自动拆箱和装箱的干扰
     */
    private List<String> paramObjectTypeLit;

}
