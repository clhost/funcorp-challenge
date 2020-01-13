package com.clhost.memes.app.integration.vk;

import com.clhost.memes.app.data.MemeBucket;
import com.clhost.memes.app.integration.MemeLoader;
import com.clhost.memes.app.sources.SourceData;
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

@Service
public class VkLoader implements MemeLoader {
    private static final String ONLY_GROUP = "vk:group";

    @Value("${service.vk.memes_count}")
    private int memesCount;

    @Value("${service.vk.startup_memes_count}")
    private int startupMemesCount;

    private final VkClient vkClient;
    private final ExecutorService executor;

    @Autowired
    public VkLoader(VkClient vkClient) {
        this.vkClient = vkClient;
        this.executor = Executors.newCachedThreadPool();
    }

    @Override
    public boolean isAccepted(SourceData sourceData) {
        return ONLY_GROUP.equals(sourceData.sourceDesc().replaceFirst(":\\w+$", ""));
    }

    @Override
    public List<Future<List<MemeBucket>>> onStartup(SourceData source) {
        if (memesCount > startupMemesCount) {
            throw new IllegalStateException();
        }

        int offsetCounter = 0;
        List<Future<List<MemeBucket>>> futures = new ArrayList<>();

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

    @Override
    public Future<List<MemeBucket>> onRegular(SourceData source) {
        return executor.submit(() -> memes(source, memesCount, 0));
    }

    private List<MemeBucket> memes(SourceData data, int count, int offset) throws ClientException, ApiException {
        List<MemeBucket> memes = vkClient.memes(data.getSubSource(), count, offset);
        memes.forEach(meme -> {
            meme.setLang(data.getLang());
            meme.setSource(data.sourceDesc());
        });
        return memes;
    }
}
