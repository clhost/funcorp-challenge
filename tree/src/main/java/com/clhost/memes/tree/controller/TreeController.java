package com.clhost.memes.tree.controller;

import com.clhost.memes.tree.data.MetaMeme;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import javax.validation.Valid;
import java.time.OffsetDateTime;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Validated
@RestController
public class TreeController implements TreeApi {
    private static final Logger LOGGER = LogManager.getLogger(TreeController.class);

    private final EntryPoint entryPoint;
    private final ExecutorService executor;
    private final ArrayBlockingQueue<MetaMeme> incomingMemes;
    private final int imageLoadersCount;

    @Autowired
    public TreeController(EntryPoint entryPoint,
                          @Value("${service.queue_capacity}") int queueCapacity,
                          @Value("${service.image_loaders_count}") int imageLoadersCount) {
        this.entryPoint = entryPoint;
        this.imageLoadersCount = imageLoadersCount;
        this.executor = Executors.newFixedThreadPool(imageLoadersCount);
        this.incomingMemes = new ArrayBlockingQueue<>(queueCapacity);
    }

    @PostConstruct
    public void initRead() {
        for (int i = 0; i < imageLoadersCount; i++) { executor.execute(this::doWithMeme); }
    }

    @Override
    public void putAsync(@RequestBody @Valid MetaMeme metaMeme) {
        putMeme(metaMeme);
    }

    private void putMeme(MetaMeme metaMeme) {
        try {
            incomingMemes.put(metaMeme);
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private void doWithMeme() {
        try {
            while (true) {
                MetaMeme meme = incomingMemes.take();
                System.out.println("[" + OffsetDateTime.now().toString() + "] " + Thread.currentThread().getName() + " getting item");
                entryPoint.doIt(meme);
            }
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }
}
