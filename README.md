# crpc
---
一个简易的RPC框架

## 功能列表
---
+ 基于Netty的主从Reactor模型，支持异步调用方式
+ 支持多种序列化方式，Kryo、Hessian等
+ 支持多种压缩算法，gzip，deflate等
+ 支持配置文件与注册中心加载服务节点，同时支持服务端自动注册
+ 支持多种负载均衡策略，随机，一致性Hash等
+ 重构JDK中的ServiceLoader为ExtensionLoader，特点如下：
    + 提供按需动态加载，可以使用键值对方式
    + 不再需要通过迭代器获取，可以使用ExtensionLoader加载生成
    + 解决了原有的ServiceLoader存在的线程不安全问题
+ 支持SPI拓展点，可扩展负载均衡策略、压缩算法、序列化类型、注册中心等
+ 支持策略分组，通过编写注册配置文件，可以改变不同服务节点采取的策略（如果注册中心、负载均衡等策略），而不需要更改原有代码

## 注册中心使用举例（默认为Zookeeper）
---
**客户端与服务端共有服务信息：**
```Java
public interface Hello {  
  
    /**  
     * 输出Hello World！  
     */  
    public User printHello(User user);  
  
}
 
/**  
 * 模拟简单的实体  
 */  
@Data  
public class User implements Serializable {  
  
    /**  
     * 姓名  
     */  
    private String name;  
  
}
```

**启动服务端，等待连接：**
```Java
/**  
 * Hello接口的实现类  
 */  
public class HelloImpl implements Hello {  
  
    /**  
     * Hello接口的实现类  
     */  
    @Override  
    public User printHello(User user) {  
        user.setName("123");  
        return user;  
    }  
  
}

/**  
 * 开启服务器  
 */  
public class Main {  
  
    public static void main(String[] args) {  
        // 创建服务器就是开启了  
        NettyServer nettyServer = new NettyServer(9000);  
    }  
  
}
```

**客户端进行连接，获取对应服务：**
```Java
public class Main {  
  
    // 获取服务端的Hello接口的实现类  
    public static void main(String[] args) {  
        // 获取用户服务实现UserService  
        Hello hello = ProxyFactory.create(Hello.class, "userService", "HelloImpl");  
        User user = new User();  
        user.setName("陈");  
        // User(name=123)
        System.out.println(hello.printHello(user));  
    }  
  
}
```