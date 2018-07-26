package com.java.proxy;

/**
 * @author helenlee
 * @date 2018/7/26
 */
public class HelloImpl implements Hello {
    HelloImpl() {
    }

    @Override
    public String sayHello(String str) {
        System.out.println("Hello World");
        return str + ": Hello World";
    }
}
