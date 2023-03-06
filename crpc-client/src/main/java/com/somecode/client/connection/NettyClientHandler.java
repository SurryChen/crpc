package com.somecode.client.connection;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class NettyClientHandler extends ChannelInboundHandlerAdapter {

    /**
     * byte数组中id的长度
     */
    private static final Integer ID_LENGTH = 4;

    /**
     * 收集返回的数据包
     */
    private ConcurrentHashMap<String, byte[]> responseMap = new ConcurrentHashMap<>();

    /**
     * 用来阻塞线程到设置的timeout
     */
    private ConcurrentHashMap<String, CountDownLatch> countMap = new ConcurrentHashMap<>();

    /**
     * 用来解析byte数组中的Id
     */
    IdGenerator idGenerator = null;

    public NettyClientHandler(IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    /**
     * 当客户端连接服务器完成就会触发该方法
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        // 暂无操作，可以打印日志
        System.out.println(ctx.name() + "与服务器连接成功！");
    }

    /**
     * 当通道有读取事件时会触发，即服务端发送数据给客户端
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        System.out.println("触发事件，客户端返回了信息");
        // 读取数据并放入到map中
        byte[] all = (byte[]) msg;
        byte[][] bytes = takeApart(ID_LENGTH, all);
        // 解锁id
        Integer id = idGenerator.bytesToInt(bytes[0]);
        // 放入map
        responseMap.put(id + "", bytes[1]);
        // 释放注释
        countMap.get(id + "").countDown();
        // 移除
        countMap.remove(id + "");
    }

    /**
     * 读取完毕
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    /**
     * 发生异常的处理
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    /**
     * 放入数据包需要在这里放入一个CountDownLatch，用来做后续的session超时设置
     */
    public void setData(Integer id) {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countMap.put(id + "", countDownLatch);
    }

    /**
     * 获取返回的数据包
     * 需要靠解析byte数组来放入
     */
    public byte[] getData(Integer id, Integer timeout) throws Exception {
        byte[] bytes = responseMap.get(id + "");
        if (bytes == null || bytes.length == 0) {
            // 拿不到
            // 阻塞超时时间
            CountDownLatch count = countMap.get(id + "");
            if (count == null) {
                // 只会在read结束后释放，所以此时肯定是有了
                return responseMap.get(id + "");
            }
            // 如果不等于null，就算此时被删除了也没有任何关系
            // 毫秒，等待时间到了自动执行下去，或者已经读取了数据自动释放
            count.await(timeout, TimeUnit.MILLISECONDS);
            // 再一次获取
            bytes = responseMap.get(id + "");
        }
        if (bytes == null || bytes.length == 0) {
            // 拿不到，重新发送等等方式，都需要删除
            responseMap.remove(id + "");
            return null;
        }
        // 返回
        // 拿到了也需要删除
        responseMap.remove(id + "");
        return bytes;
    }

    /**
     * 拆解
     */
    private byte[][] takeApart(Integer idBytesLength, byte[] all) {
        byte[][] bytes = new byte[2][];
        bytes[0] = new byte[idBytesLength];
        bytes[1] = new byte[all.length - idBytesLength];
        // 将all的0-idLength复制到bytes[0]
        System.arraycopy(all, 0, bytes[0], 0, bytes[0].length);
        // 将all的idLength-all.length复制到bytes[1]
        System.arraycopy(all, bytes[0].length, bytes[1], 0, bytes[1].length);
        return bytes;
    }

}
