package com.clhost.memes.app.consul;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cloud.consul.discovery.ConsulDiscoveryProperties;

import java.util.UUID;

public class InstanceIdBeanPostProcessor implements BeanPostProcessor {
    public static final String instanceId = UUID.randomUUID().toString().replace("-", "");

    @Value("${spring.application.name}")
    private String appName;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        ConsulDiscoveryProperties properties = null;
        if (bean instanceof ConsulDiscoveryProperties) {
            properties = (ConsulDiscoveryProperties) bean;
            properties.setInstanceId(appName + "-" + instanceId);
        }
        return properties == null ? bean : properties;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }
}
