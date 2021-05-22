package cn.enjoy.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
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

    public final static String EXCHANGE_LOG = "order.log.producer.reply";
    public final static String KEY_LOG = "order.log.reply";

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

    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setAddresses(addresses+":"+port);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        connectionFactory.setVirtualHost(virtualHost);
        /** 如果要进行消息回调，则这里必须要设置为true */
//        connectionFactory.setPublisherConfirms(publisherConfirms);
//        connectionFactory.setPublisherReturns(publisherReturns);
        return connectionFactory;
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory){
        return new RabbitAdmin(connectionFactory);
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

    @Bean
    public Queue queuelogMessage() {
        return new Queue("order.log.queue.reply");
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
}
