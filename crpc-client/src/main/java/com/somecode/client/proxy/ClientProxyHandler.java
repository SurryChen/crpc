package com.somecode.client.proxy;

import com.somecode.client.configure.InitFromConfigureFile;
import com.somecode.client.connection.NettyClient;
import com.somecode.common.entity.NetworkNode;
import com.somecode.common.entity.RequestMessage;
import com.somecode.common.entity.ServiceInfo;
import com.somecode.common.entity.StrategyGroup;
import com.somecode.common.spi.core.CompressAlgorithm;
import com.somecode.common.spi.core.LoadBalance;
import com.somecode.common.spi.core.RegisterCenter;
import com.somecode.common.spi.core.Serialize;
import com.somecode.common.spi.extension.ExtensionLoader;
import com.somecode.common.util.IdUtil;
import com.sun.security.ntlm.Client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * 代理处理器
 *
 * @author 落阳
 * @date 2023/2/27
 */
public class ClientProxyHandler<T> implements InvocationHandler {

    /**
     * 接口对象
     */
    T t;

    /**
     * 服务名
     */
    private String serviceName;

    /**
     * 访问的类的标注名
     */
    private String className;

    /**
     * 序列化策略
     */
    private Serialize serialize;

    /**
     * 压缩策略
     */
    private CompressAlgorithm compressAlgorithm;

    /**
     * 注册中心策略
     */
    private RegisterCenter registerCenter;

    /**
     * 负载均衡策略
     */
    private LoadBalance loadBalance;

    /**
     * 服务信息
     */
    private ServiceInfo serviceInfo;

    /**
     * 连接
     */
    private NettyClient nettyClient = null;

    /**
     * 创建的时候就传递进来服务名和类名
     */
    public ClientProxyHandler(String serviceName, String className) {
        this.serviceName = serviceName;
        this.className = className;
        // 初始化
        initStrategy();
    }

    /**
     * 传入返回对象的所有接口，比如说返回UserService的实现类，那么就需要传入UserService
     * 返回一个代理对象
     */
    public Object getProxy(Class<?>[] classes) {
        return Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(), classes, this);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 在这里，将执行
        // 获取想要发送的请求信息-->序列化请求信息-->发送请求信息-->同步或者异步接收请求信息
        // -->反序列化返回请求信息-->返回结果
        // 1、如何获取想要发送的请求信息？
        //    需要的请求信息：节点的IP与端口、想要执行的类名、想要执行的方法名、想要传递的参数
        //         节点的IP与端口:定义一个SPI，获取的就是返回信息，不必关注这个节点的IP与端口如何得到
        //         但是也需要给实现SPI的类一个服务名，不然连服务名都不知道，就无从挑选了
        //         当然，就算给了服务名，也无从得知是对应的服务所在的具体位置，但是可以通过实现的SPI里面去配置
        //         想要执行的类，仅通过这个方法无法获取，所以需要在创建该类对象的时候就传进来，可以只是一个类名
        //         想要执行的方法，可以通过method获取
        //         想要传递的参数，也就是args
        //    但是其实还不够，因为到了server端，需要找到对应的方法，如果该方法重载了，因为自动装箱的缘故，
        //    基本类型和包装类型转换成Object的时候是一样的，无法做到区分，所以我们还需要加入参数的类型
        //    那么返回值类型需不需要考虑呢？
        //    由于需要的只是找到对应方法，返回值类型对于找到对应方法并无影响，并且可以自动拆箱和装箱，所以暂不考虑
        // 封装请求信息
        // 类名
        String className = this.className;
        // 方法名
        String methodName = method.getName();
        // 参数实例
        List<Object> paramObjectList;
        if (args.length == 0) {
            paramObjectList = null;
        } else {
            paramObjectList = Arrays.asList(args);
        }
        // 参数类型
        // 不能使用args去获取，因为这是已经装箱过的，应该用method去获取
        Class<?>[] parameterTypes = method.getParameterTypes();
        List<String> paramObjectTypeList = new ArrayList<>();
        for (Class clazz: parameterTypes) {
            // 获取名字存进去就好，放入一个Class去序列化太大了
            paramObjectTypeList.add(clazz.getName());
        }
        // 装载到RequestMessage上
        RequestMessage requestMessage = new RequestMessage(className, methodName, paramObjectList, paramObjectTypeList);
        // 根据配置的序列化策略序列化
        byte[] bytes = serialize.serialize(requestMessage);
        // 根据配置的压缩策略压缩
        bytes = compressAlgorithm.compress(bytes);
        // 使用注册中心或者配置得到当前服务节点
        List<NetworkNode> networkNodes = registerCenter.requireNetworkNodeList(serviceName);
        System.out.println("-------------------");
        // 可能是没有获取到注册中心
        System.out.println(registerCenter);
        //
        System.out.println(networkNodes);
        System.out.println("-------------------");
        if (networkNodes.isEmpty()) {
            // 使用配置文件
            networkNodes = serviceInfo.getNetworkNode();
        }
        // 将节点进行负载均衡获取某一个节点
        NetworkNode networkNode = loadBalance.requireBetterNodeFromList(networkNodes);
        // 使用该节点建立连接，使用Netty
        if (nettyClient == null) {
            nettyClient = new NettyClient(networkNode.getHost(), networkNode.getPort(), serviceInfo.getTimeout());
        }
        // 获取对应数据包的钥匙
        Integer idKey = nettyClient.sendMsg(bytes);
        bytes = nettyClient.getMsg(idKey);
//        if (bytes == null) {
//            // 说明加载失败
//            // 展示先报错，不提供重试
//            throw new RuntimeException(networkNode + "不可用，请检查节点或传输的数据包内容等等！");
//        }
        // 合理的
        // 反压缩
        bytes = compressAlgorithm.uncompress(bytes);
        // 反序列化
        Object deserialize = serialize.deserialize(bytes);
        // 完成
        return deserialize;
    }

    private void initStrategy() {
        // 加载配置信息
        serviceInfo = InitFromConfigureFile.getServiceInfo(serviceName);
        // 获取策略组
        StrategyGroup strategyGroup = serviceInfo.getStrategyGroup();
        // 加载对应的策略实现
        ExtensionLoader<Serialize> serializeExtensionLoader = ExtensionLoader.load(Serialize.class);
        ExtensionLoader<CompressAlgorithm> compressAlgorithmExtensionLoader = ExtensionLoader.load(CompressAlgorithm.class);
        ExtensionLoader<RegisterCenter> registerCenterExtensionLoader = ExtensionLoader.load(RegisterCenter.class);
        ExtensionLoader<LoadBalance> loadBalanceExtensionLoader = ExtensionLoader.load(LoadBalance.class);
        // 获取对应实现
        this.serialize = serializeExtensionLoader.getExtension(strategyGroup.getSerialize());
        this.compressAlgorithm = compressAlgorithmExtensionLoader.getExtension(strategyGroup.getCompressAlgorithm());
        this.registerCenter = registerCenterExtensionLoader.getExtension(strategyGroup.getRegisterCenter());
        this.loadBalance = loadBalanceExtensionLoader.getExtension(strategyGroup.getLoadBalance());
    }

}
