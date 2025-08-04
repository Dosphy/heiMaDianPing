package com.hmdp;

import com.hmdp.entity.Shop;
import com.hmdp.service.IShopService;
import com.hmdp.service.impl.ShopServiceImpl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.SHOP_GEO_KEY;

@SpringBootTest
class HmDianPingApplicationTests {

    @Resource
    private IShopService shopService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

//    @Test
//    void testSaveShop() {
//        shopServiceImpl.saveShop2Redis(1L,1000L);
//    }

//    @Test
//    void loadShopData() {
//        List<Shop> list = shopService.list();
//        Map<Long, List<Shop>> map = list.stream().collect(Collectors.groupingBy(Shop::getTypeId));
//        for(Map.Entry<Long, List<Shop>> entry : map.entrySet()){
//            Long typeId = entry.getKey();
//            String key = SHOP_GEO_KEY + typeId;
//            List<Shop> value = entry.getValue();
//            List<RedisGeoCommands.GeoLocation<String>> locations = new ArrayList<>(value.size());
//            for(Shop shop : value){
//                locations.add(new RedisGeoCommands.GeoLocation<>(
//                        shop.getId().toString(),
//                        new Point(shop.getX(), shop.getY())
//                ));
//            }
//            stringRedisTemplate.opsForGeo().add(key, locations);
//        }
//    }

}
