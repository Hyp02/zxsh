package com.hmdp;

import com.hmdp.entity.Shop;
import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.HotShop;
import com.hmdp.utils.RedisConstants;

import com.hmdp.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.SHOP_GEO_KEY;

@SpringBootTest
class HmDianPingApplicationTests {
    @Resource
    private CacheClient cacheClient;
    @Resource
    private ShopServiceImpl shopService;
    @Resource
    private RedisIdWorker redisIdWorker;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 将店铺的经纬度信息导入redis
     */
    @Test
    public void loadShopGeo() {
        // 取出所有店铺
        List<Shop> shops = shopService.list();
        // 将店铺分组存放 根据店铺的typeId作为分组依据
        Map<Long, List<Shop>> map = shops.stream().collect(Collectors.groupingBy(Shop::getTypeId));
        for (Map.Entry<Long, List<Shop>> entry : map.entrySet()) {
            // 获取类型id
            Long typeId = entry.getKey();
            // 获取店铺信息
            List<Shop> shopList = entry.getValue();
            // 将店铺存放到GeoLocation中
            ArrayList<RedisGeoCommands.GeoLocation<String>> locations = new ArrayList<>(shopList.size());
            for (Shop shop : shopList) {
                locations.add(new RedisGeoCommands.GeoLocation<String>(
                        shop.getId().toString(),
                        new Point(shop.getX(), shop.getY())
                ));
            }
            // redis的key
            String key = SHOP_GEO_KEY+typeId;
            // 批量将数据写入redis
            stringRedisTemplate.opsForGeo().add(key,locations);
        }

    }

    /**
     * 写入一个店铺数据，并且设置逻辑过期时间
     */
    @Test
    public void testAddHotShop() {
        List<Long> longs = Arrays.asList(HotShop.hotShopId);
        for (Long id : longs) {
            Shop shop = shopService.getById(id);
            cacheClient.setWithLogicExpired(RedisConstants.CACHE_SHOP_KEY + id, shop, 20L, TimeUnit.SECONDS);
        }
    }

    @Test
    public void testIdWorker() {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 300; i++) {
            long order = redisIdWorker.nextId("order");
            System.out.println(order);
        }
        long end = System.currentTimeMillis();
        System.out.println(end - start);

    }

}
