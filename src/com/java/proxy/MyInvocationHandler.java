package com.java.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @author helenlee
 * @date 2018/7/26
 */
public class MyInvocationHandler implements InvocationHandler {
    /**
     * 代理的目标对象
     */
    private Object target;

    /**
     * 构造方法注入代理对象
     * @param target 代理的目标对象
     */
    MyInvocationHandler(Object target) {
        this.target = target;
    }

    /**
     *
     * @param proxy 动态生成的代理对象
     * @param method 被代理方法
     * @param args 被代理方法的入参
     * @return 返回结果
     * @throws Throwable .
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //代理类目标方法的周围可以进行代码增强
        System.out.println("invoking before...");
        Object result = method.invoke(target, args);
        System.out.println("invoking after...");
        return result;
    }
}
