package com.clhost.memes.integration.vk;

import com.clhost.memes.consul.ConsulData;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Воркер будет рулить и парсингом консул даты, и выделением из него vk, и тредпулом
 */
@Service
public class VkLoader {

    @Value("${service.vk.memes_count}")
    private int memesCount;

    @Value("${service.vk.startup_memes_count}")
    private int startupMemesCount;

    private final VkClient vkClient;
    private final ExecutorService executor;

    @Autowired
    public VkLoader(VkClient vkClient) {
        this.vkClient = vkClient;
        this.executor = Executors.newSingleThreadExecutor(); // пусть пока так
    }

    public List<Future<List<VkBucket>>> onStartup(ConsulData source) {
        if (memesCount > startupMemesCount) {
            throw new IllegalStateException();
        }

        int offsetCounter = 0;
        List<Future<List<VkBucket>>> futures = new ArrayList<>();

        while (offsetCounter + memesCount <= startupMemesCount) {
            int offset = offsetCounter;
            futures.add(executor.submit(() -> memes(source, memesCount, offset)));
            offsetCounter += memesCount;
        }

        if (offsetCounter < startupMemesCount) {
            int offset = offsetCounter;
            int count = startupMemesCount - offsetCounter;
            futures.add(executor.submit(() -> memes(source, count, offset)));
        }

        return futures;
    }

    public Future<List<VkBucket>> onRegular(ConsulData source) {
        return executor.submit(() -> memes(source, memesCount, 0));
    }

    private List<VkBucket> memes(ConsulData data, int count, int offset) throws ClientException, ApiException {
        List<VkBucket> memes = vkClient.memes(data.getSrc(), count, offset);
        memes.forEach(meme -> {
            meme.setLang(data.getLang());
            meme.setSource(data.sourceDesc());
        });
        return memes;
    }
}
