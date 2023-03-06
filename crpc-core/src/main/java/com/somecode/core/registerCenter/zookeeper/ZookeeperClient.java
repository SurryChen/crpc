package com.somecode.core.registerCenter.zookeeper;

import com.somecode.common.entity.NetworkNode;
import org.I0Itec.zkclient.Holder;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.ZkClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Zookeeper客户端的各项操作
 */
public class ZookeeperClient {

    /**
     * Zookeeper的客户端
     */
    private static ZkClient zc = null;

    /**
     * 加个锁
     */
    private static Holder holder = new Holder();

    /**
     * 服务对应的节点信息，多个并发获取，同时还有修改
     */
    private static Map<String, List<NetworkNode>> serviceMap = new ConcurrentHashMap<>();

    /**
     * Zookeeper路径中的信息
     */
    private static final String CRPC = "/crpc";

    /**
     * 初始化，连接Zookeeper
     */
    public static void init() {
        // 单例加载
        try {
            if (zc == null) {
                synchronized (holder) {
                    if (zc == null) {
                        ZookeeperInfo zookeeperInfo = Configuration.getZookeeperInfo();
                        zc = new ZkClient(zookeeperInfo.getIp() + ":" + zookeeperInfo.getPort(), zookeeperInfo.getSessionTimeout(), zookeeperInfo.getConnectionTimeout());
                    }
                }
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取某个服务下所有节点信息
     * zookeeper中节点信息格式: /crpc/服务名/节点名:ip:port
     */
    public static List<NetworkNode> getNetworkNodes(String serviceName) {
        if (zc == null) {
            init();
        }
        // 如果缓存中有，就不需要再查找了
        List<NetworkNode> networkNodeList = serviceMap.get(serviceName);
        // 不为空
        System.out.println("networkNodeList:" + networkNodeList);
        if (networkNodeList != null) {
            return serviceMap.get(serviceName);
        }
        // 为空，创建
        // 节点列表
        networkNodeList = new ArrayList<>();
        System.out.println("开始获取");
        try {
            // 服务对应的路径
            StringBuffer path = new StringBuffer(CRPC);
            path.append("/" + serviceName);
            // 获取该服务下所有结点
            List<String> networkNodeStringList = zc.getChildren(path.toString());
            System.out.println("path:" + path);
            System.out.println("networkNodeStringList:" + networkNodeStringList);
            // 节点载体
            NetworkNode networkNode = null;
            // 循环获取节点信息
            if (networkNodeStringList != null && networkNodeStringList.size() > 0) {
                for (String address : networkNodeStringList) {
                    String[] content = address.split(":");
                    networkNode = new NetworkNode();
//                    networkNode.setName(content[0]);
                    networkNode.setHost(content[0]);
                    networkNode.setPort(Integer.valueOf(content[1]));
                    networkNodeList.add(networkNode);
                }
            }
            // 放入缓存中
            serviceMap.put(serviceName, networkNodeList);
            // 订阅Zookeeper节点的变化
            subscribe(serviceName);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("获取Zookeeper服务节点失败！" + "\n" + e);
        } finally {
            return networkNodeList;
        }
    }


    /**
     * 订阅
     * 当监听的目录信息发生变化的时候，Zookeeper会发送信息到ZkClient
     * 从而到达监听的效果
     */
    private static void subscribe(String serviceName) {
        // 监听的目录
        StringBuffer path = new StringBuffer(CRPC);
        path.append(serviceName);
        // 设置监听后发生的方法
        zc.subscribeChildChanges(path.toString(), new IZkChildListener() {
            @Override
            public void handleChildChange(String s, List<String> list) throws Exception {
                // 如果可以监听变化但是不知道具体变化内容，只知道变化了，所以需要重新加载
                if (list != null && list.size() > 0) {
                    List<NetworkNode> networkNodeList = new ArrayList<>(list.size());
                    for (String network : list) {
                        String[] content = network.split(":");
                        NetworkNode networkNode = new NetworkNode();
                        networkNode.setName(content[0]);
                        networkNode.setHost(content[1]);
                        networkNode.setPort(Integer.valueOf(content[2]));
                        networkNodeList.add(networkNode);
                    }
                    // 直接切换，平滑替换
                    serviceMap.put(serviceName, networkNodeList);
                } else {
                    throw new Exception(path + "在Zookeeper中不存在！");
                }
            }
        });
    }

}
