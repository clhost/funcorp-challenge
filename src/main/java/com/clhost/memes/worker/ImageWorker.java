package com.clhost.memes.worker;

import com.clhost.memes.image.FindDuplicate;
import com.clhost.memes.integration.vk.VkBucket;
import com.clhost.memes.integration.vk.VkData;
import com.clhost.memes.redis.RedisBucket;
import com.clhost.memes.redis.RedisData;
import com.clhost.memes.redis.RedisInteract;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

// допилить так, чтобы не только vk data была
@Service
public class ImageWorker {

    private final RedisInteract redisInteract;
    private final FindDuplicate findDuplicate;
    private final ExecutorService executor;

    @Autowired
    public ImageWorker(RedisInteract redisInteract, FindDuplicate findDuplicate) {
        this.redisInteract = redisInteract;
        this.findDuplicate = findDuplicate;
        this.executor = Executors.newCachedThreadPool();
    }

    public void processWithBucketsAsync(List<VkBucket> buckets) {
        executor.execute(() -> processBuckets(buckets));
    }

    private void processBuckets(List<VkBucket> buckets) {
        List<RedisBucket> redisBuckets = redisInteract.ranged();
        buckets.forEach(bucket -> processBucket(bucket, redisBuckets));
    }

    // после сейва в редис положить в бакеты локально или запросить всё снова?
    private void processBucket(VkBucket bucket, List<RedisBucket> redisBuckets) {
        if (bucket.getData() != null && bucket.getData().size() == 1) {
            processBucketWithSingleMeme(bucket.getData().get(0), redisBuckets);
        }

        if (bucket.getData() != null && !bucket.getData().isEmpty()) {
            processBucketWithMultipleMemes(bucket.getData(), redisBuckets);
        }
    }

    private void processBucketWithMultipleMemes(List<VkData> vkData, List<RedisBucket> redisBuckets) {

    }

    private void processBucketWithSingleMeme(VkData vkData, List<RedisBucket> redisBuckets) {
        List<RedisBucket> filteredRedisBuckets = redisBuckets.stream()
                .filter(bucket -> bucket.getData() != null && bucket.getData().size() == 1)
                .collect(Collectors.toList());

        for (RedisBucket redisBucket : filteredRedisBuckets) {
            RedisData redisData = redisBucket.getData().get(0);
            int width = redisData.getWidth();
            int height = redisData.getHeight();
            String perceptiveHash = redisData.getPerceptiveHash();

            byte[] img = loadImage(vkData.getUrl());
            FindDuplicate.FindDuplicateResult result = findDuplicate.isDuplicate(perceptiveHash, width, height, img);

            if (result.isDuplicate()) return; // and will log it
            redisInteract.addBucket();
            // and then save image to minio (wrap it via @Transactional with redis saving)
        }
    }

    private byte[] loadImage(String url) {
        return null;
    }
}
