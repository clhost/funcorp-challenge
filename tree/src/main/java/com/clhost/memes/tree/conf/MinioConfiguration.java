package com.clhost.memes.tree.conf;

import io.minio.MinioClient;
import io.minio.errors.InvalidEndpointException;
import io.minio.errors.InvalidPortException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfiguration {

    @Value("${service.minio.endpoint}")
    private String endpoint;

    @Value("${service.minio.port}")
    private int port;

    @Value("${service.minio.access_key}")
    private String accessKey;

    @Value("${service.minio.secret_key}")
    private String secretKey;

    @Bean
    public MinioClient minioClient() throws InvalidPortException, InvalidEndpointException {
        return new MinioClient(endpoint, port, accessKey, secretKey, false);
    }
}
