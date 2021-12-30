package com.yl.config;


import com.yl.controller.RabbitQueueReceiver;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * @author ：jerry
 * @date ：Created in 2021/12/29 13:11
 * @description：消费消息
 * @version: V1.1
 */

@Configuration
public class MessageListenerConfig {

    @Bean
    public SimpleMessageListenerContainer simpleMessageListenerContainer(
            ConnectionFactory connectionFactory,
            RabbitQueueConfig rabbitQueueConfig,
            RabbitQueueReceiver rabbitQueuReceiver){
        SimpleMessageListenerContainer container=new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);//设置mq连接工厂对象
        container.setConcurrentConsumers(1);//设置并发消费者
        container.setMaxConcurrentConsumers(1);//设置最多的并发消费者
        container.setAcknowledgeMode(AcknowledgeMode.MANUAL); // RabbitMQ默认是自动确认，这里改为手动确认消息

//        container.setMessageConverter(jackson2JsonMessageConverter());



        //注意：此处不能使用Autowired根据类型自动注入队列，必须调用rabbitmqDirectConfig.firstQueue()获得，why?
        // 因为项目中可能存在多个队列，它们的类型都是Queue，自动注入会报错
        container.setQueues(rabbitQueueConfig.queue());
        container.setMessageListener(rabbitQueuReceiver);

        return container;
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter(){
        return new Jackson2JsonMessageConverter();
    }
}

