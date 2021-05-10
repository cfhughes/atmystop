package com.chughes.atmystop.gtfsrt_data;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RTDataApplication {

    public static void main(String[] args) {
        SpringApplication.run(RTDataApplication.class, args);
    }

}
