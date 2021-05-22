package cn.enjoy.mall.web.controller;

import cn.enjoy.core.utils.response.HttpResponseBody;
import cn.enjoy.mall.service.IKillOrderService;
import cn.enjoy.mall.vo.KillGoodsSpecPriceDetailVo;
import cn.enjoy.mall.web.service.KillGoodsService;
import cn.enjoy.sys.controller.BaseController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 秒杀服务接口
 */
@Slf4j
@RestController
@RequestMapping("/api/killgoodsSpec")
public class KillGoodsContoller extends BaseController {
    @Autowired
    private KillGoodsService killGoodsService;
    @Autowired
    private IKillOrderService iKillOrderService;

//    @Autowired
//    private RedissonClient redissonClient;
    /**
     * 查询秒杀商品
    * @author Jack
    * @date 2020/8/4
    * @throws Exception
    * @return
    * @version
    */
    @GetMapping("/queryByPage")
    public HttpResponseBody queryByPage(){
        return HttpResponseBody.successResponse("ok", killGoodsService.queryByPage());
    }

    /**
     * 根据id查询秒杀商品详细信息
    * @param id
    * @author Jack
    * @date 2020/8/4
    * @throws Exception
    * @return
    * @version
    */
    @GetMapping("/detail")
    public HttpResponseBody detail(Integer id) {
        return HttpResponseBody.successResponse("ok", killGoodsService.detail(id));
    }

    /**
     * 秒杀接口，这里对redis做了预减库存，并把秒杀了的用户做了标识缓存
    * @param killId
    * @author Jack
    * @date 2020/8/4
    * @throws Exception
    * @return
    * @version
    */
    @PostMapping("kill")
    public HttpResponseBody kill(int killId) {
        KillGoodsSpecPriceDetailVo killGoods = killGoodsService.detail(killId);
        if (killGoods.getBegainTime().getTime() > System.currentTimeMillis()){
            return HttpResponseBody.failResponse("抢购还未开始");
        }
        if (killGoods.getEndTime().getTime() < System.currentTimeMillis()){
            return HttpResponseBody.failResponse("抢购已结束");
        }
        if (!killGoodsService.secKillByLock(killId,getSessionUserId())){
            log.info("----抢购失败----");
            return HttpResponseBody.failResponse("抢购失败");
        }
        return HttpResponseBody.successResponse("ok",  killGoods);
    }

    @PostMapping("killOnlyIncr")
    public HttpResponseBody killOnlyIncr(int killId){
        KillGoodsSpecPriceDetailVo killGoods = killGoodsService.detail(killId);
        if (killGoods.getBegainTime().getTime() > System.currentTimeMillis()){
            return HttpResponseBody.failResponse("抢购还未开始");
        }
        if (killGoods.getEndTime().getTime() < System.currentTimeMillis()){
            return HttpResponseBody.failResponse("抢购已结束");
        }
        if (!killGoodsService.secKill(killId,getSessionUserId())){
            log.info("----抢购失败----");
            return HttpResponseBody.failResponse("抢购失败");
        }

        return HttpResponseBody.successResponse("ok",  killGoods);
    }

    @PostMapping("killByDb")
    public HttpResponseBody killByDb(int killId){
        KillGoodsSpecPriceDetailVo killGoods = killGoodsService.detail(killId);
        if (killGoods.getBegainTime().getTime() > System.currentTimeMillis()){
            return HttpResponseBody.failResponse("抢购还未开始");
        }
        if (killGoods.getEndTime().getTime() < System.currentTimeMillis()){
            return HttpResponseBody.failResponse("抢购已结束");
        }
        if (!killGoodsService.secKillByDb(killId,getSessionUserId())){
            log.info("----抢购失败----");
            return HttpResponseBody.failResponse("抢购失败");
        }
        return HttpResponseBody.successResponse("ok",  killGoods);
    }

    @PostMapping("killByQueue")
    public HttpResponseBody killByQueue(int killId){
        KillGoodsSpecPriceDetailVo killGoods = killGoodsService.detail(killId);
        if (killGoods.getBegainTime().getTime() > System.currentTimeMillis()){
            return HttpResponseBody.failResponse("抢购还未开始");
        }
        if (killGoods.getEndTime().getTime() < System.currentTimeMillis()){
            return HttpResponseBody.failResponse("抢购已结束");
        }
        if (!killGoodsService.secKillByQueue(killId,getSessionUserId())){
            log.info("----抢购失败----");
            return HttpResponseBody.failResponse("抢购失败");
        }

        return HttpResponseBody.successResponse("ok",  killGoods);
    }

/*    @PostMapping("killByQueueSSe")
    public SseEmitter killByQueueSSe(int killId){
        SseEmitter sseEmitter = new SseEmitter();
        KillGoodsSpecPriceDetailVo killGoods = killGoodsService.detail(killId);
        if (killGoods.getBegainTime().getTime() > System.currentTimeMillis()){
            log.info("抢购还未开始");
        }
        if (killGoods.getEndTime().getTime() < System.currentTimeMillis()){
            log.info("抢购已结束");
        }
        if (!killGoodsService.secKillByQueue(killId,getSessionUserId(),sseEmitter)){
            log.info("----抢购失败----");
        }

        return sseEmitter;
    }*/

    @PostMapping("secKillByZkLockNoLua")
    public HttpResponseBody secKillByZkLockNoLua(int killId){
        KillGoodsSpecPriceDetailVo killGoods = killGoodsService.detail(killId);
        if (killGoods.getBegainTime().getTime() > System.currentTimeMillis()){
            return HttpResponseBody.failResponse("抢购还未开始");
        }
        if (killGoods.getEndTime().getTime() < System.currentTimeMillis()){
            return HttpResponseBody.failResponse("抢购已结束");
        }
        if (!killGoodsService.secKillByZkLockNoLua(killId,getSessionUserId())){
            log.info("----抢购失败----");
            return HttpResponseBody.failResponse("抢购失败");
        }

        return HttpResponseBody.successResponse("ok",  killGoods);
    }

    @PostMapping("secKillByRedissonLockNoLua")
    public HttpResponseBody secKillByRedissonLockNoLua(int killId){
        KillGoodsSpecPriceDetailVo killGoods = killGoodsService.detail(killId);
        if (killGoods.getBegainTime().getTime() > System.currentTimeMillis()){
            return HttpResponseBody.failResponse("抢购还未开始");
        }
        if (killGoods.getEndTime().getTime() < System.currentTimeMillis()){
            return HttpResponseBody.failResponse("抢购已结束");
        }
        if (!killGoodsService.secKillByRedissonLockNoLua(killId,getSessionUserId())){
            log.info("----抢购失败----");
            return HttpResponseBody.failResponse("抢购失败");
        }

        return HttpResponseBody.successResponse("ok",  killGoods);
    }

    @PostMapping("secKillBySegStockNoLua")
    public HttpResponseBody secKillBySegStockNoLua(int killId){
        KillGoodsSpecPriceDetailVo killGoods = killGoodsService.detail(killId);
        if (killGoods.getBegainTime().getTime() > System.currentTimeMillis()){
            return HttpResponseBody.failResponse("抢购还未开始");
        }
        if (killGoods.getEndTime().getTime() < System.currentTimeMillis()){
            return HttpResponseBody.failResponse("抢购已结束");
        }
        if (!killGoodsService.secKillBySegmentLockNoLua(killId,getSessionUserId())){
            log.info("----抢购失败----");
            return HttpResponseBody.failResponse("抢购失败");
        }

        return HttpResponseBody.successResponse("ok",  killGoods);
    }

    @PostMapping("secKillByRedissonLock")
    public HttpResponseBody secKillByRedissonLock(int killId){
        KillGoodsSpecPriceDetailVo killGoods = killGoodsService.detail(killId);
        if (killGoods.getBegainTime().getTime() > System.currentTimeMillis()){
            return HttpResponseBody.failResponse("抢购还未开始");
        }
        if (killGoods.getEndTime().getTime() < System.currentTimeMillis()){
            return HttpResponseBody.failResponse("抢购已结束");
        }
        if (!killGoodsService.secKillByRedissonLock(killId,getSessionUserId())){
            log.info("----抢购失败----");
            return HttpResponseBody.failResponse("抢购失败");
        }

        return HttpResponseBody.successResponse("ok",  killGoods);
    }

    /**
     * 秒杀接口，直接从数据库减库存
     * @param killId
     * @author Jack
     * @date 2020/8/4
     * @throws Exception
     * @return
     * @version
     */
/*    @PostMapping("kill")
    public HttpResponseBody killByDb(int killId) {
        KillGoodsSpecPriceDetailVo killGoods = killGoodsService.detail(killId);
        if (killGoods.getBegainTime().getTime() > System.currentTimeMillis()){
            return HttpResponseBody.failResponse("抢购还未开始");
        }
        if (killGoods.getEndTime().getTime() < System.currentTimeMillis()){
            return HttpResponseBody.failResponse("抢购已结束");
        }
        if (!killGoodsService.secKillByDb(killId,getSessionUserId())){
            return HttpResponseBody.failResponse("抢购失败");
        }

        return HttpResponseBody.successResponse("ok",  killGoods);
    }*/

    /**
     * 秒杀订单提交，不直接修改数据库，而是把订单消息存mq，做异步处理
    * @param addressId
    * @param killId
    * @author Jack
    * @date 2020/8/4
    * @throws Exception
    * @return
    * @version
    */
    @PostMapping("submit")
    public HttpResponseBody submit(Long addressId, int killId){
//        if (!killGoodsService.chkKillOrder(String.valueOf(killId),getSessionUserId())){
//            return HttpResponseBody.failResponse("请先抢购");
//        }
//        KillGoodsSpecPriceDetailVo killGoods = killGoodsService.detail(killId);

        //创建秒杀订单
//        Integer orderId = orderService.killOrder(addressId,killGoods,getSessionUserId());
        //发送到消息队列
//        secKillSender.send(addressId,killGoods,getSessionUserId());

        String orderId = killGoodsService.submitOrder(addressId,killId,getSessionUserId());
        if (null ==orderId){
            return HttpResponseBody.failResponse("抢购失败");
        }
        return HttpResponseBody.successResponse("ok",orderId);
    }

    /**
     * 基于数据库的秒杀订单提交
    * @param addressId
     * @param killId
    * @author Jack
    * @date 2020/8/6
    * @throws Exception
    * @return
    * @version
    */
/*    @PostMapping("submit")
    public HttpResponseBody submitByDb(int addressId, int killId){
//        if (!killGoodsService.chkKillOrder(String.valueOf(killId),getSessionUserId())){
//            return HttpResponseBody.failResponse("请先抢购");
//        }
//        KillGoodsSpecPriceDetailVo killGoods = killGoodsService.detail(killId);

        //创建秒杀订单
//        Integer orderId = orderService.killOrder(addressId,killGoods,getSessionUserId());
        *//*发送到消息队列*//*
//        secKillSender.send(addressId,killGoods,getSessionUserId());

        String orderId = killGoodsService.submitOrderByDb(addressId,killId,getSessionUserId());
        if (null ==orderId){
            return HttpResponseBody.failResponse("抢购失败");
        }
        return HttpResponseBody.successResponse("ok",orderId);
    }*/

}
