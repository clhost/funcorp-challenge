package com.clhost.memes.app.worker;

import com.clhost.memes.app.integration.VkClient;
import com.clhost.memes.app.integration.VkLoader;
import com.google.common.collect.Lists;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class MemeLoaderConfiguration {
    @Bean
    public List<MemeLoader> loaders(VkClient vkClient) {
        return Lists.newArrayList(new VkLoader(vkClient));
    }
}
