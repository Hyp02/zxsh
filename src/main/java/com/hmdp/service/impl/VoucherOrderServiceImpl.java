package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.Voucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.SimpleRedisLock;
import com.hmdp.utils.UserHolder;
import com.sun.org.apache.xml.internal.security.Init;
import jdk.nashorn.internal.ir.WhileNode;
import jdk.nashorn.internal.objects.annotations.Where;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder>
            implements IVoucherOrderService {

    // 加载lua脚本
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    // 创建线程池
    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private RedisIdWorker redisIdWorker;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedissonClient redissonClient;

    // 代理对象
    private IVoucherOrderService proxy;

    @PostConstruct
    private void init() {
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }

    /**
     * 优惠券下单
     * 使用乐观锁解决超卖
     * <p>
     * 添加分布式锁
     * <p>
     * 优化，异步创建订单
     *
     * @param voucherId
     * @return
     */
    @Override
    // 实现接口业务方法，因为是异步处理的，这里只需要返回订单id
    public Result buySeckillVoucher(Long voucherId) {
        // 获取当前线程的用户id
        Long userId = UserHolder.getUser().getId();
        // 生成订单编号
        long orderId = redisIdWorker.nextId("order");
        // 执行lua脚本 资格确认，给消息队列中发送订单消息，创建订单信息到redis
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(),
                userId.toString(),
                String.valueOf(orderId)
        );
        // 判断是否为0
        int intValue = result.intValue();
        // 不为0
        if (intValue != 0) {
            //没有购买资格
            return Result.fail(intValue == 1 ? "库存不足" : "不能重复下单");
        }
        // 为0，代表有资格购买
        // TODO 异步将订单信息存放数据到数据库
        // 初始化代理对象
        proxy = (IVoucherOrderService) AopContext.currentProxy();
        // 直接将订单编号返回，处理订单业务时异步处理的，这里不用管
        return Result.ok(orderId);


    }

    /**
     * 使用stream阻塞消息队列处理订单
     */
    // 异步下单,读取阻塞消息队列【消息队列中没有消息就一直阻塞】
    private class VoucherOrderHandler implements Runnable {
        String queueName = "stream.orders";

        @Override
        public void run() {
            while (true) {
                try {
                    // 取出队列中的消息
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                            StreamOffset.create(queueName, ReadOffset.lastConsumed()) // > 取队列中没有处理的消息

                    );
                    // 判断队列中的消息是否获取成功
                    if (list == null || list.isEmpty()) {
                        // 获取失败，重新获取
                        continue;
                    }
                    // 解析消息中的订单信息
                    MapRecord<String, Object, Object> record = list.get(0);
                    Map<Object, Object> value = record.getValue();
                    VoucherOrder voucherOrder =
                            BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);
                    // 创建订单
                    handleVoucherOrder(voucherOrder);
                    // ACK确认
                    stringRedisTemplate.opsForStream().acknowledge(queueName, "g1", record.getId());
                    log.info("处理订单成功");
                } catch (Exception e) {
                    log.error("异常订单处理", e);
                    handlePendingList();
                }

            }

        }

        // 处理订单
        private void handleVoucherOrder(VoucherOrder voucherOrder) {
            Long userId = voucherOrder.getUserId();
            RLock lock = redissonClient.getLock("lock:order:" + userId);
            boolean tryLock = lock.tryLock();
            if (!tryLock) {
                log.error("请勿重复下单");
                return;
            }
            try {
                proxy.createOrder(voucherOrder);
            } catch (Exception e) {
                lock.unlock();
            }

        }

        // 处理以读取但未确认的消息,pendingList中的消息
        private void handlePendingList() {
            while (true) {
                try {
                    // 取出pendingList中的消息
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                            StreamOffset.create(queueName, ReadOffset.from("0")) // 0 取队列里以读取但未确认的消息

                    );
                    // 判断队列中的消息是否获取成功
                    if (list == null || list.isEmpty()) {
                        // 获取失败，退出，说明没有异常的了
                        break;
                    }
                    // 解析消息中的订单信息
                    MapRecord<String, Object, Object> record = list.get(0);
                    Map<Object, Object> value = record.getValue();
                    VoucherOrder voucherOrder =
                            BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);
                    // 创建订单
                    handleVoucherOrder(voucherOrder);
                    // ACK确认
                    stringRedisTemplate.opsForStream().acknowledge(queueName, "g1", record.getId());
                    log.info("处理pendingList中订单成功");

                } catch (Exception e) {
                    log.error("异常订单处理", e);
                    // 防止处理频繁
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }

            }


        }
    }
    // 保存订单信息到数据库
    @Transactional
    public void createOrder(VoucherOrder voucherOrder) {
        // 一人一单
        Long userId = voucherOrder.getUserId();
        Integer count = query().eq("user_id", userId)
                .eq("voucher_id", voucherOrder.getVoucherId())
                .count();
        if (count > 0) {
            log.error("您已经购买过了");
            return;
        }
        // 扣减库存
        boolean update = seckillVoucherService.update()
                .setSql("stock=stock-1")
                .eq("voucher_id", voucherOrder.getVoucherId())
                .gt("stock", 0).update();
        if (!update) {
            log.error("库存不足");
            return;
        }
        // 保存订单
        this.save(voucherOrder);
    }

    /* @Override
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

         // 这里获取锁是从redis中获取，redis服务只有一台，所以解决了用户在不同服务器上能重复购买的问题
         //SimpleRedisLock orderLock = new SimpleRedisLock(stringRedisTemplate, "order:" + userId);
         //boolean tryLock = orderLock.tryLock(10);

         // 使用redisson
         RLock rLock = redissonClient.getLock("lock:order:" + userId);
         boolean tryLock = rLock.tryLock();

         if (!tryLock) {
             return Result.fail("不能重复下单");
         }
         try {
             // 创建订单
             IVoucherOrderService voucherOrderService = (IVoucherOrderService) AopContext.currentProxy();
             return voucherOrderService.createOrder(voucherId);
         } finally {
             //orderLock.delLock();
             rLock.unlock();
         }

         // spring事务是交给ioc容器进行管理的，事务提交的时机不确定，因为释放锁需要在事务完成后，所以需要在整个方法上加锁
         //synchronized (userId.toString().intern()) {
         //    // Spring 默认使用基于 AOP 的代理机制来实现事务管理，这意味着只有通过代理调用的方法才会触发事务切面
         //    // 要获取代理对象来调用，防止事务失效
         //    IVoucherOrderService voucherOrderService = (IVoucherOrderService) AopContext.currentProxy();
         //    // 优惠券订单，使用代理对象调用，防止事务失效
         //    return voucherOrderService.createOrder(voucherId);
         //}
     }*/
    /*@Transactional
    public Result createOrder(Long voucherId) {
        // 一人一单
        Long userId = UserHolder.getUser().getId();
        // 对同一个id的用户加锁
        // 使用intern返回字符串常量池中已经存在的对象的值，不会返回新对象
        synchronized (userId.toString().intern()) {

            Integer count = query().eq("user_id", userId)
                    .eq("voucher_id", voucherId)
                    .count();
            if (count > 0) {
                return Result.fail("您已经购买过了");
            }
            // 扣减库存
            boolean update = seckillVoucherService.update()
                    .setSql("stock=stock-1")
                    .eq("voucher_id", voucherId)
                    // where voucher_id = xx and stock = xx【乐观锁】
                    //.eq("stock", voucherStock)
                    // where voucher_id = xx and stock > 0 优化乐观锁，只要库存大于0就可以减库存  然后再加锁限制同一用户购买】
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
    }*/

    // 购买
    /*@Override
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
        // 创建代理对象调用方法，防止spring失效
        IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
        return proxy.createOrder(voucherId);

    }*/

    // 创建订单
   /*@Transactional
    public Result createOrder(Long voucherId) {
        // 一人一单
        Long userId = UserHolder.getUser().getId();
        RLock rLock = redissonClient.getLock("lock:order:" + userId);
        boolean tryLock = rLock.tryLock();
        // 获取锁失败
        if (!tryLock) {
            return Result.fail("不能重复下单");
        }
        try {
            Integer count = query().eq("user_id", userId)
                    .eq("voucher_id", voucherId)
                    .count();
            if (count > 0) {
                return Result.fail("您已经购买过了");
            }
            // 扣减库存
            boolean update = seckillVoucherService.update()
                    .setSql("stock=stock-1")
                    .eq("voucher_id", voucherId)
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
        } finally {
            rLock.unlock();
        }

    }*/


}