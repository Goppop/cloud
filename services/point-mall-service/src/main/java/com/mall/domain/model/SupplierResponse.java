package com.mall.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 供应商 API 统一返回模型
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SupplierResponse {
    private String status;  // "success" 或 "fail"
    private String message; // 详细描述信息
    private String orderId; // 订单ID（适用于下单成功后）
    private String cardCode; // 卡密（适用于兑换卡密后）
    private Object supplierData; // 供应商返回的额外数据（如 JD 订单详情）
}
