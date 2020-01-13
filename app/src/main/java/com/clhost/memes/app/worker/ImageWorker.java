package com.clhost.memes.app.worker;

import com.clhost.memes.app.cache.CacheInteract;
import com.clhost.memes.app.data.MemeBucket;
import com.clhost.memes.app.data.MemeData;
import com.clhost.memes.app.image.FindDuplicate;
import com.clhost.memes.app.cache.CacheBucket;
import com.clhost.memes.app.cache.CacheData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

// допилить так, чтобы не только vk data была
@Service
public class ImageWorker {

    private final CacheInteract cacheInteract;
    private final FindDuplicate findDuplicate;
    private final ExecutorService executor;

    @Autowired
    public ImageWorker(CacheInteract cacheInteract, FindDuplicate findDuplicate) {
        this.cacheInteract = cacheInteract;
        this.findDuplicate = findDuplicate;
        this.executor = Executors.newCachedThreadPool();
    }

    public void processWithBucketsAsync(List<MemeBucket> buckets) {
        executor.execute(() -> processBuckets(buckets));
    }

    private void processBuckets(List<MemeBucket> buckets) {
        try {
            List<CacheBucket> cacheBuckets = cacheInteract.acquireAndRanged();
            buckets.forEach(bucket -> processBucket(bucket, cacheBuckets));
        } finally {
            cacheInteract.releaseLock();
        }
    }

    private void processBucket(MemeBucket bucket, List<CacheBucket> cacheBuckets) {
        if (bucket.getData() != null && bucket.getData().size() == 1) {
            processBucketWithSingleMeme(bucket.getData().get(0), cacheBuckets);
        }

        if (bucket.getData() != null && !bucket.getData().isEmpty()) {
            processBucketWithMultipleMemes(bucket.getData(), cacheBuckets);
        }
    }

    private void processBucketWithMultipleMemes(List<MemeData> memeData, List<CacheBucket> cacheBuckets) {

    }

    private void processBucketWithSingleMeme(MemeData memeData, List<CacheBucket> cacheBuckets) {
        List<CacheBucket> filteredCacheBuckets = cacheBuckets.stream()
                .filter(bucket -> bucket.getData() != null && bucket.getData().size() == 1)
                .collect(Collectors.toList());

        for (CacheBucket cacheBucket : filteredCacheBuckets) {
            CacheData cacheData = cacheBucket.getData().get(0);
            int width = cacheData.getWidth();
            int height = cacheData.getHeight();
            String perceptiveHash = cacheData.getPerceptiveHash();

            byte[] img = loadImage(memeData.getUrl());
            //FindDuplicate.FindDuplicateResult result = findDuplicate.isDuplicate(perceptiveHash, width, height, img);

            //if (result.isDuplicate()) return; // and will log it
            //cacheInteract.addBucket();
            // and then save image to minio (wrap it via @Transactional with redis saving)
        }
    }

    private byte[] loadImage(String url) {
        return null;
    }
}
