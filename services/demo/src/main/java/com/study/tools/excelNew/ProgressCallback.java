package com.study.tools.excelNew;

/**
 * 进度回调接口，用于监控处理进度
 */
@FunctionalInterface
public interface ProgressCallback {
    /**
     * 进度更新回调方法
     *
     * @param current 当前处理进度
     * @param total 总处理量
     * @param phase 当前处理阶段
     */
    void onProgress(long current, long total, String phase);
} 