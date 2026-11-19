package com.example.tinyurllab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class TinyUrlLabApplication {

    public static void main(String[] args) {
        SpringApplication.run(TinyUrlLabApplication.class, args);
    }

}
