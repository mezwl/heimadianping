package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * ClassName: SimpleRedisLock
 * Package: com.hmdp.utils
 * Description:   分布式锁
 *
 * @Author 梓维李
 * @Create 2023/3/1 14:18
 * @Version 2.0
 */

public class SimpleRedisLock implements ILock{

    private String name ;
    private StringRedisTemplate stringRedisTemplate ;

    public SimpleRedisLock(String name,StringRedisTemplate stringRedisTemplate){
        this.name = name ;
        this.stringRedisTemplate = stringRedisTemplate ;
    }
    private static final String KEY_PREFIX = "lock:" ;
    private static final String ID_PREFIX = UUID.randomUUID().toString(true)+"-";

    // lua脚本 准备工作
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    @Override
    public boolean tryLock(long timeoutSec) {
        //获得线程标识
        String tranId = ID_PREFIX + Thread.currentThread().getId();
        //等同于 set lock thread ex 10 nx   表示添加一个不存在的键并设置存活时长
        //setIfAbsent 如果缺席，表示所添加的键名只能是redis库里不存在的
        Boolean aBoolean = stringRedisTemplate.opsForValue().setIfAbsent(
                KEY_PREFIX + name, tranId, timeoutSec, TimeUnit.SECONDS);
        //不能直接返回 aBoolean 因为涉及到自动拆箱问题，可能会发生空指针的问题
        //Boolean.TRUE.equals(aBoolean) 如果为空同样返回false，不会发生空指针的问题
        return Boolean.TRUE.equals(aBoolean);
    }


    @Override
    public void unLock() {
       //因为当获取线程标识与key相等时，马上要释放锁了这时阻塞了，那么也会有并发问题，究其原因因为不是原子性的
       //这时用 lua脚本来代替下面的代码，可保证判断和删除的原子性，避免并发安全问题
        // 调用lua脚本
        stringRedisTemplate.execute(
                UNLOCK_SCRIPT,
                Collections.singletonList(KEY_PREFIX + name),
                ID_PREFIX + Thread.currentThread().getId());
    }


//    @Override
//    public void unLock() {
//        //释放锁
//        //需要考虑一种情况，当第一个线程1进来拿到锁，但是因为一些原因线程阻塞超时，超时锁会自动释放，
//        //但是此时并第二个线程2进来拿到了锁，在线程2执行过程中，线程1苏醒，执行完成，执行删除锁，
//        //但是此时锁已经不是线程1的了，线程2在执行过程中锁被线程1释放了，那么就会出故障
//        //所以需要进行判断
//
//        //获取线程标识
//        String tranId = ID_PREFIX + Thread.currentThread().getId();
//        //判断标识是否一致
//        //获取key
//        String id = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);
//        if (tranId.equals(id)){
//            //释放锁
//            stringRedisTemplate.delete(KEY_PREFIX + name);
//        }
//
//    }
}
