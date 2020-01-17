package com.clhost.memes.app.worker;

import com.clhost.memes.app.dao.MemesDao;
import com.clhost.memes.app.sources.SourceData;
import com.clhost.memes.app.sources.SourcesProvider;
import com.clhost.memes.app.tree.MetaMeme;
import com.clhost.memes.app.tree.TreeClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
public class BasicWorker {
    private static final Logger LOGGER = LogManager.getLogger(BasicWorker.class);
    private static final String WORK_DELAY_PLACEHOLDER = "${service.worker.delay}";

    private final MemesDao memesDao;
    private final TreeClient treeClient;
    private final ExecutorService executor;
    private final List<MemeLoader> memeLoaders;
    private final SourcesProvider sourcesProvider;

    @Autowired
    public BasicWorker(MemesDao memesDao, SourcesProvider sourcesProvider,
                       List<MemeLoader> memeLoaders, TreeClient treeClient) {
        this.memesDao = memesDao;
        this.sourcesProvider = sourcesProvider;
        this.memeLoaders = memeLoaders;
        this.treeClient = treeClient;
        this.executor = Executors.newCachedThreadPool();
    }

    @Scheduled(initialDelayString = WORK_DELAY_PLACEHOLDER, fixedDelayString = WORK_DELAY_PLACEHOLDER)
    public void work() throws ExecutionException, InterruptedException {
        List<SourceData> sources = sourcesProvider.sources();
        if (sources.isEmpty()) {
            LOGGER.warn("Source provider returns empty sources! Skip worker iteration");
            return;
        }

        List<Future<List<MemeBucket>>> fromAllSources = new ArrayList<>();
        for (SourceData source : sources) {
            MemeLoader loader = memeLoader(source);
            if (loader == null) continue;

            if (memesDao.isSourceExists(source.sourceDesc())) {
                Future<List<MemeBucket>> future = loader.onRegular(source);
                fromAllSources.add(future);
            } else {
                List<Future<List<MemeBucket>>> futures = loader.onStartup(source);
                fromAllSources.addAll(futures);
            }
        }

        handle(fromAllSources);
    }

    private void handle(List<Future<List<MemeBucket>>> fromAllSources) throws ExecutionException, InterruptedException {
        int index = 0;
        while (!fromAllSources.isEmpty()) {
            Future<List<MemeBucket>> future = fromAllSources.get(index);
            if (future.isDone()) {
                List<MemeBucket> memeBuckets = future.get();
                executor.execute(() -> memeBuckets.forEach(this::putAsync));
                fromAllSources.remove(index);
                index = 0;
            } else {
                index++;
                if (index >= fromAllSources.size()) index = 0;
            }
        }
    }

    private MemeLoader memeLoader(SourceData sourceData) {
        return memeLoaders.stream()
                .filter(memeLoader -> memeLoader.isAccepted(sourceData))
                .findAny().orElse(null);
    }

    private void putAsync(MemeBucket bucket) {
        try {
            treeClient.putAsync(map(bucket));
        } catch (Exception e) {
            LOGGER.error("Failed to put meme={}, message={}", bucket, e.getMessage());
        }
    }

    private MetaMeme map(MemeBucket bucket) {
        return MetaMeme.builder()
                .lang(bucket.getLang())
                .source(bucket.getSource())
                .text(bucket.getText())
                .urls(bucket.getUrls())
                .build();
    }
}
