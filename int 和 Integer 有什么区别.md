## int 和 Integer 有什么区别？

> int 是我们常说的整形数字，是 Java 的 8 个原始数据类型（Primitive Types，boolean、byte 、short、char、int、float、double、long）之一。Java 语言虽然号称一切都是对象，但原始数据类型是例外。

> Integer 是 int 对应的包装类，它有一个 int 类型的字段存储数据，并且提供了基本操作，比如数学运算、int 和字符串之间转换等。在 Java 5 中，引入了自动装箱和自动拆箱功能（boxing/unboxing），Java 可以根据上下文，自动进行转换，极大地简化了相关编程。

- 理解自动拆箱、装箱

  自动拆箱、装箱是Java的语法糖。什么是语法糖？可以简单理解为 Java 平台为我们自动进行了一些转换，保证不同的写法在运行时等价，它们发生在编译阶段，也就是生成的字节码是一致的。

  **举例说明：**

  ```java
  public class IntegerTest {
      public static void main(String [] args){
          Integer boxing = 10;//自动装箱 编译后转换为：Integer.valueOf(10)
          int unboxing = boxing;//自动拆箱 编译后转换为：boxing.intValue()
      }
  }
  ```

  **验证编译器自动拆箱、装箱**

  `javac IntegerTest.java`//使用javac命令编译IntegerTest生成对应的class文件

  `javap -p -v IntegerTest.class`//javap -p -v 命令分析编译后的代码

  代码片段如下：

  ```java
  public static void main(java.lang.String[]);
      descriptor: ([Ljava/lang/String;)V
      flags: ACC_PUBLIC, ACC_STATIC
      Code:
        stack=1, locals=3, args_size=1
           0: bipush        10
           2: invokestatic  #2                  // Method java/lang/Integer.valueOf:(I)Ljava/lang/Integer;
           5: astore_1
           6: aload_1
           7: invokevirtual #3                  // Method java/lang/Integer.intValue:()I
          10: istore_2
          11: return
        LineNumberTable:
          line 9: 0
          line 10: 6
          line 11: 11
  }
  ```

- Integer 的缓存机制

  关于 Integer 的值缓存，这涉及 Java 5 中另一个改进。构建 Integer 对象的传统方式是直接调用构造器，直接 new 一个对象。但是根据实践，我们发现大部分数据操作都是集中在有限的、较小的数值范围，因而，在 Java 5 中新增了静态工厂方法 valueOf，在调用它的时候会利用一个缓存机制，带来了明显的性能改进。按照 Javadoc，**这个值默认缓存是 -128 到 127 之间。**

  **Integer.valueOf源码**

  ```java
  public static Integer valueOf(int i) {
      //如果是特定范围的数据则直接从缓存中取
      if (i >= IntegerCache.low && i <= IntegerCache.high)
          return IntegerCache.cache[i + (-IntegerCache.low)];
      return new Integer(i);
  }
  
  private static class IntegerCache {
          static final int low = -128;
          static final int high;
          static final Integer cache[];
  
          static {
              // 默认缓存的最大值
              int h = 127;
              // 可以根据系统配置调整缓存的最大值
              String integerCacheHighPropValue =
                  sun.misc.VM.getSavedProperty("java.lang.Integer.IntegerCache.high");
              if (integerCacheHighPropValue != null) {
                  try {
                      int i = parseInt(integerCacheHighPropValue);
                      i = Math.max(i, 127);
                      // Maximum array size is Integer.MAX_VALUE
                      h = Math.min(i, Integer.MAX_VALUE - (-low) -1);
                  } catch( NumberFormatException nfe) {
                      // If the property cannot be parsed into an int, ignore it.
                  }
              }
              high = h;
  
              cache = new Integer[(high - low) + 1];
              int j = low;
              for(int k = 0; k < cache.length; k++)
                  cache[k] = new Integer(j++);
  
              // range [-128, 127] must be interned (JLS7 5.1.7)
              assert IntegerCache.high >= 127;
          }
  
          private IntegerCache() {}
      }
  ```

- 验证Integer自动装箱的缓存机制

  我们可以使用==来判断，测试case：范围内取10，范围外取129进行验证

  ```java
  public static void main(String [] args){
      Integer box1 = 10;
      Integer box2 = 10;
      System.out.println(box1 == box2);//true
      Integer box3 = 129;
      Integer box4 = 129;
      System.out.println(box3 == box4);//false
  }
  ```

  从上面的结果我们可以看到Integer缓存机制带来的差异性，所以对应包装类型Integer、Long等的值比较我们应该避免使用==改用equals。