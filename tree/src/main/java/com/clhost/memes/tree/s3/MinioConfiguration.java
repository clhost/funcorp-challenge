package com.clhost.memes.tree.s3;

import io.minio.MinioClient;
import io.minio.errors.InvalidEndpointException;
import io.minio.errors.InvalidPortException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfiguration {

    @Value("${service.s3.endpoint}")
    private String endpoint;

    @Value("${service.s3.port}")
    private int port;

    @Value("${service.s3.access_key}")
    private String accessKey;

    @Value("${service.s3.secret_key}")
    private String secretKey;

    @Bean
    public MinioClient minioClient() throws InvalidPortException, InvalidEndpointException {
        return new MinioClient(endpoint, port, accessKey, secretKey, false);
    }
}
