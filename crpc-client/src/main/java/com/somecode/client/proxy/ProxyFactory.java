package com.somecode.client.proxy;

import java.lang.reflect.Proxy;

/**
 * 代理工厂，用来代理接口
 */
public class ProxyFactory {

    public static <T> T  create(Class<?> type, String serviceName,String className) {
        ClientProxyHandler<T> clientProxyHandler = new ClientProxyHandler(serviceName, className);
        return (T) clientProxyHandler.getProxy(new Class<?>[]{type});
    }

}
