package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisData;
import com.hmdp.utils.SystemConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.time.LocalDateTime;
import java.util.*;
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

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;



    @Override
    public Result queryById(Long id) {
        //解决缓存穿透
        //Shop shop = cacheClient.queryWithPassThrough(CACHE_SHOP_KEY,id,Shop.class,this::getById,CACHE_SHOP_TTL,TimeUnit.MINUTES);

        //互斥锁解决缓存击穿
        //Shop shop = queryWithMutex(id);

        //逻辑过期解决缓存击穿
        Shop shop = cacheClient.queryWithLogicalExpire(CACHE_SHOP_KEY,id, Shop.class,this::getById,CACHE_SHOP_TTL,TimeUnit.MINUTES);
        if (shop == null) {
            return Result.fail("店铺不存在!");
        }

        //返回数据
        return Result.ok(shop);
    }

    //缓存击穿
//    private Shop queryWithPassThrough(Long id){
//        //1.从redis查询商铺缓存
//        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
//
//        //2.判断是否存在
//        if(StrUtil.isNotBlank(shopJson)){
//            //存在则返回
//            return JSONUtil.toBean(shopJson, Shop.class);
//        }
//
//        //3.不存在，则查询数据库
//        //首先判断是否是空值
//        if(shopJson != null){
//            return null;
//        }
//        Shop shop = getById(id);
//
//        //4.数据库不存在，返回错误
//        if(shop == null){
//            //写入redis空值(避免缓存穿透)
//            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
//            return null;
//        }
//
//        //5.存在写入redis
//        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
//
//        //6.返回
//        return shop;
//    }

    //互斥锁
//    private Shop queryWithMutex(Long id) {
//        //1.从redis查询商铺缓存
//        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
//
//        //2.判断是否存在
//        if (StrUtil.isNotBlank(shopJson)) {
//            //存在则返回
//            return JSONUtil.toBean(shopJson, Shop.class);
//        }
//
//        Shop shop = null;
//        try {
//            //3.数据库不存在，实现缓存重建
//            //3.1获取互斥锁
//            boolean lock = tryLock(String.valueOf(LOCK_SHOP_KEY + id));
//
//            //3.2获取不到互斥锁则休眠重试
//            if (!lock) {
//                Thread.sleep(50);
//                return queryWithMutex(id);
//            }
//
//            //3.3获取到了互斥锁即查询数据库数据
//            //首先判断是否是空值
//            if (shopJson != null) {
//                return null;
//            }
//            shop = getById(id);
//
//            if (shop == null) {
//                //写入redis空值(避免缓存穿透)
//                stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
//                return null;
//            }
//
//            //6.存在写入redis
//            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
//
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        } finally {
//            //7.释放互斥锁
//            unlock(String.valueOf(LOCK_SHOP_KEY + id));
//        }
//
//        //8.返回
//        return shop;
//    }

    //缓存击穿逻辑过期
//    private Shop queryWithLogicalExpire(Long id){
//        //1.从redis查询商铺缓存
//        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
//
//        //2.判断是否不存在
//        if(StrUtil.isBlank(shopJson)){
//            //不存在则返回null
//            return null;
//        }
//
//        //3.命中数据
//        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
//        JSONObject shopJson2 = (JSONObject) redisData.getData();
//        Shop shop = JSONUtil.toBean(shopJson2, Shop.class);//得到的商户信息
//
//        //4.取出数据中设置的过期时间
//        LocalDateTime expireTime = redisData.getExpireTime();
//
//        //5.判断是否过期
//        if(expireTime.isAfter(LocalDateTime.now())){
//            //未过期,直接返回商铺数据
//            return shop;
//        }
//
//        //过期,缓存重建
//        //6.1获取互斥锁
//        boolean lock = tryLock(String.valueOf(LOCK_SHOP_KEY + id));
//
//        //6.2获取互斥锁是否成功
//        if (lock) {
//            //成功则开启独立线程实现缓存重建
//            CACHE_REBUILD_EXECUTOR.submit(() -> {
//                try {
//                    //重建缓存
//                    this.saveShop2Redis(id, 6000L);
//                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                } finally {
//                    //释放锁
//                    unlock(String.valueOf(LOCK_SHOP_KEY + id));
//                }
//            });
//        }
//
//        //获取互斥锁失败则代表已经有线程在执行缓存重建任务，直接返回旧数据
//        return shop;
//
//    }
    public void saveShop2Redis(Long id,Long expireSeconds) {
        //1.查询商铺信息
        Shop shop = getById(id);
        //2.封装逻辑过期时间
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        //3.写入redis
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
    }




    @Override
    @Transactional
    public Result updateShop(Shop shop) {
        if(shop.getId() == null){
            return Result.fail("店铺id不能为空!");
        }

        //1.更新数据库
        updateById(shop);

        //2.删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + shop.getId());
        return Result.ok();
    }

    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        //1.判断是否需要根据坐标查询
        if(x == null || y == null){
            // 根据类型分页查询
            Page<Shop> page = query()
                    .eq("type_id", typeId)
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            // 返回数据
            return Result.ok(page.getRecords());
        }

        //2.计算分页参数
        int from = (current - 1) * SystemConstants.DEFAULT_PAGE_SIZE;
        int end = current * SystemConstants.DEFAULT_PAGE_SIZE;

        //3.查询redis，按照距离排序分页
        String key = SHOP_GEO_KEY + typeId;
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo()
                .search(
                        key,
                        GeoReference.fromCoordinate(x, y),
                        new Distance(5000),
                        RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().limit(end)
                );

        //4.解析id
        if(results == null){
            return Result.ok(Collections.emptyList());
        }
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> list = results.getContent();
        if(list.size() <= from){
            return Result.ok(Collections.emptyList());
        }
        List<Long> ids = new ArrayList<>(list.size());
        Map<String, Distance> distanceMap = new HashMap<>(list.size());
        list.stream().skip(from).forEach(result -> {
            String shopIdStr = result.getContent().getName();
            ids.add(Long.valueOf(shopIdStr));
            Distance distance = result.getDistance();
        });

        //5.根据id查询shop
        String idStr = StrUtil.join(",",ids);
        List<Shop> shops = query().in("id", ids).last("ORDER BY FIELD(ID," + idStr + ")").list();
        for (Shop shop : shops) {
            shop.setDistance(distanceMap.get(shop.getId().toString()).getValue());
        }

        //6.返回
        return Result.ok(shops);
    }
}
