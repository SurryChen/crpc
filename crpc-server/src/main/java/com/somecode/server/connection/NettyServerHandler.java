package com.somecode.server.connection;

import com.somecode.common.entity.RequestMessage;
import com.somecode.common.spi.core.CompressAlgorithm;
import com.somecode.common.spi.core.Serialize;
import com.somecode.server.cache.MethodCache;
import com.somecode.server.configuration.InitServerConfiguration;
import com.somecode.server.configuration.InitServiceImplConfiguration;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;

import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.List;

public class NettyServerHandler extends ChannelInboundHandlerAdapter {

    /**
     * Id长度
     */
    Integer ID_LENGTH = 4;

    /**
     * 当客户端连接服务器完成就会触发该方法
     *
     * @param ctx
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("客户端连接通道建立完成");
    }

    /**
     * 读取客户端发送的数据
     *
     * @param ctx 上下文对象, 含有通道channel，管道pipeline
     * @param msg 就是客户端发送的数据
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        byte[] bytes = (byte[]) msg;
        System.out.println("数据长度: " + bytes.length);
        byte[][] idAndData = takeApart(ID_LENGTH, bytes);
        // 反压缩
        CompressAlgorithm compressAlgorithm = InitServerConfiguration.getCompressAlgorithm();
        System.out.println("压缩算法：" + compressAlgorithm);
        byte[] data = null;
        try {
            data = compressAlgorithm.uncompress(idAndData[1]);
        }catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("数据长度data：" + data.length);
        System.out.println(new String(data));
        // 反序列化
        Serialize serialize = InitServerConfiguration.getSerialize();
        System.out.println("序列化算法：" + serialize);
        RequestMessage requestMessage = null;
        try {
            requestMessage = (RequestMessage) serialize.deserialize(data);
        }catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(requestMessage.toString());
        // 根据信息中的要求加载对应的实现类的对应方法
        // 先是对应的实现类
        // 实现类的全类名
        String fullClassName = "";
        Method method = null;
        try {
            fullClassName = InitServiceImplConfiguration.getFullClassName(requestMessage.getClassName());
            System.out.println("读取的全类名：" + fullClassName);
            // 获取对应的方法
            String[] requestMessages = new String[requestMessage.getParamObjectList().size()];
            requestMessages = requestMessage.getParamObjectTypeLit().toArray(requestMessages);
            for(String a: requestMessages) {
                System.out.println(a);
            }
            method = MethodCache.getMethod(fullClassName, requestMessage.getMethodName(), requestMessages);
            System.out.println("获取的方法：" + method.getName());
        }catch (Exception e) {
            e.printStackTrace();
        }
        // 执行
        Object o = null;
        // 第一个参数是method对应的实体类对象
        try {
            System.out.println("执行方法开始");
            System.out.println(MethodCache.getObjectFromFullClassName(fullClassName).getClass().getName());
            System.out.println(requestMessage.getParamObjectList().get(0).getClass().getName());
            List<Object> paramObjectList = requestMessage.getParamObjectList();
            Object[] paramObjects = new Object[paramObjectList.size()];
            for (int i = 0;i < paramObjects.length;i++) {
                paramObjects[i] = paramObjectList.get(i);
            }
            // 用List会出错，但是idea不会有提示
            // 必须要对象数组
            o = method.invoke(MethodCache.getObjectFromFullClassName(fullClassName), paramObjects);
            System.out.println("执行方法结束");
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("111111111111");
        // 序列化
        bytes = serialize.serialize(o);
        System.out.println("序列化后长度：" + bytes.length);
        // 压缩
        bytes = compressAlgorithm.compress(bytes);
        System.out.println(compressAlgorithm.getClass().getName());
        System.out.println("压缩后长度：" + bytes.length);
        // 放入id
        bytes = merge(idAndData[0], bytes);
        System.out.println("放入id后长度：" + bytes.length);
        // 写入
        ctx.channel().writeAndFlush(bytes);
    }

    /**
     * 数据读取完毕处理方法
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        System.out.println("读取结束！");
    }

    /**
     * 处理异常, 一般是需要关闭通道
     *
     * @param ctx
     * @param cause
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
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

    /**
     * 加工
     */
    private byte[] merge(byte[] idBytes, byte[] msg) {
        byte[] newBytes = new byte[idBytes.length + msg.length];
        System.arraycopy(idBytes, 0, newBytes, 0, idBytes.length);
        System.arraycopy(msg, 0, newBytes, idBytes.length, msg.length);
        return newBytes;
    }

}
