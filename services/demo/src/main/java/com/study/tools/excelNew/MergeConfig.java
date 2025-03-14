package com.study.tools.excelNew;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Excel合并配置
 *
 * @param <T> 数据模型类型
 */
@Builder
@Getter
public class MergeConfig<T> {
    /** 源文件路径列表 */
    private final List<String> sourceFiles;

    /** 目标文件路径 */
    private final String targetFile;

    /** 数据模型类 */
    private final Class<T> modelClass;

    /** 是否启用去重 */
    @Builder.Default
    private final boolean enableDeduplication = false;

    /** 键提取器，用于去重 */
    private final Function<T, Object> keyExtractor;

    /** 合并函数，用于合并重复项 */
    private final BiFunction<T, T, T> mergeFunction;

    /** 过滤条件 */
    private final Predicate<T> filter;

    /** 自定义执行器 */
    private final ExecutorService executor;

    /** 批处理大小 */
    @Builder.Default
    private final int batchSize = 5000;

    /** 是否关闭执行器 */
    @Builder.Default
    private final boolean shutdownExecutor = true;

    /** 进度回调 */
    private final ProgressCallback progressCallback;
} 