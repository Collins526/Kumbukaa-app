package com.kumbukaa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KumbukaaApplication {

    public static void main(String[] args) {
        SpringApplication.run(KumbukaaApplication.class, args);
    }
}
