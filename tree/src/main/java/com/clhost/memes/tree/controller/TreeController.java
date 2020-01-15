package com.clhost.memes.tree.controller;

import com.clhost.memes.tree.EntryPoint;
import com.clhost.memes.tree.data.MetaMeme;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import javax.validation.Valid;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Validated
@RestController
public class TreeController implements TreeApi {
    private static final Logger LOGGER = LogManager.getLogger(TreeController.class);

    private final EntryPoint entryPoint;
    private final ExecutorService executor;
    private final ArrayBlockingQueue<MetaMeme> incomingMemes;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    public TreeController(EntryPoint entryPoint, @Value("${service.queue_capacity}") int queueCapacity) {
        this.entryPoint = entryPoint;
        this.executor = Executors.newSingleThreadExecutor();
        this.incomingMemes = new ArrayBlockingQueue<>(queueCapacity);
    }

    @PostConstruct
    public void initRead() {
        executor.execute(this::doWithMeme);
    }

    @Override
    public void putAsync(@RequestBody @Valid MetaMeme metaMeme) {
        putMeme(metaMeme);
    }

    @RequestMapping(value = "/tree/check", method = RequestMethod.GET)
    public void putAsync() {
        System.out.println("breakpoint");
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
                long a = System.currentTimeMillis();
                MetaMeme meme = incomingMemes.take();
                System.out.println("Process meme(" + (System.currentTimeMillis() - a) + "): " + meme);
                entryPoint.doIt(meme);
            }
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }
}
