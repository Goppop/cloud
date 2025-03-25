package com.study.MQ.rabbitmq;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * RabbitMQ生产者示例
 * 演示了四种不同类型的交换机：
 * 1. 直接交换机（Direct）
 * 2. 主题交换机（Topic）
 * 3. 扇出交换机（Fanout）
 * 4. 头交换机（Headers）
 */
public class RabbitMQProducer {
    // 交换机名称
    private static final String DIRECT_EXCHANGE = "example.direct";
    private static final String TOPIC_EXCHANGE = "example.topic";
    private static final String FANOUT_EXCHANGE = "example.fanout";
    private static final String HEADERS_EXCHANGE = "example.headers";
    
    // 路由键
    private static final String DIRECT_ROUTING_KEY = "direct.key";
    private static final String TOPIC_ROUTING_KEY_1 = "order.created";
    private static final String TOPIC_ROUTING_KEY_2 = "user.registered";
    
    // 连接配置
    private static final String HOST = "localhost";
    private static final int PORT = 5672;
    private static final String USERNAME = "guest";
    private static final String PASSWORD = "guest";
    
    public static void main(String[] args) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(HOST);
        factory.setPort(PORT);
        factory.setUsername(USERNAME);
        factory.setPassword(PASSWORD);
        
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            
            // 声明交换机
            declareExchanges(channel);
            
            // 发送消息到不同类型的交换机
            sendToDirectExchange(channel);
            sendToTopicExchange(channel);
            sendToFanoutExchange(channel);
            sendToHeadersExchange(channel);
            
            System.out.println("所有消息发送完成");
            
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 声明各种类型的交换机
     */
    private static void declareExchanges(Channel channel) throws IOException {
        // 声明直接交换机
        channel.exchangeDeclare(DIRECT_EXCHANGE, BuiltinExchangeType.DIRECT, true);
        
        // 声明主题交换机
        channel.exchangeDeclare(TOPIC_EXCHANGE, BuiltinExchangeType.TOPIC, true);
        
        // 声明扇出交换机
        channel.exchangeDeclare(FANOUT_EXCHANGE, BuiltinExchangeType.FANOUT, true);
        
        // 声明头交换机
        channel.exchangeDeclare(HEADERS_EXCHANGE, BuiltinExchangeType.HEADERS, true);
    }
    
    /**
     * 发送消息到直接交换机
     * Direct交换机将消息路由到与绑定键完全匹配的队列
     */
    private static void sendToDirectExchange(Channel channel) throws IOException {
        String message = "这是发送到Direct交换机的消息";
        
        // 设置消息属性
        AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                .contentType("text/plain")
                .deliveryMode(2) // 持久化消息
                .build();
        
        // 发布消息到Direct交换机
        channel.basicPublish(DIRECT_EXCHANGE, DIRECT_ROUTING_KEY, properties, 
                message.getBytes(StandardCharsets.UTF_8));
        
        System.out.println("已发送消息到Direct交换机: " + message);
    }
    
    /**
     * 发送消息到主题交换机
     * Topic交换机将消息路由到与绑定模式匹配的队列
     */
    private static void sendToTopicExchange(Channel channel) throws IOException {
        // 发送第一条消息（订单创建事件）
        String message1 = "新订单已创建: ORDER123";
        channel.basicPublish(TOPIC_EXCHANGE, TOPIC_ROUTING_KEY_1, null, 
                message1.getBytes(StandardCharsets.UTF_8));
        System.out.println("已发送消息到Topic交换机: " + message1);
        
        // 发送第二条消息（用户注册事件）
        String message2 = "新用户已注册: USER456";
        channel.basicPublish(TOPIC_EXCHANGE, TOPIC_ROUTING_KEY_2, null, 
                message2.getBytes(StandardCharsets.UTF_8));
        System.out.println("已发送消息到Topic交换机: " + message2);
    }
    
    /**
     * 发送消息到扇出交换机
     * Fanout交换机将消息广播到所有绑定的队列
     */
    private static void sendToFanoutExchange(Channel channel) throws IOException {
        String message = "这是广播到所有绑定队列的消息";
        
        // 发布消息到Fanout交换机（路由键会被忽略）
        channel.basicPublish(FANOUT_EXCHANGE, "", null, 
                message.getBytes(StandardCharsets.UTF_8));
        
        System.out.println("已发送消息到Fanout交换机: " + message);
    }
    
    /**
     * 发送消息到头交换机
     * Headers交换机使用消息头的属性进行匹配
     */
    private static void sendToHeadersExchange(Channel channel) throws IOException {
        String message = "这是基于消息头匹配的消息";
        
        // 设置消息头属性
        Map<String, Object> headers = new HashMap<>();
        headers.put("format", "pdf");
        headers.put("type", "report");
        headers.put("x-match", "all"); // 需要匹配所有指定的头
        
        AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                .headers(headers)
                .build();
        
        // 发布消息到Headers交换机（路由键会被忽略）
        channel.basicPublish(HEADERS_EXCHANGE, "", properties, 
                message.getBytes(StandardCharsets.UTF_8));
        
        System.out.println("已发送消息到Headers交换机: " + message);
    }
} 