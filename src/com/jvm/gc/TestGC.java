package com.jvm.gc;

/**
 * Created by helenlee on 2018/3/5.
 *
 */
public class TestGC {
    public static void main(String [] args){
        byte [] bytes = null;
        for (int i = 0; i < 100; i++){
            bytes = new byte[1*1024*1024];
        }

    }
}
