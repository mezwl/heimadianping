package com.hmdp.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.hmdp.utils.RedisConstants.LOCK_SHOP_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Autowired
    private ShopTypeMapper shopTypeMapper ;

    @Autowired
    private StringRedisTemplate stringRedisTemplate ;


    @Override
    public Result selectTypeShop() {
        //查询是否命中
        String s = stringRedisTemplate.opsForValue().get(LOCK_SHOP_KEY);
        //若命中直接返回
        if (s != null && !"".equals(s)){
            List<ShopType> shopTypes = JSONUtil.toList(s, ShopType.class);
            return Result.ok(shopTypes);
        }
        //若没命中去查询数据库
        LambdaQueryWrapper<ShopType> lambdaQueryWrapper = new LambdaQueryWrapper<>() ;
        lambdaQueryWrapper.orderByAsc(ShopType::getSort) ;
        List<ShopType> shopTypes = shopTypeMapper.selectList(lambdaQueryWrapper);
        //若查询数据库结果不存在，返回提示信息
        if (shopTypes == null){
            return Result.fail("商品类型不存在") ;
        }
        //若存在
        //写入redis
        stringRedisTemplate.opsForValue().set(LOCK_SHOP_KEY,JSONUtil.toJsonStr(shopTypes));
        //返回结果
        return Result.ok(shopTypes);
    }
}
