package com.somecode.test.client;

import com.somecode.client.proxy.ProxyFactory;
import com.somecode.test.service.Hello;
import com.somecode.test.service.User;

public class Main {

    // 获取服务端的Hello接口的实现类
    public static void main(String[] args) {
        // 获取用户服务实现UserService
        Hello hello = ProxyFactory.create(Hello.class, "userService", "HelloImpl");
        User user = new User();
        user.setName("陈");
        System.out.println(hello.printHello(user));
    }

}
