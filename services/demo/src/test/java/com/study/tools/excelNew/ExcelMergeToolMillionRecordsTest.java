package com.study.tools.excelNew;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.study.tools.excelNew.util.ExcelMergeUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Excel百万级数据合并测试
 * - 生成多个多Sheet的Excel文件，每个文件包含多个Sheet，每个Sheet包含大量数据
 * - 测试合并、去重和过滤功能
 * - 测试百万级数据处理性能
 */
@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ExcelMergeToolMillionRecordsTest {

    // 测试数据配置
    private static final int TOTAL_FILES = 3;           // 生成的文件数量
    private static final int SHEETS_PER_FILE = 5;       // 每个文件的Sheet数量
    private static final int ROWS_PER_SHEET = 50_000;   // 每个Sheet的行数(5万行，总计75万行)
    private static final String TEST_DIR = "test-data"; // 测试数据目录
    private static final String OUTPUT_DIR = "test-output"; // 输出目录
    
    // 生成的文件路径列表
    private List<String> testFiles = new ArrayList<>();
    
    // 监控内存消耗
    private long initialMemory;
    private long peakMemory;
    
    // 性能指标
    private long generationTimeMs;
    private long processingTimeMs;
    private long mergeTimeMs;
    
    /**
     * 测试用户数据模型
     */
    @Data
    public static class UserRecord {
        @ExcelProperty("用户ID")
        private String id;
        
        @ExcelProperty("用户名")
        private String username;
        
        @ExcelProperty("年龄")
        private Integer age;
        
        @ExcelProperty("邮箱")
        private String email;
        
        @ExcelProperty("手机号")
        private String mobile;
        
        @ExcelProperty("城市")
        private String city;
        
        @ExcelProperty("省份")
        private String province;
        
        @ExcelProperty("注册时间")
        private Date registerTime;
        
        @ExcelProperty("积分")
        private Integer points;
        
        @ExcelProperty("等级")
        private Integer level;
        
        @ExcelProperty("最后登录时间")
        private Date lastLoginTime;
        
        @ExcelProperty("备注")
        private String remark;
    }
    
    /**
     * 在所有测试前生成测试数据
     */
    @BeforeAll
    public void setUp() throws Exception {
        initialMemory = getUsedMemory();
        log.info("开始生成测试数据，初始内存使用: {} MB", initialMemory / (1024 * 1024));
        
        // 创建测试目录
        Path testPath = Paths.get(TEST_DIR);
        if (!Files.exists(testPath)) {
            Files.createDirectories(testPath);
        }
        
        // 创建输出目录
        Path outputPath = Paths.get(OUTPUT_DIR);
        if (!Files.exists(outputPath)) {
            Files.createDirectories(outputPath);
        }
        
        // 生成测试文件
        Instant start = Instant.now();
        generateTestFiles();
        Instant end = Instant.now();
        generationTimeMs = Duration.between(start, end).toMillis();
        
        peakMemory = getUsedMemory();
        log.info("测试数据生成完成，耗时: {} ms, 峰值内存使用: {} MB", 
                generationTimeMs, peakMemory / (1024 * 1024));
    }
    
    /**
     * 生成多个测试Excel文件，每个文件包含多个Sheet
     */
    private void generateTestFiles() {
        for (int fileIndex = 0; fileIndex < TOTAL_FILES; fileIndex++) {
            String fileName = TEST_DIR + "/users_" + fileIndex + ".xlsx";
            testFiles.add(fileName);
            
            try (ExcelWriter excelWriter = EasyExcel.write(fileName, UserRecord.class).build()) {
                // 为每个文件创建多个Sheet
                for (int sheetIndex = 0; sheetIndex < SHEETS_PER_FILE; sheetIndex++) {
                    WriteSheet writeSheet = EasyExcel.writerSheet(sheetIndex, "用户数据" + sheetIndex).build();
                    
                    // 生成当前Sheet的数据
                    List<UserRecord> data = generateSheetData(fileIndex, sheetIndex, ROWS_PER_SHEET);
                    
                    // 写入数据
                    excelWriter.write(data, writeSheet);
                    log.info("已生成文件 {}, Sheet {}, 行数: {}", fileName, sheetIndex, data.size());
                    
                    // 释放内存
                    data.clear();
                    System.gc();
                }
            }
        }
    }
    
    /**
     * 生成单个Sheet的测试数据
     */
    private List<UserRecord> generateSheetData(int fileIndex, int sheetIndex, int rowCount) {
        List<UserRecord> records = new ArrayList<>(rowCount);
        
        // 基础ID偏移，确保每个Sheet的ID不重复
        int baseOffset = fileIndex * SHEETS_PER_FILE * ROWS_PER_SHEET + sheetIndex * ROWS_PER_SHEET;
        
        // 生成随机数据
        Random random = new Random();
        String[] cities = {"北京", "上海", "广州", "深圳", "杭州", "南京", "武汉", "成都", "重庆", "西安"};
        String[] provinces = {"北京", "上海", "广东", "广东", "浙江", "江苏", "湖北", "四川", "重庆", "陕西"};
        
        // 生成当前日期前后10年的范围
        long now = System.currentTimeMillis();
        long tenYearsInMillis = 10L * 365 * 24 * 60 * 60 * 1000;
        
        for (int i = 0; i < rowCount; i++) {
            UserRecord record = new UserRecord();
            
            // 设置唯一ID (部分重复，用于测试去重功能)
            boolean makeDuplicate = random.nextDouble() < 0.1; // 10%的重复率
            int idIndex = makeDuplicate && i > 0 ? random.nextInt(i) : baseOffset + i;
            record.setId("USER" + String.format("%08d", idIndex));
            
            // 设置用户名
            record.setUsername("用户" + idIndex);
            
            // 设置年龄 (18-60)
            record.setAge(18 + random.nextInt(43));
            
            // 设置邮箱
            record.setEmail("user" + idIndex + "@example.com");
            
            // 设置手机号
            record.setMobile("1" + (3 + random.nextInt(6)) + generateRandomDigits(9));
            
            // 设置城市和省份
            int locationIndex = random.nextInt(cities.length);
            record.setCity(cities[locationIndex]);
            record.setProvince(provinces[locationIndex]);
            
            // 设置注册时间 (过去10年内的随机时间)
            long registerTime = now - (long)(random.nextDouble() * tenYearsInMillis);
            record.setRegisterTime(new Date(registerTime));
            
            // 设置积分 (0-10000)
            record.setPoints(random.nextInt(10001));
            
            // 设置等级 (1-10)
            record.setLevel(1 + random.nextInt(10));
            
            // 设置最后登录时间 (注册时间之后的随机时间)
            long lastLoginOffset = (long)(random.nextDouble() * (now - registerTime));
            record.setLastLoginTime(new Date(registerTime + lastLoginOffset));
            
            // 设置备注
            if (random.nextDouble() < 0.3) { // 30%的记录有备注
                record.setRemark("这是用户" + idIndex + "的备注信息");
            }
            
            records.add(record);
        }
        
        return records;
    }
    
    /**
     * 生成指定长度的随机数字字符串
     */
    private String generateRandomDigits(int length) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
    
    /**
     * 测试合并Excel文件 (简单合并)
     */
    @Test
    public void testMergeExcelFiles() {
        log.info("开始测试合并 {} 个Excel文件", testFiles.size());
        log.info("每个文件 {} 个Sheet，每个Sheet {} 行，总计约 {} 行数据", 
                SHEETS_PER_FILE, ROWS_PER_SHEET, 
                TOTAL_FILES * SHEETS_PER_FILE * ROWS_PER_SHEET);
        
        String outputFile = OUTPUT_DIR + "/merged_result.xlsx";
        
        long memoryBefore = getUsedMemory();
        log.info("合并前内存使用: {} MB", memoryBefore / (1024 * 1024));
        
        // 记录进度的计数器
        AtomicInteger progressCounter = new AtomicInteger();
        
        // 实现进度回调
        ProgressCallback progressCallback = (current, total, phase) -> {
            if (progressCounter.incrementAndGet() % 5 == 0) { // 每5次回调打印一次日志
                log.info("处理进度: {} - {}/{}", phase, current, total);
            }
        };
        
        ExcelMergeTool mergeTool = new ExcelMergeTool();
        
        // 配置合并参数
        MergeConfig<UserRecord> config = MergeConfig.<UserRecord>builder()
                .sourceFiles(testFiles)
                .targetFile(outputFile)
                .modelClass(UserRecord.class)
                .progressCallback(progressCallback)
                .batchSize(10000) // 使用较大的批次大小
                .build();
        
        Instant start = Instant.now();
        
        // 执行合并
        MergeResult<UserRecord> result = mergeTool.mergeExcel(config);
        
        Instant end = Instant.now();
        mergeTimeMs = Duration.between(start, end).toMillis();
        
        long memoryAfter = getUsedMemory();
        
        // 打印结果统计
        if (result.isSuccess()) {
            log.info("合并成功: 总行数={}, 耗时={} ms, 速度={} 行/秒", 
                    result.getTotalRows(), 
                    mergeTimeMs,
                    result.getRowsPerSecond());
        } else {
            log.error("合并失败: {}", result.getErrorMessage());
        }
        
        log.info("合并后内存使用: {} MB, 内存差异: {} MB", 
                memoryAfter / (1024 * 1024),
                (memoryAfter - memoryBefore) / (1024 * 1024));
    }
    
    /**
     * 测试带去重的合并 (按ID去重)
     */
    @Test
    public void testMergeWithDeduplication() {
        log.info("开始测试带去重的合并");
        
        String outputFile = OUTPUT_DIR + "/merged_dedup_result.xlsx";
        
        // 记录统计信息的Map
        Map<String, Integer> cityStats = new ConcurrentHashMap<>();
        AtomicInteger totalRecords = new AtomicInteger();
        AtomicInteger duplicateCount = new AtomicInteger();
        Set<String> uniqueIds = ConcurrentHashMap.newKeySet();
        
        long memoryBefore = getUsedMemory();
        
        // 实现进度回调
        ProgressCallback progressCallback = (current, total, phase) -> {
            if (current % 10 == 0 || current == total) {
                log.info("处理进度: {} - {}/{}", phase, current, total);
            }
        };
        
        ExcelMergeTool mergeTool = new ExcelMergeTool();
        
        // 配置去重合并参数
        MergeConfig<UserRecord> config = MergeConfig.<UserRecord>builder()
                .sourceFiles(testFiles)
                .targetFile(outputFile)
                .modelClass(UserRecord.class)
                .enableDeduplication(true)
                .keyExtractor(UserRecord::getId) // 按ID去重
                .progressCallback(progressCallback)
                // 定义如何合并重复记录 (选择积分更高的记录)
                .mergeFunction((record1, record2) -> {
                    duplicateCount.incrementAndGet();
                    if (record1.getPoints() != null && record2.getPoints() != null &&
                            record1.getPoints() < record2.getPoints()) {
                        return record2;
                    }
                    return record1;
                })
                .build();
        
        Instant start = Instant.now();
        
        // 执行去重合并
        MergeResult<UserRecord> result = mergeTool.mergeExcel(config);
        
        Instant end = Instant.now();
        processingTimeMs = Duration.between(start, end).toMillis();
        
        long memoryAfter = getUsedMemory();
        
        // 分析结果数据
        if (result.isSuccess() && result.getData() != null) {
            analyzeResults(result.getData(), cityStats, totalRecords, uniqueIds);
            
            log.info("去重合并成功: 原始数据约{}行, 去重后{}行, 发现{}个重复记录", 
                    TOTAL_FILES * SHEETS_PER_FILE * ROWS_PER_SHEET,
                    result.getTotalRows(), 
                    duplicateCount.get());
                    
            log.info("耗时: {} ms, 速度: {} 行/秒", 
                    processingTimeMs,
                    result.getRowsPerSecond());
                    
            // 打印城市分布统计
            log.info("城市分布统计:");
            cityStats.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .forEach(entry -> log.info("  {}: {} 人 ({}%)", 
                            entry.getKey(), 
                            entry.getValue(),
                            String.format("%.2f", (entry.getValue() * 100.0 / totalRecords.get()))));
        } else {
            log.error("去重合并失败: {}", result.getErrorMessage());
        }
        
        log.info("去重合并后内存使用: {} MB, 内存差异: {} MB", 
                memoryAfter / (1024 * 1024),
                (memoryAfter - memoryBefore) / (1024 * 1024));
    }
    
    /**
     * 测试过滤条件 (只保留成年人且积分大于1000的记录)
     */
    @Test
    public void testMergeWithFilter() {
        log.info("开始测试带过滤条件的合并");
        
        String outputFile = OUTPUT_DIR + "/merged_filtered_result.xlsx";
        
        long memoryBefore = getUsedMemory();
        
        // 记录进度的计数器
        AtomicInteger progressCounter = new AtomicInteger();
        
        // 实现进度回调
        ProgressCallback progressCallback = (current, total, phase) -> {
            if (progressCounter.incrementAndGet() % 5 == 0) {
                log.info("处理进度: {} - {}/{}", phase, current, total);
            }
        };
        
        ExcelMergeTool mergeTool = new ExcelMergeTool();
        
        // 配置带过滤的合并参数
        MergeConfig<UserRecord> config = MergeConfig.<UserRecord>builder()
                .sourceFiles(testFiles)
                .targetFile(outputFile)
                .modelClass(UserRecord.class)
                .progressCallback(progressCallback)
                // 只保留成年人且积分大于1000的记录
                .filter(record -> 
                    record.getAge() != null && record.getAge() >= 18 && 
                    record.getPoints() != null && record.getPoints() > 1000)
                .build();
        
        Instant start = Instant.now();
        
        // 执行过滤合并
        MergeResult<UserRecord> result = mergeTool.mergeExcel(config);
        
        Instant end = Instant.now();
        processingTimeMs = Duration.between(start, end).toMillis();
        
        long memoryAfter = getUsedMemory();
        
        // 打印结果统计
        if (result.isSuccess()) {
            log.info("过滤合并成功: 原始数据约{}行, 过滤后{}行", 
                    TOTAL_FILES * SHEETS_PER_FILE * ROWS_PER_SHEET,
                    result.getTotalRows());
                    
            double filterRate = 100.0 - (result.getTotalRows() * 100.0 / 
                    (TOTAL_FILES * SHEETS_PER_FILE * ROWS_PER_SHEET));
                    
            log.info("过滤率: {}%, 耗时: {} ms, 速度: {} 行/秒", 
                    String.format("%.2f", filterRate),
                    processingTimeMs,
                    result.getRowsPerSecond());
        } else {
            log.error("过滤合并失败: {}", result.getErrorMessage());
        }
        
        log.info("过滤合并后内存使用: {} MB, 内存差异: {} MB", 
                memoryAfter / (1024 * 1024),
                (memoryAfter - memoryBefore) / (1024 * 1024));
    }
    
    /**
     * 分析结果数据
     */
    private void analyzeResults(List<UserRecord> data, Map<String, Integer> cityStats, 
                              AtomicInteger totalRecords, Set<String> uniqueIds) {
        data.forEach(record -> {
            totalRecords.incrementAndGet();
            uniqueIds.add(record.getId());
            
            if (record.getCity() != null) {
                cityStats.compute(record.getCity(), (k, v) -> (v == null) ? 1 : v + 1);
            }
        });
    }
    
    /**
     * 获取当前已使用内存(字节)
     */
    private long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
    
    /**
     * 测试完成后的清理工作
     */
    @AfterAll
    public void tearDown() {
        log.info("测试完成，测试数据生成耗时: {} ms, 合并处理耗时: {} ms", 
                generationTimeMs, processingTimeMs);
                
        // 汇总内存使用情况
        long finalMemory = getUsedMemory();
        log.info("内存使用情况: 初始 {} MB, 峰值 {} MB, 最终 {} MB", 
                initialMemory / (1024 * 1024),
                peakMemory / (1024 * 1024),
                finalMemory / (1024 * 1024));
                
        // 可选：清理生成的测试文件
        // cleanupTestFiles();
    }
    
    /**
     * 清理生成的测试文件
     */
    private void cleanupTestFiles() {
        log.info("开始清理测试文件...");
        
        // 清理测试目录中的文件
        try {
            File testDir = new File(TEST_DIR);
            if (testDir.exists() && testDir.isDirectory()) {
                for (File file : testDir.listFiles()) {
                    if (file.isFile() && file.getName().endsWith(".xlsx")) {
                        if (file.delete()) {
                            log.info("已删除文件: {}", file.getAbsolutePath());
                        } else {
                            log.warn("无法删除文件: {}", file.getAbsolutePath());
                        }
                    }
                }
            }
            
            // 清理输出目录中的文件
            File outputDir = new File(OUTPUT_DIR);
            if (outputDir.exists() && outputDir.isDirectory()) {
                for (File file : outputDir.listFiles()) {
                    if (file.isFile() && file.getName().endsWith(".xlsx")) {
                        if (file.delete()) {
                            log.info("已删除文件: {}", file.getAbsolutePath());
                        } else {
                            log.warn("无法删除文件: {}", file.getAbsolutePath());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("清理测试文件时发生错误", e);
        }
    }
} 