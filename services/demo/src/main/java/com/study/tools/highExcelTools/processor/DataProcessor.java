package com.study.tools.highExcelTools.processor;

import com.study.tools.highExcelTools.config.ExcelConfig;
import com.study.tools.highExcelTools.config.ProgressCallback;
import com.study.tools.highExcelTools.model.ErrorCollector;
import com.study.tools.highExcelTools.model.ErrorRecord;
import com.study.tools.highExcelTools.util.MemoryMonitor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 数据处理器
 * 提供数据过滤和去重功能
 */
@Slf4j
public class DataProcessor<T> {
    private static final int PROCESS_BATCH_SIZE = 100_000; // 处理批次大小
    
    private final ExcelConfig<T> config;
    private final ProgressCallback progressCallback;
    private final String processId;
    
    // 异常数据收集器
    @Getter
    private final ErrorCollector<T> errorCollector;
    
    public DataProcessor(ExcelConfig<T> config, String processId) {
        this.config = config;
        this.progressCallback = config.getProgressCallback();
        this.processId = processId;
        
        // 初始化错误收集器
        this.errorCollector = new ErrorCollector<>(
                processId, 
                !config.isContinueOnError(),  // failFast模式与continueOnError相反
                config.getMaxErrorCount());
    }
    
    /**
     * 处理数据（过滤和去重）
     */
    public List<T> process(List<T> data) {
        if (data == null || data.isEmpty()) {
            return new ArrayList<>();
        }
        
        int originalSize = data.size();
        List<T> result = data;
        
        // 应用过滤器
        if (config.getFilter() != null) {
            try {
                result = applyFilter(data);
                log.info("[{}] 应用过滤器后，数据行数: {} -> {}", processId, originalSize, result.size());
                reportProgress(40, 100, "过滤完成");
            } catch (Exception e) {
                // 记录过滤操作异常
                if (config.isCollectErrors()) {
                    ErrorRecord<T> errorRecord = ErrorRecord.processError(
                            "过滤操作", null, "过滤数据异常: " + e.getMessage(), e);
                    boolean shouldStop = errorCollector.collectError(errorRecord);
                    
                    if (shouldStop && !config.isContinueOnError()) {
                        throw new RuntimeException("过滤数据异常，已停止处理", e);
                    }
                    
                    log.warn("[{}] 过滤数据异常，使用原始数据继续处理", processId, e);
                    // 使用原始数据继续处理
                    result = data;
                } else if (!config.isContinueOnError()) {
                    throw new RuntimeException("过滤数据异常", e);
                } else {
                    log.error("[{}] 过滤数据异常，使用原始数据继续处理", processId, e);
                    // 使用原始数据继续处理
                    result = data;
                }
            }
        }
        
        // 应用去重
        if (config.isEnableDeduplication() && config.getKeyExtractor() != null) {
            try {
                int beforeDedup = result.size();
                result = deduplicateData(result);
                log.info("[{}] 应用去重后，数据行数: {} -> {}", processId, beforeDedup, result.size());
                reportProgress(50, 100, "去重完成");
            } catch (Exception e) {
                // 记录去重操作异常
                if (config.isCollectErrors()) {
                    ErrorRecord<T> errorRecord = ErrorRecord.processError(
                            "去重操作", null, "去重数据异常: " + e.getMessage(), e);
                    boolean shouldStop = errorCollector.collectError(errorRecord);
                    
                    if (shouldStop && !config.isContinueOnError()) {
                        throw new RuntimeException("去重数据异常，已停止处理", e);
                    }
                    
                    log.warn("[{}] 去重数据异常，使用未去重数据继续处理", processId, e);
                } else if (!config.isContinueOnError()) {
                    throw new RuntimeException("去重数据异常", e);
                } else {
                    log.error("[{}] 去重数据异常，使用未去重数据继续处理", processId, e);
                }
            }
        }
        
        return result;
    }
    
    /**
     * 应用过滤器（并行处理）
     */
    private List<T> applyFilter(List<T> data) {
        log.info("[{}] 开始应用过滤器，数据量: {}", processId, data.size());
        
        // 对大数据集分批处理
        if (data.size() > PROCESS_BATCH_SIZE) {
            return applyFilterBatched(data);
        }
        
        try {
            // 并行过滤
            return data.parallelStream()
                    .filter(item -> {
                        try {
                            return config.getFilter().test(item);
                        } catch (Exception e) {
                            // 记录单条数据过滤异常
                            handleFilterError(item, e);
                            // 根据配置决定是否保留或跳过
                            return !config.isSkipInvalidData();
                        }
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("[{}] 过滤过程中发生异常", processId, e);
            
            if (config.isCollectErrors()) {
                ErrorRecord<T> errorRecord = ErrorRecord.processError(
                        "过滤操作", null, "批量过滤异常: " + e.getMessage(), e);
                errorCollector.collectError(errorRecord);
            }
            
            if (config.isContinueOnError()) {
                // 在失败时返回原始数据
                return new ArrayList<>(data);
            } else {
                throw e;
            }
        }
    }
    
    /**
     * 分批应用过滤器（防止OOM）
     */
    private List<T> applyFilterBatched(List<T> data) {
        List<T> result = new ArrayList<>();
        int totalSize = data.size();
        
        // 分批处理
        for (int i = 0; i < totalSize; i += PROCESS_BATCH_SIZE) {
            int endIndex = Math.min(i + PROCESS_BATCH_SIZE, totalSize);
            List<T> batch = data.subList(i, endIndex);
            
            try {
                // 并行过滤当前批次
                List<T> filteredBatch = batch.parallelStream()
                        .filter(item -> {
                            try {
                                return config.getFilter().test(item);
                            } catch (Exception e) {
                                // 记录单条数据过滤异常
                                handleFilterError(item, e);
                                // 根据配置决定是否保留或跳过
                                return !config.isSkipInvalidData();
                            }
                        })
                        .collect(Collectors.toList());
                        
                result.addAll(filteredBatch);
            } catch (Exception e) {
                log.error("[{}] 批次过滤异常: 批次{}-{}", processId, i, endIndex, e);
                
                if (config.isCollectErrors()) {
                    ErrorRecord<T> errorRecord = ErrorRecord.processError(
                            "批次过滤", null, String.format("批次过滤异常: 批次%d-%d: %s", i, endIndex, e.getMessage()), e);
                    boolean shouldStop = errorCollector.collectError(errorRecord);
                    
                    if (shouldStop && !config.isContinueOnError()) {
                        throw new RuntimeException("批次过滤异常，已停止处理", e);
                    }
                } else if (!config.isContinueOnError()) {
                    throw new RuntimeException("批次过滤异常", e);
                }
                
                // 在配置为继续处理时，添加未过滤的批次数据
                if (config.isContinueOnError()) {
                    result.addAll(batch);
                }
            }
            
            // 报告进度
            reportProgress(i + batch.size(), totalSize, "过滤数据");
            
            // 检查内存使用情况
            MemoryMonitor.checkForGC();
        }
        
        return result;
    }
    
    /**
     * 处理过滤错误
     */
    private void handleFilterError(T item, Exception e) {
        if (config.isCollectErrors()) {
            ErrorRecord<T> errorRecord = ErrorRecord.processError(
                    "过滤操作", item, "过滤数据项异常: " + e.getMessage(), e);
            boolean shouldStop = errorCollector.collectError(errorRecord);
            
            if (shouldStop && !config.isContinueOnError()) {
                throw new RuntimeException("过滤数据项异常，已停止处理", e);
            }
        }
        
        if (config.isLogErrors()) {
            log.warn("[{}] 过滤数据项异常: {}", processId, e.getMessage());
        }
    }
    
    /**
     * 数据去重（使用ConcurrentHashMap保证线程安全）
     */
    private List<T> deduplicateData(List<T> data) {
        log.info("[{}] 开始去重，数据量: {}", processId, data.size());
        
        // 对大数据集分批处理
        if (data.size() > PROCESS_BATCH_SIZE) {
            return deduplicateDataBatched(data);
        }
        
        ConcurrentHashMap<Object, T> uniqueMap = new ConcurrentHashMap<>(data.size());
        
        // 并行填充去重Map
        data.parallelStream().forEach(item -> {
            try {
                Object key = config.getKeyExtractor().apply(item);
                if (key == null) return;
                
                uniqueMap.compute(key, (k, existingItem) -> {
                    if (existingItem == null) {
                        return item;
                    } else if (config.getMergeFunction() != null) {
                        try {
                            // 应用自定义合并逻辑
                            return config.getMergeFunction().apply(existingItem, item);
                        } catch (Exception e) {
                            // 记录合并异常
                            handleMergeError(existingItem, item, e);
                            return existingItem; // 失败时保留原值
                        }
                    } else {
                        // 如果没有合并函数，保留第一个遇到的值
                        return existingItem;
                    }
                });
            } catch (Exception e) {
                // 记录提取键异常
                handleKeyExtractionError(item, e);
            }
        });
        
        return new ArrayList<>(uniqueMap.values());
    }
    
    /**
     * 分批去重数据（防止OOM）
     */
    private List<T> deduplicateDataBatched(List<T> data) {
        ConcurrentHashMap<Object, T> uniqueMap = new ConcurrentHashMap<>();
        int totalSize = data.size();
        
        // 分批处理
        for (int i = 0; i < totalSize; i += PROCESS_BATCH_SIZE) {
            int endIndex = Math.min(i + PROCESS_BATCH_SIZE, totalSize);
            List<T> batch = data.subList(i, endIndex);
            
            try {
                // 处理当前批次
                for (T item : batch) {
                    try {
                        Object key = config.getKeyExtractor().apply(item);
                        if (key == null) continue;
                        
                        uniqueMap.compute(key, (k, existingItem) -> {
                            if (existingItem == null) {
                                return item;
                            } else if (config.getMergeFunction() != null) {
                                try {
                                    // 应用自定义合并逻辑
                                    return config.getMergeFunction().apply(existingItem, item);
                                } catch (Exception e) {
                                    // 记录合并异常
                                    handleMergeError(existingItem, item, e);
                                    return existingItem; // 失败时保留原值
                                }
                            } else {
                                // 如果没有合并函数，保留第一个遇到的值
                                return existingItem;
                            }
                        });
                    } catch (Exception e) {
                        // 记录提取键异常
                        handleKeyExtractionError(item, e);
                    }
                }
            } catch (Exception e) {
                log.error("[{}] 批次去重异常: 批次{}-{}", processId, i, endIndex, e);
                
                if (config.isCollectErrors()) {
                    ErrorRecord<T> errorRecord = ErrorRecord.processError(
                            "批次去重", null, String.format("批次去重异常: 批次%d-%d: %s", i, endIndex, e.getMessage()), e);
                    boolean shouldStop = errorCollector.collectError(errorRecord);
                    
                    if (shouldStop && !config.isContinueOnError()) {
                        throw new RuntimeException("批次去重异常，已停止处理", e);
                    }
                } else if (!config.isContinueOnError()) {
                    throw new RuntimeException("批次去重异常", e);
                }
            }
            
            // 报告进度
            reportProgress(i + batch.size(), totalSize, "去重数据");
            
            // 检查内存使用情况
            MemoryMonitor.checkForGC();
        }
        
        return new ArrayList<>(uniqueMap.values());
    }
    
    /**
     * 处理键提取错误
     */
    private void handleKeyExtractionError(T item, Exception e) {
        if (config.isCollectErrors()) {
            ErrorRecord<T> errorRecord = ErrorRecord.processError(
                    "键提取", item, "提取键异常: " + e.getMessage(), e);
            boolean shouldStop = errorCollector.collectError(errorRecord);
            
            if (shouldStop && !config.isContinueOnError()) {
                throw new RuntimeException("提取键异常，已停止处理", e);
            }
        }
        
        if (config.isLogErrors()) {
            log.warn("[{}] 提取键异常: {}", processId, e.getMessage());
        }
    }
    
    /**
     * 处理合并错误
     */
    private void handleMergeError(T item1, T item2, Exception e) {
        if (config.isCollectErrors()) {
            ErrorRecord<T> errorRecord = ErrorRecord.processError(
                    "数据合并", item2, "合并数据异常: " + e.getMessage(), e);
            boolean shouldStop = errorCollector.collectError(errorRecord);
            
            if (shouldStop && !config.isContinueOnError()) {
                throw new RuntimeException("合并数据异常，已停止处理", e);
            }
        }
        
        if (config.isLogErrors()) {
            log.warn("[{}] 合并数据异常: {}", processId, e.getMessage());
        }
    }
    
    /**
     * 报告进度
     */
    private void reportProgress(long current, long total, String phase) {
        if (progressCallback != null) {
            progressCallback.onProgress(current, total, phase);
        }
    }
} 