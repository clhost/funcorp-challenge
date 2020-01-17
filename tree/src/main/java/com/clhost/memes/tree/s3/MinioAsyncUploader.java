package com.clhost.memes.tree.s3;

import io.minio.MinioClient;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class MinioAsyncUploader {
    private static final Logger LOGGER = LogManager.getLogger(MinioAsyncUploader.class);

    private final MinioClient minioClient;
    private final ExecutorService executor;

    @Value("${service.minio.bucket_name}")
    private String minioBucketName;

    @Autowired
    public MinioAsyncUploader(MinioClient minioClient,
                              @Value("${service.minio.uploader-workers-count}") int workersCount) {
        this.minioClient = minioClient;
        this.executor = Executors.newFixedThreadPool(workersCount);
    }

    public void uploadAsync(MinioObject object) {
        CompletableFuture
                .runAsync(() -> putObject(object), executor)
                .thenRun(() -> LOGGER.debug("Successfully loaded object = {}", object.objectName));
    }

    private void putObject(MinioObject object) {
        try {
            minioClient.putObject(
                    minioBucketName, object.objectName,
                    new ByteArrayInputStream(object.image), (long) object.image.length, null, null, null);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Data
    @AllArgsConstructor
    public static class MinioObject {
        private byte[] image;
        private String objectName;
    }
}
