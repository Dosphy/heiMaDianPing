package com.hmdp.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author Dosphy
 * @Date 2025/9/22 11:41
 */
@Configuration
public class RabbitMqConfig {
    private static final String EXCHANGE = "exchange";
    private static final String QUEUE = "queue";

    //创建直连交换机
    @Bean
    public Exchange exchange() {
        return ExchangeBuilder.directExchange(EXCHANGE).build();
    }

    //创建队列
    @Bean
    public Queue queue() {
        return QueueBuilder.durable(QUEUE).build();
    }

    //绑定交换机和队列
    @Bean
    public Binding binding(@Qualifier(QUEUE) Queue queue, @Qualifier(EXCHANGE) Exchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with("").noargs();
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
