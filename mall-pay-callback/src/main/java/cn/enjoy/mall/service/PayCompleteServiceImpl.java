package cn.enjoy.mall.service;

import cn.enjoy.dao.MessageLogCallbackMapper;
import cn.enjoy.mall.model.*;
import cn.enjoy.mall.service.manage.IKillSpecManageService;
import com.alibaba.fastjson.JSONObject;
import io.seata.core.context.RootContext;
import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.saga.engine.StateMachineEngine;
import io.seata.saga.statelang.domain.ExecutionStatus;
import io.seata.saga.statelang.domain.StateMachineInstance;
import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 支付完成后的业务处理
 *
 * @Classname PayCompleteServiceImpl
 * @Description TODO
 * @Author Jack
 * Date 2020/8/21 21:01
 * Version 1.0
 */
@Service
public class PayCompleteServiceImpl implements PayCompleteService {

    @Autowired
    private IKillOrderService killorderService;

    @Autowired
    private IOrderService orderService;

    @Autowired
    private IOrderActionService orderActionService;

    @Autowired
    private IKillOrderActionService killOrderActionService;

    @Autowired
    private IKillSpecManageService killSpecManageService;

    @Autowired
    private IGoodsService goodsService;

    @Autowired
    private IPayService iPayService;

    @Autowired
    private IKillPayService iKillPayService;

    @Autowired
    private MessageLogCallbackMapper messageLogCallbackMapper;

    /**
     * AT模式
     *
     * @param
     * @return
     * @throws Exception
     * @author Jack
     * @date 2020/10/22
     * @version
     */
    @GlobalTransactional
    @Override
    public void payCompleteBusiness(String actionId, MessageLog messageLog) {
        //1、根据订单id查询是什么订单
//        Order order = orderService.selectOrderDetail(Long.valueOf(orderId));
        OrderAction orderAction = orderActionService.queryByActionId(Long.valueOf(actionId));
        if (orderAction != null) {
            Order order = iPayService.updateStatusByActionId(actionId);
            List<SpecGoodsPrice> sgps = new ArrayList<>();
            for (OrderGoods orderGoods : order.getOrderGoodsList()) {
                SpecGoodsPrice specGoodsPrice = new SpecGoodsPrice();
                specGoodsPrice.setId(orderGoods.getSpecGoodsId());
                specGoodsPrice.setStoreCount(Integer.valueOf(orderGoods.getGoodsNum()));
                sgps.add(specGoodsPrice);
            }
            //扣减库存
            goodsService.updateBySpecGoodsIds(sgps);
        } else {
            Order order = iKillPayService.updateStatusByActionId(actionId);
            KillGoodsPrice killGoodsPrice = new KillGoodsPrice();
            killGoodsPrice.setSpecGoodsId(order.getOrderGoodsList().get(0).getSpecGoodsId());
            killGoodsPrice.setKillCount(1);
            //扣减库存
            killSpecManageService.updateBySpecGoodsId(killGoodsPrice);
        }
        //消费成功后，把记录存储到本地消息表
        messageLogCallbackMapper.insertC(messageLog);
    }

    /**
     * TCC模式
     *
     * @param
     * @return
     * @throws Exception
     * @author Jack
     * @date 2020/10/22
     * @version
     */
    @GlobalTransactional
    @Override
    public boolean payCompleteBusinessTcc(String actionId, MessageLog messageLog) {
        BusinessActionContext businessActionContext = new BusinessActionContext();
        businessActionContext.setXid(RootContext.getXID());
        Order order = iKillPayService.updateStatusByActionIdTcc(businessActionContext, actionId);
        KillGoodsPrice killGoodsPrice = new KillGoodsPrice();
        killGoodsPrice.setSpecGoodsId(order.getOrderGoodsList().get(0).getSpecGoodsId());
        killGoodsPrice.setKillCount(1);
        //扣减库存
        boolean spectryResult = killSpecManageService.updateBySpecGoodsIdTcc(businessActionContext, JSONObject.toJSONString(killGoodsPrice));
        messageLogCallbackMapper.insertC(messageLog);
        return true;
    }

    @Autowired
    private StateMachineEngine stateMachineEngine;

    @Override
    public boolean payCompleteBusinessSaga(String actionId, MessageLog messageLog) {
        Map<String, Object> startParams = new HashMap<>(3);
        String businessKey = String.valueOf(System.currentTimeMillis());
        startParams.put("businessKey", businessKey);
        startParams.put("actionId", actionId);
        startParams.put("specGoodsId", 416);
        startParams.put("messageLog",messageLog);
        startParams.put("mockReduceKillStorageFail", "true");
        startParams.put("mockAddMessageLogFail", "true");

        StateMachineInstance inst = stateMachineEngine.startWithBusinessKey("payStatusAndReduce", null, businessKey, startParams);

        Assert.isTrue(ExecutionStatus.SU.equals(inst.getStatus()), "saga transaction execute failed. XID: " + inst.getId());
        System.out.println("saga transaction commit succeed. XID: " + inst.getId());

//        inst = stateMachineEngine.getStateMachineConfig().getStateLogStore().getStateMachineInstanceByBusinessKey(businessKey, null);
//        Assert.isTrue(ExecutionStatus.SU.equals(inst.getStatus()), "saga transaction execute failed. XID: " + inst.getId());

        return true;
    }
}
