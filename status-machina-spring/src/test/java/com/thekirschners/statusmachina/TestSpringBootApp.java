package com.thekirschners.statusmachina;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.thekirschners.statusmachina")
@EnableJpaRepositories(basePackages = "com.thekirschners.statusmachina")
@EntityScan(basePackages = "com.thekirschners.statusmachina")
public class TestSpringBootApp {
    public static void main(String[] args) {
        SpringApplication.run(TestSpringBootApp.class, args);
    }
}
