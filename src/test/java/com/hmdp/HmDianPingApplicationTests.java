package com.hmdp;

import com.hmdp.entity.Shop;
import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.HotShop;
import com.hmdp.utils.RedisConstants;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@SpringBootTest
class HmDianPingApplicationTests {
    @Resource
    private CacheClient cacheClient;
    @Resource
    private ShopServiceImpl shopService;

    /**
     * 写入一个店铺数据，并且设置逻辑过期时间
     */
    @Test
    public void testAddHotShop(){
        List<Long> longs = Arrays.asList(HotShop.hotShopId);
        for (Long id : longs) {
            Shop shop = shopService.getById(id);
            cacheClient.setWithLogicExpired(RedisConstants.CACHE_SHOP_KEY+id,shop,20L, TimeUnit.SECONDS);
        }
    }

}
