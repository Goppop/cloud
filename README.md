# ExcelMergeTool

## 📌 项目介绍
**ExcelMergeTool** 是一个高性能的 Excel 合并工具，支持多线程并行处理 Excel 文件，提供数据去重、过滤和进度回调等功能，适用于大数据量的 Excel 处理场景。

## ✨ 功能特点
- ✅ **多线程并行处理**：利用线程池高效读取和写入 Excel 文件。
- ✅ **数据去重 & 过滤**：支持自定义去重逻辑和过滤条件。
- ✅ **进度回调**：实时监控 Excel 处理进度，适用于长时间任务。
- ✅ **分批写入**：防止内存溢出，适合百万级数据处理。
- ✅ **快速合并 & 目录批量合并**：支持多种方式快速合并 Excel 文件。

## 🚀 使用方法
### 1️⃣ **快速合并多个 Excel 文件**
```java
List<String> sourceFiles = Arrays.asList("file1.xlsx", "file2.xlsx");
String targetFile = "merged.xlsx";

boolean success = ExcelMergeUtil.quickMerge(sourceFiles, targetFile, YourModel.class);
System.out.println("合并结果：" + success);
```

### 2️⃣ **合并并去重**
```java
boolean success = ExcelMergeUtil.quickMergeWithDedup(
    sourceFiles, targetFile, YourModel.class, YourModel::getId
);
```

### 3️⃣ **合并目录下所有 Excel 文件**
```java
MergeResult<YourModel> result = ExcelMergeUtil.mergeExcelInDirectory("input_dir", "output.xlsx", YourModel.class);
System.out.println("合并成功：" + result.isSuccess());
```

## 🛠️ 依赖项
项目基于 **Java 8+**，并使用以下库：
- **[EasyExcel](https://github.com/alibaba/easyexcel)** 解析和写入 Excel 文件。
- **Lombok** 简化代码。

Maven 依赖：
```xml
<dependencies>
    <dependency>
        <groupId>com.alibaba</groupId>
        <artifactId>easyexcel</artifactId>
        <version>3.2.1</version>
    </dependency>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>1.18.26</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

## 📜 贡献指南
欢迎贡献代码！请遵循以下流程：
1. **Fork** 本仓库
2. **创建** `feature-xxx` 分支
3. **提交 PR**，并描述你的修改内容

## 📄 许可证
本项目采用 **MIT 许可证** 进行开源。

---
