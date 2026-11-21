package com.karatly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class KaratlyApplication {

    public static void main(String[] args) {
        SpringApplication.run(KaratlyApplication.class, args);
    }
}
