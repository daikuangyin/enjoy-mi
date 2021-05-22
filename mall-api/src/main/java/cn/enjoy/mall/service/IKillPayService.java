package cn.enjoy.mall.service;

import cn.enjoy.mall.model.Order;
import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.Map;

@LocalTCC
@RequestMapping("/kill/order/service/IPayService")
public interface IKillPayService {

    @RequestMapping(value = "/doPrePay", method = RequestMethod.POST,consumes = MediaType.APPLICATION_JSON_VALUE)
    Map<String, String> doPrePay(@RequestParam("orderId") Long orderId, @RequestParam("payCode") String payCode,
                                 @RequestParam("payAmount") BigDecimal payAmount, @RequestParam("userId") String userId) ;

    @RequestMapping(value = "/updateByActionId", method = RequestMethod.POST)
    String updateByActionId(@RequestParam("actionId") String actionId) ;

    @RequestMapping(value = "/updateStatusByActionId", method = RequestMethod.POST)
    Order updateStatusByActionId(@RequestParam("actionId") String actionId) ;

    @RequestMapping(value = "/updateStatusByActionIdSaga", method = RequestMethod.POST)
    boolean updateStatusByActionIdSaga(@RequestParam("businessKey") String businessKey, @RequestParam("actionId") String actionId) ;

    @RequestMapping(value = "/compensateOrderStatusSaga", method = RequestMethod.POST)
    boolean compensateOrderStatusSaga(@RequestParam("businessKey") String businessKey, @RequestParam("actionId") String actionId) ;

    @TwoPhaseBusinessAction(name = "updateStatusByActionIdTcc", commitMethod = "updateStatusByActionIdTccCommit", rollbackMethod = "updateStatusByActionIdTccRollback")
    @RequestMapping(value = "/updateStatusByActionIdTcc", method = RequestMethod.POST)
    Order updateStatusByActionIdTcc(@RequestBody BusinessActionContext businessActionContext, @BusinessActionContextParameter(paramName = "actionId")
    @RequestParam("actionId") String actionId) ;

    @RequestMapping(value = "/updateStatusByActionIdTccCommit", method = RequestMethod.POST)
    public boolean updateStatusByActionIdTccCommit(@RequestBody BusinessActionContext businessActionContext);

    @RequestMapping(value = "/updateStatusByActionIdTccRollback", method = RequestMethod.POST)
    public boolean updateStatusByActionIdTccRollback(@RequestBody BusinessActionContext businessActionContext);

    @RequestMapping(value = "/queryByPrepayId", method = RequestMethod.POST)
    String queryByPrepayId(@RequestParam("prepayId") String prepayId) ;

    @RequestMapping(value = "/doPay", method = RequestMethod.POST,consumes = MediaType.APPLICATION_JSON_VALUE)
    String doPay(@RequestParam("orderId") Long orderId, @RequestParam("payCode") String payCode,
                 @RequestParam("payAmount") BigDecimal payAmount, @RequestParam("userId") String userId) ;
}
