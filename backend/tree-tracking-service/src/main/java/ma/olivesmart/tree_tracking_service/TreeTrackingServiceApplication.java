package ma.olivesmart.tree_tracking_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients

public class TreeTrackingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TreeTrackingServiceApplication.class, args);
    }

}
