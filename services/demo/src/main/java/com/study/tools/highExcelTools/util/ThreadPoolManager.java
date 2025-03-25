package com.study.tools.highExcelTools.util;

import com.alibaba.nacos.shaded.javax.annotation.concurrent.ThreadSafe;
import com.study.tools.highExcelTools.config.ExcelConfig;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程池管理器
 * 负责创建和管理线程池资源
 * 
 * @ThreadSafe 该工具类线程安全
 */
@Slf4j
@ThreadSafe
public class ThreadPoolManager {
    /**
     * 默认核心线程数
     * 核心线程会一直存活，即使没有任务需要执行
     * 当线程数小于核心线程数时，即使有线程空闲，线程池也会优先创建新线程处理
     */
    private static final int DEFAULT_CORE_POOL_SIZE = 2;
    
    /**
     * 默认最大线程数
     * 当线程数大于或等于核心线程数，且任务队列已满时，线程池会创建新线程处理任务
     * 当线程数等于最大线程数，且任务队列已满时，线程池会拒绝处理任务
     */
    private static final int DEFAULT_MAX_POOL_SIZE = 4;
    
    /**
     * 默认队列容量
     * 当核心线程数达到最大时，新任务会放入队列中排队等待执行
     */
    private static final int DEFAULT_QUEUE_CAPACITY = 100;
    
    /**
     * 默认线程存活时间（秒）
     * 当线程空闲时间达到存活时间时，线程会退出，直到线程数量等于核心线程数
     */
    private static final int DEFAULT_KEEP_ALIVE_TIME = 60;
    
    /**
     * 线程名称前缀
     * 用于在日志和线程转储中标识线程池中的线程
     */
    private static final String THREAD_NAME_PREFIX = "excel-worker-";
    
    /**
     * 创建线程池
     * 根据配置或系统资源动态调整线程池参数
     * 
     * 线程池工作原理：
     * 1. 当线程数小于核心线程数时，创建线程
     * 2. 当线程数大于等于核心线程数，任务放入队列
     * 3. 当队列已满，线程数小于最大线程数时，创建线程
     * 4. 当队列已满，线程数等于最大线程数时，执行拒绝策略
     * 
     * @param config Excel配置对象，包含线程池相关配置
     * @return 根据配置创建的线程池实例
     */
    public static ExecutorService createThreadPool(ExcelConfig<?> config) {
        // 检查是否有自定义执行器
        if (config.getExecutor() != null) {
            // 如果用户提供了自定义执行器，直接使用，不进行额外配置
            return config.getExecutor();
        }
        
        // 根据系统资源和配置动态确定线程池参数
        // 获取配置的核心线程数，如果未配置则使用默认值
        int corePoolSize = config.getCorePoolSize() > 0 ? 
                config.getCorePoolSize() : DEFAULT_CORE_POOL_SIZE;
        
        // 获取配置的最大线程数，如果未配置则使用默认值
        int maximumPoolSize = config.getMaximumPoolSize() > 0 ? 
                config.getMaximumPoolSize() : DEFAULT_MAX_POOL_SIZE;
                
        // 动态调整最大线程数，不超过可用处理器数量
        // 避免创建过多线程导致系统资源争用
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        maximumPoolSize = Math.min(maximumPoolSize, availableProcessors);
        
        // 确保核心线程数不大于最大线程数
        // 防止配置错误导致的问题
        corePoolSize = Math.min(corePoolSize, maximumPoolSize);
        
        // 获取队列容量
        int queueCapacity = config.getQueueCapacity() > 0 ? 
                config.getQueueCapacity() : DEFAULT_QUEUE_CAPACITY;
        
        // 获取线程保活时间
        long keepAliveTime = config.getKeepAliveTime() > 0 ? 
                config.getKeepAliveTime() : DEFAULT_KEEP_ALIVE_TIME;
        
        // 创建线程工厂
        // 用于自定义线程的创建过程，如设置线程名称、优先级等
        ThreadFactory threadFactory = new ThreadFactory() {
            // 线程计数器，确保每个线程有唯一的编号
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            
            @Override
            public Thread newThread(Runnable r) {
                // 创建新线程，并设置线程名称
                Thread thread = new Thread(r, THREAD_NAME_PREFIX + threadNumber.getAndIncrement());
                // 设置为非守护线程，避免主线程结束时线程池中的任务被强制中断
                thread.setDaemon(false);
                // 设置线程优先级为普通优先级
                thread.setPriority(Thread.NORM_PRIORITY);
                return thread;
            }
        };
        
        // 创建拒绝策略
        // CallerRunsPolicy: 当线程池满时，在调用者线程中执行任务
        // 这有助于减缓新任务的提交速度，防止系统资源耗尽
        RejectedExecutionHandler rejectedHandler = new ThreadPoolExecutor.CallerRunsPolicy();
        
        // 创建线程池
        // 使用有界队列(LinkedBlockingQueue)，避免任务无限堆积导致内存溢出
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                corePoolSize,                  // 核心线程数
                maximumPoolSize,               // 最大线程数
                keepAliveTime, TimeUnit.SECONDS, // 线程空闲超时时间
                new LinkedBlockingQueue<>(queueCapacity), // 工作队列
                threadFactory,                 // 线程工厂
                rejectedHandler                // 拒绝策略
        );
        
        // 允许核心线程超时
        // 当设置为true时，核心线程在空闲时间超过keepAliveTime后也会被终止
        // 可以在负载较低时释放系统资源
        executor.allowCoreThreadTimeOut(true);
        
        log.info("创建线程池: 核心线程={}, 最大线程={}, 队列容量={}, 保活时间={}秒",
                corePoolSize, maximumPoolSize, queueCapacity, keepAliveTime);
                
        return executor;
    }
    
    /**
     * 安全关闭线程池
     * 尝试优雅地关闭线程池，等待任务完成或强制关闭
     * 
     * 关闭流程：
     * 1. 调用shutdown()拒绝新任务，等待已提交任务完成
     * 2. 等待最多60秒完成所有任务
     * 3. 如果60秒后仍有任务未完成，根据force参数决定是否调用shutdownNow()强制关闭
     * 
     * @param executor 要关闭的线程池
     * @param force 是否在等待超时后强制关闭
     */
    public static void shutdownThreadPool(ExecutorService executor, boolean force) {
        if (executor != null && !executor.isShutdown()) {
            try {
                // 拒绝新任务但允许已提交任务完成
                // shutdown()执行后，不会立即终止线程池，而是等待所有任务完成后才终止
                executor.shutdown();
                
                // 等待任务完成，最多等待60秒
                // awaitTermination会阻塞当前线程，直到以下情况之一发生：
                // 1. 所有任务完成，线程池关闭
                // 2. 超时发生
                // 3. 当前线程被中断
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    if (force) {
                        // 强制关闭线程池
                        // shutdownNow()会尝试停止所有正在执行的任务，并返回等待执行的任务列表
                        executor.shutdownNow();
                        log.warn("强制关闭线程池");
                    } else {
                        log.warn("线程池未能在60秒内完成所有任务");
                    }
                }
            } catch (InterruptedException e) {
                // 恢复中断状态
                // 当前线程被中断时，应恢复中断状态以便上层调用者能够处理
                Thread.currentThread().interrupt();
                // 强制关闭线程池
                executor.shutdownNow();
                log.error("关闭线程池时被中断", e);
            }
        }
    }

    /**
     * 监控线程池状态
     * 记录线程池当前状态并动态调整线程池大小
     * 
     * 动态调整策略：
     * 1. 当队列有等待任务且所有线程都在工作时，增加核心线程数（不超过最大线程数）
     * 2. 当队列为空且活动线程数不足线程池大小一半时，减少核心线程数（不低于1）
     * 
     * 这种自适应调整能够根据负载情况优化资源使用
     * 
     * @param executor 要监控的线程池执行器
     */
    public static void monitorThreadPool(ThreadPoolExecutor executor) {
        if (executor == null) return;
        
        // 获取线程池当前状态
        int activeCount = executor.getActiveCount();      // 当前活动线程数
        int poolSize = executor.getPoolSize();            // 当前线程池大小
        int corePoolSize = executor.getCorePoolSize();    // 核心线程数
        int maxPoolSize = executor.getMaximumPoolSize();  // 最大线程数
        int queueSize = executor.getQueue().size();       // 等待队列大小
        
        log.debug("线程池状态: 活动线程={}/{}, 核心线程={}, 最大线程={}, 队列任务={}",
                activeCount, poolSize, corePoolSize, maxPoolSize, queueSize);
                
        // 动态调整线程池大小
        if (queueSize > 0 && activeCount >= poolSize && poolSize < maxPoolSize) {
            // 负载较高情况：队列有等待任务，且所有线程都在工作
            // 增加核心线程数，但不超过最大线程数
            int newCoreSize = Math.min(corePoolSize + 1, maxPoolSize);
            executor.setCorePoolSize(newCoreSize);
            log.info("动态调整核心线程数: {} -> {}", corePoolSize, newCoreSize);
        } else if (queueSize == 0 && activeCount < poolSize / 2 && poolSize > 1) {
            // 负载较低情况：队列为空，且活动线程数不足线程池大小一半
            // 减少核心线程数，但不低于1
            int newCoreSize = Math.max(corePoolSize - 1, 1);
            executor.setCorePoolSize(newCoreSize);
            log.info("动态减少核心线程数: {} -> {}", corePoolSize, newCoreSize);
        }
    }
} 