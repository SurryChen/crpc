package com.somecode.test.server;

import com.somecode.test.service.Hello;
import com.somecode.test.service.User;

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
