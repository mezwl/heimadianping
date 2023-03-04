package com.hmdp.utils;

/**
 * ClassName: ILock
 * Package: com.hmdp.utils
 * Description:  自定义分布式锁接口
 *
 * @Author 梓维李
 * @Create 2023/3/1 14:16
 * @Version 2.0
 */
public interface ILock {

    /**
     * 尝试获取锁
     * timeoutSec锁持有的超时时间，过期后自动释放
     * @param timeoutSec
     * @return
     */
    boolean tryLock(long timeoutSec) ;

    /**
     * 释放锁
     */
    void unLock() ;
}
