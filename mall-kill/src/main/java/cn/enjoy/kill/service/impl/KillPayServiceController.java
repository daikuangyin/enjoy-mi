package cn.enjoy.kill.service.impl;

import cn.enjoy.kill.dao.OrderActionMapper;
import cn.enjoy.kill.dao.OrderMapper;
import cn.enjoy.kill.dao.TccTransactionStatusMapper;
import cn.enjoy.mall.constant.PayStatus;
import cn.enjoy.mall.constant.PayType;
import cn.enjoy.mall.model.Order;
import cn.enjoy.mall.model.OrderAction;
import cn.enjoy.mall.model.TccTransactionStatus;
import cn.enjoy.mall.service.IKillOrderActionService;
import cn.enjoy.mall.service.IKillPayService;
import cn.enjoy.mall.service.IWxPayService;
import com.alibaba.fastjson.JSONObject;
import io.seata.rm.tcc.api.BusinessActionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * 秒杀商品支付管理
 *
 * @author Jack
 * @date 2020/9/8
 */
@Slf4j
@RestController
//@RequestMapping("/kill/order/service/IPayService")
public class KillPayServiceController implements IKillPayService {

    @Resource
    private OrderMapper orderMapper;
    @Resource
    private OrderActionMapper orderActionMapper;
    @Resource
    private IKillOrderActionService orderActionService;
    @Autowired
    private IWxPayService iWxPayService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TccTransactionStatusMapper tccTransactionStatusMapper;

    /**
     * 秒杀商品预支付
     *
     * @param orderId
     * @param payCode
     * @param payAmount
     * @param userId
     * @return
     * @throws Exception
     * @author Jack
     * @date 2020/9/8
     * @version
     */
    //@RequestMapping(value = "/doPrePay", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    @Override
    public Map<String, String> doPrePay(Long orderId, String payCode, BigDecimal payAmount, String userId) {
        Order order = orderMapper.selectByPrimaryKey(orderId);
        Map<String, String> return_map = new HashMap<>();
        if (payAmount.compareTo(order.getOrderAmount()) != 0) {
            return_map.put("result_code", "fail");
            return_map.put("return_msg", "支付金额不正确");
            return return_map;
        }
        String payName = PayType.getDescByCode(payCode);
        if (StringUtils.isEmpty(payName)) {
            return_map.put("result_code", "fail");
            return_map.put("return_msg", "支付方式不存在");
            return return_map;
        }
        if (order.getPayStatus() == 1) {
            return_map.put("result_code", "fail");
            return_map.put("return_msg", "此订单已经付款完成！");
            return return_map;
        }
        order.setPayStatus(PayStatus.UNPAID.getCode());
        order.setPayCode(payCode);
        order.setPayName(PayType.getDescByCode(payCode));
        order.setPayTime(System.currentTimeMillis());
        orderMapper.updateByPrimaryKeySelective(order);
        String orderStr = JSONObject.toJSONString(order);
        Long action_id = orderActionService.savePre(orderStr, null, "微信-预支付订单", userId, "微信-预支付订单");
        Map<String, String> map = iWxPayService.unifiedorder(String.valueOf(action_id), payAmount, userId, "K");
        orderActionService.updatePre(action_id, map);
        return map;
    }

    /**
     * 修改订单日志
     *
     * @param actionId
     * @return
     * @throws Exception
     * @author Jack
     * @date 2020/9/8
     * @version
     */
    //@RequestMapping(value = "/updateByActionId", method = RequestMethod.POST)
    @Transactional
    @Override
    public String updateByActionId(String actionId) {
        OrderAction orderAction = orderActionMapper.selectByPrimaryKey(Long.parseLong(actionId));
        Order order = orderMapper.selectByPrimaryKey(orderAction.getOrderId());
        order.setOrderId(order.getOrderId());
        order.setPayStatus(PayStatus.PAID.getCode());
        order.setPayTime(System.currentTimeMillis());
        orderMapper.updateByPrimaryKeySelective(order);

        orderAction.setPayStatus(1);
        orderAction.setLogTime(System.currentTimeMillis());
        orderActionMapper.updateByPrimaryKey(orderAction);
        return "SUCCESS";
    }

    /**
     * 修改订单和订单日志
     *
     * @param actionId
     * @return
     * @throws Exception
     * @author Jack
     * @date 2020/9/8
     * @version
     */
    //@RequestMapping(value = "/updateStatusByActionId", method = RequestMethod.POST)
    @Override
    public Order updateStatusByActionId(String actionId) {
        OrderAction orderAction = orderActionMapper.selectByPrimaryKey(Long.parseLong(actionId));
        Order order = orderMapper.selectByPrimaryKey(orderAction.getOrderId());
        String updateOrderSql = "update tp_order_kill set pay_status = ?,pay_time = ? where order_id = ?";
        jdbcTemplate.update(updateOrderSql, new PreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps) throws SQLException {
                ps.setInt(1, PayStatus.PAID.getCode());
                ps.setLong(2, System.currentTimeMillis());
                ps.setLong(3, order.getOrderId());
            }
        });

        String orderActionSql = "update tp_order_action_kill set pay_status = ?,log_time = ? where action_id = ?";
        jdbcTemplate.update(orderActionSql, new PreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps) throws SQLException {
                ps.setInt(1, PayStatus.PAID.getCode());
                ps.setLong(2, System.currentTimeMillis());
                ps.setLong(3, orderAction.getActionId());
            }
        });
        if (false) throw new RuntimeException("异常测试");
        return order;
    }

    @Override
    public boolean updateStatusByActionIdSaga(String businessKey, String actionId) {
        return true;
    }

    @Override
    public boolean compensateOrderStatusSaga(String businessKey, String actionId) {
        return true;
    }

    /**
     * try阶段，不做业务表操作，只是把操作暂时记录在预操作表中，或者冻结表中
     *
     * @param
     * @return
     * @throws Exception
     * @author Jack
     * @date 2020/10/20
     * @version
     */
    @Transactional
    @Override
    public Order updateStatusByActionIdTcc(BusinessActionContext businessActionContext, String actionId) {
        log.info("----------try------updateStatusByActionIdTcc-------");

        //1、避免悬挂，网络拥堵导致cancel先执行，然后再执行try方法，会导致预留资源没办法再处理的情况
        //查询事务状态表，如果存在就说明二阶段已经执行过，就不执行try了
        TccTransactionStatus status = new TccTransactionStatus();
        status.setTxId(businessActionContext.getXid());
        status.setBranchId(businessActionContext.getBranchId() + "");
        //如果存在就不再执行
        if (isExistStatus(status)) {
            return new Order();
        }

        //2、如果没有发生悬挂，则插入事务状态表
        insertStatus(status);

        OrderAction orderAction = orderActionMapper.selectByPrimaryKey(Long.parseLong(actionId));
        Order order = orderMapper.selectByPrimaryKey(orderAction.getOrderId());
        order.setPayStatus(PayStatus.PAID.getCode());
        order.setPayTime(System.currentTimeMillis());
        //预制表插入数据
        orderMapper.insertSelectiveTcc(order);
        //第一阶段成功，添加成功标识
        //这个标志的作用就是限制如果第一阶段不成功，那么就不允许操作第二阶段
        //反正网络延迟导致，第二阶段比第一阶段先执行的情况
//        TccTryFlagUtil.setFlag(businessActionContext.getXid(),"T");
        return order;
    }

    @Transactional
    @Override
    public boolean updateStatusByActionIdTccCommit(BusinessActionContext businessActionContext) {
        log.info("----------commit------updateStatusByActionIdTccCommit-------");
        //判断第一阶段的成功标记，没有标记则不执行提交操作
//        if (TccTryFlagUtil.getFlag(businessActionContext.getXid()) == null){
//            return true;
//        }
        TccTransactionStatus status = new TccTransactionStatus();
        status.setTxId(businessActionContext.getXid());
        status.setBranchId(businessActionContext.getBranchId() + "");
        //如果不存在，则不允许提交
        if (!isExistStatus(status)) {
            return true;
        }

        //幂等控制，判断状态是否已经支付了
        OrderAction orderAction = orderActionMapper.selectByPrimaryKey(Long.parseLong(businessActionContext.getActionContext("actionId").toString()));
        //修改一定要有幂等控制，如果修改过则不修改了
        //如果状态是已经支付，则不需要再次修改了，可能是因为超时导致重复调用commit
        if (PayStatus.PAID.getCode() == orderAction.getPayStatus()) {
            return true;
        }

        //从预留表中查询状态
        Order order = orderMapper.selectByPrimaryKeyTcc(orderAction.getOrderId());
        orderAction.setLogTime(order.getPayTime());
        orderAction.setPayStatus(order.getPayStatus());
        orderActionMapper.updateByPrimaryKeySelective(orderAction);
        orderMapper.updateByPrimaryKeySelective(order);
        orderMapper.deleteByPrimaryKeyTcc(order.getOrderId());
//        //设置提交成功标识 防止一直重复提交
//        TccTryFlagUtil.removeFlag(businessActionContext.getXid());
        //修改事务状态表状态
        updateStatus(status,1);
        return true;
    }

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Transactional
    @Override
    public boolean updateStatusByActionIdTccRollback(BusinessActionContext businessActionContext) {
        log.info("----------Rollback------updateStatusByActionIdTccRollback-------");
        //判断第一阶段的成功标记，没有标记则不执行提交操作
//        if (TccTryFlagUtil.getFlag(businessActionContext.getXid()) == null){
//            return true;
//        }
        TccTransactionStatus status = new TccTransactionStatus();
        status.setTxId(businessActionContext.getXid());
        status.setBranchId(businessActionContext.getBranchId() + "");
        //如果不存在，则是空回滚，这时候需要防止悬挂现象，这里需要往事务状态表插入一条记录
        if (!isExistStatus(status)) {
            try {
                //如果这里能插入成功，则说明try还没有执行
                insertStatus(status);
            } catch (Exception e) {
                //如果插入失败，则说明try已经在这时候执行成功了，则这里抛出异常，由TC再次调用cancel
                e.printStackTrace();
                throw e;
            }
            return false;
        }
        //如果存在记录，并且记录是回滚状态，则不让再次回滚，幂等设计
        if (status.getStatus() == 2) {
            return true;
        }

//        OrderAction orderAction = orderActionMapper.selectByPrimaryKey(Long.parseLong(businessActionContext.getActionContext("actionId").toString()));
//        Order order = new Order();
//        order.setOrderId(orderAction.getOrderId());
//        order.setPayStatus(PayStatus.UNPAID.getCode());
//        orderMapper.updateByPrimaryKeySelective(order);
//        orderAction.setPayStatus(PayStatus.UNPAID.getCode());
//        orderActionMapper.updateByPrimaryKeySelective(orderAction);
        OrderAction orderAction = orderActionMapper.selectByPrimaryKey(Long.parseLong(businessActionContext.getActionContext("actionId").toString()));
        Order order = orderMapper.selectByPrimaryKeyTcc(orderAction.getOrderId());
        orderMapper.deleteByPrimaryKeyTcc(order.getOrderId());
        //修改事务状态表状态
        updateStatus(status,2);
        return true;
    }

    private boolean isExistStatus(TccTransactionStatus status) {
        TccTransactionStatus tccTransactionStatus = tccTransactionStatusMapper.queryStatusById(status);
        if (tccTransactionStatus != null) {
            status.setStatus(tccTransactionStatus.getStatus());
            return true;
        }
        return false;
    }

    private void insertStatus(TccTransactionStatus status) {
        status.setCreateTime(System.currentTimeMillis());
        status.setUpdateTime(System.currentTimeMillis());
        status.setStatus(0);
        tccTransactionStatusMapper.insertStatus(status);
    }

    private void updateStatus(TccTransactionStatus status,Integer s) {
        status.setStatus(s);
        status.setUpdateTime(System.currentTimeMillis());
        tccTransactionStatusMapper.updateStatus(status);
    }

    /**
     * 支付成功后修改状态
     *
     * @param orderId
     * @param payCode
     * @param payAmount
     * @param userId
     * @return
     * @throws Exception
     * @author Jack
     * @date 2020/9/8
     * @version
     */
    //@RequestMapping(value = "/doPay", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    @Override
    public String doPay(Long orderId, String payCode, BigDecimal payAmount, String userId) {
        Order order = orderMapper.selectByPrimaryKey(orderId);
        if (payAmount.compareTo(order.getOrderAmount()) != 0) {
            return "支付金额不正确";
        }
        String payName = PayType.getDescByCode(payCode);
        if (StringUtils.isEmpty(payName)) {
            return "支付方式不存在";
        }
        order.setPayStatus(PayStatus.PAID.getCode());
        order.setPayCode(payCode);
        order.setPayName(PayType.getDescByCode(payCode));
        order.setPayTime(System.currentTimeMillis());
        orderMapper.updateByPrimaryKeySelective(order);
        orderActionService.save(order, "支付成功", userId);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("respCode", "0");
        jsonObject.put("respMsg", "成功");
        return jsonObject.toJSONString();
    }

    /**
     * 判断支付是否成功
     *
     * @param prepayId
     * @return
     * @throws Exception
     * @author Jack
     * @date 2020/9/8
     * @version
     */
    //@RequestMapping(value = "/queryByPrepayId", method = RequestMethod.POST)
    @Override
    public String queryByPrepayId(String prepayId) {

        OrderAction orderAction = orderActionService.queryByPrepayId(prepayId);
        if (orderAction != null && orderAction.getPayStatus() != null) {
            return orderAction.getPayStatus().toString();
        } else {
            return "0";
        }
    }
}
