package com.java.datastruct;

/**
 * @author helenlee
 * @date 2018/7/28
 */
public class IntegerTest {
    public static void main(String [] args){
        //自动装箱 编译后转换为：Integer.valueOf(10)
        Integer boxing = 10;
        //自动拆箱 编译后转换为：boxing.intValue()
        int unboxing = boxing;
        Integer box1 = 10;
        Integer box2 = 10;
        System.out.println(box1 == box2);
        Integer box3 = 129;
        Integer box4 = 129;
        System.out.println(box3 == box4);
        box1.equals(box2);
    }
}
