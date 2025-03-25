package com.study.MQ.kafka;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Kafka生产者示例
 * 演示了三种发送消息的方式：
 * 1. 发送并忘记（不关心结果）
 * 2. 同步发送（等待结果）
 * 3. 异步发送（回调处理结果）
 */
public class KafkaProducerExample {
    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerExample.class);
    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String TOPIC = "example-topic";

    public static void main(String[] args) {
        // 创建生产者配置
        Properties props = createProducerProps();

        // 创建生产者
        try (Producer<String, String> producer = new KafkaProducer<>(props)) {
            // 1. 发送并忘记
            sendFireAndForget(producer, 5);

            // 2. 同步发送
            sendSync(producer, 5);

            // 3. 异步发送
            sendAsync(producer, 5);
        }
    }

    /**
     * 创建生产者配置
     */
    private static Properties createProducerProps() {
        Properties props = new Properties();
        
        // 基础配置
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        
        // 可靠性配置
        props.put(ProducerConfig.ACKS_CONFIG, "all"); // 所有副本都确认
        props.put(ProducerConfig.RETRIES_CONFIG, 3);  // 重试次数
        props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 100); // 重试间隔
        
        // 性能配置
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);    // 批次大小
        props.put(ProducerConfig.LINGER_MS_CONFIG, 5);         // 等待时间，增加批量发送几率
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432); // 缓冲区大小(32MB)
        
        return props;
    }

    /**
     * 1. 发送并忘记：不关心发送结果，最快但可能丢失消息
     */
    private static void sendFireAndForget(Producer<String, String> producer, int count) {
        logger.info("开始发送并忘记模式的消息...");
        
        for (int i = 0; i < count; i++) {
            String key = "key-" + i;
            String value = "fire-and-forget-" + i;
            
            // 创建消息
            ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, key, value);
            
            // 发送消息（不等待结果）
            producer.send(record);
            
            logger.info("发送消息: {}", record);
        }
        
        logger.info("发送并忘记模式完成");
    }

    /**
     * 2. 同步发送：等待发送结果，可靠但较慢
     */
    private static void sendSync(Producer<String, String> producer, int count) {
        logger.info("开始同步发送消息...");
        
        for (int i = 0; i < count; i++) {
            String key = "key-" + i;
            String value = "sync-" + i;
            
            // 创建消息
            ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, key, value);
            
            try {
                // 同步发送并等待结果
                Future<RecordMetadata> future = producer.send(record);
                RecordMetadata metadata = future.get(); // 阻塞等待
                
                logger.info("消息发送成功: topic={}, partition={}, offset={}", 
                        metadata.topic(), metadata.partition(), metadata.offset());
            } catch (InterruptedException | ExecutionException e) {
                logger.error("发送消息失败", e);
            }
        }
        
        logger.info("同步发送完成");
    }

    /**
     * 3. 异步发送：通过回调处理结果，平衡了性能与可靠性
     */
    private static void sendAsync(Producer<String, String> producer, int count) {
        logger.info("开始异步发送消息...");
        
        final CountDownLatch latch = new CountDownLatch(count);
        
        for (int i = 0; i < count; i++) {
            String key = "key-" + i;
            String value = "async-" + i;
            
            // 创建消息
            ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, key, value);
            
            // 异步发送并设置回调
            producer.send(record, new Callback() {
                @Override
                public void onCompletion(RecordMetadata metadata, Exception exception) {
                    if (exception == null) {
                        // 发送成功
                        logger.info("异步消息发送成功: topic={}, partition={}, offset={}", 
                                metadata.topic(), metadata.partition(), metadata.offset());
                    } else {
                        // 发送失败
                        logger.error("异步消息发送失败", exception);
                    }
                    latch.countDown();
                }
            });
            
            logger.info("提交异步发送请求: {}", record);
        }
        
        try {
            // 等待所有回调完成
            latch.await();
            logger.info("异步发送完成");
        } catch (InterruptedException e) {
            logger.error("等待异步结果被中断", e);
            Thread.currentThread().interrupt();
        }
    }
} 