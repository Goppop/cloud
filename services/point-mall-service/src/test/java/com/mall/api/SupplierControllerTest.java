package com.mall.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.application.SupplierFacade;
import com.mall.domain.model.SupplierRequest;
import com.mall.domain.model.SupplierResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SupplierController.class) // 只加载 SupplierController
public class SupplierControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SupplierFacade supplierFacade; // Mock Facade

    @Autowired
    private ObjectMapper objectMapper; // 用于序列化 JSON

    @Test
    public void testCreateOrder() throws Exception {
        // 1️⃣ Mock 预期返回的响应
        SupplierResponse mockResponse = new SupplierResponse("success", "订单创建成功", "ORDER123", null, null);
        Mockito.when(supplierFacade.createOrder(Mockito.anyString(), Mockito.any(SupplierRequest.class)))
                .thenReturn(mockResponse);

//        private String action;  // 操作类型（如：create_order, check_status, cancel_order, redeem_card）
//        private String userId;  // 用户ID
//        private String productId;  // 商品ID
//        private Integer quantity;  // 购买数量（适用于 JD 购物）
//        private String orderId;  // 订单ID（适用于查询订单）
//        private String cardCode;  // 兑换卡密时使用
//        private String supplierExtraData; // 供应商特殊参数（如 JD 的地址、好医生的医生 ID）


        // 2️⃣ 创建 SupplierRequest JSON
        SupplierRequest request = new SupplierRequest("create_order", "123", "456", 1, "11111", "33333","JD");
        String jsonRequest = objectMapper.writeValueAsString(request);

        // 3️⃣ 发送请求并断言
        mockMvc.perform(MockMvcRequestBuilders.post("/api/supplier/jd/order") // ✅ 确保用 MockMvcRequestBuilders
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk()) // ✅ 期望 HTTP 200
                .andExpect(jsonPath("$.status").value("success")) // ✅ 期望 JSON 里有 status=success
                .andExpect(jsonPath("$.orderId").value("ORDER123")); // ✅ 期望 JSON 里有 orderId=ORDER123
    }
}
