package io.statusmachina;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "io.statusmachina")
@EnableJpaRepositories(basePackages = "io.statusmachina")
@EntityScan(basePackages = "io.statusmachina")
public class TestSpringBootApp {
    public static void main(String[] args) {
        SpringApplication.run(TestSpringBootApp.class, args);
    }
}
