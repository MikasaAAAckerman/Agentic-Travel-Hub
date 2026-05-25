package com.travel.starter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
    "com.travel"
})
public class HubStarterApplication {

    public static void main(String[] args) {
        SpringApplication.run(HubStarterApplication.class, args);
    }

}
