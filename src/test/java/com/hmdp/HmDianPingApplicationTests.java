package com.hmdp;

import com.alibaba.fastjson2.JSON;
import com.hmdp.entity.Blog;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IShopService;
import com.hmdp.service.impl.ShopServiceImpl;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.threads.TaskThread;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.redisson.api.RedissonClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.SHOP_GEO_KEY;

@Slf4j
@SpringBootTest
class HmDianPingApplicationTests {
    
    // 模拟RedissonClient，避免依赖缺失问题
    @MockBean
    private RedissonClient redissonClient;

//    @Resource
//    private IShopService shopService;
//    @Autowired
//    private StringRedisTemplate stringRedisTemplate;

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
    @Autowired
    private BlogMapper blogMapper;

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Autowired
    @Qualifier("taskExecutor")
    private ExecutorService executorService;

    private String BLOG_INDEX = "tb_blog";

    class TaskThread implements Runnable {

        List<Blog> blogList;
        CountDownLatch cdl;

        public TaskThread(List<Blog> blogList, CountDownLatch cdl) {
            this.blogList = blogList;
            this.cdl = cdl;
        }

        @Override
        public void run() {
            //批量导入
            BulkRequest bulkRequest = new BulkRequest(BLOG_INDEX);

            for (Blog blog : blogList) {
                bulkRequest.add(new IndexRequest().id(blog.getId().toString())
                        .source(JSON.toJSONString(blog), XContentType.JSON));
            }
            //发送请求
            try {
                restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            //计数器减1
            cdl.countDown();
        }
    }

    @Test
    public void loadDataToES() throws InterruptedException {
        //总条数
        int count = blogMapper.selectCount(null);
        //总页数
        int totalPageSize = count % 2000 == 0 ? count / 2000 : count / 2000 + 1;
        //开始时间
        long startTime = System.currentTimeMillis();
        //创建countdownLatch
        CountDownLatch countDownLatch = new CountDownLatch(totalPageSize);

        int fromIndex;
        List<Blog> blogList = new ArrayList<>();
        for(int i = 0; i < totalPageSize; i++){
            fromIndex = i * 2000;
            blogList = blogMapper.loadBlogData(fromIndex, 2000);
            //创建线程导入es数据库
            TaskThread taskThread = new TaskThread(blogList, countDownLatch);
            //线程执行
            executorService.execute(taskThread);
        }
        //等待计数归零
        countDownLatch.await();
        //结束时间
        long finishTime = System.currentTimeMillis();
        log.info("es索引数据批量导入共:{}条,共消耗时间:{}秒",count,(finishTime - startTime)/1000);
    }

}
