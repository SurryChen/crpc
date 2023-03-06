package com.somecode.server.cache;

import com.somecode.common.util.Holder;
import com.somecode.server.configuration.InitServiceImplConfiguration;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 方法的缓存
 */
public class MethodCache {

    /**
     * 方法的缓存
     * 键值对：类名+方法名+参数的类名 = method
     */
    private static ConcurrentHashMap<String, Method> methodMap = new ConcurrentHashMap<>();

    /**
     * 实现类的缓存
     * 键值对：类名 = class
     */
    private static ConcurrentHashMap<String, Class> classImplMap = new ConcurrentHashMap<>();

    /**
     * 基本类型与实体类的缓存
     */
    private static ConcurrentHashMap<String, Class> classBaseAndEntityMap = new ConcurrentHashMap<>();

    /**
     * 实例化对象的缓存
     */
    private static ConcurrentHashMap<String, Object> objectMap = new ConcurrentHashMap<>();

    /**
     * 加载方法的锁
     */
    private static Holder methodHolder = new Holder();

    /**
     * 加载实现类的锁
     */
    private static Holder implHolder = new Holder();

    /**
     * 加载实体类的锁
     */
    private static Holder entityHolder = new Holder<>();

    /**
     * 加载实现类的对象的锁
     */
    private static Holder objectHolder = new Holder();

    static {
        // 填充基本类型进去
        classBaseAndEntityMap.put("byte", byte.class);
        classBaseAndEntityMap.put("short", short.class);
        classBaseAndEntityMap.put("int", int.class);
        classBaseAndEntityMap.put("long", long.class);
        classBaseAndEntityMap.put("float", float.class);
        classBaseAndEntityMap.put("double", double.class);
        classBaseAndEntityMap.put("boolean", boolean.class);
        classBaseAndEntityMap.put("char", char.class);
    }

    /**
     * 不支持创建
     */
    private MethodCache () { }

    /**
     * 获取一个方法
     */
    public static Method getMethod(String className, String methodName, String[] paramTypeNames) {
        // 将所有参数的类名拼接在一起
        StringBuilder stringBuilder = new StringBuilder();
        for (String paramTypeName: paramTypeNames) {
            stringBuilder.append(paramTypeName);
        }
        // 键名
        String key = getKey(className, methodName, paramTypeNames);
        // 去拿这个方法
        Method method = methodMap.get(key);
        // 如果为空就去加载
        if (method == null) {
            synchronized (methodHolder) {
                if (methodMap.get(key) == null) {
                    loadMethod(className, methodName, paramTypeNames);
                }
            }
        }
        method = methodMap.get(key);
        return method;
    }

    /**
     * 根据全类名获取一个实例化对象
     */
    public static Object getObjectFromFullClassName(String fullClassName) {
        Object object = objectMap.get(fullClassName);
        if (object == null) {
            synchronized (objectHolder) {
                if (objectMap.get(fullClassName) == null) {
                    try {
                        object = classImplMap.get(fullClassName).newInstance();
                        objectMap.put(fullClassName, object);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return object;
    }

    /**
     * 加载一个方法
     */
    private static void loadMethod(String className, String methodName, String[] paramTypeNames) {
        // 找到类
        Class clazz = classImplMap.get(className);
        if (clazz == null) {
            // 加载类
            synchronized (implHolder) {
                clazz = classImplMap.get(className);
                if (clazz == null) {
                    // 加载
                    loadClass(className);
                }
            }
            // 还是为空，说明不存在对应的class
            clazz = classImplMap.get(className);
            if (clazz == null) {
                throw new RuntimeException(className + "没有对应的类！");
            }
        }
        // 类不为空，开始获取方法
        // 先要根据名字获取对应的类
        System.out.println("类型：" + paramTypeNames[0]);
        Class<?>[] classes = getEntityClasses(paramTypeNames);
        // 可以获取方法
        try {
            System.out.println(methodName);
            System.out.println(clazz.getName());
            System.out.println(classes[0].getName());
            Method method = clazz.getMethod(methodName, classes);
            // 放入缓存
            methodMap.put(getKey(className, methodName, paramTypeNames), method);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 加载一个类
     */
    private static void loadClass(String className) {
        // 获取全类名
        String fullClassName = className;
        if (fullClassName != null) {
            // 加载class
            try {
                Class clazz = MethodCache.class.getClassLoader().loadClass(fullClassName);
                if (clazz != null) {
                    classImplMap.put(className, clazz);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 根据一个List<基本类型或实体类名>获取一个基本或实体类的数组
     */
    private static Class<?>[] getEntityClasses(String[] entityNames) {
        int entityNum = entityNames.length;
        Class[] classes = new Class[entityNum];
        for (int i = 0;i < entityNum;i++) {
            classes[i] = getEntityClass(entityNames[i]);
        }
        return classes;
    }

    /**
     * 获取一个EntityClass
     */
    private static Class getEntityClass(String entityName) {
        Class clazz = classBaseAndEntityMap.get(entityName);
        if (clazz == null) {
            synchronized (entityHolder) {
                if (classBaseAndEntityMap.get(entityName) == null) {
                    loadEntityClass(entityName);
                }
            }
        }
        clazz = classBaseAndEntityMap.get(entityName);
        System.out.println("getEntityClass:" + clazz);
        return clazz;
    }

    // 基础类型与传输的实体类的映射
    // 为什么要独立出来？
    // 因为后续扩展可以做一个实体类映射的配置文件，更方便去使用
    private static void loadEntityClass(String entityName) {
        System.out.println("最里面的加载" + entityName);
        // 此时的entityName本身就是一个全类名
        try {
            Class clazz = MethodCache.class.getClassLoader().loadClass(entityName);
            System.out.println("实际情况：" + clazz);
            if (clazz != null) {
                classBaseAndEntityMap.put(entityName, clazz);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 拼接键名
     */
    private static String getKey(String className, String methodName, String[] paramTypeNames) {
        StringBuilder key = new StringBuilder(className + "-" + methodName + "-");
        for (int i = 0;i < paramTypeNames.length - 1;i++) {
            key.append(paramTypeNames[i] + "-");
        }
        key.append(paramTypeNames[paramTypeNames.length - 1]);
        return key.toString();
    }

}
