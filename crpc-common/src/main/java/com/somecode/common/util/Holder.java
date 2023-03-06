package com.somecode.common.util;

/**
 * 作为锁，让synchronized去控制线程互斥
 * @param <T>
 */
public class Holder<T> {

    private volatile T value;

    public void set(T value) {
        this.value = value;
    }

    public T get() {
        return value;
    }

}
