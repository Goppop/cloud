# 高性能Excel处理工具

基于EasyExcel的高性能Excel文件处理工具，支持百万级数据量的导入、导出和合并操作。

## 一、架构图

### 系统架构图

```
┌─────────────────────────────────────────────────────────────┐
│                    高性能Excel处理工具架构                     │
└─────────────────────────────────────────────────────────────┘
                              │
         ┌──────────────────────────────────────┐
         ▼                    ▼                 ▼
┌─────────────────┐  ┌──────────────────┐  ┌──────────────┐
│    核心API层     │  │     处理引擎层     │  │   工具类层   │
└─────────────────┘  └──────────────────┘  └──────────────┘
│                    │                     │
│ - ExcelMergeTool   │ - ExcelReader      │ - MemoryMonitor  
│ - MergeConfig      │ - ExcelWriter      │ - ThreadPoolManager
│ - MergeResult      │ - DataProcessor    │ - ErrorCollector
│ - ProgressCallback │ - ErrorCallback    │ - ErrorRecord
└─────────────────┬──┴──────────────┬─────┴────────────┬───┘
                  │                 │                  │
                  ▼                 ▼                  ▼
         ┌────────────────┐ ┌───────────────┐ ┌─────────────────┐
         │    配置管理     │ │   数据处理     │ │    错误处理      │
         └────────────────┘ └───────────────┘ └─────────────────┘
```

### 数据流图

```
┌───────────┐        ┌───────────┐        ┌───────────┐       ┌───────────┐
│           │        │           │        │           │       │           │
│ Excel文件  ├───────►│ 读取组件   ├───────►│ 处理组件   ├──────►│ 写入组件   │
│  (多个)    │        │           │        │           │       │           │
└───────────┘        └─────┬─────┘        └─────┬─────┘       └─────┬─────┘
                           │                    │                   │
                           ▼                    ▼                   ▼
                     ┌────────────┐      ┌────────────┐      ┌────────────┐
                     │ 批量读取    │      │ 过滤/去重   │      │ 批量写入    │
                     │ 内存管理    │      │ 数据转换    │      │ 性能优化    │
                     └────────────┘      └────────────┘      └────────────┘
                           │                    │                   │
                           └───────────┬────────┴───────────┬──────┘
                                       │                    │
                                       ▼                    ▼
                              ┌─────────────────┐   ┌─────────────────┐
                              │  进度回调通知    │   │  错误收集与处理  │
                              └─────────────────┘   └─────────────────┘
```

### 核心组件关系图

```
                   ┌───────────────────┐
                   │   ExcelMergeTool  │
                   └─────────┬─────────┘
                             │ 使用
      ┌───────────────┬──────┴──────┬───────────────┐
      ▼               ▼             ▼               ▼
┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐
│ MergeConfig │ │ ExcelReader│ │DataProcessor│ │ExcelWriter │
└────────────┘ └────────────┘ └────────────┘ └────────────┘
      │              │ 使用         │ 使用         │ 使用
      │              ▼             ▼             ▼
      │       ┌────────────┐ ┌────────────┐ ┌────────────┐
      │       │ProgressCall│ │ 过滤/去重   │ │ 批量写入   │
      │       │  -back     │ │ 功能       │ │ 功能       │
      │       └────────────┘ └────────────┘ └────────────┘
      │              │             │             │
      └──────────────┴─────────────┴─────────────┘
                           │
                           ▼
                    ┌────────────────┐
                    │  MergeResult   │
                    └────────────────┘
```

## 二、特性

- **高性能**：基于EasyExcel的流式读写，内存占用低，处理速度可达15,000+行/秒
- **易扩展**：模块化设计，各组件可独立使用和扩展
- **线程安全**：采用线程安全的数据结构和并发控制
- **内存优化**：智能内存管理，避免OOM问题
- **多线程处理**：并行处理多个文件，提高效率
- **进度监控**：实时监控处理进度，支持自定义回调
- **动态线程池**：智能调整线程池参数，适应不同负载

## 三、主要功能

1. **Excel合并**：合并多个Excel文件，支持过滤和去重
2. **Excel导出**：高性能导出大量数据到Excel文件
3. **自定义表头**：支持自定义Excel表头结构
4. **动态过滤**：支持自定义过滤条件
5. **数据去重**：支持基于指定字段的数据去重
6. **错误处理**：详细的错误收集和处理机制
7. **内存监控**：自动监控和管理内存使用

## 四、快速开始

### 基本合并

```java
// 简单合并
List<String> sourceFiles = Arrays.asList("file1.xlsx", "file2.xlsx", "file3.xlsx");
String targetFile = "merged_result.xlsx";

ExcelMergeTool mergeTool = new ExcelMergeTool();
MergeResult<UserModel> result = mergeTool.quickMerge(sourceFiles, targetFile, UserModel.class);

if (result.isSuccess()) {
    System.out.println("合并成功! 总行数: " + result.getTotalRows());
    System.out.println("处理速度: " + result.getRowsPerSecond() + " 行/秒");
}
```

### 带去重的合并

```java
// 按ID字段去重合并
ExcelMergeTool mergeTool = new ExcelMergeTool();
MergeResult<UserModel> result = mergeTool.quickMergeWithDedup(
    sourceFiles,
    "merged_dedup.xlsx",
    UserModel.class,
    UserModel::getId  // ID字段提取器
);
```

### 自定义合并配置

```java
// 创建进度回调
ProgressCallback progressCallback = (current, total, phase) -> {
    System.out.printf("处理进度: %s - %d/%d%n", phase, current, total);
};

// 创建合并配置
MergeConfig<UserModel> config = MergeConfig.<UserModel>builder()
    .sourceFiles(sourceFiles)
    .targetFile("custom_merged.xlsx")
    .modelClass(UserModel.class)
    // 设置过滤条件
    .filter(user -> user.getAge() != null && user.getAge() >= 18)
    // 设置去重
    .enableDeduplication(true)
    .keyExtractor(UserModel::getId)
    // 设置合并逻辑
    .mergeFunction((u1, u2) -> {
        // 保留积分更高的记录
        if (u1.getPoints() < u2.getPoints()) {
            return u2;
        }
        return u1;
    })
    // 性能参数
    .batchSize(10000)
    .progressCallback(progressCallback)
    .build();

// 执行合并
MergeResult<UserModel> result = mergeTool.mergeExcel(config);
```

## 五、数据模型定义

使用该工具，您需要定义与Excel对应的数据模型类：

```java
@Data
public class UserModel {
    @ExcelProperty("用户ID")
    private String id;
    
    @ExcelProperty("用户名")
    private String username;
    
    @ExcelProperty("年龄")
    private Integer age;
    
    @ExcelProperty("邮箱")
    private String email;
    
    // 其他字段...
}
```

## 六、高级功能

### 进度监控

实现`ProgressCallback`接口可以监控处理进度：

```java
ProgressCallback callback = (current, total, phase) -> {
    double percentage = total > 0 ? (current * 100.0 / total) : 0;
    System.out.printf("%s: %.2f%% (%d/%d)%n", phase, percentage, current, total);
};
```

### 错误处理

配置错误处理策略：

```java
MergeConfig<UserModel> config = MergeConfig.<UserModel>builder()
    // ...其他配置
    .continueOnError(true)  // 遇到错误时继续处理
    .skipInvalidData(true)  // 跳过无效数据
    .collectErrors(true)    // 收集错误信息
    .maxErrorCount(1000)    // 最大错误数量
    .build();
```

### 内存优化

```java
// 调整批处理大小
.batchSize(5000)  // 较小的值减少内存使用，较大的值提高性能

// 使用内存模式
.useInMemory(true)  // 提高性能，但增加内存使用
```

## 七、性能调优

### 批处理大小调整

- 一般建议值：5,000-10,000
- 内存受限环境：1,000-5,000
- 高性能环境：10,000-50,000

### 线程池参数

```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    4,                      // 核心线程数
    8,                      // 最大线程数
    60L, TimeUnit.SECONDS,  // 线程保活时间
    new LinkedBlockingQueue<>(100),  // 工作队列
    new ThreadPoolExecutor.CallerRunsPolicy()  // 拒绝策略
);

MergeConfig<UserModel> config = MergeConfig.<UserModel>builder()
    // ...其他配置
    .executor(executor)
    .shutdownExecutor(true)  // 完成后关闭执行器
    .build();
```

### 内存管理

- **文件并发数**：默认值为3，可根据系统CPU核心数和内存调整
- **批处理写入**：定期触发GC释放内存
- **流式处理**：避免一次加载全部数据

## 八、最佳实践

### 性能优化建议

1. **合理配置批处理大小**：根据数据量和可用内存调整
2. **限制并发文件数**：避免过多线程导致资源竞争
3. **过滤优先**：先过滤再去重，减少处理数据量
4. **自定义线程池**：针对不同硬件环境优化线程参数
5. **定期触发GC**：处理大量数据时主动释放内存

### 错误处理建议

1. **启用错误收集**：便于分析和排查问题
2. **设置最大错误数**：避免错误记录占用过多内存
3. **实现错误回调**：及时处理关键错误

### 数据模型设计

1. **使用包装类型**：如`Integer`而非`int`，允许空值处理
2. **实现equals/hashCode**：确保去重功能正确工作
3. **优化内存占用**：避免不必要的字段

## 九、常见问题解答

**Q: 如何处理超大文件？**  
A: 使用较小的批处理大小(1000-3000)，减少并发文件数至1-2，确保启用skipInvalidData。

**Q: 处理速度慢怎么办？**  
A: 增加批处理大小，调整线程池参数，使用内存模式(useInMemory=true)。

**Q: 出现内存溢出怎么办？**  
A: 减小批处理大小，减少并发文件数，增加GC频率，确保流式处理。

**Q: 如何处理自定义Excel格式？**  
A: 可以自定义数据模型类，使用EasyExcel注解映射特殊格式。

## 十、性能参考数据

基于测试结果，处理75万行数据的性能参考：

| 操作类型 | 处理时间(ms) | 处理速度(行/秒) | 内存增长(MB) |
|---------|------------|--------------|------------|
| 简单合并 | 8,697      | 17,251       | 2,382      |
| 过滤合并 | 9,156      | 14,735       | 1,192      |
| 去重合并 | 7,301      | 18,661       | 2,025      |

*注：实际性能可能因硬件配置和数据特性而异*

## 十一、版本历史

### v1.0.0 (2023-07-15)
- 初始版本发布
- 实现基本合并、过滤和去重功能

### v1.1.0 (2023-09-01)
- 增加错误处理功能
- 优化内存管理
- 提高处理性能

### v1.2.0 (2023-12-10)
- 增加并行流处理
- 增加动态线程池
- 完善错误收集机制

### v2.0.0 (2024-03-20)
- 重构核心架构
- 实现流式处理
- 支持百万级数据处理
- 增加性能监控与优化功能 