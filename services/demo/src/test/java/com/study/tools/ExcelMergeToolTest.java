package com.study.tools;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Excel合并工具单元测试
 */
public class ExcelMergeToolTest {

    @TempDir
    Path tempDir;
    
    private Path dataDir;
    private Path outputDir;
    
    // 测试文件路径
    private String productsFile1;
    private String productsFile2;
    private String ordersFile1;
    private String ordersFile2;
    private String nonExistentFile;
    
    // 测试数据
    private List<Product> products1;
    private List<Product> products2;
    private List<Order> orders1;
    private List<Order> orders2;
    
    @BeforeEach
    void setUp() throws IOException {
        // 创建数据目录和输出目录
        dataDir = tempDir.resolve("test-data");
        outputDir = tempDir.resolve("test-output");
        Files.createDirectories(dataDir);
        Files.createDirectories(outputDir);
        
        // 设置文件路径
        productsFile1 = dataDir.resolve("products1.xlsx").toString();
        productsFile2 = dataDir.resolve("products2.xlsx").toString();
        ordersFile1 = dataDir.resolve("orders1.xlsx").toString();
        ordersFile2 = dataDir.resolve("orders2.xlsx").toString();
        nonExistentFile = dataDir.resolve("non-existent.xlsx").toString();
        
        // 准备测试数据
        prepareTestData();
        
        // 生成测试文件
        generateTestFiles();
    }
    
    @AfterEach
    void tearDown() {
        // 清理测试文件
        new File(productsFile1).delete();
        new File(productsFile2).delete();
        new File(ordersFile1).delete();
        new File(ordersFile2).delete();
        
        // 清理输出目录中的所有文件
        File[] outputFiles = new File(outputDir.toString()).listFiles();
        if (outputFiles != null) {
            for (File file : outputFiles) {
                file.delete();
            }
        }
    }
    
    /**
     * 准备测试数据
     */
    private void prepareTestData() {
        // 产品数据1
        products1 = new ArrayList<>();
        products1.add(new Product("P001", "Product 1", new BigDecimal("100.00"), 10, "Category A", true));
        products1.add(new Product("P002", "Product 2", new BigDecimal("200.00"), 20, "Category B", true));
        products1.add(new Product("P003", "Product 3", new BigDecimal("300.00"), 0, "Category A", true));
        products1.add(new Product("P004", "Product 4", new BigDecimal("400.00"), 40, "Category C", false));
        
        // 产品数据2（包含一些重复项和新项）
        products2 = new ArrayList<>();
        products2.add(new Product("P001", "Product 1", new BigDecimal("100.00"), 5, "Category A", true)); // 库存减少了
        products2.add(new Product("P003", "Product 3", new BigDecimal("350.00"), 30, "Category A", true)); // 价格和库存更新了
        products2.add(new Product("P005", "Product 5", new BigDecimal("500.00"), 50, "Category B", true)); // 新产品
        products2.add(new Product("P006", "Product 6", new BigDecimal("600.00"), 0, "Category C", false)); // 新产品但无库存且未激活
        
        // 订单数据1
        orders1 = new ArrayList<>();
        orders1.add(new Order("O001", "Customer A", new Date(System.currentTimeMillis() - 86400000), "Completed", new BigDecimal("100.00")));
        orders1.add(new Order("O002", "Customer B", new Date(System.currentTimeMillis() - 172800000), "Processing", new BigDecimal("200.00")));
        
        // 订单数据2（包含更新的订单状态）
        orders2 = new ArrayList<>();
        orders2.add(new Order("O001", "Customer A", new Date(System.currentTimeMillis()), "Shipped", new BigDecimal("100.00"))); // 状态更新了
        orders2.add(new Order("O003", "Customer C", new Date(), "New", new BigDecimal("300.00"))); // 新订单
    }
    
    /**
     * 生成测试Excel文件
     */
    private void generateTestFiles() {
        // 写入产品数据
        EasyExcel.write(productsFile1, Product.class).sheet("Products").doWrite(products1);
        EasyExcel.write(productsFile2, Product.class).sheet("Products").doWrite(products2);
        
        // 写入订单数据
        EasyExcel.write(ordersFile1, Order.class).sheet("Orders").doWrite(orders1);
        EasyExcel.write(ordersFile2, Order.class).sheet("Orders").doWrite(orders2);
    }
    
    /**
     * 测试基本合并功能
     */
    @Test
    void testBasicMerge() {
        // 设置合并配置
        String outputFile = outputDir.resolve("merged_products.xlsx").toString();
        ExcelMergeTool.MergeConfig<Product> config = ExcelMergeTool.MergeConfig.<Product>builder()
                .sourceFiles(Arrays.asList(productsFile1, productsFile2))
                .targetFile(outputFile)
                .modelClass(Product.class)
                .build();
        
        // 执行合并
        ExcelMergeTool.MergeResult<Product> result = ExcelMergeTool.mergeExcel(config);
        
        // 验证合并成功
        assertTrue(result.isSuccess());
        assertEquals(8, result.getTotalRows()); // 所有产品都应该被合并
        assertTrue(new File(outputFile).exists());
        
        // 读取合并后的Excel文件，验证内容
        List<Product> mergedProducts = EasyExcel.read(outputFile).head(Product.class).sheet().doReadSync();
        
        // 验证所有产品ID都存在
        Set<String> productIds = mergedProducts.stream()
                .map(Product::getProductId)
                .collect(Collectors.toSet());
        
        assertEquals(6, productIds.size());
        assertTrue(productIds.contains("P001"));
        assertTrue(productIds.contains("P002"));
        assertTrue(productIds.contains("P003"));
        assertTrue(productIds.contains("P004"));
        assertTrue(productIds.contains("P005"));
        assertTrue(productIds.contains("P006"));
    }
    
    /**
     * 测试去重合并功能
     */
    @Test
    void testDeduplicationMerge() {
        // 设置合并配置，根据产品ID去重，保留库存最高的版本
        String outputFile = outputDir.resolve("merged_products_deduplicated.xlsx").toString();
        ExcelMergeTool.MergeConfig<Product> config = ExcelMergeTool.MergeConfig.<Product>builder()
                .sourceFiles(Arrays.asList(productsFile1, productsFile2))
                .targetFile(outputFile)
                .modelClass(Product.class)
                .enableDeduplication(true)
                .keyExtractor(Product::getProductId)
                .valueSelector((existing, newItem) -> 
                    existing.getStock() > newItem.getStock() ? existing : newItem)
                .build();
        
        // 执行合并
        ExcelMergeTool.MergeResult<Product> result = ExcelMergeTool.mergeExcel(config);
        
        // 验证合并成功
        assertTrue(result.isSuccess());
        assertEquals(6, result.getTotalRows()); // 去重后应该有6个产品
        assertTrue(new File(outputFile).exists());
        
        // 读取合并后的Excel文件，验证内容
        List<Product> mergedProducts = EasyExcel.read(outputFile).head(Product.class).sheet().doReadSync();
        
        // 把合并后的产品转换为Map，方便查找
        Map<String, Product> productMap = mergedProducts.stream()
                .collect(Collectors.toMap(Product::getProductId, Function.identity()));
        
        // 验证P001应该保留库存为10的版本（products1中的）
        assertEquals(10, productMap.get("P001").getStock());
        
        // 验证P003应该保留库存为30的版本（products2中的）
        assertEquals(30, productMap.get("P003").getStock());
        assertEquals(new BigDecimal("350.00"), productMap.get("P003").getPrice());
    }
    
    /**
     * 测试过滤合并功能
     */
    @Test
    void testFilteredMerge() {
        // 设置合并配置，只保留激活状态且有库存的产品
        String outputFile = outputDir.resolve("merged_products_filtered.xlsx").toString();
        ExcelMergeTool.MergeConfig<Product> config = ExcelMergeTool.MergeConfig.<Product>builder()
                .sourceFiles(Arrays.asList(productsFile1, productsFile2))
                .targetFile(outputFile)
                .modelClass(Product.class)
                .filter(product -> product.isActive() && product.getStock() > 0)
                .build();
        
        // 执行合并
        ExcelMergeTool.MergeResult<Product> result = ExcelMergeTool.mergeExcel(config);
        
        // 验证合并成功
        assertTrue(result.isSuccess());
        
        // 读取合并后的Excel文件，验证内容
        List<Product> mergedProducts = EasyExcel.read(outputFile).head(Product.class).sheet().doReadSync();
        
        // 验证过滤条件生效：所有产品都应该是激活状态且有库存
        assertEquals(5, mergedProducts.size());
        for (Product product : mergedProducts) {
            assertTrue(product.isActive());
            assertTrue(product.getStock() > 0);
        }
        
        // 验证产品P003、P004和P006不应该出现在结果中（P003在products1中无库存，P004和P006未激活或无库存）
        Set<String> productIds = mergedProducts.stream()
                .map(Product::getProductId)
                .collect(Collectors.toSet());
        
        assertTrue(productIds.contains("P001")); // 两个文件中都有，且满足条件
        assertTrue(productIds.contains("P002")); // 只在file1中有，满足条件
        assertTrue(productIds.contains("P003")); // 在file2中满足条件
        assertFalse(productIds.contains("P004")); // 未激活，不符合条件
        assertTrue(productIds.contains("P005")); // 满足条件
        assertFalse(productIds.contains("P006")); // 未激活且无库存，不符合条件
    }
    
    /**
     * 测试订单去重功能（基于更新时间）
     */
    @Test
    void testOrderDeduplicationMerge() {
        // 设置合并配置，根据订单ID去重，保留最新更新时间的版本
        String outputFile = outputDir.resolve("merged_orders_deduplicated.xlsx").toString();
        ExcelMergeTool.MergeConfig<Order> config = ExcelMergeTool.MergeConfig.<Order>builder()
                .sourceFiles(Arrays.asList(ordersFile1, ordersFile2))
                .targetFile(outputFile)
                .modelClass(Order.class)
                .enableDeduplication(true)
                .keyExtractor(Order::getOrderId)
                .valueSelector((existing, newItem) -> 
                    existing.getUpdateTime().after(newItem.getUpdateTime()) ? existing : newItem)
                .build();
        
        // 执行合并
        ExcelMergeTool.MergeResult<Order> result = ExcelMergeTool.mergeExcel(config);
        
        // 验证合并成功
        assertTrue(result.isSuccess());
        assertEquals(3, result.getTotalRows()); // 去重后应该有3个订单
        assertTrue(new File(outputFile).exists());
        
        // 读取合并后的Excel文件，验证内容
        List<Order> mergedOrders = EasyExcel.read(outputFile).head(Order.class).sheet().doReadSync();
        
        // 把合并后的订单转换为Map，方便查找
        Map<String, Order> orderMap = mergedOrders.stream()
                .collect(Collectors.toMap(Order::getOrderId, Function.identity()));
        
        // 验证O001应该保留状态为"Shipped"的版本（orders2中的，时间更新）
        assertEquals("Shipped", orderMap.get("O001").getStatus());
        
        // 验证所有订单ID都存在
        assertTrue(orderMap.containsKey("O001"));
        assertTrue(orderMap.containsKey("O002"));
        assertTrue(orderMap.containsKey("O003"));
    }
    
    /**
     * 测试错误处理 - 文件不存在
     */
    @Test
    void testNonExistentFile() {
        // 设置合并配置，包含一个不存在的文件
        String outputFile = outputDir.resolve("error_output.xlsx").toString();
        ExcelMergeTool.MergeConfig<Product> config = ExcelMergeTool.MergeConfig.<Product>builder()
                .sourceFiles(Arrays.asList(productsFile1, nonExistentFile))
                .targetFile(outputFile)
                .modelClass(Product.class)
                .build();
        
        // 执行合并
        ExcelMergeTool.MergeResult<Product> result = ExcelMergeTool.mergeExcel(config);
        
        // 验证结果
        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("不存在") || 
                   result.getErrorMessage().contains("not exist") ||
                   result.getErrorMessage().contains("not found"));
    }
    
    /**
     * 测试错误处理 - 缺少必要参数
     */
    @Test
    void testMissingRequiredParameters() {
        // 缺少源文件
        ExcelMergeTool.MergeConfig<Product> config1 = ExcelMergeTool.MergeConfig.<Product>builder()
                .targetFile(outputDir.resolve("missing_source.xlsx").toString())
                .modelClass(Product.class)
                .build();
        
        Exception exception1 = assertThrows(IllegalArgumentException.class, () -> {
            ExcelMergeTool.mergeExcel(config1);
        });
        assertTrue(exception1.getMessage().contains("sourceFiles") || 
                   exception1.getMessage().contains("source files"));
        
        // 缺少目标文件
        ExcelMergeTool.MergeConfig<Product> config2 = ExcelMergeTool.MergeConfig.<Product>builder()
                .sourceFiles(Arrays.asList(productsFile1))
                .modelClass(Product.class)
                .build();
        
        Exception exception2 = assertThrows(IllegalArgumentException.class, () -> {
            ExcelMergeTool.mergeExcel(config2);
        });
        assertTrue(exception2.getMessage().contains("targetFile") || 
                   exception2.getMessage().contains("target file"));
        
        // 缺少模型类
        ExcelMergeTool.MergeConfig<Product> config3 = ExcelMergeTool.MergeConfig.<Product>builder()
                .sourceFiles(Arrays.asList(productsFile1))
                .targetFile(outputDir.resolve("missing_model.xlsx").toString())
                .build();
        
        Exception exception3 = assertThrows(IllegalArgumentException.class, () -> {
            ExcelMergeTool.mergeExcel(config3);
        });
        assertTrue(exception3.getMessage().contains("modelClass") || 
                   exception3.getMessage().contains("model class"));
    }
    
    /**
     * 测试错误处理 - 缺少去重键提取器
     */
    @Test
    void testDeduplicationWithoutKeyExtractor() {
        // 启用去重但未提供键提取器
        ExcelMergeTool.MergeConfig<Product> config = ExcelMergeTool.MergeConfig.<Product>builder()
                .sourceFiles(Arrays.asList(productsFile1, productsFile2))
                .targetFile(outputDir.resolve("dedup_error.xlsx").toString())
                .modelClass(Product.class)
                .enableDeduplication(true) // 启用去重
                // 但未设置keyExtractor
                .build();
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            ExcelMergeTool.mergeExcel(config);
        });
        assertTrue(exception.getMessage().contains("keyExtractor") || 
                   exception.getMessage().contains("key extractor"));
    }
    
    /**
     * 产品测试数据模型
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Product {
        @ExcelProperty("产品ID")
        private String productId;
        
        @ExcelProperty("产品名称")
        private String name;
        
        @ExcelProperty("价格")
        private BigDecimal price;
        
        @ExcelProperty("库存")
        private Integer stock;
        
        @ExcelProperty("类别")
        private String category;
        
        @ExcelProperty("是否激活")
        private boolean active;
    }
    
    /**
     * 订单测试数据模型
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Order {
        @ExcelProperty("订单ID")
        private String orderId;
        
        @ExcelProperty("客户")
        private String customer;
        
        @ExcelProperty("更新时间")
        private Date updateTime;
        
        @ExcelProperty("状态")
        private String status;
        
        @ExcelProperty("金额")
        private BigDecimal amount;
    }
} 