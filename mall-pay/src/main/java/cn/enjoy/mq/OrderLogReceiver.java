package cn.enjoy.mq;

import cn.enjoy.mall.model.MessageLog;
import cn.enjoy.mall.service.MessageLogService;
import com.alibaba.fastjson.JSON;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 类说明：
 */
@Slf4j
@Component
public class OrderLogReceiver {

    @Autowired
    private MessageLogService messageLogService;

    @RabbitListener(queues = "order.log.queue.reply")
    @RabbitHandler // 此注解加上之后可以接受对象型消息
    public void onMessage(Message message, Channel channel,@Headers Map<String, Object> headers) throws Exception {
        try {
            String msg = new String(message.getBody());
            log.info("OrderLogReceiver>>>>>>>message received:" + msg);
            MessageLog messageLog = JSON.parseObject(msg, MessageLog.class);
            try {
                messageLog.setStatus(9);
                messageLog.setTryCount(null);
                messageLogService.updateMessageLog(messageLog);
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);//手工确认，可接下一条
            } catch (Exception e) {
                e.printStackTrace();
                log.info(e.getMessage());
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);//失败，则直接忽略此订单
                log.info("OrderLogReceiver>>>>>>message nack");
                throw e;
            }
        } catch (Exception e) {
            log.info(e.getMessage());
        }
    }
}

