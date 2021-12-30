package com.yl.controller;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.util.JSONPObject;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.listener.adapter.AbstractAdaptableMessageListener;
import org.springframework.stereotype.Component;

/**
 * @author ：jerry
 * @date ：Created in 2021/12/29 11:31
 * @description：MQ消费者
 * @version: V1.1
 */
@Slf4j
@Component
@RabbitListener(queues = {"rollback_queue"})
public class RabbitQueueReceiver extends AbstractAdaptableMessageListener {

    /**
     * 消息确认机制，消息不会重复消费
     * */
    @Override
    public void onMessage(Message message, Channel channel) throws Exception {
        //消息的唯一ID，单调递增正整数，从1开始，当multiple=trues，一次性处理<=deliveryTag的所有
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        boolean multiple=false;  //false单条   true 批量

//       channel.basicAck(deliveryTag, multiple);	//正确消费消息
//       channel.basicReject(deliveryTag,true);     //为true会重新放回队列
//       channel.basicNack(deliveryTag, multiple,true)
        try {
            String msg=new String(message.getBody(),"UTF-8");
            JSONObject json = JSONObject.parseObject(msg);
            Long id = json.getLong("id");
            log.info("消费的消息id"+id+"-------->>>>>>>"+"消费者消费消息的消息体：{}----->>>>>"+message);

            //睡眠四秒
            for(int i=0;i<4;i++){
                Thread.sleep(1000);
                System.out.println("...");
            }

//            if(deliveryTag%2==0){
//                throw new RuntimeException("偶数必须为0");
//            }

            log.info("消息已被正确消费--->>>>>>>>"+deliveryTag);
            //当前模式为单条消费
            channel.basicAck(deliveryTag, multiple);

        } catch (Exception e) {
            e.printStackTrace();
            //报异常重新投递
            channel.basicReject(deliveryTag,true);
        }
    }


//    @RabbitHandler
//    public void handlerMessage(Journal orderVo) {
//        log.info("消费者消费消息"+orderVo.toString());
//    }
}
