package com.study.tools.highExcelTools.config;

import com.study.tools.highExcelTools.model.ErrorRecord;
import lombok.extern.slf4j.Slf4j;

/**
 * 错误回调接口
 * 用于在处理过程中遇到错误时的自定义处理
 * @param <T> 数据类型
 */
@FunctionalInterface
public interface ErrorCallback<T> {
    
    /**
     * 处理错误
     * @param errorRecord 错误记录
     * @param currentErrorCount 当前错误计数
     * @param phase 处理阶段
     * @return 是否继续处理（返回false则终止处理）
     */
    boolean onError(ErrorRecord<T> errorRecord, int currentErrorCount, String phase);
    
    /**
     * 创建一个简单的日志记录错误回调
     * 记录错误并始终继续处理
     */
    @Slf4j
    class LogErrorCallback<T> implements ErrorCallback<T> {
        @Override
        public boolean onError(ErrorRecord<T> errorRecord, int currentErrorCount, String phase) {
            log.warn("处理错误: {} (当前错误计数: {})", errorRecord.getShortDescription(), currentErrorCount);
            return true; // 始终继续处理
        }
    }
    
    /**
     * 创建一个简单的默认错误日志回调
     */
    static <T> ErrorCallback<T> logErrorCallback() {
        return new LogErrorCallback<>();
    }
    
    /**
     * 创建一个带错误数量限制的回调
     * 当错误数量超过限制时停止处理
     * @param errorLimit 错误数量上限
     */
    static <T> ErrorCallback<T> limitedErrorCallback(int errorLimit) {
        return (errorRecord, currentErrorCount, phase) -> {
            boolean shouldContinue = currentErrorCount < errorLimit;
            if (!shouldContinue) {
                LogErrorCallback.log.error("错误数量({})超过限制({}), 停止处理", 
                        currentErrorCount, errorLimit);
            } else {
                LogErrorCallback.log.warn("处理错误: {} (错误计数: {}/{})", 
                        errorRecord.getShortDescription(), currentErrorCount, errorLimit);
            }
            return shouldContinue;
        };
    }
} 