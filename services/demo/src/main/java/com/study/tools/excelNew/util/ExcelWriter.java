package com.study.tools.excelNew.util;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.study.tools.excelNew.MergeConfig;
import com.study.tools.excelNew.ProgressCallback;
import lombok.extern.slf4j.Slf4j;

import java.io.FileOutputStream;
import java.util.List;

/**
 * Excel写入工具
 */
@Slf4j
public class ExcelWriter<T> {
    private final MergeConfig<T> config;
    private final ProgressCallback progressCallback;
    private final String mergeId;
    
    public ExcelWriter(MergeConfig<T> config, String mergeId) {
        this.config = config;
        this.progressCallback = config.getProgressCallback();
        this.mergeId = mergeId;
    }
    
    /**
     * 写入Excel文件
     */
    public void write(List<T> data) throws Exception {
        log.info("[{}] 开始写入合并文件: {}, 数据行数: {}", mergeId, config.getTargetFile(), data.size());
        
        int batchSize = config.getBatchSize() > 0 ? config.getBatchSize() : 5000;
        
        try (FileOutputStream fos = new FileOutputStream(config.getTargetFile())) {
            com.alibaba.excel.ExcelWriter excelWriter = EasyExcel.write(fos, config.getModelClass()).build();
            WriteSheet writeSheet = EasyExcel.writerSheet("Sheet1").build();
            
            // 分批写入
            for (int i = 0; i < data.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, data.size());
                List<T> batch = data.subList(i, endIndex);
                excelWriter.write(batch, writeSheet);
                log.debug("[{}] 写入进度: {}/{}", mergeId, endIndex, data.size());
                
                // 报告写入进度
                int progress = 60 + (int) (((double) endIndex / data.size()) * 40);
                reportProgress(progress, 100, "写入数据");
            }
            
            excelWriter.finish();
        }
        
        log.info("[{}] 文件写入完成: {}", mergeId, config.getTargetFile());
    }
    
    private void reportProgress(long current, long total, String phase) {
        if (progressCallback != null) {
            progressCallback.onProgress(current, total, phase);
        }
    }
} 