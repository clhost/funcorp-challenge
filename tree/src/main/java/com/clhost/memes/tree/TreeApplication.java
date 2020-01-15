package com.clhost.memes.tree;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@EnableWebMvc
@SpringBootApplication
public class TreeApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(TreeApplication.class, args);
        System.out.println("Context started");
    }
}
