package com.study.tools;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.write.metadata.WriteSheet;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Excel合并工具 - 高性能实现
 * <p>
 * 提供Excel文件的高效合并、去重和过滤功能
 * 支持百万级数据处理，采用多线程并行读取和分批写入
 * 无需依赖任何外部中间件
 * </p>
 */
@Slf4j
public class ExcelMergeTool {

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

    /**
     * 合并Excel文件
     *
     * @param config 合并配置
     * @param <T> 数据模型类型
     * @return 合并结果
     */
    public static <T> MergeResult<T> mergeExcel(MergeConfig<T> config) {
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
        try {
            tempDir = Files.createTempDirectory("excel_merge_" + mergeId);
            
            // 创建输出目录
            Path outputPath = Paths.get(config.getTargetFile());
            Files.createDirectories(outputPath.getParent());
            
            // 报告进度：开始阶段
            reportProgress(config.getProgressCallback(), 0, 100, "初始化");
            
            // 读取所有文件
            List<T> allData = readAllExcelFiles(config, mergeId);
            
            // 报告进度：读取完成
            reportProgress(config.getProgressCallback(), 30, 100, "读取完成");
            
            // 应用过滤和去重
            List<T> processedData = processData(allData, config, mergeId);
            
            // 报告进度：处理完成
            reportProgress(config.getProgressCallback(), 60, 100, "处理完成");
            
            // 写入合并结果
            writeToExcel(processedData, config, mergeId);
            
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
        }
    }
    
    /**
     * 报告进度
     *
     * @param callback 进度回调
     * @param current 当前进度
     * @param total 总数
     * @param phase 阶段
     */
    private static void reportProgress(ProgressCallback callback, long current, long total, String phase) {
        if (callback != null) {
            callback.onProgress(current, total, phase);
        }
    }
    
    /**
     * 验证合并配置
     *
     * @param <T> 数据模型类型
     * @param config 合并配置
     * @return 错误列表，如果为空则表示验证通过
     */
    private static <T> List<String> validateConfig(MergeConfig<T> config) {
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
        
        if (config.getBatchSize() <= 0) {
            errors.add("批处理大小必须大于0");
        }
        
        return errors;
    }
    
    /**
     * 读取所有Excel文件
     *
     * @param <T> 数据模型类型
     * @param config 合并配置
     * @param mergeId 合并ID
     * @return 合并的数据列表
     * @throws Exception 如果读取过程中发生错误
     */
    private static <T> List<T> readAllExcelFiles(MergeConfig<T> config, String mergeId) throws Exception {
        List<String> files = config.getSourceFiles();
        
        // 确定使用的执行器
        ExecutorService executor = config.getExecutor();
        if (executor == null) {
            int threads = Math.min(files.size(), Runtime.getRuntime().availableProcessors());
            // 使用ThreadPoolExecutor替代Executors
            executor = new ThreadPoolExecutor(
                threads,             // 核心线程数
                threads,             // 最大线程数
                60L,                 // 空闲线程存活时间
                TimeUnit.SECONDS,    // 时间单位
                new LinkedBlockingQueue<>(1000), // 工作队列
                new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略
            );
        }
        
        try {
            List<Future<List<T>>> futures = new ArrayList<>();
            
            // 提交读取任务
            for (String file : files) {
                futures.add(executor.submit(() -> readExcelFile(file, config, mergeId)));
            }
            
            // 收集结果
            List<T> result = new ArrayList<>();
            for (int i = 0; i < futures.size(); i++) {
                result.addAll(futures.get(i).get());
                // 报告读取进度
                reportProgress(config.getProgressCallback(), i + 1, futures.size(), "读取文件");
            }
            
            log.info("[{}] 成功读取 {} 个文件，总数据行数: {}", mergeId, files.size(), result.size());
            return result;
            
        } finally {
            // 如果是内部创建的执行器，则关闭它
            if (config.getExecutor() == null) {
                executor.shutdown();
            }
        }
    }
    
    /**
     * 读取单个Excel文件
     *
     * @param <T> 数据模型类型
     * @param filePath 文件路径
     * @param config 合并配置
     * @param mergeId 合并ID
     * @return 文件中的数据列表
     */
    private static <T> List<T> readExcelFile(String filePath, MergeConfig<T> config, String mergeId) {
        log.info("[{}] 开始读取文件: {}", mergeId, filePath);
        List<T> data = new ArrayList<>();
        
        EasyExcel.read(filePath, config.getModelClass(), new AnalysisEventListener<T>() {
            @Override
            public void invoke(T t, AnalysisContext analysisContext) {
                data.add(t);
            }
            
            @Override
            public void doAfterAllAnalysed(AnalysisContext analysisContext) {
                log.info("[{}] 文件读取完成: {}，行数: {}", mergeId, filePath, data.size());
            }
        }).sheet().doRead();
        
        return data;
    }
    
    /**
     * 处理数据（应用过滤和去重）
     *
     * @param <T> 数据模型类型
     * @param data 原始数据
     * @param config 合并配置
     * @param mergeId 合并ID
     * @return 处理后的数据
     */
    private static <T> List<T> processData(List<T> data, MergeConfig<T> config, String mergeId) {
        int originalSize = data.size();
        List<T> result = data;
        
        // 应用过滤器
        if (config.getFilter() != null) {
            result = new ArrayList<>();
            for (T item : data) {
                if (config.getFilter().test(item)) {
                    result.add(item);
                }
            }
            log.info("[{}] 应用过滤器后，数据行数: {} -> {}", mergeId, originalSize, result.size());
            
            // 报告过滤进度
            reportProgress(config.getProgressCallback(), 40, 100, "过滤完成");
        }
        
        // 应用去重
        if (config.isEnableDeduplication() && config.getKeyExtractor() != null) {
            int beforeDedup = result.size();
            result = deduplicateData(result, config);
            log.info("[{}] 应用去重后，数据行数: {} -> {}", mergeId, beforeDedup, result.size());
            
            // 报告去重进度
            reportProgress(config.getProgressCallback(), 50, 100, "去重完成");
        }
        
        return result;
    }
    
    /**
     * 数据去重
     *
     * @param <T> 数据模型类型
     * @param data 原始数据
     * @param config 合并配置
     * @return 去重后的数据
     */
    private static <T> List<T> deduplicateData(List<T> data, MergeConfig<T> config) {
        Map<Object, T> uniqueMap = new HashMap<>(data.size());
        
        for (T item : data) {
            Object key = config.getKeyExtractor().apply(item);
            if (key == null) continue;
            
            if (uniqueMap.containsKey(key)) {
                if (config.getMergeFunction() != null) {
                    // 应用自定义合并逻辑
                    T existingItem = uniqueMap.get(key);
                    T mergedItem = config.getMergeFunction().apply(existingItem, item);
                    uniqueMap.put(key, mergedItem);
                }
                // 如果没有合并函数，保留第一个遇到的值
            } else {
                uniqueMap.put(key, item);
            }
        }
        
        return new ArrayList<>(uniqueMap.values());
    }
    
    /**
     * 写入Excel文件
     *
     * @param <T> 数据模型类型
     * @param data 数据列表
     * @param config 合并配置
     * @param mergeId 合并ID
     * @throws Exception 如果写入过程中发生错误
     */
    private static <T> void writeToExcel(List<T> data, MergeConfig<T> config, String mergeId) throws Exception {
        log.info("[{}] 开始写入合并文件: {}, 数据行数: {}", mergeId, config.getTargetFile(), data.size());
        
        int batchSize = config.getBatchSize();
        if (batchSize <= 0) {
            batchSize = 5000; // 默认批次大小
        }
        
        try (FileOutputStream fos = new FileOutputStream(config.getTargetFile())) {
            ExcelWriter excelWriter = EasyExcel.write(fos, config.getModelClass()).build();
            WriteSheet writeSheet = EasyExcel.writerSheet("Sheet1").build();
            
            // 分批写入
            for (int i = 0; i < data.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, data.size());
                List<T> batch = data.subList(i, endIndex);
                excelWriter.write(batch, writeSheet);
                log.debug("[{}] 写入进度: {}/{}", mergeId, endIndex, data.size());
                
                // 报告写入进度
                int progress = 60 + (int) (((double) endIndex / data.size()) * 40);
                reportProgress(config.getProgressCallback(), progress, 100, "写入数据");
            }
            
            excelWriter.finish();
        }
        
        log.info("[{}] 文件写入完成: {}", mergeId, config.getTargetFile());
    }
    
    /**
     * 清理临时目录
     *
     * @param tempDir 临时目录
     */
    private static void cleanupTempDir(Path tempDir) {
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
    
    /**
     * 合并配置
     *
     * @param <T> 数据模型类型
     */
    @Builder
    @Getter
    public static class MergeConfig<T> {
        /** 源文件路径列表 */
        private final List<String> sourceFiles;
        
        /** 目标文件路径 */
        private final String targetFile;
        
        /** 数据模型类 */
        private final Class<T> modelClass;
        
        /** 是否启用去重 */
        @Builder.Default
        private final boolean enableDeduplication = false;
        
        /** 键提取器，用于去重 */
        private final Function<T, Object> keyExtractor;
        
        /** 合并函数，用于合并重复项 */
        private final BiFunction<T, T, T> mergeFunction;
        
        /** 过滤条件 */
        private final Predicate<T> filter;
        
        /** 自定义执行器 */
        private final ExecutorService executor;
        
        /** 批处理大小 */
        @Builder.Default
        private final int batchSize = 5000;
        
        /** 是否关闭执行器 */
        @Builder.Default
        private final boolean shutdownExecutor = true;
        
        /** 进度回调 */
        private final ProgressCallback progressCallback;
        
        /**
         * MergeConfig构建器类，提供额外的方法
         */
        public static class MergeConfigBuilder<T> {
            /**
             * 值选择器，mergeFunction的别名
             * 用于支持测试中使用的valueSelector方法
             *
             * @param valueSelector 值选择器函数
             * @return 构建器
             */
            public MergeConfigBuilder<T> valueSelector(BiFunction<T, T, T> valueSelector) {
                this.mergeFunction = valueSelector;
                return this;
            }
            
            /**
             * 设置进度回调
             *
             * @param progressCallback 进度回调函数
             * @return 构建器
             */
            public MergeConfigBuilder<T> progressCallback(ProgressCallback progressCallback) {
                this.progressCallback = progressCallback;
                return this;
            }
        }
    }
    
    /**
     * 合并结果
     *
     * @param <T> 数据模型类型
     */
    @Builder
    @Data
    public static class MergeResult<T> {
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
         * 
         * @return 处理速度
         */
        public double getRowsPerSecond() {
            if (timeMillis <= 0) return 0;
            return (double) totalRows / (timeMillis / 1000.0);
        }
    }

    /**
     * 合并Excel文件（流式处理版本）
     *
     * @param config 合并配置
     * @param <T> 数据模型类型
     * @return 合并结果
     */
    public static <T> MergeResult<T> mergeExcelStreaming(MergeConfig<T> config) {
        long startTime = System.currentTimeMillis();
        String mergeId = UUID.randomUUID().toString().substring(0, 8);
        log.info("[{}] 开始流式合并Excel文件，文件数量: {}", mergeId, config.getSourceFiles().size());
        
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
        try {
            tempDir = Files.createTempDirectory("excel_merge_" + mergeId);
            
            // 创建输出目录
            Path outputPath = Paths.get(config.getTargetFile());
            Files.createDirectories(outputPath.getParent());
            
            // 报告进度：开始阶段
            reportProgress(config.getProgressCallback(), 0, 100, "初始化");
            
            // 使用流式处理读取数据
            List<T> processedData = readExcelFilesStreaming(config, mergeId);
            
            // 报告进度：读取完成
            reportProgress(config.getProgressCallback(), 30, 100, "读取完成");
            
            // 应用过滤和去重
            processedData = processDataStreaming(processedData, config, mergeId);
            
            // 报告进度：处理完成
            reportProgress(config.getProgressCallback(), 60, 100, "处理完成");
            
            // 写入合并结果
            writeToExcelStreaming(processedData, config, mergeId);
            
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
        }
    }

    /**
     * 流式读取Excel文件
     */
    private static <T> List<T> readExcelFilesStreaming(MergeConfig<T> config, String mergeId) throws Exception {
        List<String> files = config.getSourceFiles();
        ExecutorService executor = config.getExecutor();
        if (executor == null) {
            executor = createOptimizedThreadPool();
        }
        
        try {
            List<Future<List<T>>> futures = new ArrayList<>();
            
            // 提交读取任务
            for (String file : files) {
                futures.add(executor.submit(() -> readExcelFileStreaming(file, config, mergeId)));
            }
            
            // 收集结果
            List<T> result = new ArrayList<>();
            for (int i = 0; i < futures.size(); i++) {
                result.addAll(futures.get(i).get());
                // 报告读取进度
                reportProgress(config.getProgressCallback(), i + 1, futures.size(), "读取文件");
            }
            
            log.info("[{}] 成功读取 {} 个文件，总数据行数: {}", mergeId, files.size(), result.size());
            return result;
            
        } finally {
            if (config.getExecutor() == null) {
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
     * 流式读取单个Excel文件
     */
    private static <T> List<T> readExcelFileStreaming(String filePath, MergeConfig<T> config, String mergeId) {
        log.info("[{}] 开始读取文件: {}", mergeId, filePath);
        List<T> data = new ArrayList<>();
        
        EasyExcel.read(filePath, config.getModelClass(), new AnalysisEventListener<T>() {
            @Override
            public void invoke(T t, AnalysisContext analysisContext) {
                data.add(t);
                // 监控内存使用
                monitorMemoryUsage("读取文件");
            }
            
            @Override
            public void doAfterAllAnalysed(AnalysisContext analysisContext) {
                log.info("[{}] 文件读取完成: {}，行数: {}", mergeId, filePath, data.size());
            }
        }).sheet().doRead();
        
        return data;
    }

    /**
     * 流式处理数据（应用过滤和去重）
     */
    private static <T> List<T> processDataStreaming(List<T> data, MergeConfig<T> config, String mergeId) {
        int originalSize = data.size();
        List<T> result = data;
        
        // 应用过滤器
        if (config.getFilter() != null) {
            result = data.parallelStream()
                    .filter(config.getFilter())
                    .collect(Collectors.toList());
            log.info("[{}] 应用过滤器后，数据行数: {} -> {}", mergeId, originalSize, result.size());
            
            // 报告过滤进度
            reportProgress(config.getProgressCallback(), 40, 100, "过滤完成");
        }
        
        // 应用去重
        if (config.isEnableDeduplication() && config.getKeyExtractor() != null) {
            int beforeDedup = result.size();
            result = deduplicateDataParallel(result, config);
            log.info("[{}] 应用去重后，数据行数: {} -> {}", mergeId, beforeDedup, result.size());
            
            // 报告去重进度
            reportProgress(config.getProgressCallback(), 50, 100, "去重完成");
        }
        
        return result;
    }

    /**
     * 并行去重
     */
    private static <T> List<T> deduplicateDataParallel(List<T> data, MergeConfig<T> config) {
        return data.parallelStream()
                .collect(Collectors.groupingBy(
                    config.getKeyExtractor(),
                    Collectors.reducing(null, (a, b) -> 
                        config.getMergeFunction() != null ? 
                            config.getMergeFunction().apply(a, b) : a)
                ))
                .values()
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 流式写入Excel文件
     */
    private static <T> void writeToExcelStreaming(List<T> data, MergeConfig<T> config, String mergeId) throws Exception {
        log.info("[{}] 开始写入合并文件: {}, 数据行数: {}", mergeId, config.getTargetFile(), data.size());
        
        // 计算最优批处理大小
        int batchSize = calculateOptimalBatchSize(data.size());
        
        try (FileOutputStream fos = new FileOutputStream(config.getTargetFile());
             ExcelWriter excelWriter = EasyExcel.write(fos, config.getModelClass())
                     .useDefaultStyle(false)  // 禁用默认样式
                     .build()) {
            
            WriteSheet writeSheet = EasyExcel.writerSheet("Sheet1").build();
            
            // 使用 CompletableFuture 异步写入
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                // 分批写入
                for (int i = 0; i < data.size(); i += batchSize) {
                    int endIndex = Math.min(i + batchSize, data.size());
                    List<T> batch = data.subList(i, endIndex);
                    excelWriter.write(batch, writeSheet);
                    
                    // 报告写入进度
                    int progress = 60 + (int) (((double) endIndex / data.size()) * 40);
                    reportProgress(config.getProgressCallback(), progress, 100, "写入数据");
                    
                    // 监控内存使用
                    monitorMemoryUsage("写入数据");
                }
            }, config.getExecutor());
            
            // 等待写入完成，设置超时
            future.get(30, TimeUnit.MINUTES);
        }
        
        log.info("[{}] 文件写入完成: {}", mergeId, config.getTargetFile());
    }

    /**
     * 创建优化的线程池
     */
    private static ExecutorService createOptimizedThreadPool() {
        int processors = Runtime.getRuntime().availableProcessors();
        return new ThreadPoolExecutor(
            processors,                    // 核心线程数
            processors * 2,               // 最大线程数
            60L,                          // 空闲线程存活时间
            TimeUnit.SECONDS,             // 时间单位
            new LinkedBlockingQueue<>(1000),  // 工作队列
            new ThreadFactory() {
                private final AtomicInteger threadNumber = new AtomicInteger(1);
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "excel-merge-thread-" + threadNumber.getAndIncrement());
                    t.setDaemon(true);  // 设置为守护线程
                    return t;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()  // 拒绝策略
        );
    }

    /**
     * 计算最优批处理大小
     */
    private static int calculateOptimalBatchSize(long totalRows) {
        if (totalRows < 10000) {
            return 1000;
        } else if (totalRows < 100000) {
            return 5000;
        } else if (totalRows < 1000000) {
            return 10000;
        } else {
            return 20000;
        }
    }

    /**
     * 监控内存使用
     */
    private static void monitorMemoryUsage(String phase) {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        double memoryUsage = (double) usedMemory / maxMemory * 100;
        
        if (memoryUsage > 80) {  // 内存使用超过80%
            System.gc();  // 触发垃圾回收
            log.warn("内存使用率过高: {}%, 当前阶段: {}", memoryUsage, phase);
        }
    }
}
