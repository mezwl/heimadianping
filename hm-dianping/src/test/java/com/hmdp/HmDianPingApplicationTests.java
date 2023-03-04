package com.hmdp;

import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.service.impl.VoucherOrderServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
class HmDianPingApplicationTests {


    @Autowired
    private ShopServiceImpl service ;

    @Resource
    private RedisIdWorker redisIdWorker ;

    @Autowired
    private VoucherOrderServiceImpl voucherOrderService ;

    @Test
    void test(){
        service.saveShopRedis(3L,10L);
    }



    @Test
    void test1() throws InterruptedException {
//        for (int i = 0; i < 500; i++) {
//            executorService.submit(new Thread(()->{
//                for (int j = 0; j < 100; j++) {
//                    long node = redisIdWorker.nextKey("node");
//                    System.out.println("全局唯一id："+node);
//                }
//            }));
//        }
        CountDownLatch countDownLatch = new CountDownLatch(300) ;
        ExecutorService executorService = Executors.newFixedThreadPool(300);
        System.out.println("-----------");
        Thread thread = new Thread(() -> {
            for (int j = 0; j < 100; j++) {
                long node = redisIdWorker.nextKey("node");
                System.out.println("全局唯一id：" + node);
            }
            countDownLatch.countDown();
        });
        long l = System.currentTimeMillis();
        for (int i = 0; i < 300; i++) {
            executorService.submit(thread);
        }
        countDownLatch.await();
        long l1 = System.currentTimeMillis();
        System.out.println("总耗时："+ (l1 - l));

    }

    @Test
    void test02() throws InterruptedException {
        int ticket = 100 ;
        ExecutorService executorService = Executors.newFixedThreadPool(300);
        //CountDownLatch countDownLatch = new CountDownLatch(300);
        Thread thread = new Thread(() -> {
           // for (int j = 0; j < 100; j++) {
                // voucherOrderService.seckillOrder(10L);
         //   }
           // countDownLatch.countDown();

        });
        long l = System.currentTimeMillis();
        for (int i = 0; i < 300; i++) {
            executorService.submit(thread);
        }
        //countDownLatch.await();
        long l1 = System.currentTimeMillis();
        System.out.println("总耗时："+ (l1 - l));
    }

    @Test
    void test03(){

    }
}
