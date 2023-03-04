package com.hmdp.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ClassName: RedissonConfig
 * Package: com.hmdp.config
 * Description:
 *
 * @Author 梓维李
 * @Create 2023/3/1 22:24
 * @Version 2.0
 */
@Configuration
public class RedissonConfig {

    @Bean
    public RedissonClient redissonClient(){
        Config config = new Config();
        config.useSingleServer().setAddress("redis://192.168.254.36:6379").setPassword("123321");
        return Redisson.create(config);
    }
}
