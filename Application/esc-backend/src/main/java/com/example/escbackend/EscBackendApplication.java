package com.example.escbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EscBackendApplication {

    public static void main(String[] args) {

        SpringApplication.run(EscBackendApplication.class, args);
        System.out.println("EscrowX BackendApplication started");
    }

}
