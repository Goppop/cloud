package com.atguigu.product;

import com.alibaba.cloud.nacos.discovery.NacosDiscoveryClient;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

@SpringBootTest
public class DiscoveryTest {
    @Resource
    DiscoveryClient discoveryClient;
    @Resource
    NacosDiscoveryClient nacosDiscoveryClient;

    @Test
    void discoveryClientTest(){
        for (String service : discoveryClient.getServices()) {
            System.out.println("注册中心中微服务的名字:"+service);
            //获取ip和端口
            var instances = discoveryClient.getInstances(service);
            for (ServiceInstance instance : instances) {
                System.out.println("ip:"+instance.getHost()+"port:"+instance.getPort());

            }
        }
        System.out.println("===============");
        for (String service : nacosDiscoveryClient.getServices()) {
            System.out.println("注册中心中微服务的名字:"+service);
            //获取ip和端口
            var instances = nacosDiscoveryClient.getInstances(service);
            for (ServiceInstance instance : instances) {
                System.out.println("ip:"+instance.getHost()+"port:"+instance.getPort());

            }
        }
    }
}
