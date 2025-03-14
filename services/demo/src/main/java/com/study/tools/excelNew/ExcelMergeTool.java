package com.study.tools.excelNew;

import com.study.tools.excelNew.processor.DataProcessor;
import com.study.tools.excelNew.util.ExcelReader;
import com.study.tools.excelNew.util.ExcelWriter;
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
        ExecutorService executor = null;
        boolean needShutdownExecutor = false;
        
        try {
            tempDir = Files.createTempDirectory("excel_merge_" + mergeId);

            // 创建输出目录
            Path outputPath = Paths.get(config.getTargetFile());
            Files.createDirectories(outputPath.getParent());

            // 确定使用的执行器
            executor = config.getExecutor();
            if (executor == null) {
                int threads = Math.min(config.getSourceFiles().size(), Runtime.getRuntime().availableProcessors());
                executor = new ThreadPoolExecutor(
                        threads, threads, 60L, TimeUnit.SECONDS,
                        new LinkedBlockingQueue<>(1000),
                        new ThreadPoolExecutor.CallerRunsPolicy()
                );
                needShutdownExecutor = config.isShutdownExecutor();
            }

            // 报告进度：开始阶段
            reportProgress(config.getProgressCallback(), 0, 100, "初始化");

            // 读取所有文件
            ExcelReader<T> reader = new ExcelReader<>(executor, config.getProgressCallback(), mergeId);
            List<T> allData = reader.readAllFiles(config.getSourceFiles(), config.getModelClass());

            // 报告进度：读取完成
            reportProgress(config.getProgressCallback(), 30, 100, "读取完成");

            // 处理数据（过滤、去重）
            DataProcessor<T> processor = new DataProcessor<>(config, mergeId);
            List<T> processedData = processor.process(allData);

            // 报告进度：处理完成
            reportProgress(config.getProgressCallback(), 60, 100, "处理完成");

            // 写入合并结果
            ExcelWriter<T> writer = new ExcelWriter<>(config, mergeId);
            writer.write(processedData);

            // 报告进度：写入完成
            reportProgress(config.getProgressCallback(), 100, 100, "写入完成");

            long duration = System.currentTimeMillis() - startTime;
            log.info("[{}] 合并完成，总行数: {}，耗时: {}ms", mergeId, processedData.size(), duration);

            return MergeResult.<T>builder()
                    .success(true)
                    .data(processedData)
                    .totalRows(processedData.size())
                    .timeMillis(duration)
                    .outputFile(config.getTargetFile())
                    .build();

        } catch (Exception e) {
            log.error("[{}] 合并过程中发生错误", mergeId, e);
            return MergeResult.<T>builder()
                    .success(false)
                    .errorMessage("合并失败: " + e.getMessage())
                    .build();
        } finally {
            // 清理临时文件
            cleanupTempDir(tempDir);
            
            // 关闭执行器
            if (needShutdownExecutor && executor != null) {
                executor.shutdown();
                try {
                    if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                        executor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    executor.shutdownNow();
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