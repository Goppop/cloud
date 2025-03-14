package com.study.tools;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.write.metadata.WriteSheet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Excel合并工具性能测试
 * 这些测试会生成大量数据，运行时间较长，仅用于性能测试
 */
@Tag("performance")
public class ExcelMergeToolPerformanceTest {

    @TempDir
    Path tempDir;
    
    private Path dataDir;
    private Path outputDir;
    
    private static final int SMALL_DATASET_SIZE = 1_000;
    private static final int MEDIUM_DATASET_SIZE = 10_000;
    private static final int LARGE_DATASET_SIZE = 1_000_000;
    
    private String smallFile1;
    private String smallFile2;
    private String mediumFile1;
    private String mediumFile2;
    private String largeFile;
    
    @BeforeEach
    void setUp() throws IOException {
        // 创建数据目录和输出目录
        dataDir = tempDir.resolve("perf-data");
        outputDir = tempDir.resolve("perf-output");
        Files.createDirectories(dataDir);
        Files.createDirectories(outputDir);
        
        // 设置文件路径
        smallFile1 = dataDir.resolve("small1.xlsx").toString();
        smallFile2 = dataDir.resolve("small2.xlsx").toString();
        mediumFile1 = dataDir.resolve("medium1.xlsx").toString();
        mediumFile2 = dataDir.resolve("medium2.xlsx").toString();
        largeFile = dataDir.resolve("large.xlsx").toString();
        
        // 生成测试数据
        generateTestFiles();
    }
    
    /**
     * 生成测试数据文件
     */
    private void generateTestFiles() {
        System.out.println("正在生成测试数据文件...");
        
        // 生成小数据量文件
        List<TestData> smallData1 = generateTestData(SMALL_DATASET_SIZE, "small1_");
        List<TestData> smallData2 = generateTestData(SMALL_DATASET_SIZE, "small2_");
        EasyExcel.write(smallFile1, TestData.class).sheet("Sheet1").doWrite(smallData1);
        EasyExcel.write(smallFile2, TestData.class).sheet("Sheet1").doWrite(smallData2);
        System.out.println("小数据量文件生成完成: " + smallFile1 + ", " + smallFile2);
        
        // 生成中等数据量文件
        List<TestData> mediumData1 = generateTestData(MEDIUM_DATASET_SIZE, "medium1_");
        List<TestData> mediumData2 = generateTestData(MEDIUM_DATASET_SIZE, "medium2_");
        EasyExcel.write(mediumFile1, TestData.class).sheet("Sheet1").doWrite(mediumData1);
        EasyExcel.write(mediumFile2, TestData.class).sheet("Sheet1").doWrite(mediumData2);
        System.out.println("中等数据量文件生成完成: " + mediumFile1 + ", " + mediumFile2);
        
        // 生成大数据量文件，分批写入
       EasyExcel.write(largeFile, TestData.class)
           .sheet("Sheet1")
           .doWrite(() -> {
               List<TestData> dataList = new ArrayList<>();
               int batchSize = 10000;
               int totalBatches = LARGE_DATASET_SIZE / batchSize;

               for (int i = 0; i < totalBatches; i++) {
                   dataList.clear();
                   String prefix = "large_batch" + i + "_";
                   dataList.addAll(generateTestData(batchSize, prefix));

                   System.out.println("生成大数据量文件进度: " + (i + 1) + "/" + totalBatches);
                   return dataList;
               }
               return null;
           });
       System.out.println("大数据量文件生成完成: " + largeFile);

    }
    
    /**
     * 生成测试数据
     */
    private List<TestData> generateTestData(int count, String prefix) {
        List<TestData> dataList = new ArrayList<>(count);
        
        for (int i = 0; i < count; i++) {
            TestData data = new TestData();
            data.setId(prefix + i);
            data.setName("Name " + i);
            data.setValue(new BigDecimal(String.format("%.2f", Math.random() * 10000)));
            data.setCount(i % 100);
            data.setCategory("Category " + (i % 5));
            data.setActive(i % 10 != 0);
            data.setCreatedAt(new Date());
            data.setCode("CODE" + String.format("%06d", i));
            data.setDescription("This is a description for item " + i + " with some additional text to make it longer.");
            data.setTags("tag1,tag2,tag" + (i % 10));
            
            dataList.add(data);
        }
        
        return dataList;
    }
    
    /**
     * 测试小数据量合并性能
     */
    @Test
    void testSmallDatasetPerformance() {
        System.out.println("\n===== 小数据量合并性能测试 =====");
        
        // 设置合并配置
        String outputFile = outputDir.resolve("small_merged.xlsx").toString();
        ExcelMergeTool.MergeConfig<TestData> config = ExcelMergeTool.MergeConfig.<TestData>builder()
                .sourceFiles(Arrays.asList(smallFile1, smallFile2))
                .targetFile(outputFile)
                .modelClass(TestData.class)
                .build();
        
        // 执行合并并记录时间
        long startTime = System.currentTimeMillis();
        ExcelMergeTool.MergeResult<TestData> result = ExcelMergeTool.mergeExcel(config);
        long endTime = System.currentTimeMillis();
        
        // 输出性能统计
        printPerformanceStats("小数据量", result, startTime, endTime);
    }
    
    /**
     * 测试中等数据量合并性能
     */
    @Test
    void testMediumDatasetPerformance() {
        System.out.println("\n===== 中等数据量合并性能测试 =====");
        
        // 设置合并配置
        String outputFile = outputDir.resolve("medium_merged.xlsx").toString();
        ExcelMergeTool.MergeConfig<TestData> config = ExcelMergeTool.MergeConfig.<TestData>builder()
                .sourceFiles(Arrays.asList(mediumFile1, mediumFile2))
                .targetFile(outputFile)
                .modelClass(TestData.class)
                .batchSize(5000) // 使用更大的批处理大小
                .build();
        
        // 执行合并并记录时间
        long startTime = System.currentTimeMillis();
        ExcelMergeTool.MergeResult<TestData> result = ExcelMergeTool.mergeExcel(config);
        long endTime = System.currentTimeMillis();
        
        // 输出性能统计
        printPerformanceStats("中等数据量", result, startTime, endTime);
    }
    
    /**
     * 测试中等数据量合并性能 - 带去重
     */
    @Test
    void testMediumDatasetWithDeduplicationPerformance() {
        System.out.println("\n===== 中等数据量去重合并性能测试 =====");
        
        // 设置合并配置
        String outputFile = outputDir.resolve("medium_merged_dedup.xlsx").toString();
        ExcelMergeTool.MergeConfig<TestData> config = ExcelMergeTool.MergeConfig.<TestData>builder()
                .sourceFiles(Arrays.asList(mediumFile1, mediumFile2))
                .targetFile(outputFile)
                .modelClass(TestData.class)
                .enableDeduplication(true)
                .keyExtractor(TestData::getId)
                .batchSize(5000)
                .build();
        
        // 执行合并并记录时间
        long startTime = System.currentTimeMillis();
        ExcelMergeTool.MergeResult<TestData> result = ExcelMergeTool.mergeExcel(config);
        long endTime = System.currentTimeMillis();
        
        // 输出性能统计
        printPerformanceStats("中等数据量去重", result, startTime, endTime);
    }
    
    /**
     * 测试中等数据量合并性能 - 多线程
     */
    @Test
    void testMediumDatasetWithCustomThreadPoolPerformance() {
        System.out.println("\n===== 中等数据量多线程合并性能测试 =====");
        
        // 创建自定义线程池
        ExecutorService executor = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors() * 2);
        
        try {
            // 设置合并配置
            String outputFile = outputDir.resolve("medium_merged_threads.xlsx").toString();
            ExcelMergeTool.MergeConfig<TestData> config = ExcelMergeTool.MergeConfig.<TestData>builder()
                    .sourceFiles(Arrays.asList(mediumFile1, mediumFile2))
                    .targetFile(outputFile)
                    .modelClass(TestData.class)
                    .executor(executor)
                    .batchSize(5000)
                    .build();
            
            // 执行合并并记录时间
            long startTime = System.currentTimeMillis();
            ExcelMergeTool.MergeResult<TestData> result = ExcelMergeTool.mergeExcel(config);
            long endTime = System.currentTimeMillis();
            
            // 输出性能统计
            printPerformanceStats("中等数据量多线程", result, startTime, endTime);
            
        } finally {
            executor.shutdown();
        }
    }
    
    /**
     * 测试大数据量合并性能
     * 注意：此测试生成和处理大量数据，运行时间较长
     */
    @Test
    void testLargeDatasetPerformance() {
        System.out.println("\n===== 大数据量合并性能测试 =====");
        
        // 设置合并配置，调整批处理大小
        String outputFile = outputDir.resolve("large_merged.xlsx").toString();
        ExcelMergeTool.MergeConfig<TestData> config = ExcelMergeTool.MergeConfig.<TestData>builder()
                .sourceFiles(Arrays.asList(largeFile))
                .targetFile(outputFile)
                .modelClass(TestData.class)
                .batchSize(10000) // 更大的批处理大小
                .build();
        
        // 执行合并并记录时间
        long startTime = System.currentTimeMillis();
        ExcelMergeTool.MergeResult<TestData> result = ExcelMergeTool.mergeExcel(config);
        long endTime = System.currentTimeMillis();
        
        // 输出性能统计
        printPerformanceStats("大数据量", result, startTime, endTime);
    }
    
    /**
     * 测试综合性能优化配置
     */
    @Test
    void testOptimizedConfiguration() {
        System.out.println("\n===== 综合性能优化测试 =====");
        
        // 创建优化的线程池
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(
                Math.max(2, Runtime.getRuntime().availableProcessors()));
        
        try {
            // 设置合并配置
            String outputFile = outputDir.resolve("optimized_merged.xlsx").toString();
            ExcelMergeTool.MergeConfig<TestData> config = ExcelMergeTool.MergeConfig.<TestData>builder()
                    .sourceFiles(Arrays.asList(mediumFile1, mediumFile2))
                    .targetFile(outputFile)
                    .modelClass(TestData.class)
                    .executor(executor)
                    .batchSize(20000)
                    .enableDeduplication(true)
                    .keyExtractor(TestData::getId)
                    .filter(data -> data.isActive() && data.getCount() > 0)
                    .build();
            
            // 执行合并并记录时间
            long startTime = System.currentTimeMillis();
            ExcelMergeTool.MergeResult<TestData> result = ExcelMergeTool.mergeExcel(config);
            long endTime = System.currentTimeMillis();
            
            // 输出性能统计
            printPerformanceStats("综合优化配置", result, startTime, endTime);
            printThreadPoolStats(executor);
            
        } finally {
            executor.shutdown();
            try {
                executor.awaitTermination(1, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * 输出性能统计信息
     */
    private void printPerformanceStats(String testName, ExcelMergeTool.MergeResult<?> result, long startTime, long endTime) {
        long totalTime = endTime - startTime;
        double rowsPerSecond = result.getRowsPerSecond();
        System.out.println("--------------------------");
        System.out.println("测试: " + testName);
        System.out.println("结果: " + (result.isSuccess() ? "成功" : "失败 - " + result.getErrorMessage()));
        System.out.println("总行数: " + result.getTotalRows());
        System.out.println("总时间: " + totalTime + " ms");
        System.out.println("处理速度: " + String.format("%.2f", rowsPerSecond) + " 行/秒");
        System.out.println("--------------------------");
    }
    
    /**
     * 输出线程池统计信息
     */
    private void printThreadPoolStats(ThreadPoolExecutor executor) {
        System.out.println("线程池统计:");
        System.out.println("核心线程数: " + executor.getCorePoolSize());
        System.out.println("最大线程数: " + executor.getMaximumPoolSize());
        System.out.println("当前线程数: " + executor.getPoolSize());
        System.out.println("活跃线程数: " + executor.getActiveCount());
        System.out.println("已完成任务数: " + executor.getCompletedTaskCount());
        System.out.println("--------------------------");
    }
    
    /**
     * 测试数据模型
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestData {
        @ExcelProperty("ID")
        private String id;
        
        @ExcelProperty("名称")
        private String name;
        
        @ExcelProperty("金额")
        private BigDecimal value;
        
        @ExcelProperty("数量")
        private Integer count;
        
        @ExcelProperty("类别")
        private String category;
        
        @ExcelProperty("是否有效")
        private boolean active;
        
        @ExcelProperty("创建时间")
        private Date createdAt;
        
        @ExcelProperty("编码")
        private String code;
        
        @ExcelProperty("描述")
        private String description;
        
        @ExcelProperty("标签")
        private String tags;
    }
} 