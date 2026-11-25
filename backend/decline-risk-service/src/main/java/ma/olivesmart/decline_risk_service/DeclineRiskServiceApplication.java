package ma.olivesmart.decline_risk_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients

public class DeclineRiskServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeclineRiskServiceApplication.class, args);
    }

}
