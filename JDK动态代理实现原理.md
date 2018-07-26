- 动态代理

  动态代理是一种方便运行时动态构建代理、动态处理代理方法调用的机制，很多场景都是利用类似机制做到的，比如用来包装 RPC 调用、面向切面的编程（AOP）。
  实现动态代理的方式很多，比如 JDK 自身提供的动态代理，就是主要利用了上面提到的反射机制。还有其他的实现方式，比如利用传说中更高性能的字节码操作机制，类似 ASM、cglib（基于 ASM）、Javassist 等。  

- JDK动态代理的实现原理  

  - 前言：本文将涉及那些问题  

    1. 动态代理demo

    2. 生成动态代理对象的流程

    3. 回调InvocationHandler.invoke何时被谁触发？

    4. JDK动态代理为何只支持接口类型

    5. InvocationHandler.invoke第一个参数(Object proxy)的作用是什么

       

  - 动态代理demo

    ```java
    package com.java.proxy;
    
    import java.lang.reflect.InvocationHandler;
    import java.lang.reflect.Method;
    import java.lang.reflect.Proxy;
    
    /**
     * @author helenlee
     * @date 2018/7/7
     */
    public class MyDynamicProxy {
        public static void main(String [] args){
            //FIXME 2:将生成的代理对象持久化到磁盘
            System.setProperty("sun.misc.ProxyGenerator.saveGeneratedFiles","true");
            HelloImpl hello = new HelloImpl();
            //FIXME 1
            MyInvocationHandler handler = new MyInvocationHandler(hello);
            //FIXME 3
            Hello instance = (Hello) Proxy.newProxyInstance(hello.getClass().getClassLoader()
                    , HelloImpl.class.getInterfaces(), handler);
            //FIXME 4
            String out = instance.sayHello("out");
            System.out.println(out);
        }
    }
    interface Hello{
        String sayHello(String str);
    }
    
    class HelloImpl implements Hello {
        @Override
        public String sayHello(String str) {
            System.out.println("Hello World");
            return str + ": Hello World";
        }
    }
    
    class MyInvocationHandler implements InvocationHandler {
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
    ```

    1. 示例代码功能很简单：1）定义Hello.sayHello方法 2）FIXME 1:创建MyInvocationHandler回调类并invoke方法中对Hello.sayHello进行代码增强方法调用前后打印日志  3）FIXME 3:创建代理对象并执行Hello.sayHello方法
    2. FIXME 2: System.setProperty("sun.misc.ProxyGenerator.saveGeneratedFiles","true");这个参数设置是将生成的代理对象持久化到磁盘，后面反编译代理对象分析方法的调用链
    3. FIXME 3:Proxy.newProxyInstance创建代理对象的流程是怎样？此时代理对象的具体类型是什么？
    4. FIXME 4: 调用instance.sayHello()为什么会触发MyInvocationHandler.invoke方法？

  - 生成动态代理对象的流程

    ![](/Users/helenlee/Documents/starUML/JDK%E5%8A%A8%E6%80%81%E4%BB%A3%E7%90%86%E5%AF%B9%E8%B1%A1%E7%94%9F%E6%88%90%E6%97%B6%E5%BA%8F%E5%9B%BE.png)

    上图是动态代理生成的时序图，下面会对关键步骤进行分析：

    1. Proxy.newProxyInstance 方法涉及到步骤2、16、17

       ```java
       /**
        * @param loader : 具体类型的类加载器
        * @param interfaces ：具体类型的接口列表
        * @param h ：回调函数对应类
        */
       public static Object newProxyInstance(ClassLoader loader,
                                                 Class<?>[] interfaces,
                                                 InvocationHandler h)
               throws IllegalArgumentException
           {
               Objects.requireNonNull(h);
               final Class<?>[] intfs = interfaces.clone();
               final SecurityManager sm = System.getSecurityManager();
               if (sm != null) {
               	// 安全检查
                   checkProxyAccess(Reflection.getCallerClass(), loader, intfs);
               }
               // 查找或生成制定的代理类Class 对应步骤2
               Class<?> cl = getProxyClass0(loader, intfs);
               try {
                   if (sm != null) {
                   	// 权限检查
                       checkNewProxyPermission(Reflection.getCallerClass(), cl);
                   }
                   // 根据类型代理类Class获取其构造器 对应步骤16
                   final Constructor<?> cons = cl.getConstructor(constructorParams);
                   final InvocationHandler ih = h;
                   // 如果是非Public修饰则设置修改权限 
                   if (!Modifier.isPublic(cl.getModifiers())) {
                       AccessController.doPrivileged(new PrivilegedAction<Void>() {
                           public Void run() {
                               cons.setAccessible(true);
                               return null;
                           }
                       });
                   }
                   // 根据构造器实例化代理对象注意传入了InvocationHandler实例 对应步骤17
                   return cons.newInstance(new Object[]{h});
               } catch (IllegalAccessException|InstantiationException e) {
                   throw new InternalError(e.toString(), e);
               } catch (InvocationTargetException e) {
                   Throwable t = e.getCause();
                   if (t instanceof RuntimeException) {
                       throw (RuntimeException) t;
                   } else {
                       throw new InternalError(t.toString(), t);
                   }
               } catch (NoSuchMethodException e) {
                   throw new InternalError(e.toString(), e);
               }
           }
       ```

    2. 承接步骤2 getProxyClass0(loader, intfs)  方法 涉及步骤3

       ```java
       //代理对象的缓存(此处使用一二级缓存；一级缓存：类加载器维度，二级缓存接口维度)
       private static final WeakCache<ClassLoader, Class<?>[], Class<?>>
               proxyClassCache = new WeakCache<>(new KeyFactory(), new ProxyClassFactory());
       private static Class<?> getProxyClass0(ClassLoader loader,
                                                  Class<?>... interfaces) {
           	//如果实现的接口大于65535则抛出异常(class结构中interfaces长度定义成了2字节)
               if (interfaces.length > 65535) {
                   throw new IllegalArgumentException("interface limit exceeded");
               }
           	// 如果给定的类装载器、接口存在于缓存中则返回缓存中的副本，否则将通过ProxyClassFactory创建代理类 对应步骤3
               return proxyClassCache.get(loader, interfaces);
           }
       ```

    3. 承接步骤3 proxyClassCache.get(loader, interfaces) 方法 涉及步骤 4、5、6、8、9

       ```java
       public V get(K key, P parameter) {
               Objects.requireNonNull(parameter);
               expungeStaleEntries();
           	// 根据类加载器获取一级缓存的key 步骤4
               Object cacheKey = CacheKey.valueOf(key, refQueue);
       
               // 根据一级缓存key获取value 步骤5
               ConcurrentMap<Object, Supplier<V>> valuesMap = map.get(cacheKey);
               if (valuesMap == null) {
                   // 如果不存在新建一个value并put进去
                   ConcurrentMap<Object, Supplier<V>> oldValuesMap
                       = map.putIfAbsent(cacheKey,
                                         valuesMap = new ConcurrentHashMap<>());
                   if (oldValuesMap != null) {
                       valuesMap = oldValuesMap;
                   }
               }
       
               // 根据接口创建二级缓存的key 步骤6
               Object subKey = Objects.requireNonNull(subKeyFactory.apply(key, parameter));
           	// 根据二级缓存key获取代理对象对应的工厂 步骤8
               Supplier<V> supplier = valuesMap.get(subKey);
               Factory factory = null;
       
               while (true) {
                   if (supplier != null) {
                       // 注意supplier可能是缓存CacheValue<V>或工厂实例，如果是工厂则根据工厂获取代理类，如果不存在则创建代理类 步骤9
                       V value = supplier.get();
                       if (value != null) {
                           return value;
                       }
                   }
                   // 如果工厂不存在则创建
                   if (factory == null) {
                       factory = new Factory(key, parameter, subKey, valuesMap);
                   }
                   if (supplier == null) {
                       supplier = valuesMap.putIfAbsent(subKey, factory);
                       if (supplier == null) {
                           supplier = factory;
                       }
                       // else retry with winning supplier
                   } else {
                       if (valuesMap.replace(subKey, supplier, factory)) {
                           // successfully replaced
                           // cleared CacheEntry / unsuccessful Factory
                           // with our Factory
                           supplier = factory;
                       } else {
                           // retry with current supplier
                           supplier = valuesMap.get(subKey);
                       }
                   }
               }
           }
       ```

    4. 承接步骤9 supplier.get() 方法 涉及步骤11

       ```java
       		private final K key;
               private final P parameter;
               private final Object subKey;
               private final ConcurrentMap<Object, Supplier<V>> valuesMap;
       
               Factory(K key, P parameter, Object subKey,
                       ConcurrentMap<Object, Supplier<V>> valuesMap) {
                   this.key = key;
                   this.parameter = parameter;
                   this.subKey = subKey;
                   this.valuesMap = valuesMap;
               }
       
               @Override
               public synchronized V get() { // serialize access
                   // 再次检查
                   Supplier<V> supplier = valuesMap.get(subKey);
                   if (supplier != this) {
                       // 检查不通过返回null，为防止等待过程中的变化
                       return null;
                   }
                   // else still us (supplier == this)
       
                   // create new value
                   V value = null;
                   try {
                       // 此处是真正创建代理类的入口：使用Proxy.ProxyClassFactory.apply 创建代理类 步骤11
                       value = Objects.requireNonNull(valueFactory.apply(key, parameter));
                   } finally {
                       if (value == null) { // 失败就删除
                           valuesMap.remove(subKey, this);
                       }
                   }
                   // 非空校验
                   assert value != null;
                   // 使用弱引用包装value(CacheValue继承自WeakReference)
                   CacheValue<V> cacheValue = new CacheValue<>(value);
       
                   // 设置到缓存中
                   if (valuesMap.replace(subKey, this, cacheValue)) {
                       // put also in reverseMap
                       reverseMap.put(cacheValue, Boolean.TRUE);
                   } else {
                       throw new AssertionError("Should not reach here");
                   }
                   return value;
               }
       ```

    5. 承接 步骤 11 valueFactory.apply(key, parameter) 方法 涉及步骤12、13、15

       ```java
       @Override
               public Class<?> apply(ClassLoader loader, Class<?>[] interfaces) {
       
                   Map<Class<?>, Boolean> interfaceSet = new IdentityHashMap<>(interfaces.length);
                   for (Class<?> intf : interfaces) {
                       Class<?> interfaceClass = null;
                       try {
                           // 获取接口类 步骤12
                           interfaceClass = Class.forName(intf.getName(), false, loader);
                       } catch (ClassNotFoundException e) {
                       }
                       if (interfaceClass != intf) {
                           throw new IllegalArgumentException(
                               intf + " is not visible from class loader");
                       }
                       // 判断是否是接口类型(为什么JDK只支持接口类型，这地方是校验不是原因)
                       if (!interfaceClass.isInterface()) {
                           throw new IllegalArgumentException(
                               interfaceClass.getName() + " is not an interface");
                       }
                       /*
                        * Verify that this interface is not a duplicate.
                        */
                       if (interfaceSet.put(interfaceClass, Boolean.TRUE) != null) {
                           throw new IllegalArgumentException(
                               "repeated interface: " + interfaceClass.getName());
                       }
                   }
       
                   String proxyPkg = null;     // 包路径
                   int accessFlags = Modifier.PUBLIC | Modifier.FINAL;
       
                   // 判断非public在同一个包下
                   for (Class<?> intf : interfaces) {
                       int flags = intf.getModifiers();
                       if (!Modifier.isPublic(flags)) {
                           accessFlags = Modifier.FINAL;
                           String name = intf.getName();
                           int n = name.lastIndexOf('.');
                           String pkg = ((n == -1) ? "" : name.substring(0, n + 1));
                           if (proxyPkg == null) {
                               proxyPkg = pkg;
                           } else if (!pkg.equals(proxyPkg)) {
                               throw new IllegalArgumentException(
                                   "non-public interfaces from different packages");
                           }
                       }
                   }
       
                   if (proxyPkg == null) {
                       //默认的包路径
                       proxyPkg = ReflectUtil.PROXY_PACKAGE + ".";
                   }
       
                   /*
                    * Choose a name for the proxy class to generate.
                    */
                   long num = nextUniqueNumber.getAndIncrement();
                   // 实例："com.java.proxy" + "$Proxy" + 0
                   String proxyName = proxyPkg + proxyClassNamePrefix + num;
       
                   // 生成代理类字节码 步骤13
                   byte[] proxyClassFile = ProxyGenerator.generateProxyClass(
                       proxyName, interfaces, accessFlags);
                   try {
                       // 生成代理类 步骤15
                       return defineClass0(loader, proxyName,
                                           proxyClassFile, 0, proxyClassFile.length);
                   } catch (ClassFormatError e) {
                       throw new IllegalArgumentException(e.toString());
                   }
               }
           }
       ```

    6. 承接步骤13 ProxyGenerator.generateProxyClass( proxyName, interfaces, accessFlags) 动态生成类字节码这不对学习类字节码很有帮助

       ```java
       public static byte[] generateProxyClass(final String var0, Class<?>[] var1, int var2) {
               ProxyGenerator var3 = new ProxyGenerator(var0, var1, var2);
           	// 最关键的这步，生成类的字节码
               final byte[] var4 = var3.generateClassFile();
           	// 系统是否配置了持久化代理类
           	// saveGeneratedFiles = (Boolean)AccessController.doPrivileged(new GetBooleanAction("sun.misc.ProxyGenerator.saveGeneratedFiles"));
           	// 呼应JDK动态代理demo 的设置
               if (saveGeneratedFiles) {
                   AccessController.doPrivileged(new PrivilegedAction<Void>() {
                       public Void run() {
                           try {
                               int var1 = var0.lastIndexOf(46);
                               Path var2;
                               if (var1 > 0) {
                                   Path var3 = Paths.get(var0.substring(0, var1).replace('.', File.separatorChar));
                                   Files.createDirectories(var3);
                                   var2 = var3.resolve(var0.substring(var1 + 1, var0.length()) + ".class");
                               } else {
                                   var2 = Paths.get(var0 + ".class");
                               }
       
                               Files.write(var2, var4, new OpenOption[0]);
                               return null;
                           } catch (IOException var4x) {
                               throw new InternalError("I/O exception saving generated file: " + var4x);
                           }
                       }
                   });
               }
       
               return var4;
           }
       
       // 承接上步 最关键的这步，生成类的字节码 var3.generateClassFile();
       // 根据字节码接口准备各种结构数据
       private byte[] generateClassFile() {
               this.addProxyMethod(hashCodeMethod, Object.class);//hashCode 方法
               this.addProxyMethod(equalsMethod, Object.class);//equals 方法
               this.addProxyMethod(toStringMethod, Object.class);//toString 方法
               Class[] var1 = this.interfaces;// 所有的接口类
               int var2 = var1.length;
       
               int var3;
               Class var4;
           	//遍历所有接口并记录所有的方法
               for(var3 = 0; var3 < var2; ++var3) {
                   var4 = var1[var3];
                   Method[] var5 = var4.getMethods();
                   int var6 = var5.length;
       
                   for(int var7 = 0; var7 < var6; ++var7) {
                       Method var8 = var5[var7];
                       this.addProxyMethod(var8, var4);
                   }
               }
       
               Iterator var11 = this.proxyMethods.values().iterator();
       
               List var12;
               while(var11.hasNext()) {
                   var12 = (List)var11.next();
                   checkReturnTypes(var12);
               }
       
               Iterator var15;
               try {
                   this.methods.add(this.generateConstructor());
                   var11 = this.proxyMethods.values().iterator();
       
                   while(var11.hasNext()) {
                       var12 = (List)var11.next();
                       var15 = var12.iterator();
       
                       while(var15.hasNext()) {
                           ProxyGenerator.ProxyMethod var16 = (ProxyGenerator.ProxyMethod)var15.next();
                           this.fields.add(new ProxyGenerator.FieldInfo(var16.methodFieldName, "Ljava/lang/reflect/Method;", 10));
                           this.methods.add(var16.generateMethod());
                       }
                   }
       
                   this.methods.add(this.generateStaticInitializer());
               } catch (IOException var10) {
                   throw new InternalError("unexpected I/O Exception", var10);
               }
       
               if (this.methods.size() > 65535) {
                   throw new IllegalArgumentException("method limit exceeded");
               } else if (this.fields.size() > 65535) {
                   throw new IllegalArgumentException("field limit exceeded");
               } else {
                   this.cp.getClass(dotToSlash(this.className));
                   this.cp.getClass("java/lang/reflect/Proxy");
                   var1 = this.interfaces;
                   var2 = var1.length;
       
                   for(var3 = 0; var3 < var2; ++var3) {
                       var4 = var1[var3];
                       this.cp.getClass(dotToSlash(var4.getName()));
                   }
       
                   this.cp.setReadOnly();
                   ByteArrayOutputStream var13 = new ByteArrayOutputStream();
                   DataOutputStream var14 = new DataOutputStream(var13);
       
                   try {
                       //下面就是按照字节码接口输出
                       //-889275714对应的16进制0xcafebabe也就是Java的魔数
                       var14.writeInt(-889275714);
                       var14.writeShort(0);//次版本号
                       var14.writeShort(49);//主版本号
                       this.cp.write(var14);//var14 是准备好的常量池数据
                       var14.writeShort(this.accessFlags);//访问标记 public|private等
                       
       			// 全类名常量池索引
                    var14.writeShort(this.cp.getClass(dotToSlash(this.className)));
                    // 父类常量池索引 这地方指定了父类java/lang/reflect/Proxy，注意：再想一下文章开头的疑惑为什么JDK只支持接口类型，Java是单继承（已经继承了Proxy）所以不能像CGLIB那样通过子类的方式派生代理类
                     var14.writeShort(this.cp.getClass("java/lang/reflect/Proxy"));
                       //实现的接口数量
                       var14.writeShort(this.interfaces.length);
                       Class[] var17 = this.interfaces;
                       int var18 = var17.length;
       
                       for(int var19 = 0; var19 < var18; ++var19) {
                           Class var22 = var17[var19];
                           
       			//接口常量池索引
                   var14.writeShort(this.cp.getClass(dotToSlash(var22.getName())));
                       }
                       // 字段数量
                       var14.writeShort(this.fields.size());
                       var15 = this.fields.iterator();
       
                       while(var15.hasNext()) {
                           ProxyGenerator.FieldInfo var20 = (ProxyGenerator.FieldInfo)var15.next();
                           //字段集合表
                           var20.write(var14);
                       }
       				// 方法数量
                       var14.writeShort(this.methods.size());
                       var15 = this.methods.iterator();
       
                       while(var15.hasNext()) {
                           ProxyGenerator.MethodInfo var21 = (ProxyGenerator.MethodInfo)var15.next();
                           // 方法表
                           var21.write(var14);
                       }
       
                       var14.writeShort(0);
                       return var13.toByteArray();
                   } catch (IOException var9) {
                       throw new InternalError("unexpected I/O Exception", var9);
                   }
               }
           }
       ```

    - 回调InvocationHandler.invoke何时被谁触发？

      要解决这个问题就要分析代理类 $Proxy0、InvocationHandler、代理目标对象Hello之间的关系

      反编译生成的$Proxy0代码片段如下：

      ```java
      //
      // Source code recreated from a .class file by IntelliJ IDEA
      // (powered by Fernflower decompiler)
      //
      
      package com.java.proxy;
      
      import java.lang.reflect.InvocationHandler;
      import java.lang.reflect.Method;
      import java.lang.reflect.Proxy;
      import java.lang.reflect.UndeclaredThrowableException;
      /**
      * $Proxy0继承Proxy并实现了Hello
      * 为什么JDK动态代理只支持接口：
      * Java是单继承（已经继承了Proxy）所以不能像CGLIB那样通过子类的方式派生代理类
      */
      final class $Proxy0 extends Proxy implements Hello {
          private static Method m1;
          private static Method m3;
          private static Method m2;
          private static Method m0;
      
          public $Proxy0(InvocationHandler var1) throws  {
              // Proxy类有InvocationHandler类型的属性
              super(var1);
          }
      
          public final boolean equals(Object var1) throws  {
              try {
                  return (Boolean)super.h.invoke(this, m1, new Object[]{var1});
              } catch (RuntimeException | Error var3) {
                  throw var3;
              } catch (Throwable var4) {
                  throw new UndeclaredThrowableException(var4);
              }
          }
      
          // 代理类实现了目标对象的sayHello方法
          public final String sayHello() throws  {
              try {
                  // 代理对象（触发）调用InvocationHandler.invoke 
                  // this:代理对象本身，m3：目标方法，new Object[]{var1}：目标方法入参
                  return (String)super.h.invoke(this, m3, new Object[]{var1});
              } catch (RuntimeException | Error var2) {
                  throw var2;
              } catch (Throwable var3) {
                  throw new UndeclaredThrowableException(var3);
              }
          }
      
          public final String toString() throws  {
              try {
                  return (String)super.h.invoke(this, m2, (Object[])null);
              } catch (RuntimeException | Error var2) {
                  throw var2;
              } catch (Throwable var3) {
                  throw new UndeclaredThrowableException(var3);
              }
          }
      
          public final int hashCode() throws  {
              try {
                  return (Integer)super.h.invoke(this, m0, (Object[])null);
              } catch (RuntimeException | Error var2) {
                  throw var2;
              } catch (Throwable var3) {
                  throw new UndeclaredThrowableException(var3);
              }
          }
      
          static {
              try {
                  m1 = Class.forName("java.lang.Object").getMethod("equals", Class.forName("java.lang.Object"));
                  m3 = Class.forName("com.java.proxy.Hello").getMethod("sayHello");
                  m2 = Class.forName("java.lang.Object").getMethod("toString");
                  m0 = Class.forName("java.lang.Object").getMethod("hashCode");
              } catch (NoSuchMethodException var2) {
                  throw new NoSuchMethodError(var2.getMessage());
              } catch (ClassNotFoundException var3) {
                  throw new NoClassDefFoundError(var3.getMessage());
              }
          }
      }
      
      ```

      代理对象执行代理方法时序图：

      ![](/Users/helenlee/Documents/starUML/%E4%BB%A3%E7%90%86%E7%B1%BB%E6%89%A7%E8%A1%8C%E4%BB%A3%E7%90%86%E6%96%B9%E6%B3%95%E6%97%B6%E5%BA%8F%E5%9B%BE.png)

      JDK动态代理类图模型

      ![](/Users/helenlee/Documents/starUML/JDK%E5%8A%A8%E6%80%81%E4%BB%A3%E7%90%86%E7%B1%BB%E5%9B%BE.png)

    - InvocationHandler.invoke第一个参数(Object proxy)的作用是什么

      先看一下InvocationHandler.invoke的代码

      ```java
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
      ```

      从代码上看proxy是代理对象的实例，并且invoke在进行代码增强的时候并没有用到这个代理对象。那么为什么要提供这个代理对象的入参呢？我觉得可能是拓展的一种形式：我们可以代理对象实例proxy获取动态代理类的信息如：proxy.getClass().getName()