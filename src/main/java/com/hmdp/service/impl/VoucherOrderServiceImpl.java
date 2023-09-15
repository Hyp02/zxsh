package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.Voucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IVoucherService;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * <p>
 * 服务实现类
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
     * 使用乐观锁解决超卖
     *
     * @param voucherId
     * @return
     */
    @Override
    public Result buySeckillVoucher(Long voucherId) {
        // 校验优惠券状态
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        // 校验有效期
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            return Result.fail("活动未开始时间");
        }
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            return Result.fail("活动已结束");
        }
        // 校验库存
        Integer voucherStock = voucher.getStock();
        if (voucherStock < 1) {
            return Result.fail("库存不足");
        }
        Long userId = UserHolder.getUser().getId();
        // spring事务是交给ioc容器进行管理的，事务提交的时机不确定，因为释放锁需要在事务完成后，所以需要在整个方法上加锁
        synchronized (userId.toString().intern()) {
            // Spring 默认使用基于 AOP 的代理机制来实现事务管理，这意味着只有通过代理调用的方法才会触发事务切面
            // 要获取代理对象来调用，防止事务失效
            IVoucherOrderService voucherOrderService = (IVoucherOrderService) AopContext.currentProxy();
            return voucherOrderService.createOrder(voucherId);
        }
    }

    @Transactional
    public Result createOrder(Long voucherId) {
        // 一人一单
        Long userId = UserHolder.getUser().getId();
        // 对同一个id的用户加锁
        // 使用intern返回字符串常量池中已经存在的对象的值，不会返回新对象
        synchronized (userId.toString().intern()) {

            Integer count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
            if (count > 0) {
                return Result.fail("您已经购买过了");
            }
            // 扣减库存
            boolean update = seckillVoucherService.update()
                    .setSql("stock=stock-1")
                    .eq("voucher_id", voucherId)
                    // where voucher_id = xx and stock = xx【乐观锁】
                    //.eq("stock", voucherStock)
                    // where voucher_id = xx and stock > 0 优化乐观锁，只要库存大于0就可以减鲁村】
                    .gt("stock", 0).update();
            if (!update) {
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
}
