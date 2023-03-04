package com.hmdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.Voucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.SeckillVoucherMapper;
import com.hmdp.mapper.VoucherMapper;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.SimpleRedisLock;
import com.hmdp.utils.UserHolder;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.aop.framework.AopProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yaml.snakeyaml.events.Event;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;

import static com.hmdp.utils.RedisConstants.SECKILL_STOCK_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Autowired
    private VoucherMapper voucherMapper ;   //优惠劵

    @Autowired
    private SeckillVoucherMapper seckillVoucherMapper ;  //秒杀券

    @Autowired
    private RedisIdWorker redisIdWorker ;

    @Autowired
    private VoucherOrderMapper voucherOrderMapper ;

    @Autowired
    private StringRedisTemplate stringRedisTemplate ;

    @Autowired
    private RedissonClient redissonClient ;

    // lua脚本 准备工作
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

   private BlockingQueue<VoucherOrder> orderTasks = new ArrayBlockingQueue<>(1024 * 1024) ;

    //获取代理对象
    private IVoucherOrderService proxy ;

    //基于消息队列 redis stream
    @Override
    public Result seckillOrder(Long voucherId) {
        //获取用户
        Long id = UserHolder.getUser().getId();
        //订单id
        long orderId = redisIdWorker.nextKey("order:");
        //执行lua脚本，判断结果
        //脚本内执行的是判断购买资格，发送信息到消息队列中
        Long execute = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(),
                id.toString(),
                String.valueOf(orderId)
        );
        int i = execute.intValue();
        //若不为0 则没有购买资格 直接返回
        if (i != 0){
            return Result.fail(i == 1 ? "库存不足" : "不能重复下单");
        }
         proxy = (IVoucherOrderService) AopContext.currentProxy();
        //返回订单id
     return  Result.ok(orderId);

    }






    //原方案
//    @Override
//    public Result seckillOrder(Long voucherId) {
//        //1、查询优惠劵
//        SeckillVoucher seckillVoucher = seckillVoucherMapper.selectById(voucherId);
//        //判断秒杀是否开始
//        LocalDateTime beginTime = seckillVoucher.getBeginTime();
//        LocalDateTime now = LocalDateTime.now();
//        if (beginTime.isAfter(now)){
//            //秒杀未开始
//            return Result.fail("秒杀尚未开始") ;
//        }
//        //判断秒杀是否已经结束
//        LocalDateTime endTime = seckillVoucher.getEndTime();
//        if (endTime.isBefore(now)){
//            //秒杀已经结束
//            return Result.fail("秒杀已经结束") ;
//        }
//
//
//        //判断库存数量是否够
//        Integer stock = seckillVoucher.getStock();
//        if (stock < 1){
//            //库存不足
//            return Result.fail("库存不足") ;
//        }
//        Long id = UserHolder.getUser().getId();
//
//        //当方法上有事务注解时，加锁不能直接加在方法的内部，因为事务会失效，
//        //因此需要把锁加在方法的外部，可避免事务失效
//        // String.intern()是一个Native方法,它的作用是:如果字符常量池中已经包含一个等于此String对象的字符串,
//        // 则返回常量池中字符串的引用,否则,将新的字符串放入常量池,并返回新字符串的引用
//        //锁的是用户，减少锁的范围，就是说并发时只有相同用户进来会加锁，如如果进来的是不同用户那么不会加锁
//       // synchronized (id.toString().intern()){
//            //想想当前调用此方法的是谁，是this.  那么this指代的是当前类的非代理对象
//            //但是事务执行是通过aop来的，因此也就是代理对象，所以我们直接调用方法拿的不是代理对象，那么自然事务也就失效
//            //解决方案：①注入自己，拿到自己的代理对象再去调用  ②利用AopContext拿到当前的代理对象
//
//            //利用AopContext 步骤：①导入依赖 ②在启动类加上@EnableAspectJAutoProxy(exposeProxy = true)
////            IVoucherOrderService voucherOrderService = (IVoucherOrderService)AopContext.currentProxy();
////            return voucherOrderService.createVoucherOrder(voucherId,seckillVoucher,stock);
////        }
//
//        //目前是在单体服务下用synchronized是可行的，但是在分布式下是失效的，因此还需要改进
//        //创建锁对象
//        //SimpleRedisLock simpleRedisLock = new SimpleRedisLock("order:"+id, stringRedisTemplate);
//
//        //用redission提供的分布式锁对象
//        RLock simpleRedisLock = redissonClient.getLock("lock:order:" + id);
//
//
//        //获取锁
//        boolean b = simpleRedisLock.tryLock();
//        //获取锁失败
//        if (!b){
//            return Result.fail("不允许重复下单");
//        }
//
//        try {
//            IVoucherOrderService voucherOrderService = (IVoucherOrderService)AopContext.currentProxy();
//            return voucherOrderService.createVoucherOrder(voucherId,seckillVoucher,stock);
//        } finally {
//            //手动释放锁
//                simpleRedisLock.unlock();
//        }
//
//    }

    @Transactional
    public Result createVoucherOrder(Long voucherId, SeckillVoucher seckillVoucher, Integer stock) {
            //实现一人只能抢一个优惠券， 模拟一人一单
            //带着优惠卷id和用户id去优惠劵订单表里查询，如果已经存在，那么直接打回
            //虽然我们已经判断用户一人一单，但在压测情况下还是会出现一人多单
            //针对此只能加悲观锁，
            LambdaQueryWrapper<VoucherOrder> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(VoucherOrder::getVoucherId, voucherId)
                    .eq(VoucherOrder::getUserId, UserHolder.getUser().getId());
            //不需要查询具体值，只需要看数量是否大于0，大于0说明此用户已下过单
            Integer integer = voucherOrderMapper.selectCount(lambdaQueryWrapper);
            if (integer > 0) {
                return Result.fail("该用户已经购买过一次");
            }


            //扣减库存  在此位置就会涉及到
            // 并发安全问题，超卖，
            //解决方案：①悲观锁(性能不好) ②乐观锁(性能好) 因此选择乐观锁   乐观锁只能用于更新数据  悲观锁可用于更新和添加数据
            //原本是基于数据库的版本号来看，但是那就涉及到改表，因此将版本号替换为库存，也就是我们修改时需要带个条件
            //就是修改时的库存必须与我们当前线程查到的库存相等，才能做修改，这种又叫做cas理论，基于乐观锁
            //cas理论良好解决了多线程下CPU上下文切换导致的并发安全问题
            //加个条件构造器，带着库存来查

            //但是还需要注意：上面的思路确实可以防止超卖现象，但是还有一个问题，那就是失败率大大增加了，因为只要修改失败，
            //立刻返回库存不足，但是其实数据库还有库存，针对此问题解决方案：
            //在修改的条件处不一定非得让其库存相等，只要库存大于0，就让其往里进
            LambdaUpdateWrapper<SeckillVoucher> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
            lambdaUpdateWrapper.gt(SeckillVoucher::getStock, 0);
            seckillVoucher.setStock(stock - 1);
            int i = seckillVoucherMapper.update(seckillVoucher, lambdaUpdateWrapper);
            if (i != 1) {
                //扣减库存失败
                return Result.fail("库存不足");
            }
            //创建订单
            VoucherOrder voucherOrder = new VoucherOrder();
            voucherOrder.setId(redisIdWorker.nextKey("order"));
            voucherOrder.setVoucherId(voucherId);
            voucherOrder.setUserId(UserHolder.getUser().getId());
            voucherOrderMapper.insert(voucherOrder);


            return Result.ok(redisIdWorker.nextKey("order"));
    }
}
