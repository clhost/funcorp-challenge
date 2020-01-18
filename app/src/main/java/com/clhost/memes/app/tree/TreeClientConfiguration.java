package com.clhost.memes.app.tree;

import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TreeClientConfiguration {

    @Value("${service.tree.name}")
    private String name;

    @Value("${service.tree.read-timeout}")
    private int readTimeout;

    @Value("${service.tree.connection-timeout}")
    private int connectionTimeout;

    @Bean
    public IClientConfig ribbonClientConfig() {
        DefaultClientConfigImpl config = new DefaultClientConfigImpl();
        config.loadProperties(this.name);
        config.set(CommonClientConfigKey.ConnectTimeout, readTimeout);
        config.set(CommonClientConfigKey.ReadTimeout, connectionTimeout);
        config.set(CommonClientConfigKey.GZipPayload, true);
        return config;
    }
}
