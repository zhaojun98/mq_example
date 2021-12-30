package com.yl.controller;


import com.yl.common.Result;
import com.yl.common.SnowflakeIdWorker;
import com.yl.entity.Journal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * @author ：jerry
 * @date ：Created in 2021/12/29 11:11
 * @description：mq生产者
 * @version: V1.1
 */

@RestController
@Slf4j
public class SendController {

    final static SnowflakeIdWorker idWorker = new SnowflakeIdWorker(0, 0);
    @Autowired
    private RabbitTemplate rabbitTemplate;


    @RequestMapping("send")
    public Result send(){
        String exchange="rollback-exchange";
        String routingkey="rollback-routingkey";

        log.info("生产者开始发送消息");
        Journal journal = new Journal();
        journal.setId(idWorker.nextId());
        journal.setTitle("听闻广陵不知寒,大雪龙骑下江南");
//        journal.setCreateTime(LocalDateTime.now());
        journal.setTitleDesc("怒发冲冠⑵，凭阑处⑶、潇潇雨歇⑷。抬望眼，仰天长啸⑸，壮怀激烈⑹。三十功名尘与土⑺，八千里路云和月⑻。莫等闲⑼、白了少年头，空悲切⑽。 靖康耻⑾，犹未雪。臣子恨，何时灭。驾长车，踏破贺兰山缺⑿。壮志饥餐胡虏肉⒀，笑谈渴饮匈奴血⒁。待从头、收拾旧山河，朝天阙⒂。");
        //注意：将消息推送到正常的交换机中
        //参数一：交换机名称
        //参数二：路由键
        //参数三：传递参数
        //流程：生产者-->交换机-->路由键-->队列
        rabbitTemplate.convertAndSend(exchange,routingkey,journal);
        log.info("生产者发送消息完成");
        return Result.succ("操作成功");
    }
}

