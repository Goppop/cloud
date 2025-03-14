package com.mall.infrastructure.client;

import com.mall.domain.model.SupplierRequest;
import com.mall.domain.model.SupplierResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class SupplierApiClient {

    private final Map<String, SupplierService> supplierMap;


    @Autowired
    public SupplierApiClient(List<SupplierService> supplierServices) {
        System.out.println("Spring 注入的 SupplierService 数量: " + supplierServices.size());
        supplierServices.forEach(service -> System.out.println("Loaded SupplierService: " + service.getSupplierName()));

        this.supplierMap = supplierServices.stream()
                .collect(Collectors.toMap(SupplierService::getSupplierName, Function.identity()));
    }

    public SupplierResponse createOrder(String supplierName, SupplierRequest request) {
        return getSupplierService(supplierName).createOrder(request);
    }

    public SupplierResponse getOrderStatus(String supplierName, String orderId) {
        return getSupplierService(supplierName).getOrderStatus(orderId);
    }

    public SupplierResponse cancelOrder(String supplierName, String orderId) {
        return getSupplierService(supplierName).cancelOrder(orderId);
    }

    public SupplierResponse redeemCard(String supplierName, SupplierRequest request) {
        return getSupplierService(supplierName).redeemCard(request);
    }

    private SupplierService getSupplierService(String supplierName) {
        SupplierService supplierService = supplierMap.get(supplierName);
        if (supplierService == null) {
            throw new IllegalArgumentException("未找到供应商：" + supplierName);
        }
        return supplierService;
    }
}
