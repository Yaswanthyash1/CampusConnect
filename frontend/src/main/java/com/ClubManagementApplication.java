package com;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ClubManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClubManagementApplication.class, args);

        System.out.println("\u001B[32mHello World!\u001B[0m");
    }
}
