package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisData;
import jdk.nashorn.internal.ir.ReturnNode;
import jdk.nashorn.internal.ir.VarNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.KeyBoundCursor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate ;

    @Autowired
    private ShopMapper shopMapper ;

    @Autowired
    private CacheClient cacheClient ;  //注入工具类
    @Override
    public Result querybyId(Long id) {
        //缓存穿透
        Shop shop = cacheClient.queryWithPassThrough(CACHE_SHOP_KEY, id, Shop.class, shopMapper::selectById, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        //互斥锁解决缓存击穿
//        Shop shop = queryWithMutex(id);
//        if (shop == null){
//            return Result.fail("店铺不存在");
//        }

        //逻辑过期解决缓存击穿问题
      // cacheClient.queryWithLogicalExpire(id,CACHE_SHOP_KEY,Shop.class,LOCK_SHOP_KEY,shopMapper::selectById,CACHE_SHOP_TTL, TimeUnit.MINUTES);
        //将结果返回
        return Result.ok(shop) ;
    }

    //将缓存击穿代码封装起来  互斥锁思路
//    public Shop queryWithMutex(Long id){
//        //根据id 去redis查询
//        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
//        System.out.println("查询结果"+shopJson);
//
//        //判断是否存在，若存在直接返回，需要反序列化
//        if (shopJson != null && !"".equals(shopJson)){
//            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
//            return shop;
//        }
//
//        if (shopJson != null){
//            //空值    如果不等于null，那么就是空字符串，所以直接打回
//            //因为如果在redis中没有查到该key，那么返回结果一定是null
//            //但是如果在redis中查到了key，但是该key对应的value为空串，那么直接就进来，然后打回，防止缓存穿透
//            return null;
//        }
//
//        //此段开始与缓存穿透不同   加锁
//        String lockKey = LOCK_SHOP_KEY + id ;
//        //获取互斥锁
//        Shop shop = null;
//        try {
//            boolean b = tryLock(lockKey);
//            //判断是否获取成功
//            if (!b){
//                //失败，则休眠并重试
//                Thread.sleep(50);
//                //需要重复以上步骤，递归调用
//                return queryWithMutex(id);
//            }
//            //再次查询数据库，构成双重检查锁
//            String shopJsonDouble = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
//            //判断是否存在，若存在直接返回，需要反序列化
//            if (shopJsonDouble != null && !"".equals(shopJsonDouble)){
//                 shop = JSONUtil.toBean(shopJsonDouble, Shop.class);
//                return shop;
//            }
//            //第二次检查仍为空值，故去库里查询
//            //获取锁成功 则去查询数据库，并写入redis
//            shop = shopMapper.selectById(id);
//            //若不存在，返回错误结果
//            if (shop == null){
//                //将空值写入redis 防止缓存穿透
//                stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id,"",CACHE_NULL_TTL,TimeUnit.MINUTES);
//                return null ;
//            }
//            //若存在
//            //查询成功将结果写入redis
//            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id,JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);
//
//
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        } finally {
//            //释放互斥锁
//            unLock(lockKey);
//        }
//
//
//        return shop ;
//    }


    //数据预热，先将数据导入redis
    public void saveShopRedis(Long id,Long expireSeconds){
        //1、查询店铺数据
        Shop shop = shopMapper.selectById(id);
        //2、封装逻辑过期时间
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        //3、写入缓存
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id,JSONUtil.toJsonStr(redisData));

    }






    @Override
    @Transactional
    public Result update(Shop shop) {
        //先更新数据库再删除redis缓存

        if (shop.getId() == null){
            return Result.fail("店铺id不能为空") ;
        }
        //1、更新数据库
        shopMapper.updateById(shop);
        //2、删除redis缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + shop.getId());
        return Result.ok();
    }
}
