//package com.study.tools;
//
//import com.alibaba.excel.EasyExcel;
//import com.alibaba.excel.ExcelWriter;
//import com.alibaba.excel.annotation.ExcelProperty;
//import com.alibaba.excel.write.metadata.WriteSheet;
//import com.study.tools.excelNew.ExcelMergeTool;
//import lombok.AllArgsConstructor;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Tag;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.io.TempDir;
//
//import java.io.IOException;
//import java.math.BigDecimal;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Date;
//import java.util.List;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.ThreadPoolExecutor;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.atomic.AtomicInteger;
//import java.util.concurrent.LinkedBlockingQueue;
//import java.util.concurrent.ThreadFactory;
//
//
///**
// * Excel合并工具性能测试
// * 这些测试会生成大量数据，运行时间较长，仅用于性能测试
// */
//@Tag("performance")
//public class ExcelMergeToolPerformanceTest {
//
//    @TempDir
//    Path tempDir;
//
//    private Path dataDir;
//    private Path outputDir;
//
//    private static final int SMALL_DATASET_SIZE = 1_000;
//    private static final int MEDIUM_DATASET_SIZE = 10_000;
//    private static final int LARGE_DATASET_SIZE = 1_000_000;
//
//    private String smallFile1;
//    private String smallFile2;
//    private String mediumFile1;
//    private String mediumFile2;
//    private String largeFile;
//
//    @BeforeEach
//    void setUp() throws IOException {
//        // 创建数据目录和输出目录
//        dataDir = tempDir.resolve("perf-data");
//        outputDir = tempDir.resolve("perf-output");
//        Files.createDirectories(dataDir);
//        Files.createDirectories(outputDir);
//
//        // 设置文件路径
//        smallFile1 = dataDir.resolve("small1.xlsx").toString();
//        smallFile2 = dataDir.resolve("small2.xlsx").toString();
//        mediumFile1 = dataDir.resolve("medium1.xlsx").toString();
//        mediumFile2 = dataDir.resolve("medium2.xlsx").toString();
//        largeFile = dataDir.resolve("large.xlsx").toString();
//
//        // 生成测试数据
//        generateTestFiles();
//    }
//
//    /**
//     * 生成测试数据文件
//     */
//    private void generateTestFiles() {
//        System.out.println("正在生成测试数据文件...");
//
//        // 生成小数据量文件
//        List<TestData> smallData1 = generateTestData(SMALL_DATASET_SIZE, "small1_");
//        List<TestData> smallData2 = generateTestData(SMALL_DATASET_SIZE, "small2_");
//        EasyExcel.write(smallFile1, TestData.class).sheet("Sheet1").doWrite(smallData1);
//        EasyExcel.write(smallFile2, TestData.class).sheet("Sheet1").doWrite(smallData2);
//        System.out.println("小数据量文件生成完成: " + smallFile1 + ", " + smallFile2);
//
//        // 生成中等数据量文件
//        List<TestData> mediumData1 = generateTestData(MEDIUM_DATASET_SIZE, "medium1_");
//        List<TestData> mediumData2 = generateTestData(MEDIUM_DATASET_SIZE, "medium2_");
//        EasyExcel.write(mediumFile1, TestData.class).sheet("Sheet1").doWrite(mediumData1);
//        EasyExcel.write(mediumFile2, TestData.class).sheet("Sheet1").doWrite(mediumData2);
//        System.out.println("中等数据量文件生成完成: " + mediumFile1 + ", " + mediumFile2);
//
//        // 生成大数据量文件，分批写入
//        try (ExcelWriter excelWriter = EasyExcel.write(largeFile, TestData.class)
//                .useDefaultStyle(false)  // 禁用默认样式，提高性能
//                .build()) {
//            WriteSheet writeSheet = EasyExcel.writerSheet("Sheet1").build();
//            int batchSize = 50000;  // 增加批处理大小
//            int totalBatches = LARGE_DATASET_SIZE / batchSize;
//
//            for (int i = 0; i < totalBatches; i++) {
//                String prefix = "large_batch" + i + "_";
//                List<TestData> dataList = generateTestData(batchSize, prefix);
//                excelWriter.write(dataList, writeSheet);
//                if ((i + 1) % 2 == 0) {  // 减少进度输出频率
//                    System.out.println("生成大数据量文件进度: " + (i + 1) + "/" + totalBatches);
//                }
//            }
//        }
//        System.out.println("大数据量文件生成完成: " + largeFile);
//    }
//
//    /**
//     * 生成测试数据
//     */
//    private List<TestData> generateTestData(int count, String prefix) {
//        List<TestData> dataList = new ArrayList<>(count);
//        // 预创建一些常用对象，避免重复创建
//        String[] categories = {"Category 0", "Category 1", "Category 2", "Category 3", "Category 4"};
//        String[] tags = {"tag1", "tag2", "tag3", "tag4", "tag5", "tag6", "tag7", "tag8", "tag9", "tag0"};
//        Date now = new Date();
//
//        // 使用StringBuilder来构建字符串
//        StringBuilder sb = new StringBuilder(100);
//
//        for (int i = 0; i < count; i++) {
//            TestData data = new TestData();
//            // 使用StringBuilder优化字符串拼接
//            sb.setLength(0);
//            sb.append(prefix).append(i);
//            data.setId(sb.toString());
//
//            sb.setLength(0);
//            sb.append("Name ").append(i);
//            data.setName(sb.toString());
//
//            data.setValue(new BigDecimal(Math.random() * 10000).setScale(2, BigDecimal.ROUND_HALF_UP));
//            data.setCount(i % 100);
//            data.setCategory(categories[i % 5]);
//            data.setActive(i % 10 != 0);
//            data.setCreatedAt(now);  // 使用同一个Date对象
//
//            sb.setLength(0);
//            sb.append("CODE").append(String.format("%06d", i));
//            data.setCode(sb.toString());
//
//            sb.setLength(0);
//            sb.append("This is a description for item ").append(i).append(" with some additional text to make it longer.");
//            data.setDescription(sb.toString());
//
//            data.setTags(tags[i % 10] + "," + tags[(i + 1) % 10] + "," + tags[(i + 2) % 10]);
//
//            dataList.add(data);
//        }
//
//        return dataList;
//    }
//
//    /**
//     * 测试小数据量合并性能
//     */
//    @Test
//    void testSmallDatasetPerformance() {
//        System.out.println("\n===== 小数据量合并性能测试 =====");
//
//        // 设置合并配置
//        String outputFile = outputDir.resolve("small_merged.xlsx").toString();
//        ExcelMergeTool.MergeConfig<TestData> config = ExcelMergeTool.MergeConfig.<TestData>builder()
//                .sourceFiles(Arrays.asList(smallFile1, smallFile2))
//                .targetFile(outputFile)
//                .modelClass(TestData.class)
//                .build();
//
//        // 执行合并并记录时间
//        long startTime = System.currentTimeMillis();
//        ExcelMergeTool.MergeResult<TestData> result = ExcelMergeTool.mergeExcel(config);
//        long endTime = System.currentTimeMillis();
//
//        // 输出性能统计
//        printPerformanceStats("小数据量", result, startTime, endTime);
//    }
//
//    /**
//     * 测试中等数据量合并性能
//     */
//    @Test
//    void testMediumDatasetPerformance() {
//        System.out.println("\n===== 中等数据量合并性能测试 =====");
//
//        // 设置合并配置
//        String outputFile = outputDir.resolve("medium_merged.xlsx").toString();
//        ExcelMergeTool.MergeConfig<TestData> config = ExcelMergeTool.MergeConfig.<TestData>builder()
//                .sourceFiles(Arrays.asList(mediumFile1, mediumFile2))
//                .targetFile(outputFile)
//                .modelClass(TestData.class)
//                .batchSize(5000) // 使用更大的批处理大小
//                .build();
//
//        // 执行合并并记录时间
//        long startTime = System.currentTimeMillis();
//        ExcelMergeTool.MergeResult<TestData> result = ExcelMergeTool.mergeExcel(config);
//        long endTime = System.currentTimeMillis();
//
//        // 输出性能统计
//        printPerformanceStats("中等数据量", result, startTime, endTime);
//    }
//
//    /**
//     * 测试中等数据量合并性能 - 带去重
//     */
//    @Test
//    void testMediumDatasetWithDeduplicationPerformance() {
//        System.out.println("\n===== 中等数据量去重合并性能测试 =====");
//
//        // 设置合并配置
//        String outputFile = outputDir.resolve("medium_merged_dedup.xlsx").toString();
//        ExcelMergeTool.MergeConfig<TestData> config = ExcelMergeTool.MergeConfig.<TestData>builder()
//                .sourceFiles(Arrays.asList(mediumFile1, mediumFile2))
//                .targetFile(outputFile)
//                .modelClass(TestData.class)
//                .enableDeduplication(true)
//                .keyExtractor(TestData::getId)
//                .batchSize(5000)
//                .build();
//
//        // 执行合并并记录时间
//        long startTime = System.currentTimeMillis();
//        ExcelMergeTool.MergeResult<TestData> result = ExcelMergeTool.mergeExcel(config);
//        long endTime = System.currentTimeMillis();
//
//        // 输出性能统计
//        printPerformanceStats("中等数据量去重", result, startTime, endTime);
//    }
//
//    /**
//     * 测试中等数据量合并性能 - 多线程
//     */
//    @Test
//    void testMediumDatasetWithCustomThreadPoolPerformance() {
//        System.out.println("\n===== 中等数据量多线程合并性能测试 =====");
//
//        // 创建自定义线程池
//        ExecutorService executor = Executors.newFixedThreadPool(
//                Runtime.getRuntime().availableProcessors() * 2);
//
//        try {
//            // 设置合并配置
//            String outputFile = outputDir.resolve("medium_merged_threads.xlsx").toString();
//            ExcelMergeTool.MergeConfig<TestData> config = ExcelMergeTool.MergeConfig.<TestData>builder()
//                    .sourceFiles(Arrays.asList(mediumFile1, mediumFile2))
//                    .targetFile(outputFile)
//                    .modelClass(TestData.class)
//                    .executor(executor)
//                    .batchSize(5000)
//                    .build();
//
//            // 执行合并并记录时间
//            long startTime = System.currentTimeMillis();
//            ExcelMergeTool.MergeResult<TestData> result = ExcelMergeTool.mergeExcel(config);
//            long endTime = System.currentTimeMillis();
//
//            // 输出性能统计
//            printPerformanceStats("中等数据量多线程", result, startTime, endTime);
//
//        } finally {
//            executor.shutdown();
//        }
//    }
//
//    /**
//     * 测试大数据量合并性能
//     * 注意：此测试生成和处理大量数据，运行时间较长
//     */
//    @Test
//    void testLargeDatasetPerformance() {
//        System.out.println("\n===== 大数据量合并性能测试 =====");
//
//        // 设置合并配置，调整批处理大小
//        String outputFile = outputDir.resolve("large_merged.xlsx").toString();
//        ExcelMergeTool.MergeConfig<TestData> config = ExcelMergeTool.MergeConfig.<TestData>builder()
//                .sourceFiles(Arrays.asList(largeFile))
//                .targetFile(outputFile)
//                .modelClass(TestData.class)
//                .batchSize(10000) // 更大的批处理大小
//                .build();
//
//        // 执行合并并记录时间
//        long startTime = System.currentTimeMillis();
//        ExcelMergeTool.MergeResult<TestData> result = ExcelMergeTool.mergeExcel(config);
//        long endTime = System.currentTimeMillis();
//
//        // 输出性能统计
//        printPerformanceStats("大数据量", result, startTime, endTime);
//    }
//
//    /**
//     * 测试综合性能优化配置
//     */
//    @Test
//    void testOptimizedConfiguration() {
//        System.out.println("\n===== 综合性能优化测试 =====");
//
//        // 创建优化的线程池
//        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(
//                Math.max(2, Runtime.getRuntime().availableProcessors()));
//
//        try {
//            // 设置合并配置
//            String outputFile = outputDir.resolve("optimized_merged.xlsx").toString();
//            ExcelMergeTool.MergeConfig<TestData> config = ExcelMergeTool.MergeConfig.<TestData>builder()
//                    .sourceFiles(Arrays.asList(mediumFile1, mediumFile2))
//                    .targetFile(outputFile)
//                    .modelClass(TestData.class)
//                    .executor(executor)
//                    .batchSize(20000)
//                    .enableDeduplication(true)
//                    .keyExtractor(TestData::getId)
//                    .filter(data -> data.isActive() && data.getCount() > 0)
//                    .build();
//
//            // 执行合并并记录时间
//            long startTime = System.currentTimeMillis();
//            ExcelMergeTool.MergeResult<TestData> result = ExcelMergeTool.mergeExcel(config);
//            long endTime = System.currentTimeMillis();
//
//            // 输出性能统计
//            printPerformanceStats("综合优化配置", result, startTime, endTime);
//            printThreadPoolStats(executor);
//
//        } finally {
//            executor.shutdown();
//            try {
//                executor.awaitTermination(1, TimeUnit.MINUTES);
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//            }
//        }
//    }
//
//    /**
//     * 输出性能统计信息
//     */
//    private void printPerformanceStats(String testName, ExcelMergeTool.MergeResult<?> result, long startTime, long endTime) {
//        long totalTime = endTime - startTime;
//        double rowsPerSecond = result.getRowsPerSecond();
//        System.out.println("--------------------------");
//        System.out.println("测试: " + testName);
//        System.out.println("结果: " + (result.isSuccess() ? "成功" : "失败 - " + result.getErrorMessage()));
//        System.out.println("总行数: " + result.getTotalRows());
//        System.out.println("总时间: " + totalTime + " ms");
//        System.out.println("处理速度: " + String.format("%.2f", rowsPerSecond) + " 行/秒");
//        System.out.println("--------------------------");
//    }
//
//    /**
//     * 输出线程池统计信息
//     */
//    private void printThreadPoolStats(ThreadPoolExecutor executor) {
//        System.out.println("线程池统计:");
//        System.out.println("核心线程数: " + executor.getCorePoolSize());
//        System.out.println("最大线程数: " + executor.getMaximumPoolSize());
//        System.out.println("当前线程数: " + executor.getPoolSize());
//        System.out.println("活跃线程数: " + executor.getActiveCount());
//        System.out.println("已完成任务数: " + executor.getCompletedTaskCount());
//        System.out.println("--------------------------");
//    }
//
//    /**
//     * 测试小数据量流式合并性能
//     */
//    @Test
//    void testSmallDatasetStreamingPerformance() {
//        System.out.println("\n===== 小数据量流式合并性能测试 =====");
//
//        // 设置合并配置
//        String outputFile = outputDir.resolve("small_merged_streaming.xlsx").toString();
//        ExcelMergeTool.MergeConfig<TestData> config = ExcelMergeTool.MergeConfig.<TestData>builder()
//                .sourceFiles(Arrays.asList(smallFile1, smallFile2))
//                .targetFile(outputFile)
//                .modelClass(TestData.class)
//                .build();
//
//        // 执行流式合并并记录时间
//        long startTime = System.currentTimeMillis();
//        ExcelMergeTool.MergeResult<TestData> result = ExcelMergeTool.mergeExcelStreaming(config);
//        long endTime = System.currentTimeMillis();
//
//        // 输出性能统计
//        printPerformanceStats("小数据量流式", result, startTime, endTime);
//    }
//
//    /**
//     * 测试中等数据量流式合并性能
//     */
//    @Test
//    void testMediumDatasetStreamingPerformance() {
//        System.out.println("\n===== 中等数据量流式合并性能测试 =====");
//
//        // 设置合并配置
//        String outputFile = outputDir.resolve("medium_merged_streaming.xlsx").toString();
//        ExcelMergeTool.MergeConfig<TestData> config = ExcelMergeTool.MergeConfig.<TestData>builder()
//                .sourceFiles(Arrays.asList(mediumFile1, mediumFile2))
//                .targetFile(outputFile)
//                .modelClass(TestData.class)
//                .batchSize(5000) // 使用更大的批处理大小
//                .build();
//
//        // 执行流式合并并记录时间
//        long startTime = System.currentTimeMillis();
//        ExcelMergeTool.MergeResult<TestData> result = ExcelMergeTool.mergeExcelStreaming(config);
//        long endTime = System.currentTimeMillis();
//
//        // 输出性能统计
//        printPerformanceStats("中等数据量流式", result, startTime, endTime);
//    }
//
//    /**
//     * 测试中等数据量流式合并性能 - 带去重
//     */
//    @Test
//    void testMediumDatasetStreamingWithDeduplicationPerformance() {
//        System.out.println("\n===== 中等数据量流式去重合并性能测试 =====");
//
//        // 设置合并配置
//        String outputFile = outputDir.resolve("medium_merged_streaming_dedup.xlsx").toString();
//        ExcelMergeTool.MergeConfig<TestData> config = ExcelMergeTool.MergeConfig.<TestData>builder()
//                .sourceFiles(Arrays.asList(mediumFile1, mediumFile2))
//                .targetFile(outputFile)
//                .modelClass(TestData.class)
//                .enableDeduplication(true)
//                .keyExtractor(TestData::getId)
//                .batchSize(5000)
//                .build();
//
//        // 执行流式合并并记录时间
//        long startTime = System.currentTimeMillis();
//        ExcelMergeTool.MergeResult<TestData> result = ExcelMergeTool.mergeExcelStreaming(config);
//        long endTime = System.currentTimeMillis();
//
//        // 输出性能统计
//        printPerformanceStats("中等数据量流式去重", result, startTime, endTime);
//    }
//
//    /**
//     * 测试大数据量流式合并性能
//     * 注意：此测试生成和处理大量数据，运行时间较长
//     */
//    @Test
//    void testLargeDatasetStreamingPerformance() {
//        System.out.println("\n===== 大数据量流式合并性能测试 =====");
//
//        // 创建优化的线程池
//        ExecutorService executor = new ThreadPoolExecutor(
//            Runtime.getRuntime().availableProcessors(),  // 核心线程数
//            Runtime.getRuntime().availableProcessors() * 2,  // 最大线程数
//            60L,  // 空闲线程存活时间
//            TimeUnit.SECONDS,  // 时间单位
//            new LinkedBlockingQueue<>(1000),  // 工作队列
//            new ThreadFactory() {
//                private final AtomicInteger threadNumber = new AtomicInteger(1);
//                @Override
//                public Thread newThread(Runnable r) {
//                    Thread t = new Thread(r, "excel-merge-thread-" + threadNumber.getAndIncrement());
//                    t.setDaemon(true);  // 设置为守护线程
//                    return t;
//                }
//            },
//            new ThreadPoolExecutor.CallerRunsPolicy()  // 拒绝策略
//        );
//
//        try {
//            // 设置合并配置，调整批处理大小
//            String outputFile = outputDir.resolve("large_merged_streaming.xlsx").toString();
//            ExcelMergeTool.MergeConfig<TestData> config = ExcelMergeTool.MergeConfig.<TestData>builder()
//                    .sourceFiles(Arrays.asList(largeFile))
//                    .targetFile(outputFile)
//                    .modelClass(TestData.class)
//                    .batchSize(10000) // 更大的批处理大小
//                    .executor(executor)  // 设置执行器
//                    .build();
//
//            // 执行流式合并并记录时间
//            long startTime = System.currentTimeMillis();
//            ExcelMergeTool.MergeResult<TestData> result = ExcelMergeTool.mergeExcelStreaming(config);
//            long endTime = System.currentTimeMillis();
//
//            // 输出性能统计
//            printPerformanceStats("大数据量流式", result, startTime, endTime);
//
//            // 输出线程池统计
//            printThreadPoolStats((ThreadPoolExecutor) executor);
//
//        } finally {
//            // 关闭线程池
//            executor.shutdown();
//            try {
//                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
//                    executor.shutdownNow();
//                }
//            } catch (InterruptedException e) {
//                executor.shutdownNow();
//                Thread.currentThread().interrupt();
//            }
//        }
//    }
//
////    /**
////     * 测试流式处理与普通处理的性能对比
////     */
////    @Test
////    void testStreamingVsNormalPerformance() {
////        System.out.println("\n===== 流式处理与普通处理性能对比测试 =====");
////
////        // 准备测试配置
////        String normalOutputFile = outputDir.resolve("medium_merged_normal.xlsx").toString();
////        String streamingOutputFile = outputDir.resolve("medium_merged_streaming.xlsx").toString();
////
////        ExcelMergeTool.MergeConfig<TestData> config = ExcelMergeTool.MergeConfig.<TestData>builder()
////                .sourceFiles(Arrays.asList(mediumFile1, mediumFile2))
////                .modelClass(TestData.class)
////                .batchSize(5000)
////                .enableDeduplication(true)
////                .keyExtractor(TestData::getId)
////                .build();
////
////        // 测试普通处理
////        config.setTargetFile(normalOutputFile);
////        long normalStartTime = System.currentTimeMillis();
////        ExcelMergeTool.MergeResult<TestData> normalResult = ExcelMergeTool.mergeExcel(config);
////        long normalEndTime = System.currentTimeMillis();
////
////        // 测试流式处理
////        config.setTargetFile(streamingOutputFile);
////        long streamingStartTime = System.currentTimeMillis();
////        ExcelMergeTool.MergeResult<TestData> streamingResult = ExcelMergeTool.mergeExcelStreaming(config);
////        long streamingEndTime = System.currentTimeMillis();
////
////        // 输出性能对比
////        System.out.println("--------------------------");
////        System.out.println("性能对比测试结果:");
////        System.out.println("普通处理:");
////        System.out.println("  总时间: " + (normalEndTime - normalStartTime) + " ms");
////        System.out.println("  处理速度: " + String.format("%.2f", normalResult.getRowsPerSecond()) + " 行/秒");
////        System.out.println("流式处理:");
////        System.out.println("  总时间: " + (streamingEndTime - streamingStartTime) + " ms");
////        System.out.println("  处理速度: " + String.format("%.2f", streamingResult.getRowsPerSecond()) + " 行/秒");
////        System.out.println("--------------------------");
////    }
//
//    /**
//     * 测试流式处理的内存使用情况
//     */
//    @Test
//    void testStreamingMemoryUsage() {
//        System.out.println("\n===== 流式处理内存使用测试 =====");
//
//        // 设置合并配置
//        String outputFile = outputDir.resolve("medium_merged_streaming_memory.xlsx").toString();
//        ExcelMergeTool.MergeConfig<TestData> config = ExcelMergeTool.MergeConfig.<TestData>builder()
//                .sourceFiles(Arrays.asList(mediumFile1, mediumFile2))
//                .targetFile(outputFile)
//                .modelClass(TestData.class)
//                .batchSize(5000)
//                .enableDeduplication(true)
//                .keyExtractor(TestData::getId)
//                .progressCallback((current, total, phase) -> {
//                    Runtime runtime = Runtime.getRuntime();
//                    long usedMemory = runtime.totalMemory() - runtime.freeMemory();
//                    long maxMemory = runtime.maxMemory();
//                    double memoryUsage = (double) usedMemory / maxMemory * 100;
//                    System.out.println(String.format("阶段: %s, 进度: %d/%d, 内存使用: %.2f%%",
//                            phase, current, total, memoryUsage));
//                })
//                .build();
//
//        // 执行流式合并
//        long startTime = System.currentTimeMillis();
//        ExcelMergeTool.MergeResult<TestData> result = ExcelMergeTool.mergeExcelStreaming(config);
//        long endTime = System.currentTimeMillis();
//
//        // 输出性能统计
//        printPerformanceStats("流式处理内存测试", result, startTime, endTime);
//    }
//
//
//
//    /**
//     * 测试数据模型
//     */
//    @Data
//    @NoArgsConstructor
//    @AllArgsConstructor
//    public static class TestData {
//        @ExcelProperty("ID")
//        private String id;
//
//        @ExcelProperty("名称")
//        private String name;
//
//        @ExcelProperty("金额")
//        private BigDecimal value;
//
//        @ExcelProperty("数量")
//        private Integer count;
//
//        @ExcelProperty("类别")
//        private String category;
//
//        @ExcelProperty("是否有效")
//        private boolean active;
//
//        @ExcelProperty("创建时间")
//        private Date createdAt;
//
//        @ExcelProperty("编码")
//        private String code;
//
//        @ExcelProperty("描述")
//        private String description;
//
//        @ExcelProperty("标签")
//        private String tags;
//    }
//}