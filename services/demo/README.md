# 高性能Excel合并工具

基于EasyExcel的高性能Excel合并工具，支持百万级数据处理和多文件合并，无需依赖任何外部中间件。

## 主要特性

- **简洁易用**：Builder模式API设计，链式调用，简单明了
- **高性能处理**：多线程并行读取，分批处理写入，高效处理大数据量
- **内存优化**：分段处理技术，避免OOM异常
- **功能丰富**：支持多文件合并、数据去重、自定义过滤、自定义合并策略
- **无依赖**：不依赖Redis、Kafka等外部中间件，仅依赖EasyExcel核心库

## 环境要求

- JDK 8+
- EasyExcel 3.x
- Lombok

## 依赖配置

```xml
<dependencies>
    <!-- EasyExcel -->
    <dependency>
        <groupId>com.alibaba</groupId>
        <artifactId>easyexcel</artifactId>
        <version>3.3.2</version>
    </dependency>
    
    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>1.18.30</version>
        <scope>provided</scope>
    </dependency>
    
    <!-- SLF4J实现（可选） -->
    <dependency>
        <groupId>ch.qos.logback</groupId>
        <artifactId>logback-classic</artifactId>
        <version>1.4.11</version>
    </dependency>
</dependencies>
```

## 快速开始

### 基本使用

```java
// 创建合并配置
ExcelMergeTool.MergeConfig<ProductData> config = ExcelMergeTool.MergeConfig.<ProductData>builder()
        .sourceFiles(Arrays.asList("file1.xlsx", "file2.xlsx"))
        .targetFile("merged_output.xlsx")
        .modelClass(ProductData.class)
        .build();

// 执行合并
ExcelMergeTool.MergeResult<ProductData> result = ExcelMergeTool.mergeExcel(config);

// 处理结果
if (result.isSuccess()) {
    System.out.println("合并成功！总行数: " + result.getTotalRows());
    System.out.println("处理耗时: " + result.getTimeMillis() + "ms");
} else {
    System.err.println("合并失败: " + result.getErrorMessage());
}
```

### 数据去重

```java
ExcelMergeTool.MergeConfig<OrderData> config = ExcelMergeTool.MergeConfig.<OrderData>builder()
        .sourceFiles(Arrays.asList("orders1.xlsx", "orders2.xlsx"))
        .targetFile("merged_orders.xlsx")
        .modelClass(OrderData.class)
        .enableDeduplication(true)  // 启用去重
        .keyExtractor(order -> order.getOrderId())  // 提取唯一键
        .mergeFunction((existing, newItem) -> {  // 合并策略
            // 保留更新时间较新的记录
            if (newItem.getUpdateTime().after(existing.getUpdateTime())) {
                return newItem;
            } else {
                return existing;
            }
        })
        .build();
```

### 数据过滤

```java
ExcelMergeTool.MergeConfig<ProductData> config = ExcelMergeTool.MergeConfig.<ProductData>builder()
        .sourceFiles(Arrays.asList("products1.xlsx", "products2.xlsx"))
        .targetFile("filtered_products.xlsx")
        .modelClass(ProductData.class)
        .filter(product -> 
                product.isActive() && 
                product.getStock() > 0 && 
                product.getPrice().compareTo(BigDecimal.ZERO) > 0)
        .build();
```

### 性能优化

```java
// 创建自定义线程池
ExecutorService executor = Executors.newFixedThreadPool(8);

ExcelMergeTool.MergeConfig<LargeData> config = ExcelMergeTool.MergeConfig.<LargeData>builder()
        .sourceFiles(Arrays.asList("large1.xlsx", "large2.xlsx"))
        .targetFile("merged_large.xlsx")
        .modelClass(LargeData.class)
        .batchSize(10000)  // 增大批处理大小
        .executor(executor)  // 自定义线程池
        .build();

try {
    ExcelMergeTool.MergeResult<LargeData> result = ExcelMergeTool.mergeExcel(config);
    System.out.println("处理速度: " + result.getRowsPerSecond() + " 行/秒");
} finally {
    executor.shutdown();  // 记得关闭线程池
}
```

## 数据模型定义

使用EasyExcel的`@ExcelProperty`注解标记数据模型字段：

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductData {
    @ExcelProperty("产品ID")
    private String productId;
    
    @ExcelProperty("产品名称")
    private String name;
    
    @ExcelProperty("价格")
    private BigDecimal price;
    
    @ExcelProperty("库存")
    private Integer stock;
    
    @ExcelProperty("是否活跃")
    private boolean active;
}
```

## 配置选项说明

| 配置项 | 说明 | 默认值 |
| ---- | ---- | ---- |
| sourceFiles | 源Excel文件路径列表 | 必填 |
| targetFile | 目标Excel文件路径 | 必填 |
| modelClass | 数据模型类 | 必填 |
| enableDeduplication | 是否启用去重 | false |
| keyExtractor | 键提取器，用于去重 | null |
| mergeFunction | 数据合并函数，用于合并重复项 | null |
| filter | 数据过滤条件 | null |
| executor | 自定义线程池 | null (自动创建) |
| batchSize | 批处理大小 | 5000 |

## 性能优化建议

1. **批处理大小**：根据数据复杂度和内存大小调整`batchSize`参数
2. **线程池配置**：对于大型文件，考虑使用自定义线程池并调整线程数量
3. **内存设置**：处理大文件时，适当增加JVM堆内存，如`-Xmx4g`
4. **模型设计**：避免在数据模型中包含不必要的大对象

## 常见问题

1. **内存溢出**：处理大文件时可能出现OOM，可以增加JVM堆内存或减小批处理大小
2. **处理速度慢**：可能是线程池配置不当，建议调整线程数量或使用SSD存储
3. **合并结果不正确**：检查`keyExtractor`和`mergeFunction`的逻辑是否正确

## 许可证

MIT 