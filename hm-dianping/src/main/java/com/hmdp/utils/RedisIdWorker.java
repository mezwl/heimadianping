package com.hmdp.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * ClassName: RedisIdWorker
 * Package: com.hmdp.utils
 * Description:
 *
 * @Author 梓维李
 * @Create 2023/2/28 21:52
 * @Version 2.0
 */
@Component
public class RedisIdWorker {

    @Autowired
    private StringRedisTemplate stringRedisTemplate ;


    /**
     * 开始时间戳
     */
    private static final long BEGIN_TIMESTAMP = 1672531200 ;

    /**
     * 序列号的位数
     */
    private static final int COUNT_BTTS = 32 ;


   public long nextKey(String preKey){
       //1、生成时间戳
       LocalDateTime now = LocalDateTime.now();
       //得到当前秒数
       long l = now.toEpochSecond(ZoneOffset.UTC);

       long timeStamp = l - BEGIN_TIMESTAMP;
       //2、生成序列号
       //获取当前日期，精确到天
       String format = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
       //自增长
       long dateMsg = stringRedisTemplate.opsForValue().increment("icr:" + preKey + format);
       //3、拼接并返回
       return timeStamp << COUNT_BTTS | dateMsg ;
   }




    //测试2023年一月一号零时零分零秒的时间戳
    public static void main(String[] args) {
        LocalDateTime time = LocalDateTime.of(2023,1,1,0,0,0) ;
        long l = time.toEpochSecond(ZoneOffset.UTC);
        System.out.println("second:"+l);
    }


}
