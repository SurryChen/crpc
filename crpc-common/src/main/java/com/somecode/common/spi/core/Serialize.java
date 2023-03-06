package com.somecode.common.spi.core;

/**
 * 序列化的SPI
 */
public interface Serialize {

    /**
     * 序列化
     */
    public byte[] serialize(Object object);

    /**
     * 反序列化
     */
    public Object deserialize(byte[] bytes);

}
