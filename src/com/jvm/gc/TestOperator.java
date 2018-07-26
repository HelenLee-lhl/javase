package com.jvm.gc;

/**
 * Created by helenlee on 2018/4/20.
 */
public class TestOperator {
    public static void main(String [] args){
        int s = 2;
        System.out.println(2<<16);
        System.out.println(2 << 16);
        System.out.println(16 >>> 1);
        System.out.println(16 >> 1);
    }
}
