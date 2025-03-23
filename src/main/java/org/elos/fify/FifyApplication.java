package org.elos.fify;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling


public class FifyApplication {

    public static void main(String[] args) {
        SpringApplication.run(FifyApplication.class, args);
    }

}
