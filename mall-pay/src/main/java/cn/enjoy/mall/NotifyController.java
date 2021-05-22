package cn.enjoy.mall;


import cn.enjoy.dao.MessageLogMapper;
import cn.enjoy.mall.constant.PayStatus;
import cn.enjoy.mall.model.MessageLog;
import cn.enjoy.mall.model.OrderAction;
import cn.enjoy.mall.service.IKillOrderActionService;
import cn.enjoy.mall.service.IOrderActionService;
import cn.enjoy.mall.service.IPayService;
import cn.enjoy.mall.wxsdk.WXPay;
import cn.enjoy.mall.wxsdk.WXPayUtil;
import cn.enjoy.mall.wxsdk.WxPayConfigImpl;
import cn.enjoy.mq.OrderLogSender;
import com.alibaba.fastjson.JSONObject;
import com.baidu.fsg.uid.impl.DefaultUidGenerator;
import org.redisson.RedissonRedLock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


/**
 * 支付成功回调
 *
 * @author Jack
 * @date 2020/9/8
 */
@Controller
//http请求控制类  Contoller
public class NotifyController {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private IPayService payService;
    @Autowired
    WxPayConfigImpl wxPayConfig;
    @Autowired
    WXPay wxPay;
    @Value("${wx.appId}")
    private String appId = "";
    @Value("${wx.mchId}")
    private String mchId = "";
    @Value("${wx.key}")
    private String partnerKey = "";
    @Value("${wx.certPath}")
    private String certPath = "";
    @Value("${wx.notify_url}")
    private String notify_url = "http://www.weixin.qq.com/wxpay/pay.php";

    @Autowired
    private OrderLogSender orderLogSender;

    @Autowired
    @Qualifier("redissonClient1")
    private RedissonClient redissonClient1;

    @Autowired
    @Qualifier("redissonClient2")
    private RedissonClient redissonClient2;

    @Autowired
    @Qualifier("redissonClient3")
    private RedissonClient redissonClient3;

    @Autowired
    private IOrderActionService orderActionService;

    @Autowired
    private IKillOrderActionService killOrderActionService;

    @Autowired
    private DefaultUidGenerator defaultUidGenerator;

    @Autowired
    private MessageLogMapper messageLogMapper;

    /**
     * 支付成功后的回调接口
     *
     * @param request
     * @param response
     * @return
     * @throws Exception
     * @author Jack
     * @date 2020/9/8
     * @version
     */
    @RequestMapping(value = "/wx/notify", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String payNotifyUrl(HttpServletRequest request, HttpServletResponse response) throws Exception {
        //先加锁，避免并发函数重入
        String resourceName = "PAY_REDLOCK_KEY";
        RLock lock1 = redissonClient1.getLock(resourceName);
        RLock lock2 = redissonClient2.getLock(resourceName);
        RLock lock3 = redissonClient3.getLock(resourceName);
        RedissonRedLock redLock = new RedissonRedLock(lock1, lock2, lock3);

        try {
            redLock.lock(2, TimeUnit.SECONDS);
            BufferedReader reader = null;
            reader = request.getReader();
            String line = "";
            String xmlString = null;
            StringBuffer inputString = new StringBuffer();
            while ((line = reader.readLine()) != null) {
                inputString.append(line);
            }
            xmlString = inputString.toString();
            request.getReader().close();
            logger.info("----pay callback data---" + xmlString);
            Map<String, String> map = new HashMap<String, String>();
            String result_code = "";
            String return_code = "";
            String out_trade_no = "";
            map = WXPayUtil.xmlToMap(xmlString);

            //幂等判断，判断该单子是否已经回调处理过
            String actionId = map.get("out_trade_no");
            //获取订单类型，判断是否是秒杀或者普通订单
            String attach = map.get("attach");
            logger.info("-------attach------" + attach);
            if(attach != null && !"".equals(attach)) {
                if ("K".equalsIgnoreCase(attach)) {
                    OrderAction orderAction = killOrderActionService.queryByActionId(Long.valueOf(actionId));
                    //如果已经支付
                    if (PayStatus.PAID.getCode() == orderAction.getPayStatus()) {
                        return "SUCCESS";
                    }
                } else if ("N".equalsIgnoreCase(attach)) {
                    OrderAction orderAction = orderActionService.queryByActionId(Long.valueOf(actionId));
                    //如果已经支付
                    if (PayStatus.PAID.getCode() == orderAction.getPayStatus()) {
                        return "SUCCESS";
                    }
                }
            }

            result_code = map.get("result_code");
            return_code = map.get("return_code");
            logger.info("--------map ------" + JSONObject.toJSONString(map));
            if (return_code.equals("SUCCESS")) {
                if (result_code.equals("SUCCESS")) {
                    //异步记录日志，修改订单状态
                    messageLogAndSend(actionId);
                    return "SUCCESS";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            redLock.unlock();
        }
        return "fail";
    }

    @Transactional
    public void messageLogAndSend(String actionId) throws Exception {
        MessageLog messageLog = new MessageLog();
        messageLog.setMessageId(defaultUidGenerator.getUID());
        messageLog.setMessage(actionId);
        messageLog.setStatus(0);
        messageLog.setTryCount(0);
        Long now = System.currentTimeMillis();
        messageLog.setCreateTime(now);
        messageLog.setUpdateTime(now);
        //延迟5秒被定时器扫描判断是否需要重发
        messageLog.setDelayTime(now + 5000);
        messageLogMapper.insertP(messageLog);
        CorrelationData correlationData = new CorrelationData(messageLog.getMessageId().toString());
        orderLogSender.send(messageLog,correlationData);
    }
}

