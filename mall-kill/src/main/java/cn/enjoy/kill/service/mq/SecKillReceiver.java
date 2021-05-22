package cn.enjoy.kill.service.mq;

import cn.enjoy.kill.service.impl.SequenceGenerator;
import cn.enjoy.mall.constant.KillConstants;
import cn.enjoy.mall.constant.OrderStatus;
import cn.enjoy.mall.constant.PayStatus;
import cn.enjoy.mall.constant.ShippingStatus;
import cn.enjoy.mall.model.Order;
import cn.enjoy.mall.model.OrderGoods;
import cn.enjoy.mall.model.UserAddress;
import cn.enjoy.mall.service.IKillOrderService;
import cn.enjoy.mall.service.IUserAddressService;
import cn.enjoy.mall.vo.KillOrderVo;
import com.alibaba.fastjson.JSON;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 类说明：
 */
@Slf4j
@Component
public class SecKillReceiver {

    @Resource
    private IKillOrderService orderService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private SecKillSender secKillSender;

    private static String STOCK_LUA_INCR = "";

    /**
     * 库存没有初始化，库存key在redis里面不存在
     */
    public static final long UNINITIALIZED_STOCK = -3L;

    @Autowired
    private RedisTemplate redisTemplate;

    static {
        /**
         *
         * @desc 扣减库存Lua脚本
         * 库存（stock）-1：表示不限库存
         * 库存（stock）0：表示没有库存
         * 库存（stock）大于0：表示剩余库存
         *
         * @params 库存key
         * @return
         * 		-3:库存未初始化
         * 		-2:库存不足
         * 		-1:不限库存
         * 		大于等于0:剩余库存（扣减之后剩余的库存）
         * 	    redis缓存的库存(value)是-1表示不限库存，直接返回1
         */
        StringBuilder sb2 = new StringBuilder();
        sb2.append("if (redis.call('exists', KEYS[1]) == 1) then");
        sb2.append("    local stock = tonumber(redis.call('get', KEYS[1]));");
        sb2.append("    local num = tonumber(ARGV[1]);");
        sb2.append("    if (stock >= 0) then");
        sb2.append("        return redis.call('incrby', KEYS[1], num);");
        sb2.append("    end;");
        sb2.append("    return -2;");
        sb2.append("end;");
        sb2.append("return -3;");
        STOCK_LUA_INCR = sb2.toString();
    }

    /**
     * 扣库存
     *
     * @param key 库存key
     * @param num 扣减库存数量
     * @return 扣减之后剩余的库存【-3:库存未初始化; -2:库存不足; -1:不限库存; 大于等于0:扣减库存之后的剩余库存】
     */
    public Long stock(String key, int num, String script) {
        // 脚本里的KEYS参数
        List<String> keys = new ArrayList<>();
        keys.add(key);
        // 脚本里的ARGV参数
        List<String> args = new ArrayList<>();
        args.add(Integer.toString(num));

        long result = (long) redisTemplate.execute(new RedisCallback<Long>() {
            @Override
            public Long doInRedis(RedisConnection connection) throws DataAccessException {
                Object nativeConnection = connection.getNativeConnection();
                // 集群模式和单机模式虽然执行脚本的方法一样，但是没有共同的接口，所以只能分开执行
                // 集群模式
                if (nativeConnection instanceof JedisCluster) {
                    return (Long) ((JedisCluster) nativeConnection).eval(script, keys, args);
                }

                // 单机模式
                else if (nativeConnection instanceof Jedis) {
                    return (Long) ((Jedis) nativeConnection).eval(script, keys, args);
                }
                /*else if (nativeConnection instanceof Redisson) {
                    Redisson redisson = (Redisson)nativeConnection;
                    return redisson.getScript().eval(RScript.Mode.READ_WRITE,STOCK_LUA,RScript.ReturnType.INTEGER, Collections.singletonList(keys), new List[]{args});
                }*/
                return UNINITIALIZED_STOCK;
            }
        });
        return result;
    }

    @RabbitListener(queues = "order.seckill.producer"/*,errorHandler = "rabbitConsumerListenerErrorHandler"*/)
    @RabbitHandler // 此注解加上之后可以接受对象型消息
    public void process(Message message, Channel channel, @Headers Map<String, Object> headers) throws Exception {
        try {
            String msg = new String(message.getBody());
            log.info("UserReceiver>>>>>>>接收到消息:" + msg);
            try {
                KillOrderVo vo = JSON.parseObject(msg, KillOrderVo.class);
                Long orderId = orderService.killOrder(vo);
                //把订单信息存储到缓存中
//                setOrderToRedis(vo);
                //发送消息到延迟队列
                secKillSender.send(vo);
                log.info("UserReceiver>>>>>>消息已消费");
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);//手工确认，可接下一条
            } catch (Exception e) {
                System.out.println(e.getMessage());
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);//失败，则直接忽略此订单
                log.info("UserReceiver>>>>>>拒绝消息，直接忽略");
                throw e;
            }
        } catch (Exception e) {
            log.info(e.getMessage());
        }
    }

    @RabbitListener(queues = "order.seckill.dead.queue"/*,errorHandler = "rabbitConsumerListenerErrorHandler"*/)
    @RabbitHandler // 此注解加上之后可以接受对象型消息
    public void processDead(Message message, Channel channel, @Headers Map<String, Object> headers) throws Exception {
        try {
            String msg = new String(message.getBody());
            log.info("order.seckill.dead.queue>>>>>>>consumer:" + msg);
            try {
                KillOrderVo vo = JSON.parseObject(msg, KillOrderVo.class);
                //1、校验订单是否已经支付，查询该订单的支付状态
                Order order = orderService.search(vo.getOrderId());
                //2、如果未支付就把订单取消，修改订单状态
                if(order.getPayStatus() != null && "0".equals(order.getPayStatus().toString())) {
                    orderService.selfCancel(vo.getOrderId(),vo.getUserId());
                    //3、把库存+1操作
                    String killGoodCount = KillConstants.KILL_GOOD_COUNT + vo.getKillGoodsSpecPriceDetailVo().getId();
                    //返回的数值,执行了lua脚本
                    Long stock = stock(killGoodCount, 1, STOCK_LUA_INCR);
                    if(stock > 0) {
                        log.info("---------增加库存成功---stock:" + stock);
                    }
                }
                log.info("UserReceiver>>>>>>消息已消费");
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);//手工确认，可接下一条
            } catch (Exception e) {
                log.info(e.getMessage());
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);//失败，则直接忽略此订单
                log.info("UserReceiver>>>>>>拒绝消息，直接忽略");
                throw e;
            }
        } catch (Exception e) {
            log.info(e.getMessage());
        }
    }

    //@RabbitListener(queues = "order.seckill.producer"/*,errorHandler = "rabbitConsumerListenerErrorHandler"*/)
    //@RabbitHandler // 此注解加上之后可以接受对象型消息
    public String processCallback(Message message, Channel channel, @Headers Map<String, Object> headers) throws Exception {
        try {
            String msg = new String(message.getBody());
            log.info("UserReceiver>>>>>>>接收到消息:" + msg);
            try {
                KillOrderVo vo = JSON.parseObject(msg, KillOrderVo.class);
//                String kill_order_user = KillConstants.KILL_ORDER_USER + vo.getKillGoodsSpecPriceDetailVo().getId() + vo.getUserId();
//                if (null != stringRedisTemplate.opsForValue().get(kill_order_user)){//未超时，则业务处理
                Long orderId = orderService.killOrder(vo);
//                    String oldstr = stringRedisTemplate.opsForValue().getAndSet(kill_order_user,String.valueOf(orderId));
//                    if (null == oldstr){//已超时，生产端已拒绝
//                        orderService.cancel(orderId);
//                        stringRedisTemplate.delete(kill_order_user);
//                    }
//                }
                log.info("UserReceiver>>>>>>消息已消费");
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);//手工确认，可接下一条
                return orderId.toString();
            } catch (Exception e) {
                System.out.println(e.getMessage());
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);//失败，则直接忽略此订单
                log.info("UserReceiver>>>>>>拒绝消息，直接忽略");
                throw e;
            }
        } catch (Exception e) {
            log.info(e.getMessage());
        }
        return "";
    }

    public void onMessage(Message message, Channel channel) throws Exception {
        try {
            String msg = new String(message.getBody());
            log.info("UserReceiver>>>>>>>接收到消息:"+msg);
            try {
                KillOrderVo vo = JSON.parseObject(msg, KillOrderVo.class);

                String kill_order_user = KillConstants.KILL_ORDER_USER+vo.getKillGoodsSpecPriceDetailVo().getId()+vo.getUserId();
                if (null != stringRedisTemplate.opsForValue().get(kill_order_user)){//未超时，则业务处理
                    Long orderId = orderService.killOrder(vo);
                    String oldstr = stringRedisTemplate.opsForValue().getAndSet(kill_order_user,String.valueOf(orderId));
                    if (null == oldstr){//已超时，生产端已拒绝
                        orderService.cancel(orderId);
                        stringRedisTemplate.delete(kill_order_user);
                    }
                }

                log.info("UserReceiver>>>>>>消息已消费");
                channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);//手工确认，可接下一条
            } catch (Exception e) {
                System.out.println(e.getMessage());
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false,false);//失败，则直接忽略此订单

                log.info("UserReceiver>>>>>>拒绝消息，直接忽略");
                throw e;
            }

        } catch (Exception e) {
            log.info(e.getMessage());
        }
    }

    @Resource
    private SequenceGenerator sequenceGenerator;

    @Autowired
    private IUserAddressService userAddressService;

    private void setOrderToRedis(KillOrderVo vo) {
        BigDecimal totalAmount = new BigDecimal(0);
        Order order = new Order();
        order.setOrderId(vo.getOrderId());
        order.setOrderSn(sequenceGenerator.getOrderNo());
        order.setAddTime(System.currentTimeMillis());
        //设置订单的状态为未确定订单
        order.setOrderStatus(OrderStatus.UNCONFIRMED.getCode());
        //未支付
        order.setPayStatus(PayStatus.UNPAID.getCode());
        //未发货
        order.setShippingStatus(ShippingStatus.UNSHIPPED.getCode());
        //获取发货地址
        Map map = new HashMap();
        map.put("addressId", vo.getAddressId());
        List<UserAddress> userAddresss = userAddressService.selectById(map);
        BeanUtils.copyProperties(userAddresss.get(0), order);
        order.setUserId(vo.getUserId());
        OrderGoods orderGoods = new OrderGoods();
        orderGoods.setGoodsName(vo.getKillGoodsSpecPriceDetailVo().getGoodsName());
        orderGoods.setGoodsPrice(vo.getKillGoodsSpecPriceDetailVo().getPrice());
        List<OrderGoods> list = new ArrayList<>();
        list.add(orderGoods);
        order.setOrderGoodsList(list);
        totalAmount = totalAmount.add(vo.getKillGoodsSpecPriceDetailVo().getPrice());
        order.setGoodsPrice(totalAmount);
        order.setShippingPrice(new BigDecimal(0));
        order.setOrderAmount(totalAmount.add(order.getShippingPrice()));
        order.setTotalAmount(totalAmount.add(order.getShippingPrice()));
        redisTemplate.opsForValue().set(vo.getOrderId() + "", order);
    }
}

