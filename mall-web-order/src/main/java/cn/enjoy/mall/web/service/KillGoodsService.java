package cn.enjoy.mall.web.service;

import cn.enjoy.core.utils.GridModel;
import cn.enjoy.mall.constant.KillConstants;
import cn.enjoy.mall.constant.OrderStatus;
import cn.enjoy.mall.constant.PayStatus;
import cn.enjoy.mall.constant.ShippingStatus;
import cn.enjoy.mall.lock.RedisLock;
import cn.enjoy.mall.model.KillGoodsPrice;
import cn.enjoy.mall.model.Order;
import cn.enjoy.mall.model.OrderGoods;
import cn.enjoy.mall.model.UserAddress;
import cn.enjoy.mall.service.IKillOrderService;
import cn.enjoy.mall.service.IUserAddressService;
import cn.enjoy.mall.service.manage.IKillSpecManageService;
import cn.enjoy.mall.vo.KillGoodsSpecPriceDetailVo;
import cn.enjoy.mall.vo.KillOrderVo;
import com.alibaba.fastjson.JSONObject;
import com.baidu.fsg.uid.impl.DefaultUidGenerator;
import lombok.extern.slf4j.Slf4j;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.redisson.RedissonRedLock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀商品相关
 */
@Slf4j
@Service
public class KillGoodsService {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private IKillSpecManageService iKillSpecManageService;
    @Resource
    private RedisTemplate redisTemplate;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private SecKillSender secKillSender;

    @Autowired
    private IKillOrderService orderService;

    @Autowired
    private IKillSpecManageService killSpecManageService;

    @Autowired
    private IUserAddressService userAddressService;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private KillQueueUtil killQueueUtil;

    /**
     * 库存没有初始化，库存key在redis里面不存在
     */
    public static final long UNINITIALIZED_STOCK = -3L;
    /**
     * 执行扣库存的脚本
     */
    public static final String STOCK_LUA;

    public static String STOCK_LUA_1 = "";

    public static String STOCK_LUA_INCR = "";

    @Autowired
    @Qualifier("redissonClient")
    private RedissonClient redissonClient;

    @Autowired
    @Qualifier("redissonClient1")
    private RedissonClient redissonClient1;

    @Autowired
    @Qualifier("redissonClient2")
    private RedissonClient redissonClient2;

    @Autowired
    @Qualifier("redissonClient3")
    private RedissonClient redissonClient3;

    private RLock lock;
    //setNX
    public static String REDIS_LOCK = "stock:lock";

    @Autowired
    private DefaultUidGenerator defaultUidGenerator;

    @PostConstruct
    public void initLock() {
        lock = redissonClient.getLock("store_lock_cn_order");
    }

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
        StringBuilder sb = new StringBuilder();
        sb.append("if (redis.call('exists', KEYS[1]) == 1) then");
        sb.append("    local stock = tonumber(redis.call('get', KEYS[1]));");
        sb.append("    local num = tonumber(ARGV[1]);");
        sb.append("    if (stock == -1) then");
        sb.append("        return -1;");
        sb.append("    end;");
        sb.append("    if (stock >= num) then");
        sb.append("        return redis.call('incrby', KEYS[1], 0 - num);");
        sb.append("    end;");
        sb.append("    return -2;");
        sb.append("end;");
        sb.append("return -3;");
        STOCK_LUA = sb.toString();

        StringBuilder sb1 = new StringBuilder();
        sb1.append("if (redis.call('exists', KEYS[1]) == 1) then");
        sb1.append("    local stock = tonumber(redis.call('get', KEYS[1]));");
        sb1.append("    local num = tonumber(ARGV[1]);");
        sb1.append("    if (stock >= num) then");
        sb1.append("        return redis.call('incrby', KEYS[1], 0 - num);");
        sb1.append("    end;");
        sb1.append("    return -2;");
        sb1.append("end;");
        sb1.append("return -3;");
        STOCK_LUA_1 = sb1.toString();

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

    public static void main(String[] args) {
        System.out.println(STOCK_LUA);
    }

    int i = 0;

    public int getI() {
        return i;
    }

    public void setI(int i) {
        this.i = i;
    }

    public void redissonIncr() throws InterruptedException {
        if (lock.tryLock(100, 10, TimeUnit.SECONDS)) {
            try {
                log.info("------i-------" + ++i);
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * 避免缓存雪崩情况出现
     *
     * @param
     * @return
     * @throws Exception
     * @author Jack
     * @date 2020/9/4
     * @version
     */
    public GridModel<KillGoodsSpecPriceDetailVo> queryByPage() {
        //1、先从缓存里面拿
        GridModel<KillGoodsSpecPriceDetailVo> gridModel = null;
        Object o = redisTemplate.opsForValue().get(KillConstants.KILLGOODS_LIST);
        if (null != o) {
            return JSONObject.parseObject(o.toString(), GridModel.class);
        }

        //所有线程在这里等待，避免大量请求怼到数据库，只有获取锁成功的线程允许去查询数据库
        synchronized (iKillSpecManageService) {
            //1、获取到锁后，先从缓存里面拿
            gridModel = (GridModel) redisTemplate.opsForValue().get(KillConstants.KILLGOODS_LIST);
            if (null != gridModel) {
                return gridModel;
            }

            //2、缓存里面没有，再去数据库拿
            gridModel = iKillSpecManageService.queryView(1, 100);

            //3、如果数据库里面能拿到就设置到缓存中
            if (null != gridModel) {
                redisTemplate.opsForValue().set(KillConstants.KILLGOODS_LIST, gridModel, 50000, TimeUnit.MILLISECONDS);//set缓存
            }
        }
        return gridModel;
    }

    /**
     * 查询秒杀商品的详情
     *
     * @param
     * @return
     * @throws Exception
     * @author Jack
     * @date 2020/9/18
     * @version 理论上只需要一个用户请求数据库能成功，缓存到redis，其他用户从redis里面读就可以了
     * <p>
     * 1000W  -- 20W
     * <p>
     * 1、本地缓存
     * redis是二级缓存
     */
    public KillGoodsSpecPriceDetailVo detail(Integer id) {
        KillGoodsSpecPriceDetailVo killGoodsPrice = null;
        //1、先从缓存里面查询数据
        String killGoodsDetail = KillConstants.KILLGOOD_DETAIL + id;

        //1、从本地缓存里面查询数据
        Cache killgoodsCache = cacheManager.getCache("killgoodDetail");
        if (null != killgoodsCache.get(killGoodsDetail)) {
            log.info(Thread.currentThread().getName() + "--------ehcache缓存里面的到数据-------");
            killGoodsPrice = (KillGoodsSpecPriceDetailVo) killgoodsCache.get(killGoodsDetail).getObjectValue();
            return killGoodsPrice;
        }

        //2、本地缓存里面没有从redis里面拿
        Object killGoodsPriceOb = redisTemplate.opsForValue().get(killGoodsDetail);
        if (null != killGoodsPriceOb) {
            log.info(Thread.currentThread().getName() + "---redis缓存中得到的数据---------");
            return JSONObject.parseObject(killGoodsPriceOb.toString(), KillGoodsSpecPriceDetailVo.class);
        }
        //程序要健壮一些
        synchronized (iKillSpecManageService) {
            //1、从本地缓存里面查询数据
            if (null != killgoodsCache.get(killGoodsDetail)) {
                log.info(Thread.currentThread().getName() + "--------ehcache缓存里面的到数据-------");
                killGoodsPrice = (KillGoodsSpecPriceDetailVo) killgoodsCache.get(killGoodsDetail).getObjectValue();
                return killGoodsPrice;

            }

            //1、从缓存里面拿
            killGoodsPriceOb = redisTemplate.opsForValue().get(killGoodsDetail);
            if (null != killGoodsPriceOb) {
                log.info(Thread.currentThread().getName() + "---redis缓存中得到的数据---------");
                return JSONObject.parseObject(killGoodsPriceOb.toString(), KillGoodsSpecPriceDetailVo.class);
            }
            //2、去数据库里面查询数据
            killGoodsPrice = iKillSpecManageService.detailById(id);
            if (null != killGoodsPrice) {
                killgoodsCache.putIfAbsent(new Element(killGoodsDetail, killGoodsPrice));
                redisTemplate.opsForValue().set(killGoodsDetail, killGoodsPrice, 2, TimeUnit.DAYS);
            } else {
                //防止缓存穿透  缓存时间一定要短 ，空数据没有必要占用redis内存
                redisTemplate.opsForValue().set(killGoodsDetail, "null", 5, TimeUnit.MINUTES);
            }
        }
        return killGoodsPrice;
    }

/*    public KillGoodsSpecPriceDetailVo detail(Integer id) {
        String killgoodDetail = KillConstants.KILLGOOD_DETAIL + id;
        KillGoodsSpecPriceDetailVo killGoodsPrice = (KillGoodsSpecPriceDetailVo) redisTemplate.opsForValue()
                .get(killgoodDetail);
        if (null != killGoodsPrice) {
            log.info(Thread.currentThread().getName() + "---------缓存中得到数据----------");
            return killGoodsPrice;
        }
        synchronized (iKillSpecManageService) {
            killGoodsPrice = (KillGoodsSpecPriceDetailVo) redisTemplate.opsForValue().get(killgoodDetail);
            if (null != killGoodsPrice) {
                log.info(Thread.currentThread().getName() + "---------缓存中得到数据----------");
                return killGoodsPrice;
            }

            killGoodsPrice = iKillSpecManageService.detailById(id);
            if (null != killGoodsPrice) {
                redisTemplate.opsForValue().set(killgoodDetail, killGoodsPrice, 50000, TimeUnit.MILLISECONDS);//set缓存
            }
        }
        return killGoodsPrice;
    }*/

    /**
     * @param killId
     * @param userId
     * @return boolean
     * @throws Exception
     * @author Jack
     * @date 2020/8/3
     * @version
     */
    public boolean kill(String killId, String userId) {

        final String killGoodCount = KillConstants.KILL_GOOD_COUNT + killId;
        try {
            long count = redisTemplate.opsForValue().increment(killGoodCount, -1);
            Object obj = redisTemplate.execute(new SessionCallback() {
                @Override
                public Object execute(RedisOperations operations) throws DataAccessException {

                    operations.watch(killGoodCount);
                    Object val = operations.opsForValue().get(killGoodCount);
                    int valint = Integer.valueOf(val.toString());

                    if (valint > 0) {
                        operations.multi();
                        operations.opsForValue().increment(killGoodCount, -1);
                        Object rs = operations.exec();
                        System.out.println(rs);
                        return rs;
                    }

                    return null;
                }
            });
            if (null != obj) {
                redisTemplate.opsForSet().add(KillConstants.KILLGOOD_USER, killId + userId);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public Map<String, SseEmitter> map = new ConcurrentHashMap<>();

/*    public boolean secKillByQueue(int killId, String userId, SseEmitter sseEmitter) {
        Boolean member = redisTemplate.opsForSet().isMember(KillConstants.KILLED_GOOD_USER + killId, userId);
        if (member) {
            logger.info("--------userId:" + userId + "--has secKilled");
            return false;
        }
        killQueueUtil.addQueue(new KUBean(killId, userId));
        map.put(userId, sseEmitter);
        return true;
    }*/

    /**
     * 所有请求不处理，单纯的丢到本地队列里面去
     *
     * @param
     * @return
     * @throws Exception
     * @author Jack
     * @date 2020/9/28
     * @version
     */
    public boolean secKillByQueue(int killId, String userId) {
        Boolean member = redisTemplate.opsForSet().isMember(KillConstants.KILLED_GOOD_USER + killId, userId);
        if (member) {
            logger.info("--------userId:" + userId + "--has secKilled");
            return false;
        }
        //这里把并行改为了串行
        killQueueUtil.addQueue(new KUBean(killId, userId));
        return true;
    }

    public boolean secKillByRedissonLock(int killId, String userId) {
        Boolean member = redisTemplate.opsForSet().isMember(KillConstants.KILLED_GOOD_USER + killId, userId);
        if (member) {
            logger.info("--------userId:" + userId + "--has secKilled");
            return false;
        }
        final String killGoodCount = KillConstants.KILL_GOOD_COUNT + killId;

        long stock = stock(killGoodCount, 1, STOCK_LUA);
        // 初始化库存
        if (stock == UNINITIALIZED_STOCK) {
            RLock lock = redissonClient.getLock("store_lock_cn_order");
            try {
                // 获取锁,支持过期解锁功能 2秒钟以后自动解锁
                lock.lock(2, TimeUnit.SECONDS);
                // 双重验证，避免并发时重复回源到数据库
                stock = stock(killGoodCount, 1, STOCK_LUA);
                if (stock == UNINITIALIZED_STOCK) {
                    // 获取初始化库存
                    KillGoodsPrice killGoodsPrice = iKillSpecManageService.selectByPrimaryKey(killId);
                    // 将库存设置到redis
                    redisTemplate.opsForValue().set(killGoodCount, killGoodsPrice.getKillCount().intValue(), 60 * 60, TimeUnit.SECONDS);
                    // 调一次扣库存的操作
                    stock = stock(killGoodCount, 1, STOCK_LUA);
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            } finally {
                lock.unlock();
            }

        }
        boolean flag = stock >= 0;
        if (flag) {
            //秒杀成功，缓存秒杀用户和商品
            redisTemplate.opsForSet().add(KillConstants.KILLGOOD_USER, killId + userId);
        }
        return flag;
    }

    /**
     * redis分段锁
     *
     * @param
     * @return
     * @throws Exception
     * @author Jack
     * @date 2020/9/21
     * @version
     */
    @Value("${enjoy.lock.seg}")
    private Integer segment;

    private ConcurrentHashMap<String, Boolean> segMap = null;

    private static String SEG_PREFIX = "seg_";

    @PostConstruct
    public void initLockSegMap() {
        segMap = new ConcurrentHashMap<>(segment);
        for (int i = 1; i <= segment; i++) {
            segMap.put(SEG_PREFIX + i, true);
        }
    }

    /**
     * 获取有库存的分库
     *
     * @param
     * @return
     * @throws Exception
     * @author Jack
     * @date 2020/9/22
     * @version
     */
    private String getStockSegment() {
        List<String> stockFlag = new ArrayList();
        for (Map.Entry<String, Boolean> entry : segMap.entrySet()) {
            if (entry.getValue()) {
                stockFlag.add(entry.getKey());
            }
        }
        return stockFlag.get(new Random().nextInt(stockFlag.size()));
    }

    private boolean isStockEmpty() {
        for (Map.Entry<String, Boolean> entry : segMap.entrySet()) {
            if (entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    public boolean secKillBySegmentLockNoLua(int killId, String userId) {
        //如果全部分库库存为空，则库存不足
        if (isStockEmpty()) {
            logger.info("all--------stock not enough---------");
            return false;
        }
        Boolean member = redisTemplate.opsForSet().isMember(KillConstants.KILLED_GOOD_USER + killId, userId);
        if (member) {
            logger.info("--------userId:" + userId + "--has secKilled");
            return false;
        }
        String seg = getStockSegment();
        log.info("-------choice seg is---" + seg);
        //KILL_COUNT_24_seg_1 KILL_COUNT_24_seg_2 KILL_COUNT_24_seg_3 KILL_COUNT_24_seg_4
        final String killGoodCount = KillConstants.KILL_GOOD_COUNT + killId + "_" + seg;
        RLock lock = redissonClient.getLock("store_lock_cn_order_" + seg);
/*        if (!redisTemplate.hasKey(killGoodCount)) {
            // 获取锁,支持过期解锁功能 2秒钟以后自动解锁
            lock.lock(2, TimeUnit.SECONDS);
            try {
                // 获取初始化库存
                KillGoodsPrice killGoodsPrice = iKillSpecManageService.selectByPrimaryKey(killId);
                // 将库存设置到redis
                redisTemplate.opsForValue().set(killGoodCount, killGoodsPrice.getKillCount().intValue(), 60 * 60, TimeUnit.SECONDS);
            } finally {
                lock.unlock();
            }
        }*/
        lock.lock(2, TimeUnit.SECONDS);
        try {
            //1、先查询库存
            Integer stock = (Integer) redisTemplate.opsForValue().get(killGoodCount);
            //这里如果分段库存没有了，可能其他分段库存还有
            if (stock <= 0) {
                //把这个库存标志设置为false，表示没有库存，但是其他分库可能有库存。
                segMap.put(seg, false);
                logger.info(seg + "--------stock not enough---------");
                return secKillBySegmentLockNoLua(killId, userId);
            }
            //2、减库存
            if (redisTemplate.opsForValue().increment(killGoodCount, -1) >= 0) {
                redisTemplate.opsForSet().add(KillConstants.KILLGOOD_USER, killId + userId);
                return true;
            }
        } finally {
            lock.unlock();
        }
        return false;
    }

    @Autowired
    private CuratorFramework client;

    public boolean secKillByZkLockNoLua(int killId, String userId) {
        Boolean member = redisTemplate.opsForSet().isMember(KillConstants.KILLED_GOOD_USER + killId, userId);
        if (member) {
            logger.info("--------userId:"
                    + userId + "--has secKilled");
            return false;
        }
        final String killGoodCount = KillConstants.KILL_GOOD_COUNT + killId;
        if (!redisTemplate.hasKey(killGoodCount)) {
            // 获取锁,支持过期解锁功能 2秒钟以后自动解锁
            lock.lock(2, TimeUnit.SECONDS);
            try {
                // 获取初始化库存
                KillGoodsPrice killGoodsPrice = iKillSpecManageService.selectByPrimaryKey(killId);
                // 将库存设置到redis
                redisTemplate.opsForValue().set(killGoodCount, killGoodsPrice.getKillCount().intValue(), 60 * 60, TimeUnit.SECONDS);
            } finally {
                lock.unlock();
            }
        }
        InterProcessMutex lock = new InterProcessMutex(client, "/curator/lock");
        try {
            lock.acquire();
            //1、先查询库存
            Integer stock = (Integer) redisTemplate.opsForValue().get(killGoodCount);
            if (stock <= 0) {
                logger.info("--------stock not enough---------");
                return false;
            }
            //2、减库存
            if (redisTemplate.opsForValue().increment(killGoodCount, -1) >= 0) {
                redisTemplate.opsForSet().add(KillConstants.KILLGOOD_USER, killId + userId);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (lock.isAcquiredInThisProcess()) {
                try {
                    lock.release();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    public boolean secKillByRedissonLockNoLua(int killId, String userId) {
        Boolean member = redisTemplate.opsForSet().isMember(KillConstants.KILLED_GOOD_USER + killId, userId);
        if (member) {
            logger.info("--------userId:" + userId + "--has secKilled");
            return false;
        }
        final String killGoodCount = KillConstants.KILL_GOOD_COUNT + killId;
        RLock lock = redissonClient.getLock("store_lock_cn_order");
        if (!redisTemplate.hasKey(killGoodCount)) {
            // 获取锁,支持过期解锁功能 2秒钟以后自动解锁
            lock.lock(2, TimeUnit.SECONDS);
            try {
                // 获取初始化库存
                KillGoodsPrice killGoodsPrice = iKillSpecManageService.selectByPrimaryKey(killId);
                // 将库存设置到redis
                redisTemplate.opsForValue().set(killGoodCount, killGoodsPrice.getKillCount().intValue(), 60 * 60, TimeUnit.SECONDS);
            } finally {
                lock.unlock();
            }
        }
        lock.lock(2, TimeUnit.SECONDS);
        try {
            //1、先查询库存
            Integer stock = (Integer) redisTemplate.opsForValue().get(killGoodCount);
            if (stock <= 0) {
                logger.info("--------stock not enough---------");
                return false;
            }
            //2、减库存
            if (redisTemplate.opsForValue().increment(killGoodCount, -1) >= 0) {
                redisTemplate.opsForSet().add(KillConstants.KILLGOOD_USER, killId + userId);
                return true;
            }
        } finally {
            lock.unlock();
        }
        return false;
    }

    public boolean secKillByRedLock(int killId, String userId) {
        Boolean member = redisTemplate.opsForSet().isMember(KillConstants.KILLED_GOOD_USER + killId, userId);
        if (member) {
            logger.info("--------userId:" + userId + "--has secKilled");
            return false;
        }
        final String killGoodCount = KillConstants.KILL_GOOD_COUNT + killId;

        long stock = stock(killGoodCount, 1, STOCK_LUA);
        // 初始化库存
        if (stock == UNINITIALIZED_STOCK) {
            String resourceName = "STORE_REDLOCK_KEY";
            RLock lock1 = redissonClient1.getLock(resourceName);
            RLock lock2 = redissonClient2.getLock(resourceName);
            RLock lock3 = redissonClient3.getLock(resourceName);

            RedissonRedLock redLock = new RedissonRedLock(lock1, lock2, lock3);
            try {
                // 获取锁,支持过期解锁功能 2秒钟以后自动解锁
                redLock.lock(2, TimeUnit.SECONDS);
                // 双重验证，避免并发时重复回源到数据库
                stock = stock(killGoodCount, 1, STOCK_LUA);
                if (stock == UNINITIALIZED_STOCK) {
                    // 获取初始化库存
                    KillGoodsPrice killGoodsPrice = iKillSpecManageService.selectByPrimaryKey(killId);
                    // 将库存设置到redis
                    redisTemplate.opsForValue().set(killGoodCount, killGoodsPrice.getKillCount().intValue(), 60 * 60, TimeUnit.SECONDS);
                    // 调一次扣库存的操作
                    stock = stock(killGoodCount, 1, STOCK_LUA);
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            } finally {
                redLock.unlock();
            }

        }
        boolean flag = stock >= 0;
        if (flag) {
            //秒杀成功，缓存秒杀用户和商品
            redisTemplate.opsForSet().add(KillConstants.KILLGOOD_USER, killId + userId);
        }
        return flag;
    }

    /*    public boolean secKillByLock(int killId, String userId) {
            Boolean member = redisTemplate.opsForSet().isMember(KillConstants.KILLED_GOOD_USER + killId, userId);
            if (member) {
                logger.info("--------userId:" + userId + "--has secKilled");
                return false;
            }
            final String killGoodCount = KillConstants.KILL_GOOD_COUNT + killId;

            long stock = stock(killGoodCount, 1,STOCK_LUA);
            // 初始化库存
            if (stock == UNINITIALIZED_STOCK) {
                RedisLock redisLock = new RedisLock(redisTemplate, REDIS_LOCK);
                Timer timer = null;
                try {
                    // 获取锁
                    if (redisLock.tryLock()) {
                        //锁续命
                        timer = continueLock(REDIS_LOCK);
                        // 双重验证，避免并发时重复回源到数据库
                        stock = stock(killGoodCount, 1,STOCK_LUA);
                        if (stock == UNINITIALIZED_STOCK) {
                            // 获取初始化库存
                            KillGoodsPrice killGoodsPrice = iKillSpecManageService.selectByPrimaryKey(killId);
                            // 将库存设置到redis
                            redisTemplate.opsForValue().set(killGoodCount, killGoodsPrice.getKillCount().intValue(), 60 * 60, TimeUnit.SECONDS);
                            // 调一次扣库存的操作
                            stock = stock(killGoodCount, 1,STOCK_LUA);
                        }
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                } finally {
                    if (timer != null) {
                        timer.cancel();
                    }
                    redisLock.unlock();
                }

            }
            boolean flag = stock >= 0;
            if (flag) {
                //秒杀成功，缓存秒杀用户和商品
                redisTemplate.opsForSet().add(KillConstants.KILLGOOD_USER, killId + userId);
            }
            return flag;
        }*/
    public boolean secKillByLock(int killId, String userId) {
        //判断用户是否已经秒杀过
//        Boolean member = redisTemplate.opsForSet().isMember(KillConstants.KILLED_GOOD_USER + killId, userId);
//        if (member) {
//            logger.info("---------userId:" + userId + "----has secKilled");
//            return false;
//        }
        String killGoodCount = KillConstants.KILL_GOOD_COUNT + killId;
        //返回的数值,执行了lua脚本
        Long stock = stock(killGoodCount, 1, STOCK_LUA);
        if (stock == UNINITIALIZED_STOCK) {
            Timer timer = null;
            RedisLock redisLock = new RedisLock(redisTemplate, REDIS_LOCK);
            try {
                //如果竞争锁成功  如果其他线程没竞争锁成功，这里是阻塞的
                if (redisLock.tryLock()) {
                    //锁续命
                    timer = continueLock(REDIS_LOCK);

                    stock = stock(killGoodCount, 1, STOCK_LUA);
                    if (stock == UNINITIALIZED_STOCK) {
                        KillGoodsPrice killGoodsPrice = iKillSpecManageService.selectByPrimaryKey(killId);
                        redisTemplate.opsForValue().set(killGoodCount, killGoodsPrice.getKillCount(), 60 * 60, TimeUnit.SECONDS);
                        //再次去执行lua脚本，扣减库存
                        stock = stock(killGoodCount, 1, STOCK_LUA);
                    }
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            } finally {
                if (timer != null) {
                    timer.cancel();
                }
                //释放锁 。自己加的锁不能让别人释放，自己只能释放自己的锁
                //这里要进行一个value值的比较，只要自己的value值相等才能释放
                redisLock.unlock();
            }
        }
        //如果是这种情况，秒杀成功
        boolean flag = stock >= 0;
//        if (flag) {
//            redisTemplate.opsForSet().add(KillConstants.KILLED_GOOD_USER + killId, userId);
//        }
        return flag;
    }

    private Timer continueLock(String lockKey) {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
//                redisTemplate.opsForValue().set(lockKey, "", 60, TimeUnit.SECONDS);
                redisTemplate.expire(lockKey, 60, TimeUnit.SECONDS);
            }
        }, 0, 1);
        return timer;
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

    /**
     * @param killId
     * @param userId
     * @return boolean
     * @throws Exception
     * @author Jack
     * @date 2020/8/4
     * @version
     */
    public boolean secKill(int killId, String userId) {
        //判断用户是否已经秒杀过
        Boolean member = redisTemplate.opsForSet().isMember(KillConstants.KILLED_GOOD_USER + killId, userId);
        if (member) {
            logger.info("---------userId:" + userId + "----has secKilled");
            return false;
        }

        String killGoodCount = KillConstants.KILL_GOOD_COUNT + killId;
        //1000   只有一个1
        //库存没有办法进行补偿
        if (redisTemplate.opsForValue().increment(killGoodCount, -1) < 0) {
            logger.info("---------stock 余量不足--------");
            return false;
        }
        //把用户设置到redis，代表已经秒杀过
        redisTemplate.opsForSet().add(KillConstants.KILLED_GOOD_USER + killId, userId);
        return true;
    }

    /**
     * 基于数据库的秒杀实现
     *
     * @param
     * @return
     * @throws Exception
     * @author Jack
     * @date 2020/8/5
     * @version
     */
    public boolean secKillByDb(int killId, String userId) {

        //1、先判断有没有库存，没有库存就直接秒杀结束
        KillGoodsPrice kgp = killSpecManageService.selectByPrimaryKey(killId);
        if (kgp.getKillCount() <= 0) {
            logger.info("--------Insufficient stock:------------");
            return false;
        }

        //2、先判断该用户是否已经秒杀
        Integer count = orderService.queryCountByUserId(userId);
        if (/*orders != null && orders.size() > 0*/false) {
            logger.info("--------userId:" + userId + "--has secKilled");
            return false;
        }

        KillGoodsPrice killGoodsPrice = new KillGoodsPrice();
        killGoodsPrice.setKillCount(1);
        killGoodsPrice.setId(killId);
        int i = killSpecManageService.updateSecKill(killGoodsPrice);

        //返回为0，秒杀完了
        if (i == 0) {
            logger.info("--------Insufficient stock:------------");
            return false;
        }

        //秒杀成功，缓存秒杀用户和商品
        redisTemplate.opsForSet().add(KillConstants.KILLED_GOOD_USER + killId, userId);
        return true;
    }

    public boolean chkKillOrder(String killId, String userId) {
        //校验用户和商品是否有缓存，无则表明当前是非法请求
        boolean isKilld = redisTemplate.opsForSet().isMember(KillConstants.KILLED_GOOD_USER + killId, userId);
        if (isKilld) {
            redisTemplate.opsForSet().remove(KillConstants.KILLED_GOOD_USER + killId, userId);
        }
        return isKilld;
    }

    //version 1
    public String submitOrder1(Long addressId, int killId, String userId) {
        KillGoodsSpecPriceDetailVo killGoods = detail(killId);

        KillOrderVo vo = new KillOrderVo();
        vo.setUserId(userId);
        vo.setKillGoodsSpecPriceDetailVo(killGoods);
        vo.setAddressId(addressId);

        ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();

        //订单有效时间3秒
        String kill_order_user = KillConstants.KILL_ORDER_USER + killId + userId;
        valueOperations.set(kill_order_user, KillConstants.KILL_ORDER_USER_UNDO, 3000, TimeUnit.MILLISECONDS);
        //同步转异步，发送到消息队列
        secKillSender.send(vo);

        String orderId = "";
        try {
            while (true) {
                orderId = valueOperations.get(kill_order_user);
                if (null == orderId) {//处理超时，则直接置秒杀失败，取消秒杀订单
                    return null;
                }
                if (!KillConstants.KILL_ORDER_USER_UNDO.equals(orderId)) {//订单已处理成功
                    stringRedisTemplate.delete(kill_order_user);
                    return orderId.toString();//
                }
                Thread.sleep(300l);//300ms轮循1次
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return null;
    }

    //version 2
    public String submitOrder2(Long addressId, int killId, String userId) {
        KillGoodsSpecPriceDetailVo killGoods = detail(killId);

        KillOrderVo vo = new KillOrderVo();
        vo.setUserId(userId);
        vo.setKillGoodsSpecPriceDetailVo(killGoods);
        vo.setAddressId(addressId);

//        ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();

        //订单有效时间3秒
//        String kill_order_user = KillConstants.KILL_ORDER_USER + killId + userId;
//        valueOperations.set(kill_order_user, KillConstants.KILL_ORDER_USER_UNDO, 3000, TimeUnit.MILLISECONDS);
        //同步转异步，发送到消息队列
        try {
            String orderId = secKillSender.sendAndReceive(vo);
            return orderId;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Resource
    private SequenceGenerator sequenceGenerator;

    public String submitOrder(Long addressId, int killId, String userId) {
        KillGoodsSpecPriceDetailVo killGoods = detail(killId);

        KillOrderVo vo = new KillOrderVo();
        vo.setUserId(userId);
        vo.setKillGoodsSpecPriceDetailVo(killGoods);
        vo.setAddressId(addressId);
        Long orderId = defaultUidGenerator.getUID();
        vo.setOrderId(orderId);
        try {
            CorrelationData correlationData = new CorrelationData(UUID.randomUUID().toString());
            secKillSender.send(vo, correlationData);
            //消息发送以后order存redis，给前端查询用户订单信息，因为这时候消息可能还没消费，
            //前端需要快速查询到订单信息
            setOrderToRedis(vo, orderId, userId);
            return orderId.toString();
        } catch (Exception e) {
            //如果异常了，库存要+1，并且秒杀失败
            e.printStackTrace();
            String killGoodCount = KillConstants.KILL_GOOD_COUNT + killId;
            //返回的数值,执行了lua脚本
            Long stock = stock(killGoodCount, 1, STOCK_LUA_INCR);
            if (stock > 0) {
                log.info("---------增加库存成功---stock:" + stock);
            }
        }

        return null;
    }

    private void setOrderToRedis(KillOrderVo vo, Long orderId, String userId) {
        BigDecimal totalAmount = new BigDecimal(0);
        Order order = new Order();
        order.setOrderId(orderId);
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
        order.setUserId(userId);
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
        redisTemplate.opsForValue().set(orderId + "", order);
    }

    public String submitOrderByDb(Long addressId, int killId, String userId) {
        KillGoodsSpecPriceDetailVo killGoods = detail(killId);

        KillOrderVo vo = new KillOrderVo();
        vo.setUserId(userId);
        vo.setKillGoodsSpecPriceDetailVo(killGoods);
        vo.setAddressId(addressId);

        ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();

        //订单有效时间3秒
        String kill_order_user = KillConstants.KILL_ORDER_USER + killId + userId;
        valueOperations.set(kill_order_user, KillConstants.KILL_ORDER_USER_UNDO, 3000, TimeUnit.MILLISECONDS);
        /*同步转异步，发送到消息队列*/
//        secKillSender.send(vo);
        Long orderId = orderService.killOrder(vo);

        String flag = valueOperations.get(kill_order_user);
        if (null == flag) {//处理超时，则直接置秒杀失败，取消秒杀订单
            orderService.cancel(orderId);
            stringRedisTemplate.delete(kill_order_user);
            return null;
        }
        return orderId + "";
    }
}
