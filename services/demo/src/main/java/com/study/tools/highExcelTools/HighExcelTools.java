package com.study.tools.highExcelTools;

import com.study.tools.highExcelTools.config.ErrorCallback;
import com.study.tools.highExcelTools.config.ExcelConfig;
import com.study.tools.highExcelTools.config.ProgressCallback;
import com.study.tools.highExcelTools.core.ExcelReader;
import com.study.tools.highExcelTools.core.ExcelWriter;
import com.study.tools.highExcelTools.model.ErrorCollector;
import com.study.tools.highExcelTools.model.ErrorRecord;
import com.study.tools.highExcelTools.model.ExcelResult;
import com.study.tools.highExcelTools.processor.DataProcessor;
import com.study.tools.highExcelTools.util.MemoryMonitor;
import com.study.tools.highExcelTools.util.ThreadPoolManager;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 高性能Excel工具类
 * 提供Excel文件的高效导入、导出、合并、转换等功能
 */
@Slf4j
public class HighExcelTools {
    /**
     * 合并多个Excel文件
     * 
     * @param config 合并配置
     * @param <T> 数据模型类型
     * @return 合并结果
     */
    public <T> ExcelResult<T> mergeExcel(ExcelConfig<T> config) {
        // 生成操作ID，用于跟踪日志和关联各处理组件
        String operationId = UUID.randomUUID().toString().substring(0, 8);
        long startTime = System.currentTimeMillis();
        
        log.info("[{}] 开始合并Excel文件，文件数量: {}", operationId, config.getSourceFiles().size());
        
        // 验证配置
        List<String> validationErrors = validateConfig(config);
        if (!validationErrors.isEmpty()) {
            log.error("[{}] 配置验证失败: {}", operationId, validationErrors);
            return ExcelResult.failure("MERGE", "配置验证失败: " + String.join(", ", validationErrors), null);
        }
        
        // 线程池声明，在finally块中确保关闭
        ExecutorService executor = null;
        // 是否需要关闭线程池的标志
        boolean needShutdownExecutor = false;
        
        try {
            // 创建目标目录
            Path outputPath = Paths.get(config.getTargetFile());
            Files.createDirectories(outputPath.getParent());
            
            // 创建线程池
            // 使用ThreadPoolManager管理线程池，根据配置动态调整线程池参数
            // 包括核心线程数、最大线程数、队列容量等
            executor = ThreadPoolManager.createThreadPool(config);
            // 记录是否需要关闭线程池，由配置决定
            // 外部提供的线程池可能需要在外部管理其生命周期
            needShutdownExecutor = config.isShutdownExecutor();
            
            // 监控线程池状态
            // 创建后台守护线程定期监控线程池状态并动态调整线程池大小
            if (executor instanceof ThreadPoolExecutor) {
                final ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executor;
                // 创建守护线程，每5秒监控一次线程池状态
                Thread monitorThread = new Thread(() -> {
                    try {
                        // 当线程未被中断且线程池未关闭时，持续监控
                        while (!Thread.currentThread().isInterrupted() && !threadPoolExecutor.isShutdown()) {
                            // 调用ThreadPoolManager监控线程池，可能动态调整线程池大小
                            ThreadPoolManager.monitorThreadPool(threadPoolExecutor);
                            // 每5秒检查一次
                            Thread.sleep(5000);
                        }
                    } catch (InterruptedException e) {
                        // 恢复中断状态
                        Thread.currentThread().interrupt();
                    }
                });
                // 设置为守护线程，随主线程结束而结束
                monitorThread.setDaemon(true);
                monitorThread.start();
            }
            
            // 报告进度: 开始阶段
            reportProgress(config.getProgressCallback(), 0, 100, "初始化", "准备合并文件");
            
            // 创建读取器
            ExcelReader<T> reader = new ExcelReader<>(config, executor, operationId);
            
            // 创建数据处理器
            DataProcessor<T> processor = new DataProcessor<>(config, operationId);
            
            // 创建写入器
            ExcelWriter<T> writer = new ExcelWriter<>(config, operationId);
            
            // 流式处理数据
            // 使用线程安全的集合存储处理结果
            final List<T> processedData = Collections.synchronizedList(new ArrayList<>());
            final AtomicInteger batchCount = new AtomicInteger(0);
            
            BiConsumer<List<T>, Boolean> processBatch = (batch, isLastBatch) -> {
                try {
                    if (batch.isEmpty()) return;
                    
                    // 处理批次数据(过滤、去重)
                    List<T> processed = processor.process(batch);
                    
                    // 添加到最终结果
                    if (!processed.isEmpty()) {
                        processedData.addAll(processed);
                    }
                    
                    // 报告进度
                    int batchNum = batchCount.incrementAndGet();
                    if (batchNum % 10 == 0) {
                        log.info("[{}] 已处理 {} 个数据批次，当前内存: {}", 
                                operationId, batchNum, MemoryMonitor.getMemoryInfo());
                    }
                    
                    // 如果是最后一批，写入结果
                    if (isLastBatch && !processedData.isEmpty()) {
                        writer.write(processedData);
                    }
                } catch (Exception e) {
                    log.error("[{}] 处理数据批次时发生错误", operationId, e);
                    throw new RuntimeException("处理数据批次时发生错误", e);
                }
            };
            
            // 使用流式读取并处理
            Consumer<List<T>> batchHandler = batch -> processBatch.accept(batch, false);
            reader.readFilesWithCallback(config.getSourceFiles(), config.getModelClass(), batchHandler);
            
            // 处理最后一批
            processBatch.accept(new ArrayList<>(), true);
            
            // 完成写入
            writer.finish();
            
            // 报告进度: 完成阶段
            reportProgress(config.getProgressCallback(), 100, 100, "完成", "文件合并完成");
            
            // 计算耗时
            long timeMillis = System.currentTimeMillis() - startTime;
            
            log.info("[{}] 合并完成，总行数: {}, 耗时: {}ms", 
                    operationId, processedData.size(), timeMillis);
            
            return ExcelResult.success(
                    "MERGE",
                    processedData,
                    processedData.size(),
                    timeMillis,
                    config.getTargetFile()
            );
            
        } catch (Exception e) {
            log.error("[{}] 合并过程中发生错误", operationId, e);
            return ExcelResult.failure("MERGE", "合并失败: " + e.getMessage(), e);
        } finally {
            // 关闭线程池
            // 只有在需要关闭的情况下才关闭线程池
            // needShutdownExecutor为true说明线程池是内部创建的，需要由本类负责关闭
            if (needShutdownExecutor && executor != null) {
                // 使用ThreadPoolManager安全关闭线程池
                // 传入true参数表示如果等待超时，强制关闭线程池
                ThreadPoolManager.shutdownThreadPool(executor, true);
            }
        }
    }
    
    /**
     * 快速合并Excel文件
     * 
     * @param sourceFiles 源文件列表
     * @param targetFile 目标文件
     * @param modelClass 数据模型类
     * @param <T> 数据模型类型
     * @return 合并结果
     */
    public <T> ExcelResult<T> quickMerge(List<String> sourceFiles, String targetFile, Class<T> modelClass) {
        ExcelConfig<T> config = ExcelConfig.simpleConfig(sourceFiles, targetFile, modelClass);
        return mergeExcel(config);
    }
    
    /**
     * 快速合并Excel文件（带去重）
     * 
     * @param sourceFiles 源文件列表
     * @param targetFile 目标文件
     * @param modelClass 数据模型类
     * @param keyExtractor 主键提取器(用于去重)
     * @param <T> 数据模型类型
     * @return 合并结果
     */
    public <T> ExcelResult<T> quickMergeWithDedup(
            List<String> sourceFiles, 
            String targetFile, 
            Class<T> modelClass,
            java.util.function.Function<T, Object> keyExtractor) {
        
        ExcelConfig<T> config = ExcelConfig.dedupConfig(sourceFiles, targetFile, modelClass, keyExtractor);
        return mergeExcel(config);
    }
    
    /**
     * 导出Excel文件
     * 
     * @param data 数据
     * @param targetFile 目标文件
     * @param modelClass 数据模型类
     * @param <T> 数据模型类型
     * @return 导出结果
     */
    public <T> ExcelResult<T> exportExcel(List<T> data, String targetFile, Class<T> modelClass) {
        return exportExcel(data, targetFile, modelClass, null);
    }
    
    /**
     * 导出Excel文件（带进度回调）
     * 
     * @param data 数据
     * @param targetFile 目标文件
     * @param modelClass 数据模型类
     * @param progressCallback 进度回调
     * @param <T> 数据模型类型
     * @return 导出结果
     */
    public <T> ExcelResult<T> exportExcel(
            List<T> data,
            String targetFile,
            Class<T> modelClass,
            ProgressCallback progressCallback) {
        
        // 创建配置
        ExcelConfig<T> config = ExcelConfig.<T>builder()
                .targetFile(targetFile)
                .modelClass(modelClass)
                .progressCallback(progressCallback)
                .build();
                
        // 生成操作ID
        String operationId = UUID.randomUUID().toString().substring(0, 8);
        long startTime = System.currentTimeMillis();
        
        log.info("[{}] 开始导出Excel文件: {}, 数据行数: {}", 
                operationId, targetFile, data.size());
        
        try {
            // 创建目标目录
            Path outputPath = Paths.get(targetFile);
            Files.createDirectories(outputPath.getParent());
            
            // 创建写入器
            ExcelWriter<T> writer = new ExcelWriter<>(config, operationId);
            writer.init();
            
            // 写入数据
            writer.write(data);
            
            // 完成写入
            writer.finish();
            
            // 计算耗时
            long timeMillis = System.currentTimeMillis() - startTime;
            double rowsPerSecond = writer.getWriteSpeed();
            
            log.info("[{}] 导出完成，总行数: {}, 耗时: {}ms, 速度: {:.2f}行/秒", 
                    operationId, data.size(), timeMillis, rowsPerSecond);
            
            return ExcelResult.success(
                    "EXPORT",
                    data,
                    data.size(),
                    timeMillis,
                    targetFile
            );
            
        } catch (Exception e) {
            log.error("[{}] 导出过程中发生错误", operationId, e);
            return ExcelResult.failure("EXPORT", "导出失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 验证合并配置
     */
    private <T> List<String> validateConfig(ExcelConfig<T> config) {
        List<String> errors = new ArrayList<>();
        
        if (config.getSourceFiles() == null || config.getSourceFiles().isEmpty()) {
            errors.add("源文件列表不能为空");
        } else {
            for (String filePath : config.getSourceFiles()) {
                Path path = Paths.get(filePath);
                if (!Files.exists(path)) {
                    errors.add("文件不存在: " + filePath);
                } else if (!Files.isRegularFile(path)) {
                    errors.add("不是有效的文件: " + filePath);
                }
            }
        }
        
        if (config.getTargetFile() == null || config.getTargetFile().trim().isEmpty()) {
            errors.add("目标文件路径不能为空");
        }
        
        if (config.getModelClass() == null) {
            errors.add("数据模型类不能为空");
        }
        
        if (config.isEnableDeduplication() && config.getKeyExtractor() == null) {
            errors.add("启用去重时必须提供键提取器");
        }
        
        return errors;
    }
    
    /**
     * 报告进度
     */
    private void reportProgress(ProgressCallback callback, long current, long total, String phase, String message) {
        if (callback != null) {
            callback.onProgress(current, total, phase, message);
        }
    }
} 