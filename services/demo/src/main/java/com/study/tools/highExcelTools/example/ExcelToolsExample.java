package com.study.tools.highExcelTools.example;

import com.study.tools.highExcelTools.HighExcelTools;
import com.study.tools.highExcelTools.config.ExcelConfig;
import com.study.tools.highExcelTools.config.ProgressCallback;
import com.study.tools.highExcelTools.model.ExcelResult;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * HighExcelTools使用示例
 */
@Slf4j
public class ExcelToolsExample {

    public static void main(String[] args) {
        try {
            // 示例数据
            List<String> sourceFiles = Arrays.asList(
                    "D:\\temp\\excel\\users1.xlsx",
                    "D:\\temp\\excel\\users2.xlsx",
                    "D:\\temp\\excel\\users3.xlsx"
            );
            String targetFile = "D:\\temp\\excel\\merged_result.xlsx";
            
            // 创建工具类实例
            HighExcelTools tools = new HighExcelTools();
            
            // 示例1: 简单合并
            simpleExample(tools, sourceFiles, targetFile);
            
            // 示例2: 带去重的合并
            dedupExample(tools, sourceFiles, targetFile);
            
            // 示例3: 自定义配置的合并
            customExample(tools, sourceFiles, targetFile);
            
        } catch (Exception e) {
            log.error("示例运行失败", e);
        }
    }
    
    /**
     * 示例1: 简单合并
     */
    private static void simpleExample(HighExcelTools tools, List<String> sourceFiles, String targetFile) {
        log.info("开始执行简单合并示例...");
        
        // 简单合并，不进行去重
        ExcelResult<UserModel> result = tools.quickMerge(
                sourceFiles,
                targetFile.replace(".xlsx", "_simple.xlsx"),
                UserModel.class
        );
        
        if (result.isSuccess()) {
            log.info("简单合并成功: 总行数={}, 耗时={}ms, 速度={} 行/秒", 
                    result.getTotalRows(), 
                    result.getTimeMillis(),
                    String.format("%.2f", result.getRowsPerSecond()));
        } else {
            log.error("简单合并失败: {}", result.getErrorMessage());
        }
    }
    
    /**
     * 示例2: 带去重的合并
     */
    private static void dedupExample(HighExcelTools tools, List<String> sourceFiles, String targetFile) {
        log.info("开始执行去重合并示例...");
        
        // 定义去重键提取器 - 基于用户ID去重
        Function<UserModel, Object> keyExtractor = UserModel::getId;
        
        // 执行去重合并
        ExcelResult<UserModel> result = tools.quickMergeWithDedup(
                sourceFiles,
                targetFile.replace(".xlsx", "_dedup.xlsx"),
                UserModel.class,
                keyExtractor
        );
        
        if (result.isSuccess()) {
            log.info("去重合并成功: 总行数={}, 耗时={}ms, 速度={} 行/秒", 
                    result.getTotalRows(), 
                    result.getTimeMillis(),
                    String.format("%.2f", result.getRowsPerSecond()));
        } else {
            log.error("去重合并失败: {}", result.getErrorMessage());
        }
    }
    
    /**
     * 示例3: 自定义配置的合并
     */
    private static void customExample(HighExcelTools tools, List<String> sourceFiles, String targetFile) {
        log.info("开始执行自定义配置合并示例...");
        
        // 创建进度回调
        ProgressCallback progressCallback = (current, total, phase, message) -> {
            if (message != null) {
                log.info("进度: {}/{} - {} - {}", current, total, phase, message);
            } else {
                log.info("进度: {}/{} - {}", current, total, phase);
            }
        };
        
        // 创建自定义配置
        ExcelConfig<UserModel> config = ExcelConfig.<UserModel>builder()
                .sourceFiles(sourceFiles)
                .targetFile(targetFile.replace(".xlsx", "_custom.xlsx"))
                .modelClass(UserModel.class)
                .enableDeduplication(true)
                .keyExtractor(UserModel::getId)
                // 只保留年龄大于18岁的用户
                .filter(user -> user.getAge() != null && user.getAge() > 18)
                // 合并重复记录时，累加积分
                .mergeFunction((u1, u2) -> {
                    if (u1.getPoints() != null && u2.getPoints() != null) {
                        u1.setPoints(u1.getPoints() + u2.getPoints());
                    }
                    return u1;
                })
                .progressCallback(progressCallback)
                // 性能配置
                .batchSize(5000)
                .bufferSize(16384)
                .useInMemory(true)
                .maxConcurrentFiles(3)
                .build();
        
        // 执行自定义合并
        ExcelResult<UserModel> result = tools.mergeExcel(config);
        
        if (result.isSuccess()) {
            log.info("自定义合并成功: 总行数={}, 耗时={}ms, 速度={} 行/秒", 
                    result.getTotalRows(), 
                    result.getTimeMillis(),
                    String.format("%.2f", result.getRowsPerSecond()));
        } else {
            log.error("自定义合并失败: {}", result.getErrorMessage());
        }
    }
} 