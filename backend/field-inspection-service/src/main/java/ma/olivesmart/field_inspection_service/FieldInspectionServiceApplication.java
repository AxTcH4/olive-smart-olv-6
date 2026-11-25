package ma.olivesmart.field_inspection_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;


@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients

public class FieldInspectionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FieldInspectionServiceApplication.class, args);
    }

}
