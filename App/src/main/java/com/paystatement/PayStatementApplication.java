package com.paystatement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.paystatement")
public class PayStatementApplication {
    public static void main(String[] args) {
        SpringApplication.run(PayStatementApplication.class, args);
    }
}
