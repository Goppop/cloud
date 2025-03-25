package com.study.MQ.kafka;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Kafka消费者示例
 * 演示了三种不同的消费方式：
 * 1. 自动提交偏移量
 * 2. 手动提交偏移量
 * 3. 手动分配分区并提交特定偏移量
 */
public class KafkaConsumerExample {
    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerExample.class);
    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String TOPIC = "example-topic";
    private static final String CONSUMER_GROUP = "example-group";
    private static final Duration POLL_TIMEOUT = Duration.ofMillis(100);

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("请指定消费模式：auto, manual, assigned");
            return;
        }

        String mode = args[0].toLowerCase();
        switch (mode) {
            case "auto":
                consumeWithAutoCommit();
                break;
            case "manual":
                consumeWithManualCommit();
                break;
            case "assigned":
                consumeWithManualAssignment();
                break;
            default:
                System.out.println("未知的消费模式：" + mode);
        }
    }

    /**
     * 1. 自动提交偏移量：最简单但可能重复消费或丢失消息
     */
    private static void consumeWithAutoCommit() {
        logger.info("开始自动提交偏移量的消费模式...");
        
        // 创建消费者配置
        Properties props = createConsumerProps();
        
        // 启用自动提交
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        
        // 创建消费者
        try (Consumer<String, String> consumer = new KafkaConsumer<>(props)) {
            // 订阅主题
            consumer.subscribe(Collections.singletonList(TOPIC));
            
            // 轮询消息
            consumeMessages(consumer, 50, false);
        }
    }

    /**
     * 2. 手动提交偏移量：更精确的控制，避免消息丢失
     */
    private static void consumeWithManualCommit() {
        logger.info("开始手动提交偏移量的消费模式...");
        
        // 创建消费者配置
        Properties props = createConsumerProps();
        
        // 禁用自动提交
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        
        // 创建消费者
        try (Consumer<String, String> consumer = new KafkaConsumer<>(props)) {
            // 订阅主题
            consumer.subscribe(Collections.singletonList(TOPIC));
            
            // 轮询消息并手动提交
            consumeMessages(consumer, 50, true);
        }
    }

    /**
     * 3. 手动分配分区：对分区进行精确控制，适合特定场景
     */
    private static void consumeWithManualAssignment() {
        logger.info("开始手动分配分区的消费模式...");
        
        // 创建消费者配置
        Properties props = createConsumerProps();
        
        // 禁用自动提交
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        
        // 创建消费者
        try (Consumer<String, String> consumer = new KafkaConsumer<>(props)) {
            // 获取主题的分区信息
            List<PartitionInfo> partitions = consumer.partitionsFor(TOPIC);
            if (partitions == null || partitions.isEmpty()) {
                logger.error("无法获取主题 {} 的分区信息", TOPIC);
                return;
            }
            
            // 手动分配分区（这里简单地分配第一个分区）
            TopicPartition partition = new TopicPartition(TOPIC, partitions.get(0).partition());
            consumer.assign(Collections.singletonList(partition));
            
            // 从特定偏移量开始消费
            consumer.seek(partition, 0); // 从最开始消费
            
            // 轮询消息并手动提交特定偏移量
            AtomicBoolean running = new AtomicBoolean(true);
            int messageCount = 0;
            
            while (running.get() && messageCount < 50) {
                ConsumerRecords<String, String> records = consumer.poll(POLL_TIMEOUT);
                
                if (records.isEmpty()) {
                    continue;
                }
                
                // 处理消息
                for (ConsumerRecord<String, String> record : records) {
                    logger.info("收到消息：partition = {}, offset = {}, key = {}, value = {}",
                            record.partition(), record.offset(), record.key(), record.value());
                    
                    messageCount++;
                    
                    // 处理完每条消息后，手动提交该条消息的偏移量
                    Map<TopicPartition, OffsetAndMetadata> offsets = new HashMap<>();
                    offsets.put(
                            new TopicPartition(record.topic(), record.partition()),
                            new OffsetAndMetadata(record.offset() + 1)
                    );
                    consumer.commitSync(offsets);
                    
                    if (messageCount >= 50) {
                        running.set(false);
                        break;
                    }
                }
            }
        }
        
        logger.info("手动分配分区的消费模式完成");
    }

    /**
     * 消费消息的通用方法
     */
    private static void consumeMessages(Consumer<String, String> consumer, int maxMessages, boolean manualCommit) {
        AtomicBoolean running = new AtomicBoolean(true);
        int messageCount = 0;
        
        while (running.get() && messageCount < maxMessages) {
            ConsumerRecords<String, String> records = consumer.poll(POLL_TIMEOUT);
            
            if (records.isEmpty()) {
                continue;
            }
            
            // 处理消息
            for (ConsumerRecord<String, String> record : records) {
                logger.info("收到消息：partition = {}, offset = {}, key = {}, value = {}",
                        record.partition(), record.offset(), record.key(), record.value());
                
                messageCount++;
                if (messageCount >= maxMessages) {
                    running.set(false);
                    break;
                }
            }
            
            // 如果是手动提交模式，提交偏移量
            if (manualCommit) {
                consumer.commitSync();
                logger.info("手动提交偏移量完成");
            }
        }
        
        logger.info("消费完成，共处理 {} 条消息", messageCount);
    }

    /**
     * 创建消费者配置
     */
    private static Properties createConsumerProps() {
        Properties props = new Properties();
        
        // 基础配置
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, CONSUMER_GROUP);
        
        // 消费者配置
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10); // 单次拉取最大记录数
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"); // 新消费组从最早的偏移量开始
        
        return props;
    }
} 