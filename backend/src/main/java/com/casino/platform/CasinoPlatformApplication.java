package com.casino.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class CasinoPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(CasinoPlatformApplication.class, args);
    }
}
