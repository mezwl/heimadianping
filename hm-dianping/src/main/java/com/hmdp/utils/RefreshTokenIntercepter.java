package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.UserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;

/**
 * ClassName: RefreshTokenIntercepter
 * Package: com.hmdp.utils
 * Description:   在登录拦截器之前做一层拦截，拦截所有请求，进行大部分的业务逻辑
 *
 * @Author 梓维李
 * @Create 2023/2/27 11:03
 * @Version 2.0
 */
@Component
public class RefreshTokenIntercepter implements HandlerInterceptor {

    @Autowired
    private StringRedisTemplate stringRedisTemplate ;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        System.out.println("进入到token的拦截器");

        //todo 1、获取请求头中的token
        String authorization = request.getHeader("authorization");

        //判断是否为空
        if (authorization == null || "".equals(authorization)){
            return true ;
        }
        //todo 2、基于token获取redis中的用户
        Map<Object, Object> map = stringRedisTemplate.opsForHash().entries(LOGIN_USER_KEY + authorization);
        System.out.println("退出登录之后redis获取用户=-------"+map);
        //3、判断用户是否存在
        //不存在拦截   401未授权
        if (map == null){
           return true ;
        }
        //todo 因为用户信息存放在redis是hashmap结构，因此需要转换成userDto   第三个参数意思是是否忽略转换过程的错误，一定不能忽略
        UserDTO userDTO = BeanUtil.fillBeanWithMap(map, new UserDTO(), false);
        //4、若存在则，保存用户到ThreadLocal
        UserHolder.saveUser(userDTO);
        //todo  刷新token有效期，
        stringRedisTemplate.expire(LOGIN_USER_KEY + authorization,RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);


        return true ;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}
