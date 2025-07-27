package com.hmdp.utils;

public interface ILock {
    //尝试获取锁，timeoutSec为锁的超时时间，过期自动释放
    boolean tryLock(long timeoutSec);

    //释放锁
    void unlock();
}
