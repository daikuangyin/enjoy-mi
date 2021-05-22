package cn.enjoy.config;

import cn.enjoy.mall.model.MessageLog;
import cn.enjoy.mall.service.MessageLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 *类说明：消息队列配置
 */
@Slf4j
@Configuration
public class RabbitConfig {

    public final static String EXCHANGE_LOG = "order.log.producer";
    public final static String KEY_LOG = "order.log";

    @Value("${spring.rabbitmq.host}")
    private String addresses;

    @Value("${spring.rabbitmq.port}")
    private String port;

    @Value("${spring.rabbitmq.username}")
    private String username;

    @Value("${spring.rabbitmq.password}")
    private String password;

    @Value("${spring.rabbitmq.virtual-host}")
    private String virtualHost;

    @Value("${spring.rabbitmq.publisher-confirms}")
    private boolean publisherConfirms;

    @Value("${spring.rabbitmq.publisher-returns}")
    private boolean publisherReturns;

    @Autowired
    private MessageLogService messageLogService;

    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setAddresses(addresses+":"+port);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        connectionFactory.setVirtualHost(virtualHost);
        /** 如果要进行消息回调，则这里必须要设置为true */
        connectionFactory.setPublisherConfirms(publisherConfirms);
        connectionFactory.setPublisherReturns(publisherReturns);
        return connectionFactory;
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory){
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    public RabbitTemplate newRabbitTemplate() {
        RabbitTemplate template = new RabbitTemplate(connectionFactory());
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.info("消息成功发送到Exchange,messageId:" + correlationData.getId());
                //修改日志表状态，状态改成 1，投递成功且未确认
                if(updateMessageLog(1,Long.valueOf(correlationData.getId())) == 1) {
                    log.info("------modify status 1 ok--------");
                }
            } else {
                log.info("消息发送到Exchange失败, {}, cause: {}", correlationData, cause);
            }
        });
        template.setMandatory(true);
        template.setReturnCallback((message, replyCode, replyText, exchange, routingKey) -> {
            log.info("消息从Exchange路由到Queue失败: exchange: {}, route: {}, replyCode: {}, replyText: {}, message: {}", exchange, routingKey, replyCode, replyText, message);
        });
        //不使用临时队列
//        template.setUseTemporaryReplyQueues(false);
//        template.setReplyAddress("amq.rabbitmq.reply-to");
//        template.setUserCorrelationId(true);
//        template.setReplyTimeout(10000);
        return template;
    }

    private int updateMessageLog(int status,Long messageId) {
        MessageLog messageLog = new MessageLog();
        messageLog.setMessageId(messageId);
        messageLog.setStatus(status);
        messageLog.setTryCount(1);
        return messageLogService.updateMessageLog(messageLog);
    }


    @Bean
    public Queue queuelogMessage() {
        return new Queue("order.log.queue");
    }

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(EXCHANGE_LOG);
    }

    @Bean
    public Binding bindingLogExchangeMessage() {
        return BindingBuilder
                .bind(queuelogMessage())
                .to(exchange())
                .with(KEY_LOG);
    }

    @Bean
    public RabbitListenerContainerFactory<?> rabbitListenerContainerFactory(ConnectionFactory connectionFactory, List<SimpleMessageListenerContainer> list) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setConcurrentConsumers(2);
//抓取参数非常关键，一次抓取的消息多了，消费速度一慢，就会造成响应延迟，抓取少了又会导致并发量低，消息堵塞
        factory.setPrefetchCount(10);

        /*
         * AcknowledgeMode.NONE：自动确认
         * AcknowledgeMode.AUTO：根据情况确认
         * AcknowledgeMode.MANUAL：手动确认
         */
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
/*        factory.setDefaultRequeueRejected(false);
        factory.setAdviceChain(
                RetryInterceptorBuilder
                        .stateless()
                        .recoverer(new RejectAndDontRequeueRecoverer())
                        .retryOperations(retryTemplate())
                        .build()
        );*/
        return factory;
    }

    //===============生产者发送确认==========
    @Bean
    public RabbitTemplate.ConfirmCallback confirmCallback(){
        return new RabbitTemplate.ConfirmCallback(){

            @Override
            public void confirm(CorrelationData correlationData,
                                boolean ack, String cause) {
                if (ack) {
                    log.info("发送者确认发送给mq成功");
                } else {
                    //处理失败的消息
                    log.info("发送者发送给mq失败,考虑重发:"+cause);
                }
            }
        };
    }

    @Bean
    public RabbitTemplate.ReturnCallback returnCallback(){
        return new RabbitTemplate.ReturnCallback(){

            @Override
            public void returnedMessage(Message message,
                                        int replyCode,
                                        String replyText,
                                        String exchange,
                                        String routingKey) {
                log.info("无法路由的消息，需要考虑另外处理。");
                log.info("Returned replyText："+replyText);
                log.info("Returned exchange："+exchange);
                log.info("Returned routingKey："+routingKey);
                String msgJson  = new String(message.getBody());
                log.info("Returned Message："+msgJson);
            }
        };
    }
}
