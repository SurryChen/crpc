package com.somecode.client.connection;

import com.somecode.common.util.IdUtil;
import com.somecode.common.util.StringUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import org.apache.zookeeper.data.Id;

import java.util.Objects;

/**
 * 使用netty与服务端建立连接，并传送信息
 */
public class NettyClient {

    private String ip;

    private Integer port;

    // 会话超时时间
    private Integer timeout;

    // 通道
    private Channel channel;

    // Id包装器，没有做成工具类，是因为想最大程度地减少出现相同相同id的可能性
    private IdGenerator idGenerator = new IdGenerator();

    /**
     * 通道处理器
     */
    private NettyClientHandler nettyClientHandler = new NettyClientHandler(idGenerator);

    public NettyClient(String ip, Integer port, Integer timeout) {
        this.ip = ip;
        this.port = port;
        this.timeout = timeout;
        initChannel();
    }

    /**
     * 初始化通道
     */
    private void initChannel() {
        if (StringUtils.isEmpty(ip) || port == null) {
            throw new IllegalArgumentException("未输入IP与端口，无法对Netty连接进行初始化操作！");
        }
        // 客户端需要一个事件循环组
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            // 创建客户端启动对象
            // 注意客户端使用的不是ServerBootstrap而是Bootstrap
            Bootstrap bootstrap = new Bootstrap();
            // 设置相关参数
            bootstrap.group(group) // 设置线程组
                    .channel(NioSocketChannel.class) // 使用NioSocketChannel作为客户端的通道实现
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            //加入处理器
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
                            pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
                            pipeline.addLast("decoder", new ByteArrayDecoder());
                            pipeline.addLast("encoder", new ByteArrayEncoder());
                            pipeline.addLast(nettyClientHandler);
                        }
                    });
            // 启动客户端去连接服务器端
            channel = bootstrap.connect(ip, port).sync().channel();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 发送数据包
     * 返回获取数据包的钥匙
     */
    public Integer sendMsg(byte[] msg) throws Exception {
        // 可以发送就直接发送
        if (channel != null) {
            // 加工一层
            Integer id = idGenerator.createSessionID();
            // 转成byte数组
            byte[] idBytes = idGenerator.intToBytes(id);
            // 将idBytes与msg拼接在一起
            byte[] merge = merge(idBytes, msg);
            // 先告诉Handler建立锁
            nettyClientHandler.setData(id);
            // 写入
            channel.writeAndFlush(merge).sync();
            // 返回id，用来异步查找返回的数据包
            return id;
        } else {
            throw new RuntimeException("未建立连接！");
        }
    }

    /**
     * 获取返回的数据包
     * 返回null说明失败了
     */
    public byte[] getMsg(Integer id) throws Exception {
        return nettyClientHandler.getData(id, timeout);
    }

    /**
     * 加工
     */
    private byte[] merge(byte[] idBytes, byte[] msg) throws Exception {
        byte[] newBytes = new byte[idBytes.length + msg.length];
        System.arraycopy(idBytes, 0, newBytes, 0, idBytes.length);
        System.arraycopy(msg, 0, newBytes, idBytes.length, msg.length);
        return newBytes;
    }

}
