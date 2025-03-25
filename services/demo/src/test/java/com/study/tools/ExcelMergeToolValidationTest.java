//package com.study.tools;
//
//import com.alibaba.excel.EasyExcel;
//import com.alibaba.excel.annotation.ExcelProperty;
//import com.study.tools.excelNew.ExcelMergeTool;
//import lombok.AllArgsConstructor;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//import org.junit.jupiter.api.BeforeEach;
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
//import java.util.concurrent.TimeUnit;
//
//import static org.junit.jupiter.api.Assertions.*;
//
///**
// * Excel合并工具的验证和异常处理测试
// */
//public class ExcelMergeToolValidationTest {
//
//    @TempDir
//    Path tempDir;
//
//    private Path dataDir;
//    private Path outputDir;
//
//    private String validFile;
//    private String emptyFile;
//    private String malformedFile;
//    private String wrongModelFile;
//
//    @BeforeEach
//    void setUp() throws IOException {
//        // 创建数据目录和输出目录
//        dataDir = tempDir.resolve("validation-data");
//        outputDir = tempDir.resolve("validation-output");
//        Files.createDirectories(dataDir);
//        Files.createDirectories(outputDir);
//
//        // 设置文件路径
//        validFile = dataDir.resolve("valid.xlsx").toString();
//        emptyFile = dataDir.resolve("empty.xlsx").toString();
//        malformedFile = dataDir.resolve("malformed.txt").toString();
//        wrongModelFile = dataDir.resolve("wrong_model.xlsx").toString();
//
//        // 生成测试文件
//        generateTestFiles();
//    }
//
//    /**
//     * 生成测试文件
//     */
//    private void generateTestFiles() throws IOException {
//        // 创建有效数据
//        List<TestProduct> validProducts = new ArrayList<>();
//        validProducts.add(new TestProduct("P001", "Product 1", new BigDecimal("100.00"), 10));
//        validProducts.add(new TestProduct("P002", "Product 2", new BigDecimal("200.00"), 20));
//
//        // 写入有效Excel文件
//        EasyExcel.write(validFile, TestProduct.class).sheet("Products").doWrite(validProducts);
//
//        // 创建空Excel文件
//        EasyExcel.write(emptyFile, TestProduct.class).sheet("Empty").doWrite(new ArrayList<>());
//
//        // 创建格式错误的文件
//        Files.write(Path.of(malformedFile), Arrays.asList("This is not a valid Excel file"));
//
//        // 创建模型不匹配的Excel文件
//        List<TestOrder> wrongModelData = new ArrayList<>();
//        wrongModelData.add(new TestOrder("O001", "Customer A", new Date(), "Active"));
//        EasyExcel.write(wrongModelFile, TestOrder.class).sheet("Orders").doWrite(wrongModelData);
//    }
//
//    /**
//     * 测试空源文件列表
//     */
//    @Test
//    void testEmptySourceFilesList() {
//        String outputFile = outputDir.resolve("output.xlsx").toString();
//
//        ExcelMergeTool.MergeConfig<TestProduct> config = ExcelMergeTool.MergeConfig.<TestProduct>builder()
//                .sourceFiles(new ArrayList<>()) // 空列表
//                .targetFile(outputFile)
//                .modelClass(TestProduct.class)
//                .build();
//
//        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
//            ExcelMergeTool.mergeExcel(config);
//        });
//
//        assertTrue(exception.getMessage().contains("source") &&
//                  (exception.getMessage().contains("empty") ||
//                   exception.getMessage().contains("not provided")));
//    }
//
//    /**
//     * 测试空Excel文件
//     */
//    @Test
//    void testEmptyExcelFile() {
//        String outputFile = outputDir.resolve("empty_output.xlsx").toString();
//
//        ExcelMergeTool.MergeConfig<TestProduct> config = ExcelMergeTool.MergeConfig.<TestProduct>builder()
//                .sourceFiles(Arrays.asList(emptyFile))
//                .targetFile(outputFile)
//                .modelClass(TestProduct.class)
//                .build();
//
//        ExcelMergeTool.MergeResult<TestProduct> result = ExcelMergeTool.mergeExcel(config);
//
//        // 空文件应该被正常处理，但结果应该是0行
//        assertTrue(result.isSuccess());
//        assertEquals(0, result.getTotalRows());
//    }
//
//    /**
//     * 测试格式错误的文件
//     */
//    @Test
//    void testMalformedFile() {
//        String outputFile = outputDir.resolve("malformed_output.xlsx").toString();
//
//        ExcelMergeTool.MergeConfig<TestProduct> config = ExcelMergeTool.MergeConfig.<TestProduct>builder()
//                .sourceFiles(Arrays.asList(malformedFile))
//                .targetFile(outputFile)
//                .modelClass(TestProduct.class)
//                .build();
//
//        ExcelMergeTool.MergeResult<TestProduct> result = ExcelMergeTool.mergeExcel(config);
//
//        // 应该失败，并提示格式错误
//        assertFalse(result.isSuccess());
//        assertNotNull(result.getErrorMessage());
//        assertTrue(result.getErrorMessage().contains("format") ||
//                  result.getErrorMessage().contains("parse") ||
//                  result.getErrorMessage().contains("read"));
//    }
//
//    /**
//     * 测试模型不匹配
//     */
//    @Test
//    void testWrongModel() {
//        String outputFile = outputDir.resolve("wrong_model_output.xlsx").toString();
//
//        ExcelMergeTool.MergeConfig<TestProduct> config = ExcelMergeTool.MergeConfig.<TestProduct>builder()
//                .sourceFiles(Arrays.asList(wrongModelFile))
//                .targetFile(outputFile)
//                .modelClass(TestProduct.class) // 使用Product模型读取Order数据
//                .build();
//
//        ExcelMergeTool.MergeResult<TestProduct> result = ExcelMergeTool.mergeExcel(config);
//
//        // 模型不匹配可能会导致解析错误或数据异常
//        // 具体结果取决于EasyExcel的行为，但我们至少应该捕获异常并返回失败
//        assertFalse(result.isSuccess());
//        assertNotNull(result.getErrorMessage());
//    }
//
//    /**
//     * 测试目标文件路径问题
//     */
//    @Test
//    void testInvalidTargetPath() {
//        // 使用一个不存在的目录
//        String invalidOutputFile = "/invalid_dir/output.xlsx";
//
//        ExcelMergeTool.MergeConfig<TestProduct> config = ExcelMergeTool.MergeConfig.<TestProduct>builder()
//                .sourceFiles(Arrays.asList(validFile))
//                .targetFile(invalidOutputFile)
//                .modelClass(TestProduct.class)
//                .build();
//
//        ExcelMergeTool.MergeResult<TestProduct> result = ExcelMergeTool.mergeExcel(config);
//
//        // 应该失败，因为目标路径无效
//        assertFalse(result.isSuccess());
//        assertNotNull(result.getErrorMessage());
//        assertTrue(result.getErrorMessage().contains("directory") ||
//                  result.getErrorMessage().contains("path") ||
//                  result.getErrorMessage().contains("write"));
//    }
//
//    /**
//     * 测试自定义线程池关闭
//     */
//    @Test
//    void testCustomExecutorShutdown() throws InterruptedException {
//        String outputFile = outputDir.resolve("executor_test.xlsx").toString();
//
//        // 创建可跟踪的自定义线程池
//        ExecutorService executor = Executors.newFixedThreadPool(2);
//
//        ExcelMergeTool.MergeConfig<TestProduct> config = ExcelMergeTool.MergeConfig.<TestProduct>builder()
//                .sourceFiles(Arrays.asList(validFile))
//                .targetFile(outputFile)
//                .modelClass(TestProduct.class)
//                .executor(executor)
//                .shutdownExecutor(false) // 不自动关闭线程池
//                .build();
//
//        // 执行合并
//        ExcelMergeTool.MergeResult<TestProduct> result = ExcelMergeTool.mergeExcel(config);
//
//        // 验证合并成功
//        assertTrue(result.isSuccess());
//
//        // 验证线程池没有被关闭
//        assertFalse(executor.isShutdown());
//
//        // 手动关闭线程池
//        executor.shutdown();
//        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
//    }
//
//    /**
//     * 测试批处理大小设置
//     */
//    @Test
//    void testBatchSizeValidation() {
//        String outputFile = outputDir.resolve("batch_size_test.xlsx").toString();
//
//        // 设置过小的批处理大小
//        ExcelMergeTool.MergeConfig<TestProduct> config = ExcelMergeTool.MergeConfig.<TestProduct>builder()
//                .sourceFiles(Arrays.asList(validFile))
//                .targetFile(outputFile)
//                .modelClass(TestProduct.class)
//                .batchSize(-1) // 非法批处理大小
//                .build();
//
//        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
//            ExcelMergeTool.mergeExcel(config);
//        });
//
//        assertTrue(exception.getMessage().contains("batch") &&
//                  exception.getMessage().contains("size"));
//    }
//
//    /**
//     * 测试多文件合并错误处理
//     */
//    @Test
//    void testPartialFailure() {
//        String outputFile = outputDir.resolve("partial_failure.xlsx").toString();
//
//        // 一个有效文件和一个无效文件
//        ExcelMergeTool.MergeConfig<TestProduct> config = ExcelMergeTool.MergeConfig.<TestProduct>builder()
//                .sourceFiles(Arrays.asList(validFile, malformedFile))
//                .targetFile(outputFile)
//                .modelClass(TestProduct.class)
//                .build();
//
//        ExcelMergeTool.MergeResult<TestProduct> result = ExcelMergeTool.mergeExcel(config);
//
//        // 由于有一个文件格式错误，整个合并应该失败
//        assertFalse(result.isSuccess());
//        assertNotNull(result.getErrorMessage());
//    }
//
//    /**
//     * 测试进度回调
//     */
//    @Test
//    void testProgressCallback() {
//        String outputFile = outputDir.resolve("progress_test.xlsx").toString();
//
//        final int[] progressUpdates = {0};
//        final long[] startTime = {0};
//        final long[] endTime = {0};
//
//        ExcelMergeTool.MergeConfig<TestProduct> config = ExcelMergeTool.MergeConfig.<TestProduct>builder()
//                .sourceFiles(Arrays.asList(validFile))
////                .targetFile(outputFile)
//                .modelClass(TestProduct.class)
//                .progressCallback((current, total, phase) -> {
//                    progressUpdates[0]++;
//                    if (startTime[0] == 0) {
//                        startTime[0] = System.currentTimeMillis();
//                    }
//                    endTime[0] = System.currentTimeMillis();
//                })
//                .build();
//
//        // 执行合并
//        ExcelMergeTool.MergeResult<TestProduct> result = ExcelMergeTool.mergeExcel(config);
//
//        // 验证进度回调被调用
//        assertTrue(result.isSuccess());
//        assertTrue(progressUpdates[0] > 0);
//        assertTrue(endTime[0] > startTime[0]);
//    }
//
//    /**
//     * 测试数据模型
//     */
//    @Data
//    @NoArgsConstructor
//    @AllArgsConstructor
//    public static class TestProduct {
//        @ExcelProperty("产品ID")
//        private String id;
//
//        @ExcelProperty("名称")
//        private String name;
//
//        @ExcelProperty("价格")
//        private BigDecimal price;
//
//        @ExcelProperty("库存")
//        private Integer stock;
//    }
//
//    /**
//     * 测试订单模型（与产品模型不兼容）
//     */
//    @Data
//    @NoArgsConstructor
//    @AllArgsConstructor
//    public static class TestOrder {
//        @ExcelProperty("订单ID")
//        private String id;
//
//        @ExcelProperty("客户")
//        private String customer;
//
//        @ExcelProperty("日期")
//        private Date date;
//
//        @ExcelProperty("状态")
//        private String status;
//    }
//}