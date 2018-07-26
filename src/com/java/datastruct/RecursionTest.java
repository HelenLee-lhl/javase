package com.java.datastruct;

/**
 * 使用递归的前提条件:
 *
 * 1)基准情形:必须有某些基准情形,他们不用递归就能求解
 * 2)不断推进:对于那些要递归求解的情形,递归调用必须总能够朝着一个基准情形推进.
 *
 * @author helenlee
 * @date 2018-07-26 21:35:22
 */
public class RecursionTest {
    public static void main(String [] args){
        //1.公式求值,取0~28的随机数
        int random = (int) Math.round(Math.random() * 28);
        print("输入:" + random + " 输出:" + f(random));
        System.out.println();


        //2.随机打印,取0~9999的随机数
        int random1 = (int) Math.round(Math.random() * 9999);
        print(random1 + "");
        printOut(random1);
    }

    /**
     * 给定公式如下:
     * f(0) = 0 且 f(x) = 2f(x-1) + x * x; x >= 0
     * 要求输入任意一个数求出结果
     * 其中 f(0) = 0 就是基准情形
     * @param n 数量
     * @return int
     */
    private static int f(int n){
        if (n == 0){
            //这是一个基准情形
            return 0;
        }
        return 2*f(n - 1) + n * n;
    }

    /**
     * 给定一个 int 类型的依次打印各个位上值(高--低)
     * @param n 数量
     */
    private static void printOut(int n){
        if (n >= 10){
            printOut(n/10);
        }
        System.out.print( n % 10 + "   ");
    }


    private static void print(String p){
        System.out.println(p);
    }
}
