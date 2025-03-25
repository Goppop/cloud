package com.study.tools.highExcelTools.example;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import lombok.Data;

import java.util.Date;

/**
 * 用户数据模型
 * 用于Excel导入导出
 */
@Data
public class UserModel {
    /**
     * 用户ID
     */
    @ExcelProperty("用户ID")
    private String id;
    
    /**
     * 用户名
     */
    @ExcelProperty("用户名")
    private String username;
    
    /**
     * 年龄
     */
    @ExcelProperty("年龄")
    private Integer age;
    
    /**
     * 邮箱
     */
    @ExcelProperty("邮箱")
    private String email;
    
    /**
     * 手机号
     */
    @ExcelProperty("手机号")
    private String mobile;
    
    /**
     * 积分
     */
    @ExcelProperty("积分")
    private Integer points;
    
    /**
     * 注册时间
     */
    @ExcelProperty("注册时间")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private Date registerTime;
    
    /**
     * 状态(0-禁用, 1-正常)
     */
    @ExcelProperty("状态")
    private Integer status;
    
    /**
     * 备注
     */
    @ExcelProperty("备注")
    private String remark;
} 