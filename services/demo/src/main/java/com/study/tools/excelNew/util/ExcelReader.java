//package com.study.tools.excelNew.util;
//
//import com.alibaba.excel.EasyExcel;
//import com.alibaba.excel.context.AnalysisContext;
//import com.alibaba.excel.event.AnalysisEventListener;
//import com.study.tools.excelNew.ProgressCallback;
//import lombok.extern.slf4j.Slf4j;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Future;
//
///**
// * Excel读取工具
// */
//@Slf4j
//public class ExcelReader<T> {
//    private final ExecutorService executorService;
//    private final ProgressCallback progressCallback;
//    private final String mergeId;
//
//    public ExcelReader(ExecutorService executorService, ProgressCallback progressCallback, String mergeId) {
//        this.executorService = executorService;
//        this.progressCallback = progressCallback;
//        this.mergeId = mergeId;
//    }
//
//    /**
//     * 读取所有Excel文件
//     */
//    public List<T> readAllFiles(List<String> files, Class<T> modelClass) throws Exception {
//        List<Future<List<T>>> futures = new ArrayList<>();
//
//        // 提交读取任务
//        for (String file : files) {
//            futures.add(executorService.submit(() -> readSingleFile(file, modelClass)));
//        }
//
//        // 收集结果
//        List<T> result = new ArrayList<>();
//        for (int i = 0; i < futures.size(); i++) {
//            result.addAll(futures.get(i).get());
//            // 报告读取进度
//            reportProgress(i + 1, futures.size(), "读取文件");
//        }
//
//        log.info("[{}] 成功读取 {} 个文件，总数据行数: {}", mergeId, files.size(), result.size());
//        return result;
//    }
//
//    /**
//     * 读取单个Excel文件
//     */
//    private List<T> readSingleFile(String filePath, Class<T> modelClass) {
//        log.info("[{}] 开始读取文件: {}", mergeId, filePath);
//        List<T> data = new ArrayList<>();
//
//        EasyExcel.read(filePath, modelClass, new AnalysisEventListener<T>() {
//            @Override
//            public void invoke(T t, AnalysisContext analysisContext) {
//                data.add(t);
//            }
//
//            @Override
//            public void doAfterAllAnalysed(AnalysisContext analysisContext) {
//                log.info("[{}] 文件读取完成: {}，行数: {}", mergeId, filePath, data.size());
//            }
//        }).sheet().doRead();
//
//        return data;
//    }
//
//    private void reportProgress(long current, long total, String phase) {
//        if (progressCallback != null) {
//            progressCallback.onProgress(current, total, phase);
//        }
//    }
//}