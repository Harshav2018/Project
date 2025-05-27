package com.example.finalyearproject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories
public class FinalYearProjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinalYearProjectApplication.class, args);
    }

}
