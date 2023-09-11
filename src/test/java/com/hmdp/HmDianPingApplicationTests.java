package com.hmdp;

import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.HotShop;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

@SpringBootTest
class HmDianPingApplicationTests {
    @Resource
    private ShopServiceImpl shopService;

    /**
     * 写入一个店铺数据，并且设置逻辑过期时间
     */
    @Test
    public void testAddHotShop(){
        List<Long> longs = Arrays.asList(HotShop.hotShopId);
        for (Long id : longs) {
            shopService.addExpiredTimeByShop(id, 10L);
        }
    }

}
