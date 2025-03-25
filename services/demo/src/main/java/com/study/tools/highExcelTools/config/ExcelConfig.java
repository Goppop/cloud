package com.study.tools.highExcelTools.config;

import com.alibaba.excel.write.handler.WriteHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Excel配置类
 * 包含Excel导入导出所有可配置选项
 * 
 * @param <T> 数据模型类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExcelConfig<T> {
    /**
     * 文件相关配置
     */
    private List<String> sourceFiles;     // 源文件列表
    private String targetFile;            // 目标文件
    private Class<T> modelClass;          // 数据模型类
    
    /**
     * 头部配置
     */
    private List<List<String>> headList;  // 自定义表头
    private Map<Integer, String> headMap; // 列索引到表头的映射
    
    /**
     * 数据处理配置
     */
    private Predicate<T> filter;                // 过滤器
    private boolean enableDeduplication;        // 是否启用去重
    private Function<T, Object> keyExtractor;   // 主键提取器(用于去重)
    private BiFunction<T, T, T> mergeFunction;  // 数据合并函数
    
    /**
     * 异常处理配置
     */
    @Builder.Default
    private boolean continueOnError = true;     // 遇到异常时是否继续处理
    @Builder.Default
    private int maxErrorCount = -1;             // 最大错误数量(-1表示不限制)
    @Builder.Default
    private boolean collectErrors = true;       // 是否收集错误信息
    @Builder.Default
    private boolean logErrors = true;           // 是否记录错误日志
    @Builder.Default
    private boolean skipInvalidData = true;     // 是否跳过无效数据
    @Builder.Default
    private boolean exportErrorData = false;    // 是否导出错误数据到单独文件
    private String errorDataFile;               // 错误数据文件路径(未指定时自动生成)
    private ErrorCallback errorCallback;        // 错误回调函数
    
    /**
     * 性能配置
     */
    @Builder.Default
    private int batchSize = 5000;          // 批处理大小
    @Builder.Default 
    private int bufferSize = 8192;         // 缓冲区大小
    @Builder.Default
    private boolean useInMemory = true;    // 是否使用内存模式
    @Builder.Default
    private boolean autoCloseStream = true;// 是否自动关闭流
    
    /**
     * 线程配置
     */
    private ExecutorService executor;      // 自定义执行器
    @Builder.Default
    private boolean shutdownExecutor = true; // 是否关闭执行器
    @Builder.Default
    private int corePoolSize = 2;          // 核心线程数
    @Builder.Default
    private int maximumPoolSize = 4;       // 最大线程数
    @Builder.Default
    private int queueCapacity = 100;       // 队列容量
    @Builder.Default
    private long keepAliveTime = 60L;      // 线程保活时间(秒)
    @Builder.Default
    private int maxConcurrentFiles = 3;    // 最大并发文件数
    
    /**
     * 回调接口
     */
    private ProgressCallback progressCallback; // 进度回调
    
    /**
     * 动态创建简单配置
     * @param sourceFiles 源文件
     * @param targetFile 目标文件
     * @param modelClass 模型类
     * @return 配置对象
     */
    public static <T> ExcelConfig<T> simpleConfig(
            List<String> sourceFiles,
            String targetFile,
            Class<T> modelClass) {
        return ExcelConfig.<T>builder()
                .sourceFiles(sourceFiles)
                .targetFile(targetFile)
                .modelClass(modelClass)
                .build();
    }
    
    /**
     * 创建带去重的配置
     * @param sourceFiles 源文件
     * @param targetFile 目标文件
     * @param modelClass 模型类
     * @param keyExtractor 主键提取器
     * @return 配置对象
     */
    public static <T> ExcelConfig<T> dedupConfig(
            List<String> sourceFiles,
            String targetFile,
            Class<T> modelClass,
            Function<T, Object> keyExtractor) {
        return ExcelConfig.<T>builder()
                .sourceFiles(sourceFiles)
                .targetFile(targetFile)
                .modelClass(modelClass)
                .enableDeduplication(true)
                .keyExtractor(keyExtractor)
                .build();
    }
    
    /**
     * 创建安全处理配置
     * 启用异常数据跳过和记录
     */
    public static <T> ExcelConfig<T> safeConfig(
            List<String> sourceFiles,
            String targetFile,
            Class<T> modelClass) {
        return ExcelConfig.<T>builder()
                .sourceFiles(sourceFiles)
                .targetFile(targetFile)
                .modelClass(modelClass)
                .continueOnError(true)
                .collectErrors(true)
                .skipInvalidData(true)
                // 配置错误数据导出文件
                .exportErrorData(true)
                .errorDataFile(targetFile.replace(".xlsx", "_errors.xlsx"))
                .build();
    }
} 