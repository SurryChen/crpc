package com.somecode.common.spi.core;

/**
 * 压缩算法的SPI
 */
public interface CompressAlgorithm {

    /**
     * 压缩算法
     */
    public byte[] compress(byte[] input);

    /**
     * 解压缩算法
     */
    public byte[] uncompress(byte[] input);

}
