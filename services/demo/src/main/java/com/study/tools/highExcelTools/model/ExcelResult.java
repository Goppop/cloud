package com.study.tools.highExcelTools.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * Excel操作结果类
 * 包含操作结果和统计信息
 * 
 * @param <T> 数据模型类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExcelResult<T> {
    /**
     * 操作是否成功
     */
    private boolean success;
    
    /**
     * 操作类型
     */
    private String operationType;
    
    /**
     * 处理的数据
     */
    private List<T> data;
    
    /**
     * 总行数
     */
    private int totalRows;
    
    /**
     * 处理耗时（毫秒）
     */
    private long timeMillis;
    
    /**
     * 文件信息
     */
    private List<String> sourceFiles;
    private String outputFile;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    private Throwable exception;
    
    /**
     * 异常数据信息
     */
    private List<ErrorRecord<T>> errorRecords;
    private int errorCount;
    private String errorReport;
    private String errorDataFile;  // 错误数据导出文件
    private boolean hasErrors;     // 是否包含异常数据
    
    /**
     * 获取处理速度（行/秒）
     */
    public double getRowsPerSecond() {
        if (timeMillis <= 0) {
            return 0.0;
        }
        return (totalRows * 1000.0) / timeMillis;
    }
    
    /**
     * 创建成功结果
     */
    public static <T> ExcelResult<T> success(
            String operationType,
            List<T> data,
            int totalRows,
            long timeMillis,
            String outputFile) {
        return ExcelResult.<T>builder()
                .success(true)
                .operationType(operationType)
                .data(data)
                .totalRows(totalRows)
                .timeMillis(timeMillis)
                .outputFile(outputFile)
                .hasErrors(false)
                .build();
    }
    
    /**
     * 创建成功结果(包含异常数据)
     */
    public static <T> ExcelResult<T> successWithErrors(
            String operationType,
            List<T> data,
            int totalRows,
            long timeMillis,
            String outputFile,
            List<ErrorRecord<T>> errorRecords,
            String errorReport,
            String errorDataFile) {
        return ExcelResult.<T>builder()
                .success(true)
                .operationType(operationType)
                .data(data)
                .totalRows(totalRows)
                .timeMillis(timeMillis)
                .outputFile(outputFile)
                .errorRecords(errorRecords)
                .errorCount(errorRecords != null ? errorRecords.size() : 0)
                .errorReport(errorReport)
                .errorDataFile(errorDataFile)
                .hasErrors(errorRecords != null && !errorRecords.isEmpty())
                .build();
    }
    
    /**
     * 创建失败结果
     */
    public static <T> ExcelResult<T> failure(
            String operationType,
            String errorMessage,
            Throwable exception) {
        return ExcelResult.<T>builder()
                .success(false)
                .operationType(operationType)
                .errorMessage(errorMessage)
                .exception(exception)
                .hasErrors(true)
                .build();
    }
    
    /**
     * 创建失败结果(包含异常数据)
     */
    public static <T> ExcelResult<T> failureWithErrors(
            String operationType,
            String errorMessage,
            Throwable exception,
            List<ErrorRecord<T>> errorRecords,
            String errorReport) {
        return ExcelResult.<T>builder()
                .success(false)
                .operationType(operationType)
                .errorMessage(errorMessage)
                .exception(exception)
                .errorRecords(errorRecords)
                .errorCount(errorRecords != null ? errorRecords.size() : 0)
                .errorReport(errorReport)
                .hasErrors(true)
                .build();
    }
} 