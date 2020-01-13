package com.clhost.memes.tree.conf;

import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfiguration {
    @Bean
    public MinioClient minioClient() {
        //return new MinioClient();
        return null;
    }
}
