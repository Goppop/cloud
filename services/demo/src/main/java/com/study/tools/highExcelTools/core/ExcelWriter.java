package com.study.tools.highExcelTools.core;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import com.study.tools.highExcelTools.config.ExcelConfig;
import com.study.tools.highExcelTools.config.ProgressCallback;
import com.study.tools.highExcelTools.model.ErrorCollector;
import com.study.tools.highExcelTools.model.ErrorRecord;
import com.study.tools.highExcelTools.util.MemoryMonitor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Excel写入工具
 * 提供高性能的Excel文件写入功能
 */
@Slf4j
public class ExcelWriter<T> {
    // 默认批次大小
    private static final int DEFAULT_BATCH_SIZE = 5000;
    // 默认缓冲区大小
    private static final int DEFAULT_BUFFER_SIZE = 8192;
    // GC检查频率（批次数）
    private static final int GC_CHECK_BATCH_COUNT = 20;
    
    private final ExcelConfig<T> config;
    private final ProgressCallback progressCallback;
    private final String processId;
    
    // 写入相关对象
    private com.alibaba.excel.ExcelWriter excelWriter;
    private WriteSheet writeSheet;
    private BufferedOutputStream bufferedOutputStream;
    
    // 性能统计
    private long startTime;
    private final AtomicLong writtenRows = new AtomicLong(0);
    
    // 异常数据收集器
    @Getter
    private final ErrorCollector<T> errorCollector;
    
    // 格式化工具
    private final DecimalFormat decimalFormat = new DecimalFormat("#.##");
    
    public ExcelWriter(ExcelConfig<T> config, String processId) {
        this.config = config;
        this.progressCallback = config.getProgressCallback();
        this.processId = processId;
        
        // 初始化错误收集器
        this.errorCollector = new ErrorCollector<>(
                processId, 
                !config.isContinueOnError(),  // failFast模式与continueOnError相反
                config.getMaxErrorCount());
    }
    
    /**
     * 初始化写入器
     */
    public void init() throws Exception {
        log.info("[{}] 初始化ExcelWriter: {}", processId, config.getTargetFile());
        
        // 确保目标目录存在
        Path outputPath = Paths.get(config.getTargetFile());
        Files.createDirectories(outputPath.getParent());
        
        // 获取缓冲区大小
        int bufferSize = config.getBufferSize() > 0 ? 
                config.getBufferSize() : DEFAULT_BUFFER_SIZE;
        
        // 创建带缓冲的输出流
        this.bufferedOutputStream = new BufferedOutputStream(
                new FileOutputStream(config.getTargetFile()), bufferSize);
        
        // 创建单元格样式
        WriteCellStyle headWriteCellStyle = new WriteCellStyle();
        WriteCellStyle contentWriteCellStyle = new WriteCellStyle();
        HorizontalCellStyleStrategy styleStrategy = 
                new HorizontalCellStyleStrategy(headWriteCellStyle, contentWriteCellStyle);
        
        // 创建ExcelWriter
        this.excelWriter = EasyExcel.write(bufferedOutputStream, config.getModelClass())
                .registerWriteHandler(styleStrategy)
                .inMemory(config.isUseInMemory())             // 内存模式
                .autoCloseStream(config.isAutoCloseStream())  // 自动关闭流
                .useDefaultStyle(false)                       // 禁用默认样式提高性能
                .build();
                
        // 创建指定头的WriteSheet，考虑自定义表头
        if (config.getHeadList() != null && !config.getHeadList().isEmpty()) {
            // 使用自定义表头
            this.writeSheet = EasyExcel.writerSheet("Sheet1")
                    .head(config.getHeadList())
                    .build();
        } else {
            // 使用默认表头
            this.writeSheet = EasyExcel.writerSheet("Sheet1").build();
        }
        
        // 记录开始时间
        this.startTime = System.currentTimeMillis();
        
        log.info("[{}] ExcelWriter初始化完成，目标文件: {}", processId, config.getTargetFile());
    }
    
    /**
     * 分批写入数据
     * @param data 要写入的数据
     */
    public void write(List<T> data) throws Exception {
        if (data == null || data.isEmpty()) {
            log.info("[{}] 没有数据需要写入", processId);
            return;
        }
        
        // 如果尚未初始化，先初始化
        if (excelWriter == null) {
            init();
        }
        
        // 获取批次大小
        int batchSize = config.getBatchSize() > 0 ? config.getBatchSize() : DEFAULT_BATCH_SIZE;
        int totalSize = data.size();
        
        log.info("[{}] 开始写入数据，总行数: {}, 批次大小: {}", processId, totalSize, batchSize);
        
        try {
            // 分批写入
            for (int i = 0; i < totalSize; i += batchSize) {
                int endIndex = Math.min(i + batchSize, totalSize);
                List<T> batch = data.subList(i, endIndex);
                
                try {
                    // 写入当前批次
                    writeBatch(batch);
                    
                    // 更新进度
                    int currentTotal = endIndex;
                    reportProgress(currentTotal, totalSize, "写入数据");
                    
                    // 每N个批次检查内存情况并可能触发GC
                    if ((i / batchSize) % GC_CHECK_BATCH_COUNT == 0) {
                        MemoryMonitor.checkForGC();
                    }
                } catch (Exception e) {
                    log.error("[{}] 写入批次数据失败，批次大小: {}", processId, batch.size(), e);
                    
                    if (config.isCollectErrors()) {
                        // 记录批处理错误
                        ErrorRecord<T> errorRecord = ErrorRecord.writeError(
                                "批次写入", batch.get(0), "写入批次数据失败: " + e.getMessage(), e);
                        boolean shouldStop = errorCollector.collectError(errorRecord);
                        
                        if (shouldStop && !config.isContinueOnError()) {
                            throw new RuntimeException("写入批次数据失败并停止处理", e);
                        }
                    } else if (!config.isContinueOnError()) {
                        throw new RuntimeException("写入批次数据失败", e);
                    }
                }
            }
            
            // 计算性能指标
            long duration = System.currentTimeMillis() - startTime;
            double rowsPerSecond = duration > 0 ? (totalSize * 1000.0 / duration) : 0;
            
            log.info("[{}] 数据写入完成，共写入 {} 行，耗时 {} ms，速度 {} 行/秒", 
                    processId, totalSize, duration, decimalFormat.format(rowsPerSecond));
                    
        } catch (Exception e) {
            log.error("[{}] 写入数据过程中发生错误", processId, e);
            throw e;
        } finally {
            // 确保资源正确释放
            if (!config.isAutoCloseStream()) {
                finish();
            }
        }
    }
    
    /**
     * 写入批次数据
     */
    private void writeBatch(List<T> batch) {
        try {
            excelWriter.write(batch, writeSheet);
            long written = writtenRows.addAndGet(batch.size());
            
            if (written % 50000 == 0) {
                log.debug("[{}] 已写入 {} 行数据", processId, written);
            }
        } catch (Exception e) {
            log.error("[{}] 写入批次数据失败，批次大小: {}", processId, batch.size(), e);
            throw e;
        }
    }
    
    /**
     * 完成写入并释放资源
     */
    public void finish() {
        if (excelWriter != null) {
            try {
                excelWriter.finish();
                log.info("[{}] Excel写入器正常关闭", processId);
            } catch (Exception e) {
                log.error("[{}] 关闭Excel写入器时发生错误", processId, e);
            } finally {
                excelWriter = null;
                writeSheet = null;
            }
        }
        
        if (bufferedOutputStream != null) {
            try {
                bufferedOutputStream.close();
            } catch (Exception e) {
                log.error("[{}] 关闭输出流时发生错误", processId, e);
            } finally {
                bufferedOutputStream = null;
            }
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
     * 获取写入速度（行/秒）
     */
    public double getWriteSpeed() {
        long duration = System.currentTimeMillis() - startTime;
        if (duration <= 0) return 0;
        return (writtenRows.get() * 1000.0) / duration;
    }
} 