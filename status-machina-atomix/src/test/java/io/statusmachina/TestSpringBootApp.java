package io.statusmachina;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "io.statusmachina")
//@EnableJpaRepositories(basePackages = "com.thekirschners.statusmachina")
//@EntityScan(basePackages = "com.thekirschners.statusmachina")
public class TestSpringBootApp {
    public static void main(String[] args) {
        SpringApplication.run(TestSpringBootApp.class, args);
    }
}
