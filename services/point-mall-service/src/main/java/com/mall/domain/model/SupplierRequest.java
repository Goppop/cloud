package com.mall.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 供应商 API 统一请求模型
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SupplierRequest {
    private String action;  // 操作类型（如：create_order, check_status, cancel_order, redeem_card）
    private String userId;  // 用户ID
    private String productId;  // 商品ID
    private Integer quantity;  // 购买数量（适用于 JD 购物）
    private String orderId;  // 订单ID（适用于查询订单）
    private String cardCode;  // 兑换卡密时使用
    private String supplierExtraData; // 供应商特殊参数（如 JD 的地址、好医生的医生 ID）
}
