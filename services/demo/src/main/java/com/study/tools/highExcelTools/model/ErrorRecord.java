package com.study.tools.highExcelTools.model;

import lombok.Data;
import lombok.Getter;

/**
 * 错误记录类
 * 用于存储各阶段的错误数据和错误信息
 * @param <T> 数据类型
 */
@Data
public class ErrorRecord<T> {
    // 处理阶段（如读取、转换、处理、写入等）
    private String phase;
    
    // 数据内容（可能为空）
    private T data;
    
    // 错误来源（如文件名、数据行等）
    private String source;
    
    // 行索引（如果适用）
    private long rowIndex = -1;
    
    // 错误消息
    private String message;
    
    // 异常对象（可选）
    private Throwable exception;
    
    // 时间戳
    private long timestamp;
    
    /**
     * 私有构造函数，通过静态工厂方法创建实例
     */
    private ErrorRecord() {
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * 创建读取阶段错误记录
     * @param phase 具体读取阶段
     * @param data 错误数据
     * @param source 来源（文件名）
     * @param rowIndex 行索引
     * @param message 错误消息
     * @param exception 异常对象
     * @return 错误记录
     */
    public static <T> ErrorRecord<T> readError(String phase, T data, String source, 
                                              long rowIndex, String message, Throwable exception) {
        ErrorRecord<T> record = new ErrorRecord<>();
        record.phase = "读取-" + phase;
        record.data = data;
        record.source = source;
        record.rowIndex = rowIndex;
        record.message = message;
        record.exception = exception;
        return record;
    }
    
    /**
     * 创建转换阶段错误记录
     * @param phase 具体转换阶段
     * @param data 错误数据
     * @param source 来源
     * @param rowIndex 行索引
     * @param message 错误消息
     * @param exception 异常对象
     * @return 错误记录
     */
    public static <T> ErrorRecord<T> convertError(String phase, T data, String source, 
                                                 long rowIndex, String message, Throwable exception) {
        ErrorRecord<T> record = new ErrorRecord<>();
        record.phase = "转换-" + phase;
        record.data = data;
        record.source = source;
        record.rowIndex = rowIndex;
        record.message = message;
        record.exception = exception;
        return record;
    }
    
    /**
     * 创建处理阶段错误记录
     * @param phase 具体处理阶段
     * @param data 错误数据
     * @param message 错误消息
     * @param exception 异常对象
     * @return 错误记录
     */
    public static <T> ErrorRecord<T> processError(String phase, T data, String message, Throwable exception) {
        ErrorRecord<T> record = new ErrorRecord<>();
        record.phase = "处理-" + phase;
        record.data = data;
        record.message = message;
        record.exception = exception;
        return record;
    }
    
    /**
     * 创建写入阶段错误记录
     * @param phase 具体写入阶段
     * @param data 错误数据
     * @param message 错误消息
     * @param exception 异常对象
     * @return 错误记录
     */
    public static <T> ErrorRecord<T> writeError(String phase, T data, String message, Throwable exception) {
        ErrorRecord<T> record = new ErrorRecord<>();
        record.phase = "写入-" + phase;
        record.data = data;
        record.message = message;
        record.exception = exception;
        return record;
    }
    
    /**
     * 获取简短描述
     * @return 错误的简短描述
     */
    public String getShortDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("阶段: ").append(phase);
        
        if (source != null && !source.isEmpty()) {
            sb.append(", 来源: ").append(source);
        }
        
        if (rowIndex >= 0) {
            sb.append(", 行号: ").append(rowIndex);
        }
        
        sb.append(", 信息: ").append(message);
        
        return sb.toString();
    }
} 