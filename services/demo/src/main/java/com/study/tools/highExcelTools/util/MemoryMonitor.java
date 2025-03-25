package com.study.tools.highExcelTools.util;

import lombok.extern.slf4j.Slf4j;

/**
 * 内存监控工具类
 * 用于跟踪内存使用情况并在必要时触发垃圾回收
 */
@Slf4j
public class MemoryMonitor {
    private static final float DEFAULT_GC_THRESHOLD = 0.7f; // 70%内存占用触发GC
    private static final long MB = 1024 * 1024;
    
    // 上次GC触发时间
    private static long lastGcTime = 0;
    // 最短GC间隔(毫秒)
    private static final long MIN_GC_INTERVAL = 10_000; // 10秒
    
    /**
     * 获取当前内存使用率
     * @return 内存使用率(0.0-1.0)
     */
    public static float getMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory(); // 当前JVM总内存
        long freeMemory = runtime.freeMemory();   // 当前JVM空闲内存
        long usedMemory = totalMemory - freeMemory;
        
        return (float) usedMemory / totalMemory;
    }
    
    /**
     * 获取内存使用情况
     * @return 内存使用信息字符串
     */
    public static String getMemoryInfo() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / MB;    // 最大可用内存
        long totalMemory = runtime.totalMemory() / MB; // 当前分配内存
        long freeMemory = runtime.freeMemory() / MB;   // 当前空闲内存
        long usedMemory = totalMemory - freeMemory;     // 当前使用内存
        
        return String.format("内存使用: %dMB/%dMB (%.1f%%), 最大可用: %dMB", 
            usedMemory, totalMemory, usedMemory * 100.0 / totalMemory, maxMemory);
    }
    
    /**
     * 检查是否需要进行GC
     * 如果内存使用率超过阈值，则触发GC
     * @return 是否触发了GC
     */
    public static boolean checkForGC() {
        return checkForGC(DEFAULT_GC_THRESHOLD);
    }
    
    /**
     * 检查是否需要进行GC
     * @param threshold 内存使用率阈值
     * @return 是否触发了GC
     */
    public static boolean checkForGC(float threshold) {
        float usage = getMemoryUsage();
        long currentTime = System.currentTimeMillis();
        
        // 检查内存使用率和GC时间间隔
        if (usage > threshold && (currentTime - lastGcTime) > MIN_GC_INTERVAL) {
            log.info("内存使用率达到 {:.1f}%，触发GC", usage * 100);
            
            // 记录GC前内存
            long beforeGc = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            
            // 触发GC
            System.gc();
            
            // 更新最后GC时间
            lastGcTime = System.currentTimeMillis();
            
            // 计算GC效果
            long afterGc = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long freedMemory = (beforeGc - afterGc) / MB;
            
            log.info("GC完成，释放了 {}MB 内存，当前使用率: {:.1f}%", 
                    freedMemory, getMemoryUsage() * 100);
                    
            return true;
        }
        
        return false;
    }
    
    /**
     * 强制进行GC
     */
    public static void forceGC() {
        long beforeGc = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        log.info("强制GC，当前内存使用: {}MB", beforeGc / MB);
        
        System.gc();
        
        lastGcTime = System.currentTimeMillis();
        long afterGc = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long freedMemory = (beforeGc - afterGc) / MB;
        
        log.info("强制GC完成，释放了 {}MB 内存", freedMemory);
    }
} 