package com.study.tools.excelNew;

import com.study.tools.excelNew.processor.DataProcessor;
import com.study.tools.highExcelTools.core.ExcelReader;
import com.study.tools.highExcelTools.core.ExcelWriter;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Excel合并工具 - 高性能实现
 * <p>
 * 提供Excel文件的高效合并、去重和过滤功能
 * 支持百万级数据处理，采用多线程并行读取和分批写入
 * </p>
 */
@Slf4j
public class ExcelMergeTool {

    /**
     * 合并Excel文件
     *
     * @param config 合并配置
     * @param <T> 数据模型类型
     * @return 合并结果
     */
    public <T> MergeResult<T> mergeExcel(MergeConfig<T> config) {
        long startTime = System.currentTimeMillis();
        // 生成唯一的合并操作ID，用于日志跟踪
        String mergeId = UUID.randomUUID().toString().substring(0, 8);
        log.info("[{}] 开始合并Excel文件，文件数量: {}", mergeId, config.getSourceFiles().size());

        // 验证配置
        List<String> validationErrors = validateConfig(config);
        if (!validationErrors.isEmpty()) {
            log.error("[{}] 配置验证失败: {}", mergeId, validationErrors);
            return MergeResult.<T>builder()
                    .success(false)
                    .errorMessage("配置验证失败: " + String.join(", ", validationErrors))
                    .build();
        }

        // 创建临时目录
        Path tempDir = null;
        // 线程池声明，在finally块中确保关闭
        ExecutorService executor = null;
        // 是否需要关闭线程池的标志，避免关闭外部提供的线程池
        boolean needShutdownExecutor = false;
        
        try {
            // 创建临时目录用于处理过程中的临时文件
            tempDir = Files.createTempDirectory("excel_merge_" + mergeId);

            // 创建输出目录
            Path outputPath = Paths.get(config.getTargetFile());
            Files.createDirectories(outputPath.getParent());

            // 线程池初始化
            // 首先检查配置中是否提供了自定义的线程池
            executor = config.getExecutor();
            if (executor == null) {
                // 如果没有提供自定义线程池，则创建一个线程池
                // 线程数量根据源文件数量和可用处理器数量动态调整
                int threads = Math.min(config.getSourceFiles().size(), Runtime.getRuntime().availableProcessors());
                
                // 创建固定大小的线程池，核心线程数和最大线程数相同
                // 使用有界队列避免内存溢出，队列容量设置为1000
                // CallerRunsPolicy: 当队列满时，在调用线程中执行任务，减缓任务提交速度
                executor = new ThreadPoolExecutor(
                        threads,               // 核心线程数 = 最大线程数
                        threads,               // 最大线程数
                        60L, TimeUnit.SECONDS, // 空闲线程超时时间
                        new LinkedBlockingQueue<>(1000), // 工作队列
                        new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略
                );
                // 标记需要在操作完成后关闭此线程池
                needShutdownExecutor = config.isShutdownExecutor();
            }

            // 报告进度：开始阶段
            reportProgress(config.getProgressCallback(), 0, 100, "初始化");

            // 读取所有文件
            // 创建适配高性能Excel工具的配置
            com.study.tools.highExcelTools.config.ExcelConfig<T> readerConfig = new com.study.tools.highExcelTools.config.ExcelConfig<>();
            readerConfig.setSourceFiles(config.getSourceFiles());
            readerConfig.setModelClass(config.getModelClass());
            
            // 创建进度回调适配器，转换ProgressCallback接口
            // 这是适配器模式的应用，使旧接口兼容新接口
            com.study.tools.highExcelTools.config.ProgressCallback adaptedCallback = null;
            if (config.getProgressCallback() != null) {
                adaptedCallback = (current, total, phase, message) -> 
                    config.getProgressCallback().onProgress(current, total, phase);
            }
            readerConfig.setProgressCallback(adaptedCallback);
            
            // 设置异常处理行为
            readerConfig.setContinueOnError(true);  // 遇到错误继续处理
            // 限制并发文件数，避免打开过多文件描述符
            readerConfig.setMaxConcurrentFiles(3);
            // 设置批处理大小，控制内存占用
            readerConfig.setBatchSize(5000);
            
            // 创建Excel读取器并使用上面配置的线程池
            ExcelReader<T> reader = new ExcelReader<T>(readerConfig, executor, mergeId);
            // 读取所有文件数据
            List<T> allData = reader.readFiles(config.getSourceFiles(), config.getModelClass());

            // 报告进度：读取完成
            reportProgress(config.getProgressCallback(), 30, 100, "读取完成");

            // 处理数据（过滤、去重）
            DataProcessor<T> processor = new DataProcessor<>(config, mergeId);
            List<T> processedData = processor.process(allData);

            // 报告进度：处理完成
            reportProgress(config.getProgressCallback(), 60, 100, "处理完成");

            // 写入合并结果
            // 创建写入配置
            com.study.tools.highExcelTools.config.ExcelConfig<T> writerConfig = new com.study.tools.highExcelTools.config.ExcelConfig<>();
            writerConfig.setTargetFile(config.getTargetFile());
            writerConfig.setModelClass(config.getModelClass());
            
            // 创建进度回调适配器
            com.study.tools.highExcelTools.config.ProgressCallback writerCallback = null;
            if (config.getProgressCallback() != null) {
                writerCallback = (current, total, phase, message) -> 
                    config.getProgressCallback().onProgress(current, total, phase);
            }
            writerConfig.setProgressCallback(writerCallback);
            
            // 配置写入性能参数
            writerConfig.setBatchSize(config.getBatchSize());
            writerConfig.setUseInMemory(true);  // 使用内存模式提高性能
            writerConfig.setAutoCloseStream(true); // 自动关闭流
            
            // 创建Excel写入器
            ExcelWriter<T> writer = new ExcelWriter<T>(writerConfig, mergeId);
            // 写入处理后的数据
            writer.write(processedData);

            // 报告进度：写入完成
            reportProgress(config.getProgressCallback(), 100, 100, "写入完成");

            // 计算总耗时
            long duration = System.currentTimeMillis() - startTime;
            log.info("[{}] 合并完成，总行数: {}，耗时: {}ms", mergeId, processedData.size(), duration);

            // 构建并返回合并结果
            return MergeResult.<T>builder()
                    .success(true)
                    .data(processedData)
                    .totalRows(processedData.size())
                    .timeMillis(duration)
                    .outputFile(config.getTargetFile())
                    .build();

        } catch (Exception e) {
            // 处理过程中的任何异常
            log.error("[{}] 合并过程中发生错误", mergeId, e);
            return MergeResult.<T>builder()
                    .success(false)
                    .errorMessage("合并失败: " + e.getMessage())
                    .build();
        } finally {
            // 资源清理部分
            // 清理临时文件
            cleanupTempDir(tempDir);
            
            // 关闭线程池
            // 只有在内部创建的线程池且需要关闭时才执行关闭操作
            if (needShutdownExecutor && executor != null) {
                // 先尝试优雅关闭，拒绝新任务但允许完成已提交任务
                executor.shutdown();
                try {
                    // 等待任务完成，最多等待60秒
                    if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                        // 如果60秒内任务未完成，强制关闭
                        executor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    // 如果当前线程被中断，也强制关闭线程池
                    executor.shutdownNow();
                    // 恢复中断状态
                    Thread.currentThread().interrupt();
                }
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
    public <T> MergeResult<T> quickMerge(List<String> sourceFiles, String targetFile, Class<T> modelClass) {
        // 创建默认配置，不指定线程池，系统会自动创建并管理线程池
        MergeConfig<T> config = MergeConfig.<T>builder()
                .sourceFiles(sourceFiles)
                .targetFile(targetFile)
                .modelClass(modelClass)
                .build();
        return mergeExcel(config);
    }
    
    /**
     * 快速合并Excel文件（带去重）
     * 
     * @param sourceFiles 源文件列表
     * @param targetFile 目标文件
     * @param modelClass 数据模型类
     * @param keyExtractor 键提取器(用于去重)
     * @param <T> 数据模型类型
     * @return 合并结果
     */
    public <T> MergeResult<T> quickMergeWithDedup(
            List<String> sourceFiles, 
            String targetFile, 
            Class<T> modelClass,
            java.util.function.Function<T, Object> keyExtractor) {
        
        MergeConfig<T> config = MergeConfig.<T>builder()
                .sourceFiles(sourceFiles)
                .targetFile(targetFile)
                .modelClass(modelClass)
                .enableDeduplication(true)
                .keyExtractor(keyExtractor)
                .build();
        return mergeExcel(config);
    }

    /**
     * 验证合并配置
     */
    private <T> List<String> validateConfig(MergeConfig<T> config) {
        List<String> errors = new ArrayList<>();
        
        if (config.getSourceFiles() == null || config.getSourceFiles().isEmpty()) {
            errors.add("源文件列表不能为空");
        } else {
            for (String filePath : config.getSourceFiles()) {
                if (!Files.exists(Paths.get(filePath))) {
                    errors.add("文件不存在: " + filePath);
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
    private void reportProgress(ProgressCallback callback, long current, long total, String phase) {
        if (callback != null) {
            callback.onProgress(current, total, phase);
        }
    }
    
    /**
     * 清理临时目录
     */
    private void cleanupTempDir(Path tempDir) {
        if (tempDir != null && Files.exists(tempDir)) {
            try {
                Files.walk(tempDir)
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (Exception e) {
                                log.warn("清理临时文件失败: {}", path, e);
                            }
                        });
            } catch (Exception e) {
                log.warn("清理临时目录失败", e);
            }
        }
    }
} 