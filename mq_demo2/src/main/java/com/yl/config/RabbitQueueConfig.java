package com.yl.config;


import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author ：jerry
 * @date ：Created in 2021/12/28 17:38
 * @description：RabbitMQ配置
 * @version: V1.1
 */
@Configuration
public class RabbitQueueConfig {
    /**
     * 方式二：
     * 通道--->交换机---->路由模式
     * */
    public static final String QUEUE_NAME="rollback-queue";     //通道
    public static final String EXCHANGE_NAME="rollback-exchange";       //交换机
    public static final String ROUTINGKEY_NAME="rollback-routingkey";       //路由

    /*---------------------------------方式二-------------------------------------------------*/
    /**
     * 1.设置消息发送RPC队列
     * */
    @Bean
    public Queue queue(){
        return new Queue(QUEUE_NAME,true);
    }

    /**
     * 2.设置交换机
     */
    @Bean
    public DirectExchange directExchange(){
        return new DirectExchange(EXCHANGE_NAME);
    }

    /**
     * 3.绑定路由
     */
    @Bean
    public Binding binding(){
        return BindingBuilder.bind(queue()).to(directExchange())
                .with(ROUTINGKEY_NAME);
    }
}
