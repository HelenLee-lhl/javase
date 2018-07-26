package com.java.reflect;

import com.java.modle.User;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;

/**
 * @author helenlee
 * @date 2018-07-07 11:22:20
 */
public class ReflectUtils {
    public static void main(String args []){
        Class<User> clazz = User.class;
        //获取类属性
        getFieldDescribe(clazz);
        //获取类方法的描述
        getMethodDescribe(clazz);
    }

    /**
     * 获取类属性
     * @param clazz
     */
    private static void getFieldDescribe(Class<?> clazz) {
        Field[] declaredFields = clazz.getDeclaredFields();
        for (Field declaredField : declaredFields){
            StringBuilder sb = new StringBuilder();
            Annotation[] annotations1 = declaredField.getAnnotations();
            for (Annotation annotation:annotations1){
                sb.append(annotation.annotationType().getName() + " ");
            }
            sb.append(Modifier.toString(declaredField.getModifiers()) + " ");
            sb.append(declaredField.getType().getName() + " ");
            sb.append(declaredField.getName() + " ");
            sb.append(";");
            System.out.println(sb.toString());
        }
    }

    /**
     * 获取类方法的描述
     * @param clazz 类
     */
    private static void getMethodDescribe(Class<?> clazz) {
        if (clazz == null){
            return;
        }
        Method[] methods = clazz.getMethods();
        for (Method method: methods) {
            StringBuilder sb = new StringBuilder();
            //修饰符
            sb.append(Modifier.toString(method.getModifiers()) + " ");
            //返回类型
            sb.append(method.getReturnType().getName() + " ");
            //方法名称
            sb.append(method.getName() + " ");

            //分割符
            sb.append("(");

            //参数列表
            Parameter[] parameters = method.getParameters();
            for (Parameter parameter: parameters) {
                Annotation[] annotations1 = parameter.getAnnotations();
                for (Annotation annotation:annotations1){
                    sb.append(annotation.annotationType().getName() + " ");
                }
                sb.append(parameter.getType().getName() + " ");
                sb.append(parameter.getName());
            }
            //分割符
            sb.append(") ");

            //异常类型
            Class<?>[] exceptionTypes = method.getExceptionTypes();
            for (int i = 0; i < exceptionTypes.length; i++){
                sb.append(exceptionTypes[i].getName());
                if (i != exceptionTypes.length - 1){
                    sb.append(",");
                }
            }

            //分隔符
            sb.append(";");
            System.out.println(sb.toString());
        }
    }
}
