package com.java.proxy;

import java.lang.reflect.Proxy;

/**
 * @author helenlee
 * @date 2018/7/7
 */
public class MyDynamicProxy {
    public static void main(String [] args){
        //将生成的代理对象持久化到磁盘
        System.setProperty("sun.misc.ProxyGenerator.saveGeneratedFiles","true");
        HelloImpl hello = new HelloImpl();
        MyInvocationHandler handler = new MyInvocationHandler(hello);
        Hello instance = (Hello) Proxy.newProxyInstance(hello.getClass().getClassLoader()
                , HelloImpl.class.getInterfaces(), handler);
        String out = instance.sayHello("out");
        System.out.println(out);
    }
}

