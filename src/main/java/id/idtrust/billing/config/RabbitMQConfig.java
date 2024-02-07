package id.idtrust.billing.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;

import java.util.HashMap;
import java.util.Map;

public class RabbitMQConfig {

    @Bean
    CustomExchange delayExchange() {
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("x-delayed-type", "direct");
        return new CustomExchange("payment_void", "x-delayed-message", true, false, args);
    }

    @Bean
    public Queue testDelayQueue() {
        return new Queue("billtrust");
    }

    /**
     * Bind delay queue to exchange
     */
    @Bean
    public Binding testDelayBinding(CustomExchange testDelayDirect, Queue testDelayQueue) {
        return BindingBuilder
                .bind(testDelayQueue)
                .to(testDelayDirect)
                .with("billtrust-key")
                .noargs();
    }

}
