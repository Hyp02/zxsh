package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.Voucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private RedisIdWorker redisIdWorker;

    /**
     * 优惠券下单
     * @param voucherId
     * @return
     */
    @Override
    @Transactional
    public Result buySeckillVoucher(Long voucherId) {
        // 校验优惠券状态
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        // 校验有效期
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())){
            return Result.fail("活动未开始时间");
        }
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            return Result.fail("活动已结束");
        }
        // 校验库存
        Integer voucherStock = voucher.getStock();
        if (voucherStock<1){
            return Result.fail("库存不足");
        }
        // 扣减库存
        boolean update = seckillVoucherService.update()
                .setSql("stock=stock-1")
                .eq("voucher_id", voucherId).update();
        if (!update){
            return Result.fail("库存不足");
        }
        // 生成订单id
        long orderId = redisIdWorker.nextId("order");
        // 创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(UserHolder.getUser().getId());
        voucherOrder.setVoucherId(voucherId);
        // 保存订单
        this.save(voucherOrder);
        // 返回订单id
        return Result.ok(orderId);
    }
}
