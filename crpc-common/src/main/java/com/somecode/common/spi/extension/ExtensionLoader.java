package com.somecode.common.spi.extension;

import com.somecode.common.util.Holder;
import com.somecode.common.util.StringUtils;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * spi加载器
 *
 * @author 落阳
 * @date 2023/2/28
 */
public class ExtensionLoader<S> implements Iterable<S> {

    /**
     * JDK的SPI的缺点：
     * 1、传入一个接口，然后去找实现类，在找的过程中，会把所有的配置文件都找一遍，找出所有适配的，
     *   该问题在于，无法指定找到哪一个，就只能重新加载，借鉴dubbo
     *   做出以下规定：
     *   1、/META-INF/crpc/下的文件名，要求是实现类的全类名
     *   2、文件格式为 *** = ***，采用键值对的形式
     *   功能修改：
     *   1、提供按需加载实现类
     *     例如；加载某某接口的实现类，先找到对应的文件，并记录所有的文件的实现类全类名，等到具体使用再去实例化
     * 2、需要通过迭代器去获取使用，非常不方便，按需加载后可以返回实例化对象
     *   例如：加载某某接口的键名为某某的实现类
     * 3、类加载失败只是提示错误，但是原因并不详细，我们可以做的更详细
     * 4、ServiceLoader的实例有并发安全问题
     *   ServiceLoader的对象都是独立的，所以并发问题出现在同一个Service对象的操作
     *   出现并发的操作，主要是对变量的操作不是原子性的
     *   可修改的变量只有两个，如下：
     *   private LinkedHashMap<String, S> providers = new LinkedHashMap<>();
     *   private ExtensionLoader.LazyIterator lookupIterator;
     *   LinkedHashMap并不是线程安全的，所以可以改造成ConcurrentHashMap
     *   而lookupIterator只在创建ServiceLoader的时候进行了加载，所以不会出现线程问题
     *   我们会把实例化的对象装载到providers中，实例化对象这一过程需要放置重复实例化，实例化对象应该是单例的对象
     *   这样就保证了获取的对象是一致的吗？
     *   还需要加上要求：同一个JVM同一个类加载器同一个Class文件
     *   JVM同一个吗？同一个
     *   类加载器同一个吗？除了创建ExtensionLoader可以指定以外，其余方式不能修改，所以同一个
     *   Class文件同一个吗？
     *   假设一，中途没换过配置文件，如果有同键名的，以最后一个为准，那样的Class就是同一个
     *   假设二，中途换了配置文件，同键名的，那样就会出现不同的Class文件，但是如果已经加载过了一次配置文件，
     *   已经有缓存在里面，所以还是一个，如果reload()，之前的清空了，重新加载配置文件，也不会与之前冲突，
     *   所以只是保证实例化对象是单例即可
     */

    /**
     * 一级查找
     * 缓存已经实例化的对象
     * 键值对：文件名 + # + 标注名 = 实现类对象
     * 二级查找
     * 缓存已经加载的配置文件信息，在这里查找
     * 键值对：文件名 + # + 标注名 = 实现类全类名
     * 三级查找
     * 重新扫描配置文件
     */

    /**
     * 一级查找缓存
     * Holder用来做对应的实例加载的锁
     */
    private ConcurrentHashMap<String, Holder<S>> providers = new ConcurrentHashMap<>();

    /**
     * 二级查找缓存
     */
    private ConcurrentHashMap<String, String> configures = new ConcurrentHashMap<>();

    /**
     * 查找文件的路径
     */
    private static final String PREFIX = "META-INF/crpc/";

    // The class or interface representing the service being loaded
    // 正在被加载的类或者接口
    private final Class<S> service;

    // The class loader used to locate, load, and instantiate providers
    // 用来定位、加载和实例化的类加载器
    private final ClassLoader loader;

    // The access control context taken when the ExtensionLoader is created
    // 获取ExtensionLoader时的访问上下文的控制器
    // 可以通过该类访问特定资源
//    private final AccessControlContext acc;

    // Cached providers, in instantiation order
    // 实例化提供者，按照实例化的顺序
//    private LinkedHashMap<String, S> providers = new LinkedHashMap<>();

    // The current lazy-lookup iterator
    // 懒加载迭代器
//    private ExtensionLoader.LazyIterator lookupIterator;

    /**
     * 私有的构造方法，提供静态方法创建，可以控制创建方式
     * 传入需要实现类的接口和加载实现类所需的类加载器
     *
     * @param svc 实现类的接口
     * @param cl  加载实现类所需的类加载器
     */
    private ExtensionLoader(Class<S> svc, ClassLoader cl) {
        // 传入的接口肯定不能为空啦
        service = Objects.requireNonNull(svc, "Service interface cannot be null");
        // 传入的类加载自然也是不能为空，如果没有指定，那就给个默认指定，使用AppClassLoader
        // 使用AppClassLoader是因为classPath下的class文件，默认加载进来的方式就是AppClassLoader
        // 这样也能避免了不同类加载器加载同一个类却出现不一致的问题
        loader = (cl == null) ? ClassLoader.getSystemClassLoader() : cl;
        // 重新加载
        reload();
    }

    /**
     * 单纯的静态方法创建ExtensionLoader对象
     * 可以使用自定义的类加载器
     * 如果不是自定义的类加载器，可以使用默认的类加载器，使用另外一个方法去构建ExtensionLoader对象
     *
     * @param service
     * @param loader
     * @param <S>
     * @return
     */
    public static <S> ExtensionLoader<S> load(Class<S> service,
                                              ClassLoader loader) {
        return new ExtensionLoader<>(service, loader);
    }

    /**
     * 提供默认的类加载器的ExtensionLoader创建方式
     *
     * @param service
     * @param <S>
     * @return
     */
    public static <S> ExtensionLoader<S> load(Class<S> service) {
        // 默认使用线程上下文类加载器
        // 为什么要使用线程上下文类加载器？
        // 启动类加载器找到接口并去调用的时候，无法加载接口的实现类，就无法给这个接口分配对象
        // 启动类加载器无法加载这个类，后面不是会下放到系统类加载器吗？
        // 下放到系统类加载器，是本身就在系统类加载器中加载，但是接入接口，实例化实现类的代码是通过启动类加载器去加载的
        // 而此时启动类加载器无法加载，又因为双亲委派机制，启动类加载器无法委派给系统类加载器
        // 那怎么办？
        // 这就是线程上下文类加载器的作用了，如果没有做修改，默认就是系统类加载器
        // 就直接使用线程上下文类加载器去加载，就可以在启动类加载器的类中启动线程上下文加载器去工作
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        // 调用静态创建ExtensionLoader对象的方法
        return ExtensionLoader.load(service, cl);
    }

    // 传入接口，没有定义构造器
    public static <S> ExtensionLoader<S> loadInstalled(Class<S> service) {
        // 一开始是AppClassLoader
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        // 一开始是空
        ClassLoader prev = null;
        // 存在
        while (cl != null) {
            // prev的变化如下：prev = null --> prev = AppClassLoader --> prev = ExtClassLoader
            prev = cl;
            // cl的变化如下：cl = AppClassLoader --> cl = ExtClassLoader --> cl = null
            cl = cl.getParent();
        }
        // 总结一句就是切换到ExtClassLoader加载器去加载
        return ExtensionLoader.load(service, prev);
    }

    /**
     * 根据实现类名获取接口实现类
     */
    public S getExtension(String extensionName) {
        try {
            // 先判断非空
            if (StringUtils.isEmpty(extensionName)) {
                throw new IllegalArgumentException("拓展名不能为空！");
            }
            // 启用一级查找
            return getExtensionForLevelOne(extensionName);
        }catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 启动一级查找
     */
    private S getExtensionForLevelOne(String extensionName) throws Exception {
        // extensionName是代称
        // 一级查找缓存的键名是全类名#extensionName
        // 查找缓存
        S s = getOrCreateHolder(service.getName() + "#" + extensionName).get();
        // 判断有无
        // 不需要再判断是否是实现类，二级查找已经验证了
        if (s == null) {
            // 进入下一级查找
            getExtensionForLevelTwo(extensionName);
        }
        // 再次查找
        s = getOrCreateHolder(service.getName() + "#" + extensionName).get();
        if (s == null) {
            throw new IllegalArgumentException("一级查找：当前使用的类加载器无法加载或者找不到" + extensionName + "！");
        }
        return s;
    }

    /**
     * 启动二级查找
     */
    private void getExtensionForLevelTwo(String extensionName) throws Exception {
        String configureKey = service.getName() + "#" + extensionName;
        // extensionName是代称
        // 查找缓存获取全类名
        String extensionFullName = configures.get(configureKey);
        // 如果找不到，进入三级查找
        if(StringUtils.isEmpty(extensionFullName)) {
            // 三级查找
            getExtensionForLevelThree();
        }
        // 查找缓存获取全类名
        extensionFullName = configures.get(configureKey);
        // 还是空
        if(StringUtils.isEmpty(extensionFullName)) {
            throw new IllegalArgumentException("二级查找：当前使用的类加载器无法加载或者找不到" + extensionName + "！");
        }
        // 获取成功
        // 不对extensionFullName做验证处理，因为处理的位置应该下放到三级查找
        // c用来储存加载后的类
        Class<?> c = null;
        try {
            // 根据全类名加载一个类
            System.out.println(loader);
            c = Class.forName(extensionFullName, false, loader);
        } catch (ClassNotFoundException x) {
            // 找不到
            fail(service, "当前使用的类加载器无法加载或者找不到" + extensionFullName + "！");
        }
        // 判断c是否是service的实现类
        if (!service.isAssignableFrom(c)) {
            fail(service, extensionFullName + "并不是" + service.getName() + "的实现类！");
        }
        try {
            // 这里使用双重判断，还有一个问题在于加锁，锁必须是一个大家都可以访问的
            // 参考的dubbo的方式来实现
            final Holder<S> holder = getOrCreateHolder(configureKey);
            // 实例化，采用单例模式加载
            if(holder.get() == null) {
                synchronized (holder) {
                    // 双重检查
                    if(holder.get() == null) {
                        // 加入到一级查找缓存中
                        // 添加内容到锁中
                        holder.set((S) c.newInstance());
                    }
                }
            }
            // 返回上一级缓存
        } catch (Throwable x) {
            fail(service, "Provider " + extensionName + " could not be instantiated", x);
        }
    }

    /**
     * 三级查找
     * 主要任务是查找配置文件，并加载到configures中，所有实现类的路径都加载到configures中
     */
    private void getExtensionForLevelThree() throws Exception {
        // extensionName是代称
        // 找到文件路径
        String path = PREFIX + service.getName();
        System.out.println(path);
//        Enumeration<URL> configs;
        URL resource = loader.getResource(path);
//        System.out.println(configs.nextElement());
//        System.out.println(configs.nextElement());
//        System.out.println(configs.nextElement());
        // 读取文件
        parse(service, resource);
    }

    /**
     * Clear this loader's provider cache so that all providers will be
     * reloaded.
     *
     * <p> After invoking this method, subsequent invocations of the {@link
     * #iterator() iterator} method will lazily look up and instantiate
     * providers from scratch, just as is done by a newly-created loader.
     *
     * <p> This method is intended for use in situations in which new providers
     * can be installed into a running Java virtual machine.
     */
    /**
     * 清除已经加载的providers（实例提供者）
     * 并使用懒加载的迭代器重新加载
     */
    public void reload() {
        providers.clear();
        configures.clear();
    }

    /**
     * 抛出错误信息
     *
     * @param service 错误的类
     * @param msg     错误信息
     * @param cause   具体错误
     * @throws ServiceConfigurationError
     */
    private static void fail(Class<?> service, String msg, Throwable cause)
            throws ServiceConfigurationError {
        throw new ServiceConfigurationError(service.getName() + ": " + msg,
                cause);
    }

    /**
     * 同上，做了简化
     *
     * @param service
     * @param msg
     * @throws ServiceConfigurationError
     */
    private static void fail(Class<?> service, String msg)
            throws ServiceConfigurationError {
        throw new ServiceConfigurationError(service.getName() + ": " + msg);
    }

    /**
     * 拼接错误信息
     *
     * @param service
     * @param u
     * @param line
     * @param msg
     * @throws ServiceConfigurationError
     */
    private static void fail(Class<?> service, URL u, int line, String msg)
            throws ServiceConfigurationError {
        fail(service, u + ":" + line + ": " + msg);
    }

    // Parse a single line from the given configuration file, adding the name
    // on the line to the names list.
    //

    /**
     * 读取配置文件的一行
     *
     * @param service 装载的类
     * @param u       配置文件路径
     * @param r       缓冲字符流
     * @param lc      当前读取文件的行数，主要是用来给出具体报错
     * @return 返回下一行的行数，没有下一行就返回-1
     * @throws IOException
     * @throws ServiceConfigurationError
     */
    private int parseLine(Class<?> service, URL u, BufferedReader r, int lc)
            throws IOException, ServiceConfigurationError, IllegalAccessException {
        // 读取一行
        String ln = r.readLine();
        // 如果有空行，直接结束
        if (ln == null) {
            return -1;
        }
        // 获取到“#”这个字符的位置，处理注释
        int ci = ln.indexOf('#');
        // 获取“#”字符前面的字符串
        if (ci >= 0) {
            ln = ln.substring(0, ci);
        }
        // ln: xxx = xxx
        // 将ln分为两部分
        // clazz是全类名
        String clazz;
        // name是键名
        String name;
        int place = ln.indexOf('=');
        name = ln.substring(0, place).trim();
        clazz = ln.substring(place + 1).trim();
        // 如果clazz为空，报错
        if (StringUtils.isEmpty(name) || StringUtils.isEmpty(clazz)) {
            throw new IllegalAccessException("文件内容不符合键值对的要求！");
        }
        // 将ln替换成clazz做全类名的判断
        ln = clazz;
        int n = ln.length();
        // 说明有内容
        if (n != 0) {
            // 内容中间等位置有空格或者制表符
            if ((ln.indexOf(' ') >= 0) || (ln.indexOf('\t') >= 0)) {
                // 直接报错误
                fail(service, u, lc, "Illegal configuration-file syntax");
            }
            // 获取0号位的字符，Unicode
            int cp = ln.codePointAt(0);
            // 首位的字符是否可以作为Java命名的首字符
            if (!Character.isJavaIdentifierStart(cp)) {
                // 不可以直接报错
                fail(service, u, lc, "Illegal provider-class name: " + ln);
            }
            // Character.charCount(cp)该编码占据的字符长度
            // n是总字符长度，但是有些复杂符号占据两个字符，所以就是i += Character.charCount(cp)
            // 而不是i++
            for (int i = Character.charCount(cp); i < n; i += Character.charCount(cp)) {
                // 获取Unicode编码
                cp = ln.codePointAt(i);
                // 不是“.”又不是合法命名字符
                // 判断不是“.”，是因为填入的是全类名
                if (!Character.isJavaIdentifierPart(cp) && (cp != '.')) {
                    // 报错
                    fail(service, u, lc, "Illegal provider-class name: " + ln);
                }
            }
            // 判断出了合法的全类名
            // 加载到configures里面
            configures.put(service.getName() + "#" + name, clazz);
        }
        // 读取下一行
        return lc + 1;
    }

    // Parse the content of the given URL as a provider-configuration file.
    //
    // @param  service
    //         The service type for which providers are being sought;
    //         used to construct error detail strings
    //
    // @param  u
    //         The URL naming the configuration file to be parsed
    //
    // @return A (possibly empty) iterator that will yield the provider-class
    //         names in the given configuration file that are not yet members
    //         of the returned set
    //
    // @throws ServiceConfigurationError
    //         If an I/O error occurs while reading from the given URL, or
    //         if a configuration-file format error is detected
    //

    /**
     * @param service 需要实例化的类
     * @param u       配置文件路径
     * @return 拥有配置文件中所有全类名的迭代器
     * @throws ServiceConfigurationError
     */
    private void parse(Class<?> service, URL u)
            throws ServiceConfigurationError {
        // 字节输入流
        InputStream in = null;
        // 缓冲字符流
        BufferedReader r = null;
        try {
            // 获取输入流
            in = u.openStream();
            // 输入到缓冲区中使用字符读取
            r = new BufferedReader(new InputStreamReader(in, "utf-8"));
            int lc = 1;
            // 一行一行读取，遇到空行返回-1，循环结束
            while ((lc = parseLine(service, u, r, lc)) >= 0) {
                ;
            }
        } catch (IOException | IllegalAccessException x) {
            fail(service, "Error reading configuration file", x);
        } finally {
            try {
                // 关闭流
                if (r != null) {
                    r.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException y) {
                fail(service, "Error closing configuration file", y);
            }
        }
    }

    /**
     * 获取或者创建锁
     * @param name
     * @return
     */
    private Holder<S> getOrCreateHolder(String name) {
        Holder<S> holder = providers.get(name);
        if (holder == null) {
            providers.putIfAbsent(name, new Holder<>());
            holder = providers.get(name);
        }
        return holder;
    }


    @Override
    public Iterator<S> iterator() {
        return new Iterator<S>() {

            // 返回一个对象集合的set集合，这个对象类型是Map.Entry<String, S>
            // 获取到set集合后，再获取到迭代器
            Iterator<Map.Entry<String, Holder<S>>> knownProviders
                    = providers.entrySet().iterator();

            // 判断是否有下一个
            @Override
            public boolean hasNext() {
                return knownProviders.hasNext();
            }

            public String nextKey() {
                return knownProviders.next().getKey();
            }

            // 获取下一个值
            @Override
            public S next() {
                return knownProviders.next().getValue().get();
            }

            // 不允许操作
            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

        };
    }

    /**
     * Returns a string describing this service.
     *
     * @return A descriptive string
     */
    @Override
    public String toString() {
        return "java.util.ExtensionLoader[" + service.getName() + "]";
    }

}
