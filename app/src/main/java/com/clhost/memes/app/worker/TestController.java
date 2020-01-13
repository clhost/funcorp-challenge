package com.clhost.memes.app.worker;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/meme")
public class TestController {

    private final BasicWorker worker;

    @Autowired
    public TestController(BasicWorker worker) {
        this.worker = worker;
    }

    @GetMapping("/test")
    public String test() throws ExecutionException, InterruptedException {
        worker.act();
        return "ok!";
    }
}
