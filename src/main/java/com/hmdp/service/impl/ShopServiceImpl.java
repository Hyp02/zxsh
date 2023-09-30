package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Blog;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.HotShop;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.SystemConstants;
import javafx.scene.effect.Light;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisCommands;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisCommand;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.SHOP_GEO_KEY;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    // 注入操作Redis的对象
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private CacheClient cacheClient;

    /**
     * 查询店铺信息并使用redis缓存
     * 解决缓存穿透
     *
     * @param id
     * @return
     */
    @Override
    public Result queryById(Long id) {
        // 互斥锁解决缓存击穿
        //Shop shop = this.queryWithMutex(id);
        // 判断店铺是否是热点店铺
        Shop shop = null;
        if (Arrays.asList(HotShop.hotShopId).contains(id)) {
            // 指定逻辑过期解决高并发问题的缓存击穿
            shop = cacheClient.queryWithLogicExpiredTime(RedisConstants.CACHE_SHOP_KEY, id, Shop.class, this::getById,
                    20L, TimeUnit.SECONDS);
            if (shop == null) {
                Result.fail("店铺不见了");
            }
            return Result.ok(shop);
        }
        // 如果不是热点店铺，并发不高
        // 空缓存解决缓存穿透
        //shop = this.queryWithPassThorough(id);
        shop = cacheClient.queryWithPassThorough(RedisConstants.CACHE_SHOP_KEY, id, Shop.class,
                RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES, this::getById);
        if (shop == null) {
            return Result.fail("店铺不见了");
        }
        return Result.ok(shop);
    }

    /**
     * 更新店铺
     * 确保缓存数据一致
     *
     * @param shop 店铺对象
     * @return
     */
    @Override
    @Transactional //添加事务
    public Result updateShopById(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("餐厅不存在");
        }
        // 修改数据库
        boolean b = updateById(shop);
        if (!b) {
            return Result.fail("更新店铺信息失败");
        }
        // 删除缓存
        stringRedisTemplate.delete(RedisConstants.CACHE_SHOP_KEY + id);
        return Result.ok();
    }

    /**
     * 根据类型查询店铺
     * 根据举例进行排序
     *
     * @param typeId
     * @param pageNo
     * @param x
     * @param y
     * @return
     */
    @Override
    public Result queryShopByType(Integer typeId, Integer pageNo, Double x, Double y) {
        // 先判断是否需要根据举例排序
        if (x == null || y == null) {
            // 根据类型分页查询
            Page<Shop> page = this.query()
                    .eq("type_id", typeId)
                    .page(new Page<>(pageNo, SystemConstants.DEFAULT_PAGE_SIZE));
            // 返回数据
            return Result.ok(page.getRecords());
        }
        // 计算分页参数
        int start = (pageNo - 1) * SystemConstants.DEFAULT_PAGE_SIZE;
        int end = pageNo * SystemConstants.DEFAULT_PAGE_SIZE;
        // 查询redis，按照距离进行排序 分页
        String key = SHOP_GEO_KEY + typeId;
        // 获取到之前存放到redis中的店铺的信息 search
        GeoResults<RedisGeoCommands.GeoLocation<String>> search = stringRedisTemplate.opsForGeo().search(
                key,
                GeoReference.fromCoordinate(x, y),
                new Distance(5000),
                RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().limit(end)
        );
        if (search == null) {
            return Result.ok(Collections.emptyList());
        }
        // 根据search解析出 id 距离信息为一个集合list
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> list = search.getContent();
        if (list.size() == 0) {
            return Result.ok(Collections.emptyList());
        }
        // 创建存放id的集合
        List<Long> idList = new ArrayList<>(list.size());
        // 创建匹配id和距离的 map
        HashMap<String, Distance> id_distanceMap = new HashMap<>(list.size());
        list.stream().skip(start).forEach(result -> {
            // 获取id
            String shopIdStr = result.getContent().getName();
            // 将id存放到list中
            idList.add(Long.valueOf(shopIdStr));
            // 获取距离
            Distance distance = result.getDistance();
            // 将店铺id和距离进行匹配
            id_distanceMap.put(shopIdStr, distance);
        });
        // 根据id批量查询
        String idsStr = StrUtil.join(",", idList);
        List<Shop> shops = this.query()
                .in("id", idList)
                .last("ORDER BY FIELD(id," + idsStr + ")")
                .list();
        // 将店铺id和距离进行匹配 设置店铺的距离信息
        shops.forEach(shop -> {
            shop.setDistance(id_distanceMap.get(shop.getId().toString()).getValue());
        });

        // 返回匹配后的店铺
        return Result.ok(shops);
    }

}
