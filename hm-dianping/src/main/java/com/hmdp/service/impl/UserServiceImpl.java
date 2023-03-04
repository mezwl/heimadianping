package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexPatterns;
import com.hmdp.utils.RegexUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;
import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private UserMapper userMapper ;

    @Resource
    private StringRedisTemplate stringRedisTemplate ;


    @Override
    public Result sendCode(String phone, HttpSession session) {
        //1、校验手机号
        boolean phoneInvalid = RegexUtils.isPhoneInvalid(phone);
        //2、如果不符合格式直接打回
        if (phoneInvalid){
            return Result.fail("手机号格式不正确") ;
        }
        //3、生成随机验证码
        String s = RandomUtil.randomString(6);

        //4、保存验证码到redis  设置key存活时长2min
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone,s,LOGIN_CODE_TTL, TimeUnit.MINUTES);

        //5、发送验证码
        System.out.println("验证码"+s);

        //6、返回ok
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        System.out.println(loginForm);
        //获取传来手机号
        String phone = loginForm.getPhone();
        //1、校验手机号
        boolean phoneInvalid = RegexUtils.isPhoneInvalid(phone);
        //如果格式不符合直接打回
        if (phoneInvalid){
            return Result.fail("手机号格式错误");
        }
        //2、校验验证码
        //判断验证码格式是否符合
        String code = loginForm.getCode();
        if (RegexUtils.isCodeInvalid(code)){
            return Result.fail("验证码格式不符合") ;
        }
        // TODO 判断验证码是否相同 通过redis
        String code1 = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        if (code1 == null || !code.equals(code1)){
            return Result.fail("验证码不正确");
        }
        //3、查询库中是否存在
        LambdaQueryWrapper<User> lambdaQueryWrapper = new LambdaQueryWrapper<>() ;
        lambdaQueryWrapper.eq(User::getPhone,phone) ;
        User user = userMapper.selectOne(lambdaQueryWrapper);
        if (user == null){
            //当为新用户时，那么就创建一个用户
            user = new User();
            user.setPhone(phone);
            user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
            userMapper.insert(user);
        }

        // TODO 将用户放进redis   不管是查到的还是临时创建的user里都有值
        // ①生成随机token 作为登录令牌  放在请求头中，每次请求过来都会带着这个标识进来
        String token = UUID.randomUUID().toString(true);
        // ②将user对象转为hashmap存储
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> map = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
        String num = (LOGIN_USER_KEY + token) ;
        stringRedisTemplate.opsForHash().putAll(num,map);
        //设置token有效期
        stringRedisTemplate.expire(num,LOGIN_USER_TTL,TimeUnit.MINUTES);
        return Result.ok(token);
    }
}
