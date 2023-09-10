package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

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

    /**
     * 查询店铺信息并使用redis缓存
     *
     * @param id
     * @return
     */
    @Override
    public Result queryById(Long id) {
        // 查询redis【这里的数据类型可选择Hash和String(这次选择String)】
        String shopJson = stringRedisTemplate.opsForValue().get(RedisConstants.CACHE_SHOP_KEY + id);
        // 命中 返回
        if (StringUtils.isNotBlank(shopJson)) {
            // 转换为对象后返回
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return Result.ok(shop);
        }
        // 未命中 在数据库中查
        // 数据库中不存在 返回错误信息【getById是接口中的方法】
        Shop shop = getById(id);
        if (shop == null) {
            return Result.fail("对不起，该店铺已消失");
        }
        String toJsonShop = JSONUtil.toJsonStr(shop);
        // 数据库中存在 先保存在redis中再返回
        // 设置店铺缓存失效时间
        stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id, toJsonShop,
                RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
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
        if (!b){
            return Result.fail("更新店铺信息失败");
        }
        // 删除缓存
        stringRedisTemplate.delete(RedisConstants.CACHE_SHOP_KEY + id);
        return Result.ok();
    }
}
