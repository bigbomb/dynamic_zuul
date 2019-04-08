package com.superauto.gateway;

import org.apache.dubbo.config.spring.context.annotation.EnableDubboConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.RequestMapping;
import com.superauto.gateway.filter.AccessFilter;

@EnableZuulProxy
@SpringBootApplication
@ComponentScan(value = { "com.superauto.gateway" })
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
    @Bean
    public AccessFilter accessFilter() {
        return new AccessFilter();
    }
    
    @RequestMapping("/")
    public String index(){
        return "This is index!";
    }
}