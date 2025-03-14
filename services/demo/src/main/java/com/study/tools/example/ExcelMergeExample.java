package com.study.tools.example;

import com.alibaba.excel.annotation.ExcelProperty;
import com.study.tools.ExcelMergeTool;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Excel合并工具使用示例
 */
public class ExcelMergeExample {

    public static void main(String[] args) {
        // 示例1：基本合并
        basicMergeExample();
        
        // 示例2：去重合并
        deduplicationExample();
        
        // 示例3：数据过滤
        filteringExample();
        
        // 示例4：性能优化
        performanceOptimizedExample();
    }
    
    /**
     * 基本合并示例
     */
    private static void basicMergeExample() {
        System.out.println("\n===== 基本合并示例 =====");
        
        // 创建合并配置
        ExcelMergeTool.MergeConfig<ProductData> config = ExcelMergeTool.MergeConfig.<ProductData>builder()
                .sourceFiles(Arrays.asList(
                        "./data/products1.xlsx",
                        "./data/products2.xlsx"))
                .targetFile("./output/merged_products.xlsx")
                .modelClass(ProductData.class)
                .build();
        
        // 执行合并
        ExcelMergeTool.MergeResult<ProductData> result = ExcelMergeTool.mergeExcel(config);
        
        // 输出结果
        if (result.isSuccess()) {
            System.out.println("合并成功！");
            System.out.println("总行数: " + result.getTotalRows());
            System.out.println("处理时间: " + result.getTimeMillis() + "ms");
            System.out.println("处理速度: " + String.format("%.2f", result.getRowsPerSecond()) + " 行/秒");
            System.out.println("输出文件: " + result.getOutputFile());
        } else {
            System.err.println("合并失败: " + result.getErrorMessage());
        }
    }
    
    /**
     * 去重合并示例
     */
    private static void deduplicationExample() {
        System.out.println("\n===== 去重合并示例 =====");
        
        // 创建合并配置，使用订单ID作为唯一键
        ExcelMergeTool.MergeConfig<OrderData> config = ExcelMergeTool.MergeConfig.<OrderData>builder()
                .sourceFiles(Arrays.asList(
                        "./data/orders1.xlsx",
                        "./data/orders2.xlsx"))
                .targetFile("./output/merged_orders_deduplicated.xlsx")
                .modelClass(OrderData.class)
                .enableDeduplication(true)
                .keyExtractor(order -> order.getOrderId()) // 提取键
                .mergeFunction((existing, newItem) -> {
                    // 合并逻辑：保留修改时间较新的记录
                    if (newItem.getUpdateTime().after(existing.getUpdateTime())) {
                        return newItem;
                    } else {
                        return existing;
                    }
                })
                .build();
        
        // 执行合并
        ExcelMergeTool.MergeResult<OrderData> result = ExcelMergeTool.mergeExcel(config);
        
        // 输出结果
        if (result.isSuccess()) {
            System.out.println("去重合并成功！");
            System.out.println("总行数: " + result.getTotalRows());
            System.out.println("输出文件: " + result.getOutputFile());
        } else {
            System.err.println("合并失败: " + result.getErrorMessage());
        }
    }
    
    /**
     * 数据过滤示例
     */
    private static void filteringExample() {
        System.out.println("\n===== 数据过滤示例 =====");
        
        // 创建合并配置，过滤掉库存为0或状态非活跃的产品
        ExcelMergeTool.MergeConfig<ProductData> config = ExcelMergeTool.MergeConfig.<ProductData>builder()
                .sourceFiles(Arrays.asList(
                        "./data/products1.xlsx",
                        "./data/products2.xlsx",
                        "./data/products3.xlsx"))
                .targetFile("./output/merged_products_filtered.xlsx")
                .modelClass(ProductData.class)
                .filter(product -> 
                        product.isActive() && 
                        product.getStock() > 0 && 
                        product.getPrice().compareTo(BigDecimal.ZERO) > 0)
                .build();
        
        // 执行合并
        ExcelMergeTool.MergeResult<ProductData> result = ExcelMergeTool.mergeExcel(config);
        
        // 输出结果
        if (result.isSuccess()) {
            System.out.println("过滤合并成功！");
            System.out.println("总行数: " + result.getTotalRows());
            System.out.println("输出文件: " + result.getOutputFile());
        } else {
            System.err.println("合并失败: " + result.getErrorMessage());
        }
    }
    
    /**
     * 性能优化示例
     */
    private static void performanceOptimizedExample() {
        System.out.println("\n===== 性能优化示例 =====");
        
        // 创建优化的线程池
        ExecutorService executor = new ThreadPoolExecutor(
            8,                          // 核心线程数
            16,                         // 最大线程数
            60L,                        // 空闲线程存活时间
            TimeUnit.SECONDS,           // 时间单位
            new LinkedBlockingQueue<>(1000),  // 工作队列
            new ThreadPoolExecutor.CallerRunsPolicy()  // 拒绝策略
        );
        
        // 创建合并配置，包含性能优化参数
        ExcelMergeTool.MergeConfig<LargeVolumeData> config = ExcelMergeTool.MergeConfig.<LargeVolumeData>builder()
                .sourceFiles(Arrays.asList(
                        "./data/large1.xlsx",
                        "./data/large2.xlsx",
                        "./data/large3.xlsx",
                        "./data/large4.xlsx"))
                .targetFile("./output/merged_large_data.xlsx")
                .modelClass(LargeVolumeData.class)
                .batchSize(10000) // 增大批处理大小
                .executor(executor) // 使用优化的线程池
                .enableDeduplication(true)
                .keyExtractor(data -> data.getId())
                .mergeFunction((a, b) -> a.getValue() > b.getValue() ? a : b) // 保留值较大的记录
                .build();
        
        // 执行合并
        ExcelMergeTool.MergeResult<LargeVolumeData> result = ExcelMergeTool.mergeExcel(config);
        
        // 关闭线程池
        executor.shutdown();
        
        // 输出结果
        if (result.isSuccess()) {
            System.out.println("大数据量合并成功！");
            System.out.println("总行数: " + result.getTotalRows());
            System.out.println("处理时间: " + result.getTimeMillis() + "ms");
            System.out.println("处理速度: " + String.format("%.2f", result.getRowsPerSecond()) + " 行/秒");
            System.out.println("输出文件: " + result.getOutputFile());
        } else {
            System.err.println("合并失败: " + result.getErrorMessage());
        }
    }
    
    // 产品数据模型
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductData {
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
        
        @ExcelProperty("是否活跃")
        private boolean active;
        
        @ExcelProperty("创建时间")
        private Date createTime;
    }
    
    // 订单数据模型
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderData {
        @ExcelProperty("订单ID")
        private String orderId;
        
        @ExcelProperty("客户ID")
        private String customerId;
        
        @ExcelProperty("订单金额")
        private BigDecimal amount;
        
        @ExcelProperty("订单状态")
        private String status;
        
        @ExcelProperty("创建时间")
        private Date createTime;
        
        @ExcelProperty("更新时间")
        private Date updateTime;
    }
    
    // 大数据量示例模型
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LargeVolumeData {
        @ExcelProperty("ID")
        private String id;
        
        @ExcelProperty("值")
        private double value;
        
        @ExcelProperty("名称")
        private String name;
        
        @ExcelProperty("代码")
        private String code;
        
        @ExcelProperty("时间戳")
        private Date timestamp;
        
        // 更多字段...可根据需要扩展
    }
} 