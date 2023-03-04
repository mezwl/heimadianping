package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.*;

/**
 * ClassName: CacheClient
 * Package: com.hmdp.utils
 * Description:    将缓存穿透缓存击穿全部封装成工具类
 *
 * @Author 梓维李
 * @Create 2023/2/28 19:53
 * @Version 2.0
 */
@Slf4j
@Component
public class CacheClient {

    @Autowired
    private  StringRedisTemplate stringRedisTemplate ;

    public void set(String key, Object value, Long time, TimeUnit unit){
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value),time,unit);
    }

    //设置逻辑过期方法
    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit){
        //设置逻辑过期
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(redisData));
    }

    //缓存穿透工具方法
    public <R,ID> R queryWithPassThrough(
            String keyPrefix, ID id, Class<R> type, Function<ID,R> dbFallback,Long time, TimeUnit unit){
        //根据id 去redis查询
        String json = stringRedisTemplate.opsForValue().get(keyPrefix + id);
        //判断是否存在，若存在直接返回，需要反序列化
        if (json != null && !"".equals(json)){
            return JSONUtil.toBean(json, type);
        }

        if (json != null){
            //空值    如果不等于null，那么就是空字符串，所以直接打回
            //因为如果在redis中没有查到该key，那么返回结果一定是null
            //但是如果在redis中查到了key，但是该key对应的value为空串，那么直接就进来，然后打回，防止缓存穿透
            return null;
        }


        //若没命中则去数据库查询   我们没法去将查询数据库的方法变成一个公共的，因此在方法的参数上接上一个方法的参数
        //Function<ID,R> dbFallback 其中ID代表参数类型，R代表返回值
        R r = dbFallback.apply(id);
        //若不存在，返回错误结果
        if (r == null){
            //将空值写入redis 防止缓存穿透
            stringRedisTemplate.opsForValue().set(keyPrefix+id,"",time,unit);
            return null ;
        }
        //若存在
        //查询成功将结果写入redis
        this.set(keyPrefix+id,r,time,unit);

        return r ;
    }




    //缓存击穿，逻辑过期工具方法
    //缓存击穿 逻辑过期思路
    public <R,ID> R queryWithLogicalExpire(
            ID id,String keyPrefix,Class<R> type, String lockPrefix,Function<ID,R> dbFallback,Long time, TimeUnit unit){
        //根据id 去redis查询
        String json = stringRedisTemplate.opsForValue().get(keyPrefix + id);
        //如果为空，直接返回
        if (json == null || "".equals(json)){
            return null ;
        }
        //如果命中，将json字符串反序列化为json对象    并查看时间是否过期
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        //todo 通过反序列化转为shop对象
        R r = JSONUtil.toBean((JSONObject) redisData.getData(), type);
        LocalDateTime expireTime = redisData.getExpireTime();

        if (expireTime.isAfter(LocalDateTime.now())){
            //若时间未过期，那么直接返回
            return r ;
        }
        //2、如果时间已过期，需要进行缓存重建
        //2.1、获取互斥锁
        String shopKey = lockPrefix + id ;
        boolean b = tryLock(shopKey);
        if (b){
            //2.3、获取锁成功  双重检查锁，需要再次查看时间是否过期，因为此时拿到锁的线程可能与刚释放完锁的线程访问的是同一个商铺，
            //               所以假设前一个线程已经将该商铺最新的数据写入redis中，所以有必要再检查一下，防止重复查询
            String json1 = stringRedisTemplate.opsForValue().get(keyPrefix + id);
            //如果命中，将json字符串反序列化为json对象    并查看时间是否过期
            RedisData redisData1 = JSONUtil.toBean(json1, RedisData.class);
            //todo 通过反序列化转为shop对象
            R r1 = JSONUtil.toBean((JSONObject) redisData1.getData(), type);
            LocalDateTime expireTime1 = redisData1.getExpireTime();

            if (expireTime1.isAfter(LocalDateTime.now())){
                //若时间未过期，那么直接返回
                return r1 ;
            }
            // 时间过期
            // 开启独立线程
            CACHE_REBUILD_EXECUTOR.submit(new Thread(()->{
                try {
                    //重建缓存 根据id查询数据库，
                    R apply = dbFallback.apply(id);
                    // 并将信息写入redis，并设置逻辑过期时间
                    setWithLogicalExpire(keyPrefix + id,apply,time,unit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    //释放锁
                    unLock(shopKey);
                }
            }));
        }
        //2.2、返回商铺过期信息，无论获取锁成功亦或是失败，都需要返回过期的店铺信息
        return r ;
    }




    //手动创建线程池
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    //获取锁
    private boolean tryLock(String key){
        //setIfAbsent 等同于只能添加不存在的key，若key存在则添加失败，相当于获取锁
        Boolean aBoolean = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(aBoolean);
    }
    //释放锁
    private void unLock(String key){
        stringRedisTemplate.delete(key) ;
    }


}
