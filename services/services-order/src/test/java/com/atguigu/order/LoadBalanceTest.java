package com.atguigu.order;


import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;

@SpringBootTest
public class LoadBalanceTest {
    @Resource
    LoadBalancerClient loadBalancerClient;

    @Test
    void test(){
        var instance = loadBalancerClient.choose("service-product");
        System.out.println(instance.getHost()+":"+instance.getPort());
        var instance1 = loadBalancerClient.choose("service-product");
        System.out.println(instance1.getHost()+":"+instance1.getPort());
        var instance2 = loadBalancerClient.choose("service-product");
        System.out.println(instance2.getHost()+":"+instance2.getPort());
    }

}
