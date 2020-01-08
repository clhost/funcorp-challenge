package com.clhost.memes.worker;

import com.clhost.memes.consul.ConsulData;
import com.clhost.memes.consul.ConsulInteract;
import com.clhost.memes.integration.vk.VkBucket;
import com.clhost.memes.integration.vk.VkLoader;
import com.clhost.memes.redis.RedisInteract;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Service
public class BasicWorker {
    //private static final Logger LOGGER = LoggerFactory.getLogger(BasicWorker.class);

    private final ConsulInteract consulInteract;
    private final RedisInteract redisInteract;
    private final VkLoader vkLoader;
    private final ImageWorker imageWorker;

    @Autowired
    public BasicWorker(ConsulInteract consulInteract,
                       RedisInteract redisInteract,
                       VkLoader vkLoader,
                       ImageWorker imageWorker) {
        this.consulInteract = consulInteract;
        this.redisInteract = redisInteract;
        this.vkLoader = vkLoader;
        this.imageWorker = imageWorker;
    }

    // нет проверки на то какой это сурс
    //@Scheduled
    public void act() throws ExecutionException, InterruptedException {
        List<ConsulData> sources = consulInteract.sources();
        if (sources.isEmpty()) return; // and will log it

        List<Future<List<VkBucket>>> fromAllSources = new ArrayList<>();
        for (ConsulData source : sources) {
            boolean isExists = redisInteract.atLeastOneBucketExistsBySource(source.sourceDesc());
            if (isExists) {
                Future<List<VkBucket>> future = vkLoader.onRegular(source);
                fromAllSources.add(future);
            } else {
                List<Future<List<VkBucket>>> futures = vkLoader.onStartup(source);
                fromAllSources.addAll(futures);
            }
        }

        handle(fromAllSources);
    }

    private void handle(List<Future<List<VkBucket>>> fromAllSources) throws ExecutionException, InterruptedException {
        int index = 0;
        while (!fromAllSources.isEmpty()) {
            Future<List<VkBucket>> future = fromAllSources.get(index);
            if (future.isDone()) {
                imageWorker.processWithBucketsAsync(future.get());
                fromAllSources.remove(index);
                index = 0;
            } else {
                index++;
                if (index >= fromAllSources.size()) index = 0;
            }
        }
    }
}
