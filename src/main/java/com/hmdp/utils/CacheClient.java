package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;
import static com.hmdp.utils.RedisConstants.LOCK_SHOP_KEY;

@Component
@Slf4j
public class CacheClient {

    private final StringRedisTemplate stringRedisTemplate;

    //创建线程池
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void set(String key, Object value, Long time, TimeUnit timeUnit){
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, timeUnit);
    }

    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit timeUnit){
        //设置逻辑过期
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(timeUnit.toSeconds(time)));
        //写入redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    //缓存击穿处理
    public <T,ID> T queryWithPassThrough(String keyPrefix, ID id, Class<T> type, Function<ID, T> dbFallBack, Long time, TimeUnit timeUnit){
        String key = keyPrefix + id;
        //1.从redis中查询缓存
        String json = stringRedisTemplate.opsForValue().get(key);

        //2.判断是否存在
        if(StrUtil.isNotBlank(json)){
            //存在则直接返回
            return JSONUtil.toBean(json, type);
        }

        //3.不存在则判断是否为空值
        if(json != null){
            return null;
        }

        //4.根据id查询数据库
        T t = dbFallBack.apply(id);

        //5.数据库不存在该数据,则写空值进入redis
        if(t == null){
            stringRedisTemplate.opsForValue().set(key, "");
            return null;
        }

        //数据库存在数据,写数据进入redis
        this.set(key, t, time, timeUnit);

        return t;
    }

    //逻辑过期时间处理
    public <T,ID> T queryWithLogicalExpire(String keyPrefix, ID id, Class<T> type, Function<ID, T> dbFallBack, Long time, TimeUnit timeUnit){
        String key = keyPrefix + id;
        //1.从redis查询缓存
        String json = stringRedisTemplate.opsForValue().get(key);

        //2.判断是否不存在
        if(StrUtil.isBlank(json)){
            //不存在则返回null
            return null;
        }

        //3.redis命中数据,反序列化数据
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        T t = JSONUtil.toBean((JSONObject)redisData.getData(), type);

        //4.取出数据中设置的过期时间
        LocalDateTime expireTime = redisData.getExpireTime();

        //5.判断是否过期
        if(expireTime.isAfter(LocalDateTime.now())){
            //未过期,直接返回数据
            return t;
        }

        //过期,缓存重建
        //6.1获取互斥锁
        String lockKey = keyPrefix + id;
        boolean lock = tryLock(lockKey);

        //6.2获取互斥锁是否成功
        if (lock) {
            //成功则开启独立线程实现缓存重建
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    //重建缓存
                    T t2 = dbFallBack.apply(id);

                    //写入redis
                    this.setWithLogicalExpire(key, t2, time, timeUnit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    //释放锁
                    unlock(lockKey);
                }
            });
        }

        //获取互斥锁失败则代表已经有线程在执行缓存重建任务，直接返回旧数据
        return t;

    }

    private boolean tryLock(String key) {
        Boolean aBoolean = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(aBoolean);
    }

    private void unlock(String key) {
        Boolean aBoolean = stringRedisTemplate.delete(key);
    }
}
