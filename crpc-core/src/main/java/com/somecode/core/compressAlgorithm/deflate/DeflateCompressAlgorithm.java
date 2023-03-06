package com.somecode.core.compressAlgorithm.deflate;

import com.somecode.common.spi.core.CompressAlgorithm;

import java.io.ByteArrayOutputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Deflate压缩算法
 */
public class DeflateCompressAlgorithm implements CompressAlgorithm {

    /**
     * 压缩
     * @param input
     * @return
     */
    @Override
    public byte[] compress(byte[] input) {
        // 获取配置文件设置的等级划分
        Integer level = Configuration.getDeflateLevel();
        if (level == null) {
            // 默认等级为1
            level = 1;
        }
        // 创建压缩算法类
        Deflater compressor = new Deflater(level);
        // 字节数组输入流
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        // 字节数组
        byte[] bytes = null;
        try {
            // 输入字节数组
            compressor.setInput(input);
            // 调用时，表示压缩应以输入缓冲区的当前内容结束。
            compressor.finish();
            // 缓冲区大小
            final byte[] buf = new byte[2048];
            // 如果已到达压缩数据输出流的末尾，则返回 true
            // 没有结束就继续
            while (!compressor.finished()) {
                // 将压缩的内容输入到buf字节数组中
                int count = compressor.deflate(buf);
                // 将buf字节数组内容写入到bos中
                bos.write(buf, 0, count);
            }
            bytes = bos.toByteArray();
        } finally {
            // 关闭压缩器并丢弃任何未处理的输入
            compressor.end();
            try {
                bos.close();
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
        // 返回压缩后的字节数组
        return bytes;
    }

    /**
     * 解压
     * 操作与上面类似，就不写注释了
     * @param input
     * @return
     */
    @Override
    public byte[] uncompress(byte[] input) {
        Inflater decompressor = new Inflater();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] bytes = null;
        try {
            decompressor.setInput(input);
            final byte[] buf = new byte[2048];
            while (!decompressor.finished()) {
                int count = decompressor.inflate(buf);
                bos.write(buf, 0, count);
            }
            bytes = bos.toByteArray();
        } catch (DataFormatException e) {
            e.printStackTrace();
        } finally {
            decompressor.end();
            try{
                bos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return bytes;
    }
}
