package com.clhost.memes.tree.interceptor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InterceptorConfiguration {

    @Bean
    public HttpRequestInterceptor interceptor() {
        return new HttpRequestInterceptor();
    }
}
