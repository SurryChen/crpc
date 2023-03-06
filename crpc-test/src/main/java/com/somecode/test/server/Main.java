package com.somecode.test.server;

import com.somecode.server.connection.NettyServer;

/**
 * 开启服务器
 */
public class Main {

    public static void main(String[] args) {
        // 创建服务器就是开启了
        NettyServer nettyServer = new NettyServer(9000);
    }

}
