package cn.enjoy.mall.service;

import cn.enjoy.mall.model.MessageLog;
import cn.enjoy.mq.OrderLogSender;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @Classname ScheduledServiceImpl
 * @Description TODO
 * @Author Jack
 * Date 2020/10/14 10:57
 * Version 1.0
 */
@Slf4j
@Component
public class ScheduledServiceImpl {

    @Autowired
    private MessageLogService messageLogService;

    @Autowired
    private OrderLogSender orderLogSender;

    //10s启动一次
    @Scheduled(cron = "0/10 * * * * ? ")
    public void reSend() {
        log.info("----------reSend timer do--------");
        MessageLog messageLog = new MessageLog();
        messageLog.setTryCount(3);
        List<MessageLog> messageLogs = messageLogService.queryMessageLogByTimeAndStatus(messageLog);

        if(messageLogs != null && messageLogs.size() > 0) {
            MessageLog messageLog1 = messageLogs.get(0);
            CorrelationData correlationData = new CorrelationData(messageLog1.getMessageId().toString());
            log.info("----------reSend again--------" + JSONObject.toJSONString(messageLog1));
            orderLogSender.send(messageLog1,correlationData);
        }
    }
}
