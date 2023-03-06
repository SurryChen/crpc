package com.somecode.client.connection;

import cn.hutool.core.util.ByteUtil;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 字节Id解释和生成器
 */
public class IdGenerator {

    /**
     * Id
     */
    private AtomicInteger sessionId = new AtomicInteger(0);

    /**
     * 最大值
     */
    private final int max = Integer.MAX_VALUE;

    // 获取数字Id
    public Integer createSessionID() {
        if (sessionId.get() == max) {
            sessionId.compareAndSet(max, 0);
        }
        return sessionId.getAndIncrement();
    }

    /**
     * 根据int类型的数字获取一个长度为4的byte数组
     */
    public byte[] intToBytes(Integer num) {
        return ByteUtil.intToBytes(num);
    }

    // 传入一个长度4的byte数组，转换成int类型数字
    public Integer bytesToInt(byte[] bytes) {
        if(bytes.length != 4) {
            throw new IllegalArgumentException("字节数组长度必须为4才能进行转换！");
        }
        return ByteUtil.bytesToInt(bytes);
    }

}
