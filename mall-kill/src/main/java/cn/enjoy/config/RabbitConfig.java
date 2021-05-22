package cn.enjoy.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *@author Jack老师   享学课堂 https://enjoy.ke.qq.com
 *类说明：
 */
@Slf4j
@Configuration
public class RabbitConfig {

    public final static String EXCHANGE_SECKILL = "order.seckill.delay.exchange";
    public final static String KEY_SECKILL = "order.seckill.delay.routingkey";

    public final static String EXCHANGE_SECKILL_DEAD = "order.seckill.dead.exchange";
    public final static String KEY_SECKILL_DEAD = "order.seckill.dead.routingkey";

    @Bean(name = "queueDelayMessage")
    public Queue queueDelayMessage() {
        Map<String,Object> arguments = new HashMap<>();
        arguments.put("x-message-ttl",1000 * 60 * 10);
//        arguments.put("x-expires",1000 * 60);
//        arguments.put("x-max-length",10000);
//        arguments.put("x-max-length-bytes",50*1024);
        arguments.put("x-dead-letter-exchange",EXCHANGE_SECKILL_DEAD);
        arguments.put("x-dead-letter-routing-key",KEY_SECKILL_DEAD);
        return new Queue("order.seckill.delay.queue", true, false, false,arguments);
    }

    @Bean(name = "exchange")
    public DirectExchange exchange() {
        return new DirectExchange(EXCHANGE_SECKILL, true, false);
    }

    @Bean
    public Binding bindingSecKillExchangeMessage(@Qualifier("queueDelayMessage") Queue queueMessage,
                                                 @Qualifier("exchange") DirectExchange exchange) {
        return BindingBuilder
                .bind(queueMessage)
                .to(exchange)
                .with(KEY_SECKILL);
    }

    @Bean(name = "deadqueueMessage")
    public Queue deadqueueMessage() {
        return new Queue("order.seckill.dead.queue",true);
    }

    @Bean(name = "deadexchange")
    public DirectExchange deadexchange() {
        return new DirectExchange(EXCHANGE_SECKILL_DEAD,true,false);
    }

    @Bean
    Binding bindingDeadExchangeMessage(@Qualifier("deadqueueMessage") Queue deadqueueMessage,
                                       @Qualifier("deadexchange") DirectExchange deadexchange) {
        return BindingBuilder.bind(deadqueueMessage).to(deadexchange).
                with(KEY_SECKILL_DEAD);
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
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
/*        retryTemplate.registerListener(new RetryListener() {
            @Override
            public <T, E extends Throwable> boolean open(RetryContext retryContext, RetryCallback<T, E> retryCallback) {
                return false;
            }

            @Override
            public <T, E extends Throwable> void close(RetryContext retryContext, RetryCallback<T, E> retryCallback, Throwable throwable) {
            }

            @Override
            public <T, E extends Throwable> void onError(RetryContext retryContext, RetryCallback<T, E> retryCallback, Throwable throwable) {

            }
        });*/
        retryTemplate.setBackOffPolicy(backOffPolicy());
        retryTemplate.setRetryPolicy(retryPolicy());
        return retryTemplate;
    }

    @Bean
    public ExponentialBackOffPolicy backOffPolicy() {
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000);
        backOffPolicy.setMaxInterval(10000);
        return backOffPolicy;
    }

    @Bean
    public SimpleRetryPolicy retryPolicy() {
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        return retryPolicy;
    }

}
