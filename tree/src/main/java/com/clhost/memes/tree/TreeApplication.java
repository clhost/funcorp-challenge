package com.clhost.memes.tree;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@EnableWebMvc
@SpringBootApplication
public class TreeApplication {
    private static final Logger LOGGER = LogManager.getLogger(TreeApplication.class);

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(TreeApplication.class, args);
        LOGGER.info("{} started", TreeApplication.class.getName());
    }
}
