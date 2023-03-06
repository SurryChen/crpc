package com.somecode.common.util;

/**
 * 字符串相关工具类
 * @author 落阳
 * @date 2023/2/27
 */
public class StringUtils {

    /**
     * 判断路径是否合法的正则表达式
     * Windows系统
     */
    private static final String PATH_REGEX = "^[A-z]:\\\\\\\\(.+?\\\\\\\\)*$";

    /**
     * 判断是否为空
     */
    public static boolean isEmpty(String judge) {
        // 不是null并且长度不为0
        if(judge == null || judge.length() == 0) {
            return true;
        }
        return false;
    }

    /**
     * 判断多个字符串是否符合文件路径的要求
     */
    public static boolean isFilePath(String... paths) {
        for(String path: paths) {
            if(!path.matches(PATH_REGEX)) {
                return false;
            }
        }
        return true;
    }

}
