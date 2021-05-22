package cn.enjoy.mq;

import cn.enjoy.config.RabbitConfig;
import cn.enjoy.mall.model.MessageLog;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 类说明：
 */
@Slf4j
@Component
public class OrderLogSender {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void send(String actionId) {
        log.info("TopicSender send the 1st : " + actionId);
        this.rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_LOG, RabbitConfig.KEY_LOG, actionId);
    }

    public void send(MessageLog messageLog, CorrelationData correlationData) {
        String msg1 = JSON.toJSONString(messageLog);
        log.info("TopicSender send the 1st : " + msg1);
        this.rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_LOG, RabbitConfig.KEY_LOG, msg1,correlationData);
    }
}
