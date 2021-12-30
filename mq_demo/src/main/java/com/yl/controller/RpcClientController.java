package com.yl.controller;

import com.yl.config.RabbitConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;

/**
 * @author ：jerry
 * @date ：Created in 2021/12/29 14:02
 * @description：MQ生产者
 * @version: V1.1
 */
@Slf4j
@RestController
public class RpcClientController {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @GetMapping("/send")
    public String send(String message) {
        // 创建消息对象
        Message newMessage = MessageBuilder.withBody(message.getBytes()).build();
        log.info("生产者发送消息----->>>>>", newMessage);
        //客户端发送消息
        Message result = rabbitTemplate.sendAndReceive(RabbitConfig.RPC_EXCHANGE, RabbitConfig.RPC_QUEUE1, newMessage);

        String response = "";
        if (result != null) {
            // 获取已发送的消息的 correlationId
            String correlationId = newMessage.getMessageProperties().getCorrelationId();
            log.info("生产者----->>>>>{}", correlationId);

            // 获取响应头信息
            HashMap<String, Object> headers = (HashMap<String, Object>) result.getMessageProperties().getHeaders();

            // 获取 server 返回的消息 id
            String msgId = (String) headers.get("spring_returned_message_correlation");

            if (msgId.equals(correlationId)) {
                response = new String(result.getBody());
                log.info("生产者发送消息----->>>>>：{}", response);
            }
        }
        return response;
    }

    public static void main(String[] args) {
        double longitude = 104.0586380d;        //  经度
        double latitude = 30.5768688d;          //  纬度

        double[] output = new double[2];
        double longitude1, latitude1, longitude0, X0, Y0, xval, yval;
        //NN曲率半径，测量学里面用N表示
        //M为子午线弧长，测量学里用大X表示
        //fai为底点纬度，由子午弧长反算公式得到，测量学里用Bf表示
        //R为底点所对的曲率半径，测量学里用Nf表示
        double a, f, e2, ee, NN, T, C, A, M, iPI;
        iPI = 0.0174532925199433; //3.1415926535898/180.0;
        a = 6378137.0;
        f = 1 / 298.257222101; //CGCS2000坐标系参数
        //a=6378137.0; f=1/298.2572236; //wgs84坐标系参数
        longitude0 = 105.0;//中央子午线 根据实际进行配置
        longitude0 = longitude0 * iPI;//中央子午线转换为弧度
        longitude1 = longitude * iPI; //经度转换为弧度
        latitude1 = latitude * iPI; //纬度转换为弧度
        e2 = 2 * f - f * f;
        ee = e2 * (1.0 - e2);
        NN = a / Math.sqrt(1.0 - e2 * Math.sin(latitude1) * Math.sin(latitude1));
        T = Math.tan(latitude1) * Math.tan(latitude1);
        C = ee * Math.cos(latitude1) * Math.cos(latitude1);
        A = (longitude1 - longitude0) * Math.cos(latitude1);
        M = a * ((1 - e2 / 4 - 3 * e2 * e2 / 64 - 5 * e2 * e2 * e2 / 256) * latitude1 - (3 * e2 / 8 + 3 * e2 * e2 / 32 + 45 * e2 * e2
                * e2 / 1024) * Math.sin(2 * latitude1)
                + (15 * e2 * e2 / 256 + 45 * e2 * e2 * e2 / 1024) * Math.sin(4 * latitude1) - (35 * e2 * e2 * e2 / 3072) * Math.sin(6 * latitude1));
        xval = NN * (A + (1 - T + C) * A * A * A / 6 + (5 - 18 * T + T * T + 72 * C - 58 * ee) * A * A * A * A * A / 120);
        yval = M + NN * Math.tan(latitude1) * (A * A / 2 + (5 - T + 9 * C + 4 * C * C) * A * A * A * A / 24
                + (61 - 58 * T + T * T + 600 * C - 330 * ee) * A * A * A * A * A * A / 720);
        X0 = 500000L;
        Y0 = 0;
        xval = xval + X0;
        yval = yval + Y0;

        //转换为投影
        output[0] = xval;
        output[1] = yval;

        String x = new BigDecimal(xval + "").toString();
        String y = new BigDecimal(yval + "").toString();
        System.out.println("xval--->"+x);
        System.out.println("yval--->"+y);

    }
}