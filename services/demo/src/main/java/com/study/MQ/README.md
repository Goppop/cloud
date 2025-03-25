# 消息队列学习指南

本项目提供消息队列（Message Queue）的完整学习资料，包括Kafka、RabbitMQ的实践示例及各种常见技术方案实现。

## 目录结构

```
MQ/
├── common/          # 公共组件
├── kafka/           # Kafka相关示例
├── rabbitmq/        # RabbitMQ相关示例
├── solution/        # 技术解决方案
└── config/          # 配置文件
```

## 主要内容

1. **消息队列基础概念**
   - 消息队列的作用和应用场景
   - 常见消息队列产品对比
   - 消息模型（点对点、发布订阅）

2. **Kafka实践**
   - 生产者/消费者模式实现
   - 主题与分区管理
   - 消费者组与负载均衡
   - 消息持久化与可靠性

3. **RabbitMQ实践**
   - 交换机类型与使用
   - 队列绑定与路由
   - 消息确认机制
   - 延迟队列实现

4. **消息异步解耦**
   - 事件驱动架构
   - 异步处理模式
   - 生产者与消费者解耦

5. **顺序消费保证**
   - 分区顺序消费
   - 全局顺序消费
   - 有序性与性能的平衡

6. **幂等性处理**
   - 消息重复消费问题
   - 去重策略和实现
   - 分布式锁与幂等表

7. **高吞吐与高可用**
   - 集群部署方案
   - 消息积压处理
   - 流量控制与限流
   - 灾备与故障转移

## 学习路径

1. 先阅读基础概念文档，了解消息队列基本原理
2. 分别学习Kafka和RabbitMQ的基础API使用
3. 通过solution目录下的示例掌握各种技术方案
4. 参考config目录进行环境配置与部署

## 环境要求

- Java 8+
- Spring Boot 2.x
- Kafka 2.8.0+
- RabbitMQ 3.8.0+
- Docker (可选，用于本地测试)

## 参考资源

- [Kafka官方文档](https://kafka.apache.org/documentation/)
- [RabbitMQ官方文档](https://www.rabbitmq.com/documentation.html)
- [Spring for Apache Kafka文档](https://docs.spring.io/spring-kafka/reference/html/)
- [Spring AMQP文档](https://docs.spring.io/spring-amqp/reference/html/) 