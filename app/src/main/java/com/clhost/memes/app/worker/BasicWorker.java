package com.clhost.memes.app.worker;

import com.clhost.memes.app.data.MemeBucket;
import com.clhost.memes.app.integration.MemeLoader;
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

    private final SourcesProvider sourcesProvider;
    private final List<MemeLoader> memeLoaders;

    private final TreeClient treeClient;
    private final ExecutorService executor;
    private final HashProvider hashProvider;

    @Autowired
    public BasicWorker(SourcesProvider sourcesProvider, List<MemeLoader> memeLoaders,
                       TreeClient treeClient, HashProvider hashProvider) {
        this.sourcesProvider = sourcesProvider;
        this.memeLoaders = memeLoaders;
        this.treeClient = treeClient;
        this.hashProvider = hashProvider;
        this.executor = Executors.newCachedThreadPool();
    }

    @Scheduled
    public void act() throws ExecutionException, InterruptedException {
        List<SourceData> sources = sourcesProvider.sources();
        if (sources.isEmpty()) {
            LOGGER.warn("Source provider returns empty sources! Skip worker iteration");
            return;
        }

        List<Future<List<MemeBucket>>> fromAllSources = new ArrayList<>();
        for (SourceData source : sources) {
            MemeLoader loader = memeLoader(source);
            if (loader == null) continue;

            boolean isExists = false; // cacheInteract.atLeastOneBucketExistsBySource(source.sourceDesc());
            if (isExists) {
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
                executor.execute(() -> process(memeBuckets));
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

    private void process(List<MemeBucket> buckets) {
        buckets.forEach(bucket -> treeClient.putAsync(map(bucket)));
    }

    private MetaMeme map(MemeBucket bucket) {
        return MetaMeme.builder()
                .bucketId(hashProvider.hash(bucket.getUrls()))
                .lang(bucket.getLang())
                .source(bucket.getSource())
                .urls(bucket.getUrls())
                .build();
    }
}
