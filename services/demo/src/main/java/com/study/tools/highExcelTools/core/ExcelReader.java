package com.study.tools.highExcelTools.core;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.enums.CellExtraTypeEnum;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.exception.ExcelAnalysisException;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.study.tools.highExcelTools.config.ExcelConfig;
import com.study.tools.highExcelTools.config.ProgressCallback;
import com.study.tools.highExcelTools.model.ErrorCollector;
import com.study.tools.highExcelTools.model.ErrorRecord;
import com.study.tools.highExcelTools.util.MemoryMonitor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Excel读取工具
 * 提供高性能的Excel文件读取功能
 */
@Slf4j
public class ExcelReader<T> {
    // 默认批处理大小
    private static final int DEFAULT_BATCH_SIZE = 5000;
    // 内部监听器使用的批处理大小
    private static final int INTERNAL_BATCH_SIZE = 2000;
    // 默认并发文件数
    private static final int DEFAULT_MAX_CONCURRENT_FILES = 3;
    // 内存检查频率
    private static final int MEMORY_CHECK_ROWS = 50_000;
    // 进度报告间隔
    private static final int PROGRESS_REPORT_ROWS = 10_000;
    
    private final ExecutorService executorService;
    private final ProgressCallback progressCallback;
    private final String processId;
    private final ExcelConfig<T> config;
    private final AtomicLong totalProcessedRows = new AtomicLong(0);
    
    // 异常数据收集器
    @Getter
    private final ErrorCollector<T> errorCollector;
    
    public ExcelReader(ExcelConfig<T> config, ExecutorService executorService, String processId) {
        this.config = config;
        this.executorService = executorService;
        this.progressCallback = config.getProgressCallback();
        this.processId = processId;
        
        // 初始化错误收集器
        this.errorCollector = new ErrorCollector<>(
                processId, 
                !config.isContinueOnError(),  // failFast模式与continueOnError相反
                config.getMaxErrorCount());
    }
    
    /**
     * 流式读取所有Excel文件并通过回调处理数据
     * @param files 文件列表
     * @param modelClass 模型类
     * @param dataConsumer 数据处理回调
     */
    public void readFilesWithCallback(List<String> files, Class<T> modelClass, Consumer<List<T>> dataConsumer) throws Exception {
        if (files == null || files.isEmpty()) {
            log.warn("[{}] 没有文件需要读取", processId);
            return;
        }
        
        int maxConcurrentFiles = config.getMaxConcurrentFiles() > 0 ? 
                config.getMaxConcurrentFiles() : DEFAULT_MAX_CONCURRENT_FILES;
        int batchSize = config.getBatchSize() > 0 ? 
                config.getBatchSize() : DEFAULT_BATCH_SIZE;
        
        log.info("[{}] 开始读取 {} 个文件，最大并发数: {}, 批次大小: {}", 
                processId, files.size(), maxConcurrentFiles, batchSize);
        
        int totalFiles = files.size();
        AtomicInteger completedFiles = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(totalFiles);
        
        // 分批提交读取任务
        for (int i = 0; i < totalFiles; i += maxConcurrentFiles) {
            int endIndex = Math.min(i + maxConcurrentFiles, totalFiles);
            List<String> batchFiles = files.subList(i, endIndex);
            
            List<Future<?>> futures = new ArrayList<>();
            
            // 提交当前批次的任务
            for (String file : batchFiles) {
                futures.add(executorService.submit(() -> {
                    try {
                        readSingleFileWithCallback(file, modelClass, dataConsumer, batchSize);
                        int completed = completedFiles.incrementAndGet();
                        reportProgress(completed, totalFiles, "读取文件");
                        log.info("[{}] 完成文件 {}/{}: {}", processId, completed, totalFiles, file);
                    } catch (Exception e) {
                        if (config.isCollectErrors()) {
                            // 记录整个文件的错误
                            ErrorRecord<T> errorRecord = ErrorRecord.readError("文件读取", null, file, 
                                    -1L, "文件读取失败: " + e.getMessage(), e);
                            boolean shouldStop = errorCollector.collectError(errorRecord);
                            
                            if (shouldStop && !config.isContinueOnError()) {
                                log.error("[{}] 文件读取停止，由于异常: {}", processId, e.getMessage());
                                throw new RuntimeException("文件读取失败并停止处理: " + file, e);
                            }
                        } else {
                            log.error("[{}] 读取文件失败: {}", processId, file, e);
                            if (!config.isContinueOnError()) {
                                throw new RuntimeException("文件读取失败: " + file, e);
                            }
                        }
                    } finally {
                        latch.countDown();
                    }
                    return null;
                }));
            }
            
            // 等待当前批次的Future完成
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (Exception e) {
                    log.error("[{}] 等待文件读取任务完成时发生错误", processId, e);
                    if (!config.isContinueOnError()) {
                        throw e;
                    }
                }
            }
            
            // 清理引用
            futures.clear();
            
            // 批次间进行GC，避免内存压力
            MemoryMonitor.checkForGC();
            
            log.info("[{}] 完成批次 {}/{}, 内存: {}, 异常数据: {}", 
                    processId, Math.min(endIndex, completedFiles.get()), totalFiles, 
                    MemoryMonitor.getMemoryInfo(),
                    errorCollector.getErrorCount());
        }
        
        // 等待所有任务完成
        boolean allCompleted = latch.await(30, TimeUnit.MINUTES);
        if (!allCompleted) {
            log.warn("[{}] 部分文件读取超时未完成", processId);
        }
        
        // 输出错误统计信息
        if (errorCollector.hasErrors()) {
            log.warn("[{}] 所有文件读取完成，总处理行数: {}, 异常数据: {}条", 
                    processId, totalProcessedRows.get(), errorCollector.getErrorCount());
        } else {
            log.info("[{}] 所有文件读取完成，总处理行数: {}, 无异常数据", 
                    processId, totalProcessedRows.get());
        }
    }
    
    /**
     * 读取单个Excel文件并批量回调
     */
    private void readSingleFileWithCallback(String filePath, Class<T> modelClass, Consumer<List<T>> dataConsumer, int callbackBatchSize) {
        log.info("[{}] 开始读取文件: {}", processId, filePath);
        
        final List<T> batchBuffer = new ArrayList<>(callbackBatchSize);
        final AtomicLong fileRowCount = new AtomicLong(0);
        
        try {
            // 创建监听器
            ReadListener<T> listener = new AnalysisEventListener<T>() {
                @Override
                public void invoke(T data, AnalysisContext context) {
                    try {
                        // 只有在数据有效时才处理
                        if (data != null) {
                            batchBuffer.add(data);
                        } else if (config.isCollectErrors() && config.isSkipInvalidData()) {
                            // 记录空数据错误
                            long rowIdx = context.readRowHolder() != null ? 
                                    context.readRowHolder().getRowIndex().longValue() : -1L;
                            recordRowError(filePath, rowIdx, null, "空数据行", null);
                        }
                        
                        // 累计行数
                        long currentRow = fileRowCount.incrementAndGet();
                        long totalRows = totalProcessedRows.incrementAndGet();
                        
                        // 达到批次大小，回调处理
                        if (batchBuffer.size() >= callbackBatchSize) {
                            processBatch();
                        }
                        
                        // 定期检查内存
                        if (totalRows % MEMORY_CHECK_ROWS == 0) {
                            MemoryMonitor.checkForGC(0.75f);
                        }
                        
                        // 定期报告进度
                        if (currentRow % PROGRESS_REPORT_ROWS == 0) {
                            reportDetailedProgress(filePath, currentRow);
                        }
                    } catch (Exception e) {
                        long rowIdx = context.readRowHolder() != null ? 
                                context.readRowHolder().getRowIndex().longValue() : -1L;
                        handleRowException(filePath, rowIdx, data, e);
                    }
                }
                
                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                    // 处理最后一批数据
                    if (!batchBuffer.isEmpty()) {
                        processBatch();
                    }
                    
                    log.info("[{}] 文件读取完成: {}, 总行数: {}", 
                            processId, filePath, fileRowCount.get());
                }
                
                @Override
                public void onException(Exception exception, AnalysisContext context) throws Exception {
                    // 处理读取过程中的异常
                    long rowIdx = context.readRowHolder() != null ? 
                            context.readRowHolder().getRowIndex().longValue() : -1L;
                            
                    handleRowException(filePath, rowIdx, null, exception);
                    
                    // 如果配置为继续处理，则不抛出异常
                    if (config.isContinueOnError()) {
                        // 不抛出异常，继续处理
                    } else {
                        // 抛出异常，中断处理
                        throw exception;
                    }
                }
                
                private void processBatch() {
                    try {
                        if (batchBuffer.isEmpty()) return;
                        
                        // 创建副本以避免并发问题
                        List<T> batchCopy = new ArrayList<>(batchBuffer);
                        dataConsumer.accept(batchCopy);
                        
                        // 清空缓冲区
                        batchBuffer.clear();
                    } catch (Exception e) {
                        log.error("[{}] 处理数据批次时发生错误", processId, e);
                        
                        if (config.isCollectErrors()) {
                            // 记录批处理错误
                            ErrorRecord<T> errorRecord = ErrorRecord.processError(
                                    "批处理", null, "处理数据批次失败: " + e.getMessage(), e);
                            boolean shouldStop = errorCollector.collectError(errorRecord);
                            
                            if (shouldStop && !config.isContinueOnError()) {
                                throw new RuntimeException("处理数据批次失败并停止处理", e);
                            }
                        } else if (!config.isContinueOnError()) {
                            throw new RuntimeException("处理数据批次失败", e);
                        }
                        
                        // 清空有问题的批次
                        batchBuffer.clear();
                    }
                }
                
                private void handleRowException(String source, long rowIndex, T data, Exception e) {
                    if (config.isCollectErrors()) {
                        recordRowError(source, rowIndex, data, "处理行数据异常: " + e.getMessage(), e);
                    }
                    
                    if (config.isLogErrors()) {
                        log.warn("[{}] 行数据处理异常 {}, 行号: {}", processId, source, rowIndex, e);
                    }
                    
                    if (!config.isContinueOnError() && !config.isSkipInvalidData()) {
                        throw new RuntimeException("行数据处理异常, 文件: " + source + ", 行号: " + rowIndex, e);
                    }
                }
            };
            
            // 使用EasyExcel的流式读取
            EasyExcel.read(filePath, modelClass, listener)
                    .extraRead(CellExtraTypeEnum.COMMENT)  // 读取批注
                    .extraRead(CellExtraTypeEnum.MERGE)    // 读取合并单元格
                    .ignoreEmptyRow(true)                  // 忽略空行
                    .sheet()
                    .doRead();
        } catch (ExcelAnalysisException e) {
            // 处理EasyExcel的分析异常
            if (config.isCollectErrors()) {
                ErrorRecord<T> errorRecord = ErrorRecord.readError(
                        "Excel分析", null, filePath, -1L, "Excel分析异常: " + e.getMessage(), e);
                errorCollector.collectError(errorRecord);
            }
            
            if (!config.isContinueOnError()) {
                throw e;
            } else {
                log.error("[{}] Excel分析异常，但继续处理: {}", processId, filePath, e);
            }
        } catch (Exception e) {
            // 处理其他异常
            if (config.isCollectErrors()) {
                ErrorRecord<T> errorRecord = ErrorRecord.readError(
                        "文件读取", null, filePath, -1L, "读取文件异常: " + e.getMessage(), e);
                errorCollector.collectError(errorRecord);
            }
            
            log.error("[{}] 读取文件异常: {}", processId, filePath, e);
            if (!config.isContinueOnError()) {
                throw new RuntimeException("读取文件失败: " + filePath, e);
            }
        }
    }
    
    /**
     * 记录行错误
     */
    private void recordRowError(String source, long rowIndex, T data, String message, Exception e) {
        ErrorRecord<T> errorRecord;
        if (data != null) {
            errorRecord = ErrorRecord.convertError("数据转换", data, source, 
                    rowIndex, message, e);
        } else {
            errorRecord = ErrorRecord.readError("数据读取", null, source, 
                    rowIndex, message, e);
        }
        
        boolean shouldStop = errorCollector.collectError(errorRecord);
        
        // 触发错误回调
        if (config.getErrorCallback() != null) {
            boolean continueProcessing = config.getErrorCallback().onError(
                    errorRecord, errorCollector.getErrorCount(), "READ");
                    
            // 如果回调返回false，则停止处理
            if (!continueProcessing) {
                throw new RuntimeException("处理中止: " + message);
            }
        }
        
        // 如果需要停止处理，则抛出异常
        if (shouldStop && !config.isContinueOnError()) {
            throw new RuntimeException("达到最大错误数或配置为快速失败: " + message);
        }
    }
    
    /**
     * 读取所有Excel文件并返回合并后的数据
     */
    public List<T> readFiles(List<String> files, Class<T> modelClass) throws Exception {
        List<T> allData = new ArrayList<>();
        
        // 使用回调方式流式读取
        readFilesWithCallback(files, modelClass, batch -> {
            synchronized (allData) {
                allData.addAll(batch);
            }
        });
        
        log.info("[{}] 读取完成，总行数: {}", processId, allData.size());
        return allData;
    }
    
    /**
     * 读取指定Excel文件的多个Sheet
     */
    public List<T> readSheets(String filePath, Class<T> modelClass, List<Integer> sheetIndexes) {
        List<T> result = new ArrayList<>();
        
        try {
            if (sheetIndexes == null || sheetIndexes.isEmpty()) {
                // 读取全部Sheet
                log.info("[{}] 读取文件所有Sheet: {}", processId, filePath);
                result = EasyExcel.read(filePath).head(modelClass).doReadAllSync();
            } else {
                // 读取指定Sheet
                log.info("[{}] 读取文件指定Sheet: {}, indexes: {}", processId, filePath, sheetIndexes);
                
                // 创建自定义监听器来收集数据
                List<T> sheetData = new ArrayList<>();
                
                // 使用流式读取指定Sheet
                for (Integer sheetIndex : sheetIndexes) {
                    final int currentSheetIndex = sheetIndex;
                    EasyExcel.read(filePath, modelClass, new AnalysisEventListener<T>() {
                        @Override
                        public void invoke(T data, AnalysisContext context) {
                            if (data != null) {
                                synchronized (sheetData) {
                                    sheetData.add(data);
                                }
                            }
                        }
                        
                        @Override
                        public void doAfterAllAnalysed(AnalysisContext context) {
                            log.debug("[{}] Sheet-{} 读取完成，行数: {}", processId, currentSheetIndex, sheetData.size());
                        }
                        
                        @Override
                        public void onException(Exception exception, AnalysisContext context) throws Exception {
                            if (config.isCollectErrors()) {
                                long rowIdx = context.readRowHolder() != null ? 
                                        context.readRowHolder().getRowIndex().longValue() : -1L;
                                recordRowError(filePath, rowIdx, null, 
                                        "Sheet-" + currentSheetIndex + "读取异常: " + exception.getMessage(), exception);
                            }
                            
                            if (config.isContinueOnError()) {
                                // 继续处理，不抛出异常
                            } else {
                                throw exception;
                            }
                        }
                    }).sheet(sheetIndex).doRead();
                }
                
                // 将收集的数据添加到结果集
                result.addAll(sheetData);
            }
            
            log.info("[{}] Sheet读取完成，总行数: {}", processId, result.size());
            return result;
            
        } catch (Exception e) {
            if (config.isCollectErrors()) {
                ErrorRecord<T> errorRecord = ErrorRecord.readError(
                        "Sheet读取", null, filePath, -1L, "读取Sheet失败: " + e.getMessage(), e);
                errorCollector.collectError(errorRecord);
            }
            
            log.error("[{}] 读取Sheet失败: {}", processId, filePath, e);
            if (!config.isContinueOnError()) {
                throw new RuntimeException("读取Sheet失败: " + filePath, e);
            }
            
            return result; // 返回已读取的数据
        }
    }
    
    /**
     * 报告进度
     */
    private void reportProgress(long current, long total, String phase) {
        if (progressCallback != null) {
            progressCallback.onProgress(current, total, phase);
        }
    }
    
    /**
     * 报告详细进度
     */
    private void reportDetailedProgress(String file, long rowCount) {
        if (progressCallback != null) {
            String message = "读取: " + file + ", 行数: " + rowCount;
            progressCallback.onProgress(rowCount, -1, "读取数据", message);
        }
    }
    
    /**
     * 读取所有Excel文件的适配方法（兼容旧API）
     */
    public List<T> readAllFiles(List<String> files, Class<T> modelClass) throws Exception {
        return readFiles(files, modelClass);
    }
    
    /**
     * 为了兼容旧API的构造方法
     */
    public ExcelReader(ExecutorService executorService, ProgressCallback progressCallback, String processId) {
        this.executorService = executorService;
        this.progressCallback = progressCallback;
        this.processId = processId;
        
        // 创建默认配置
        ExcelConfig<T> defaultConfig = new ExcelConfig<>();
        defaultConfig.setContinueOnError(true);
        defaultConfig.setCollectErrors(true);
        defaultConfig.setMaxErrorCount(1000);
        this.config = defaultConfig;
        
        // 初始化错误收集器
        this.errorCollector = new ErrorCollector<>(
                processId, 
                false,  // 默认不快速失败
                1000);  // 默认最大错误数
    }
} 