package com.clhost.memes.consul;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConsulConfiguration {
    @Bean
    public BeanPostProcessor instanceIdBeanPostProcessor() {
        return new InstanceIdBeanPostProcessor();
    }
}
