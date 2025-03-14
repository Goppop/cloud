package com.study.tools.excelNew.processor;

import com.study.tools.excelNew.MergeConfig;
import com.study.tools.excelNew.ProgressCallback;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据处理器 - 提供过滤和去重功能
 */
@Slf4j
public class DataProcessor<T> {
    private final MergeConfig<T> config;
    private final ProgressCallback progressCallback;
    private final String mergeId;
    
    public DataProcessor(MergeConfig<T> config, String mergeId) {
        this.config = config;
        this.progressCallback = config.getProgressCallback();
        this.mergeId = mergeId;
    }
    
    /**
     * 处理数据（过滤和去重）
     */
    public List<T> process(List<T> data) {
        int originalSize = data.size();
        List<T> result = data;
        
        // 应用过滤器
        if (config.getFilter() != null) {
            result = applyFilter(data);
            log.info("[{}] 应用过滤器后，数据行数: {} -> {}", mergeId, originalSize, result.size());
            reportProgress(40, 100, "过滤完成");
        }
        
        // 应用去重
        if (config.isEnableDeduplication() && config.getKeyExtractor() != null) {
            int beforeDedup = result.size();
            result = deduplicateData(result);
            log.info("[{}] 应用去重后，数据行数: {} -> {}", mergeId, beforeDedup, result.size());
            reportProgress(50, 100, "去重完成");
        }
        
        return result;
    }
    
    /**
     * 应用过滤器
     */
    private List<T> applyFilter(List<T> data) {
        List<T> result = new ArrayList<>();
        for (T item : data) {
            if (config.getFilter().test(item)) {
                result.add(item);
            }
        }
        return result;
    }
    
    /**
     * 数据去重
     */
    private List<T> deduplicateData(List<T> data) {
        Map<Object, T> uniqueMap = new HashMap<>(data.size());
        
        for (T item : data) {
            Object key = config.getKeyExtractor().apply(item);
            if (key == null) continue;
            
            if (uniqueMap.containsKey(key)) {
                if (config.getMergeFunction() != null) {
                    // 应用自定义合并逻辑
                    T existingItem = uniqueMap.get(key);
                    T mergedItem = config.getMergeFunction().apply(existingItem, item);
                    uniqueMap.put(key, mergedItem);
                }
                // 如果没有合并函数，保留第一个遇到的值
            } else {
                uniqueMap.put(key, item);
            }
        }
        
        return new ArrayList<>(uniqueMap.values());
    }
    
    private void reportProgress(long current, long total, String phase) {
        if (progressCallback != null) {
            progressCallback.onProgress(current, total, phase);
        }
    }
} 