package com.clhost.memes.tree.api;

import com.clhost.memes.tree.service.MemeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import javax.validation.Valid;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Validated
@RestController
public class TreeController implements TreeApi {
    private static final Logger LOGGER = LogManager.getLogger(TreeController.class);

    private final int imageWorkersCount;
    private final MemeHandler memeHandler;
    private final ExecutorService executor;
    private final ArrayBlockingQueue<Chunk> incomingMemes;

    @Autowired
    public TreeController(MemeHandler memeHandler,
                          @Value("${service.queue_capacity}") int queueCapacity,
                          @Value("${service.image_workers_count}") int imageWorkersCount) {
        this.memeHandler = memeHandler;
        this.imageWorkersCount = imageWorkersCount;
        this.executor = Executors.newFixedThreadPool(imageWorkersCount);
        this.incomingMemes = new ArrayBlockingQueue<>(queueCapacity);
    }

    @PostConstruct
    public void initWorkers() {
        for (int i = 0; i < imageWorkersCount; i++) { executor.execute(this::handleMeme); }
    }

    @Override
    public void putAsync(@RequestBody @Valid MetaMeme metaMeme) {
        try {
            incomingMemes.put(new Chunk(metaMeme, MDC.get("cid")));
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private void handleMeme() {
        try {
            while (true) {
                Chunk chunk = incomingMemes.take();
                MDC.put("cid", chunk.cid);
                memeHandler.handleMeme(chunk.meme);
            }
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @AllArgsConstructor
    private static class Chunk {
        private MetaMeme meme;
        private String cid;
    }
}
