package com.study.tools.excelNew.util;

import com.study.tools.excelNew.ExcelMergeTool;
import com.study.tools.excelNew.MergeConfig;
import com.study.tools.excelNew.MergeResult;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Excel合并工具类 - 提供静态工具方法
 */
@Slf4j
public class ExcelMergeUtil {
    
    private static final ExcelMergeTool TOOL = new ExcelMergeTool();
    
    private ExcelMergeUtil() {
        // 工具类不允许实例化
    }
    
    /**
     * 快速合并Excel文件
     *
     * @param sourceFiles 源文件列表
     * @param targetFile 目标文件
     * @param modelClass 数据模型类
     * @param <T> 数据模型类型
     * @return 是否成功
     */
    public static <T> boolean quickMerge(List<String> sourceFiles, String targetFile, Class<T> modelClass) {
        try {
            return TOOL.quickMerge(sourceFiles, targetFile, modelClass).isSuccess();
        } catch (Exception e) {
            log.error("快速合并失败", e);
            return false;
        }
    }
    
    /**
     * 快速合并Excel文件（变长参数版本）
     *
     * @param targetFile 目标文件
     * @param modelClass 数据模型类
     * @param sourceFiles 源文件列表
     * @param <T> 数据模型类型
     * @return 是否成功
     */
    public static <T> boolean quickMerge(String targetFile, Class<T> modelClass, String... sourceFiles) {
        return quickMerge(Arrays.asList(sourceFiles), targetFile, modelClass);
    }
    
    /**
     * 合并指定目录下的所有Excel文件
     *
     * @param sourceDir 源文件目录
     * @param targetFile 目标文件
     * @param modelClass 数据模型类
     * @param <T> 数据模型类型
     * @return 合并结果
     */
    public static <T> MergeResult<T> mergeExcelInDirectory(String sourceDir, String targetFile, Class<T> modelClass) {
        try {
            File dir = new File(sourceDir);
            if (!dir.isDirectory()) {
                throw new IllegalArgumentException(sourceDir + " 不是有效的目录");
            }
            
            List<String> files = Arrays.stream(dir.listFiles())
                    .filter(file -> file.getName().endsWith(".xlsx") || file.getName().endsWith(".xls"))
                    .map(File::getAbsolutePath)
                    .collect(Collectors.toList());
            
            if (files.isEmpty()) {
                throw new IllegalArgumentException("目录 " + sourceDir + " 中没有找到Excel文件");
            }
            
            log.info("找到 {} 个Excel文件，准备合并", files.size());
            return TOOL.quickMerge(files, targetFile, modelClass);
            
        } catch (Exception e) {
            log.error("目录合并失败", e);
            return MergeResult.<T>builder()
                    .success(false)
                    .errorMessage("合并失败: " + e.getMessage())
                    .build();
        }
    }
} 