package com.study.tools.highExcelTools.model;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 错误收集器
 * 用于收集处理过程中的错误信息，支持线程安全
 * @param <T> 数据类型
 */
@Slf4j
public class ErrorCollector<T> {

    private final String processId;
    private final boolean failFast;
    private final int maxErrorCount;
    
    @Getter
    private final List<ErrorRecord<T>> errorRecords = Collections.synchronizedList(new ArrayList<>());
    
    // 记录各阶段错误数量
    private final Map<String, AtomicInteger> phaseErrorCounts = new ConcurrentHashMap<>();
    
    /**
     * 构造函数
     * @param processId 处理ID
     * @param failFast 是否快速失败模式
     * @param maxErrorCount 最大错误数量
     */
    public ErrorCollector(String processId, boolean failFast, int maxErrorCount) {
        this.processId = processId;
        this.failFast = failFast;
        this.maxErrorCount = maxErrorCount > 0 ? maxErrorCount : Integer.MAX_VALUE;
    }
    
    /**
     * 收集错误记录
     * @param errorRecord 错误记录
     * @return 是否应该停止处理
     */
    public boolean collectError(ErrorRecord<T> errorRecord) {
        if (errorRecord == null) return false;
        
        // 检查错误数是否超过限制
        boolean shouldStop = false;
        
        synchronized (this) {
            // 添加错误记录
            errorRecords.add(errorRecord);
            
            // 更新阶段错误计数
            phaseErrorCounts.computeIfAbsent(errorRecord.getPhase(), k -> new AtomicInteger(0))
                      .incrementAndGet();
            
            // 检查是否应该停止处理
            shouldStop = failFast || errorRecords.size() >= maxErrorCount;
        }
        
        // 记录错误日志
        if (shouldStop) {
            log.error("[{}] 错误数量达到上限({}), 类型: {}, 信息: {}", 
                    processId, maxErrorCount, errorRecord.getPhase(), errorRecord.getMessage());
        } else {
            log.warn("[{}] 收集到错误, 类型: {}, 信息: {}", 
                    processId, errorRecord.getPhase(), errorRecord.getMessage());
        }
        
        return shouldStop;
    }
    
    /**
     * 获取错误总数
     */
    public int getErrorCount() {
        return errorRecords.size();
    }
    
    /**
     * 获取特定阶段的错误数
     * @param phase 处理阶段
     */
    public int getPhaseErrorCount(String phase) {
        AtomicInteger count = phaseErrorCounts.get(phase);
        return count != null ? count.get() : 0;
    }
    
    /**
     * 是否有错误
     */
    public boolean hasErrors() {
        return !errorRecords.isEmpty();
    }
    
    /**
     * 生成错误报告
     * @return 错误报告字符串
     */
    public String generateErrorReport() {
        if (errorRecords.isEmpty()) {
            return "没有错误";
        }
        
        StringBuilder report = new StringBuilder();
        report.append("错误报告 [处理ID: ").append(processId).append("]\n");
        report.append("总错误数: ").append(errorRecords.size()).append("\n\n");
        
        // 按阶段统计错误
        report.append("各阶段错误数:\n");
        for (Map.Entry<String, AtomicInteger> entry : phaseErrorCounts.entrySet()) {
            report.append("- ").append(entry.getKey()).append(": ").append(entry.getValue().get()).append("\n");
        }
        report.append("\n");
        
        // 统计错误类型
        Map<String, Long> errorTypes = errorRecords.stream()
                .collect(Collectors.groupingBy(ErrorRecord::getPhase, Collectors.counting()));
        
        report.append("错误类型统计:\n");
        errorTypes.forEach((type, count) -> 
                report.append("- ").append(type).append(": ").append(count).append("\n"));
        report.append("\n");
        
        // 列出前10条错误详情
        report.append("错误样本(前10条):\n");
        int sampleSize = Math.min(10, errorRecords.size());
        for (int i = 0; i < sampleSize; i++) {
            ErrorRecord<T> record = errorRecords.get(i);
            report.append(i + 1).append(". ")
                  .append("阶段: ").append(record.getPhase())
                  .append(", 来源: ").append(record.getSource())
                  .append(", 行号: ").append(record.getRowIndex() >= 0 ? record.getRowIndex() : "未知")
                  .append("\n   信息: ").append(record.getMessage())
                  .append("\n");
        }
        
        if (errorRecords.size() > 10) {
            report.append("... 更多错误未显示 ...\n");
        }
        
        return report.toString();
    }
    
    /**
     * 清空错误记录
     */
    public void clear() {
        errorRecords.clear();
        phaseErrorCounts.clear();
    }
} 