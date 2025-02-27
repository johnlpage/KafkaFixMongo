package com.mongodb.devrel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class KafkaFixMongo {

    public static void main(String[] args) {
        SpringApplication.run(KafkaFixMongo.class, args);
    }
}
