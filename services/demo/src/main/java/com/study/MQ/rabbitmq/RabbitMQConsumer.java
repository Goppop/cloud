package com.study.MQ.rabbitmq;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * RabbitMQ消费者示例
 * 演示了如何消费不同交换机和队列的消息，以及不同的消息确认模式
 */
public class RabbitMQConsumer {
    // 交换机名称
    private static final String DIRECT_EXCHANGE = "example.direct";
    private static final String TOPIC_EXCHANGE = "example.topic";
    private static final String FANOUT_EXCHANGE = "example.fanout";
    private static final String HEADERS_EXCHANGE = "example.headers";
    
    // 队列名称
    private static final String DIRECT_QUEUE = "example.direct.queue";
    private static final String TOPIC_QUEUE_ORDERS = "example.topic.orders";
    private static final String TOPIC_QUEUE_USERS = "example.topic.users";
    private static final String TOPIC_QUEUE_ALL = "example.topic.all";
    private static final String FANOUT_QUEUE_1 = "example.fanout.queue1";
    private static final String FANOUT_QUEUE_2 = "example.fanout.queue2";
    private static final String HEADERS_QUEUE = "example.headers.queue";
    
    // 路由键
    private static final String DIRECT_ROUTING_KEY = "direct.key";
    
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
        
        try {
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            
            // 设置预取数量，每次从队列获取的消息数
            channel.basicQos(1);
            
            // 创建队列并绑定到交换机
            setupQueuesAndBindings(channel);
            
            // 设置不同队列的消费者
            setupDirectQueueConsumer(channel);
            setupTopicQueuesConsumers(channel);
            setupFanoutQueuesConsumers(channel);
            setupHeadersQueueConsumer(channel);
            
            System.out.println("等待消息. 按 CTRL+C 退出");
            
            // 主线程不退出，等待消息
            // 在实际应用中，可以使用更优雅的方式处理程序退出
            Thread.sleep(Long.MAX_VALUE);
            
        } catch (IOException | TimeoutException | InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 创建队列并绑定到相应的交换机
     */
    private static void setupQueuesAndBindings(Channel channel) throws IOException {
        // 1. 设置Direct队列
        channel.queueDeclare(DIRECT_QUEUE, true, false, false, null);
        channel.queueBind(DIRECT_QUEUE, DIRECT_EXCHANGE, DIRECT_ROUTING_KEY);
        
        // 2. 设置Topic队列
        // 订单队列 - 只接收订单相关消息
        channel.queueDeclare(TOPIC_QUEUE_ORDERS, true, false, false, null);
        channel.queueBind(TOPIC_QUEUE_ORDERS, TOPIC_EXCHANGE, "order.*");
        
        // 用户队列 - 只接收用户相关消息
        channel.queueDeclare(TOPIC_QUEUE_USERS, true, false, false, null);
        channel.queueBind(TOPIC_QUEUE_USERS, TOPIC_EXCHANGE, "user.*");
        
        // 所有消息队列 - 接收所有消息
        channel.queueDeclare(TOPIC_QUEUE_ALL, true, false, false, null);
        channel.queueBind(TOPIC_QUEUE_ALL, TOPIC_EXCHANGE, "#");
        
        // 3. 设置Fanout队列
        channel.queueDeclare(FANOUT_QUEUE_1, true, false, false, null);
        channel.queueBind(FANOUT_QUEUE_1, FANOUT_EXCHANGE, "");
        
        channel.queueDeclare(FANOUT_QUEUE_2, true, false, false, null);
        channel.queueBind(FANOUT_QUEUE_2, FANOUT_EXCHANGE, "");
        
        // 4. 设置Headers队列
        channel.queueDeclare(HEADERS_QUEUE, true, false, false, null);
        
        Map<String, Object> bindingArgs = new HashMap<>();
        bindingArgs.put("x-match", "all"); // 要求匹配所有头
        bindingArgs.put("format", "pdf");
        bindingArgs.put("type", "report");
        
        channel.queueBind(HEADERS_QUEUE, HEADERS_EXCHANGE, "", bindingArgs);
    }
    
    /**
     * 设置Direct队列的消费者（使用自动确认模式）
     */
    private static void setupDirectQueueConsumer(Channel channel) throws IOException {
        System.out.println("设置Direct队列消费者...");
        
        // 创建消费者
        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, StandardCharsets.UTF_8);
                System.out.println("Direct队列收到消息: " + message);
            }
        };
        
        // 订阅队列（自动确认模式）
        channel.basicConsume(DIRECT_QUEUE, true, consumer);
    }
    
    /**
     * 设置Topic队列的消费者（使用手动确认模式）
     */
    private static void setupTopicQueuesConsumers(Channel channel) throws IOException {
        System.out.println("设置Topic队列消费者...");
        
        // 1. 订单队列消费者
        Consumer orderConsumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, StandardCharsets.UTF_8);
                System.out.println("订单队列收到消息: " + message);
                
                // 手动确认消息
                channel.basicAck(envelope.getDeliveryTag(), false);
            }
        };
        
        // 2. 用户队列消费者
        Consumer userConsumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, StandardCharsets.UTF_8);
                System.out.println("用户队列收到消息: " + message);
                
                // 手动确认消息
                channel.basicAck(envelope.getDeliveryTag(), false);
            }
        };
        
        // 3. 所有消息队列消费者
        Consumer allConsumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, StandardCharsets.UTF_8);
                System.out.println("所有消息队列收到消息: " + message);
                
                // 手动确认消息
                channel.basicAck(envelope.getDeliveryTag(), false);
            }
        };
        
        // 订阅队列（手动确认模式）
        channel.basicConsume(TOPIC_QUEUE_ORDERS, false, orderConsumer);
        channel.basicConsume(TOPIC_QUEUE_USERS, false, userConsumer);
        channel.basicConsume(TOPIC_QUEUE_ALL, false, allConsumer);
    }
    
    /**
     * 设置Fanout队列的消费者（演示重新投递和拒绝）
     */
    private static void setupFanoutQueuesConsumers(Channel channel) throws IOException {
        System.out.println("设置Fanout队列消费者...");
        
        // 1. 第一个队列消费者 - 演示重新投递
        Consumer consumer1 = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, StandardCharsets.UTF_8);
                System.out.println("Fanout队列1收到消息: " + message);
                
                // 模拟处理失败，重新入队
                if (Math.random() < 0.3) {
                    System.out.println("处理失败，消息重新入队");
                    channel.basicReject(envelope.getDeliveryTag(), true);
                } else {
                    channel.basicAck(envelope.getDeliveryTag(), false);
                }
            }
        };
        
        // 2. 第二个队列消费者 - 演示拒绝消息
        Consumer consumer2 = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, StandardCharsets.UTF_8);
                System.out.println("Fanout队列2收到消息: " + message);
                
                // 模拟处理失败，直接拒绝（不重新入队）
                if (Math.random() < 0.3) {
                    System.out.println("消息被拒绝，不重新入队");
                    channel.basicReject(envelope.getDeliveryTag(), false);
                } else {
                    channel.basicAck(envelope.getDeliveryTag(), false);
                }
            }
        };
        
        // 订阅队列（手动确认模式）
        channel.basicConsume(FANOUT_QUEUE_1, false, consumer1);
        channel.basicConsume(FANOUT_QUEUE_2, false, consumer2);
    }
    
    /**
     * 设置Headers队列的消费者（演示批量确认）
     */
    private static void setupHeadersQueueConsumer(Channel channel) throws IOException {
        System.out.println("设置Headers队列消费者...");
        
        // 1. 创建消费者
        Consumer consumer = new DefaultConsumer(channel) {
            private int messageCount = 0;
            
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, StandardCharsets.UTF_8);
                System.out.println("Headers队列收到消息: " + message);
                
                messageCount++;
                
                // 每3条消息批量确认一次
                if (messageCount % 3 == 0) {
                    System.out.println("执行批量确认");
                    channel.basicAck(envelope.getDeliveryTag(), true);
                }
            }
        };
        
        // 订阅队列（手动确认模式）
        channel.basicConsume(HEADERS_QUEUE, false, consumer);
    }
} 