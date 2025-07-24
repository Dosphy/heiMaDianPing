package com.hmdp.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.Collection;
import java.util.List;

import static com.hmdp.utils.RedisConstants.SHOP_LIST_KEY;

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

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private ShopTypeMapper shopTypeMapper;


    @Override
    public Result getShopList() {
        //1.从redis中获取商铺列表
        List<String> shopList = stringRedisTemplate.opsForList().range(SHOP_LIST_KEY,0,-1);

        //2.判断redis中是否存在商铺列表数据
        if(CollectionUtil.isNotEmpty(shopList)){
            List<ShopType> shopTypes = JSONUtil.toList(shopList.get(0),ShopType.class);
            return Result.ok(shopTypes);
        }

        //3.redis不存在数据则查询数据库
        List<ShopType> shopTypes = query().orderByAsc("sort").list();

        //4.数据库不存在则报错
        if(CollectionUtil.isEmpty(shopTypes)){
            return Result.fail("信息不存在!");
        }

        //5.数据库存在信息则存储到redis并返回给前端
        String shopType = JSONUtil.toJsonStr(shopTypes);
        stringRedisTemplate.opsForList().leftPush(SHOP_LIST_KEY,shopType);

        //6.返回数据
        return Result.ok(shopTypes);
    }
}
