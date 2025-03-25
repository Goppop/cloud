package com.study.tools.highExcelTools.config;

/**
 * 进度回调接口
 * 用于监控Excel处理进度
 */
@FunctionalInterface
public interface ProgressCallback {
    /**
     * 进度更新回调方法
     *
     * @param current 当前处理进度
     * @param total 总处理量
     * @param phase 当前处理阶段
     * @param message 附加信息
     */
    void onProgress(long current, long total, String phase, String message);
    
    /**
     * 简化版进度回调
     */
    default void onProgress(long current, long total, String phase) {
        onProgress(current, total, phase, null);
    }
    
    /**
     * 创建简单的日志进度回调
     */
    static ProgressCallback logProgress() {
        return (current, total, phase, message) -> {
            String progressInfo = message != null 
                ? String.format("%s - %d/%d - %s", phase, current, total, message)
                : String.format("%s - %d/%d", phase, current, total);
                
            System.out.println(progressInfo);
        };
    }
} 