package com.yl.config;



import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author ：jerry
 * @date ：Created in 2021/12/29 11:10
 * @description：
 * @version: V1.1
 */

@Configuration
@Slf4j
public class RabbitTemplateConfig {

    //第二种方式
    final RabbitTemplate.ConfirmCallback confirmCallback = new RabbitTemplate.ConfirmCallback() 		{
        @Override
        public void confirm(CorrelationData correlationData, boolean ack, String cause) {
            log.info("ConfirmCallback，相关数据：{}", correlationData);
            log.info("ConfirmCallback，确认消息：{}", ack);
            log.info("ConfirmCallback，原因：{}", cause);
        }
    };

    @Bean
    public RabbitTemplate createRabbitTemplate(ConnectionFactory connectionFactory){
        RabbitTemplate rabbitTemplate=new RabbitTemplate();
        //设置连接工厂Bean
        rabbitTemplate.setConnectionFactory(connectionFactory);
        //手动开启
        rabbitTemplate.setMandatory(true);

        //设置传输数据是json格式
        rabbitTemplate.setMessageConverter(jsonMessageConverter());

        //流程：生产者-->交换机-->路由键-->队列
        //ConfirmCallback
        //流程：生产者-->交换机
        //1）成功  触发回调
        //2）失败  触发回调
        rabbitTemplate.setConfirmCallback(new RabbitTemplate.ConfirmCallback() {
            @Override
            public void confirm(CorrelationData correlationData, boolean ack, String cause) {
                log.info("ConfirmCallback，相关数据：{}", correlationData);
                log.info("ConfirmCallback，确认消息：{}", ack);
                log.info("ConfirmCallback，原因：{}", cause);
            }
        });

        //第二种方式
        //rabbitTemplate.setConfirmCallback(confirmCallback);

        //ReturnCallback：该回调函数的触发器与mandatory: true参数有必要关系
        //流程：交换机-->队列
        //成功  不触发回调
        //失败  触发回调
        rabbitTemplate.setReturnCallback(new RabbitTemplate.ReturnCallback() {
            @Override
            public void returnedMessage(Message message, int replyCode, String replyText, String exchange, String routingKey) {
                log.info("ReturnCallback，消息：{}", message);
                log.info("ReturnCallback，回应码：{}", replyCode);
                log.info("ReturnCallback，回应信息：{}", replyText);
                log.info("ReturnCallback，交换机：{}", exchange);
                log.info("ReturnCallback，路由键：{}", routingKey);
            }
        });

        return rabbitTemplate;
    }


    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter(){
        return new Jackson2JsonMessageConverter();
    }
}

