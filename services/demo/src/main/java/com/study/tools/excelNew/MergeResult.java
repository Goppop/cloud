package com.study.tools.excelNew;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * 合并结果
 *
 * @param <T> 数据模型类型
 */
@Builder
@Data
public class MergeResult<T> {
    /** 是否成功 */
    private final boolean success;

    /** 错误消息 */
    private final String errorMessage;

    /** 合并后的数据 */
    private final List<T> data;

    /** 总行数 */
    private final int totalRows;

    /** 处理耗时（毫秒） */
    private final long timeMillis;

    /** 输出文件路径 */
    private final String outputFile;

    /**
     * 获取处理速度（行/秒）
     */
    public double getRowsPerSecond() {
        if (timeMillis <= 0) return 0;
        return (double) totalRows / (timeMillis / 1000.0);
    }
} 