package com.clhost.memes.tree.service;

import com.clhost.memes.tree.api.MetaMeme;
import com.clhost.memes.tree.dao.MemesDao;
import com.clhost.memes.tree.vptree.MetricSpace;
import com.clhost.memes.tree.vptree.VPTreeService;
import com.github.kilianB.hash.Hash;
import com.github.kilianB.hashAlgorithms.HashingAlgorithm;
import com.github.kilianB.hashAlgorithms.PerceptiveHash;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class MemeHandler {
    private static final Logger LOGGER = LogManager.getLogger(MemeHandler.class);

    @Value("${service.tree.bucket_duplicate_threshold}")
    private double bucketDuplicateThreshold;

    @Value("${service.tree.count_of_hashes}")
    private long countOfHashes;

    private final MemesDao dao;
    private final MemeSaver saver;
    private final MetricSpace metricSpace;
    private final HashingAlgorithm algorithm;
    private final ReentrantLock lock;

    @Autowired
    public MemeHandler(MemesDao dao, MemeSaver saver, VPTreeService metricSpace,
                       @Value("${service.tree.bit_resolution}") int bitResolution) {
        this.dao = dao;
        this.saver = saver;
        this.metricSpace = metricSpace;
        this.lock = new ReentrantLock();
        this.algorithm = new PerceptiveHash(bitResolution);
    }

    @PostConstruct
    public void initMetricSpace() {
        List<String> hashes = dao.lastNodes(countOfHashes);
        metricSpace.load(hashes);
    }

    public void handleMeme(MetaMeme meme) {
        try {
            List<MemeSaver.MemeShort> memeShorts = collectMemeShorts(meme.getUrls());
            makeDecision(meme, memeShorts);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private List<MemeSaver.MemeShort> collectMemeShorts(List<String> urls) throws Exception {
        List<MemeSaver.MemeShort> memeShorts = new ArrayList<>();
        for (String url : urls) {
            byte[] imageBytes = loadImage(url);
            Hash hash = algorithm.hash(convertBytesToImage(imageBytes));
            memeShorts.add(new MemeSaver.MemeShort(hash, imageBytes, url));
        }
        return memeShorts;
    }

    private void makeDecision(MetaMeme meme, List<MemeSaver.MemeShort> memeShorts) {
        try {
            lock.lock();
            if (memeShorts.isEmpty()) return;
            if (memeShorts.size() == 1)
                makeDecisionSingle(meme, memeShorts.get(0));
            else
                makeDecisionMultiple(meme, memeShorts);
        } finally {
            lock.unlock();
        }
    }

    private void makeDecisionSingle(MetaMeme meme, MemeSaver.MemeShort memeShort) {
        boolean duplicate = metricSpace.isDuplicate(memeShort.hash);
        LOGGER.debug("The meme {}, duplicated = {}", meme, duplicate);
        if (duplicate) return;
        saver.save(meme, memeShort.image, memeShort.hash, memeShort.url);
    }

    private void makeDecisionMultiple(MetaMeme meme, List<MemeSaver.MemeShort> memeShorts) {
        long count = memeShorts.size();
        long duplicates = memeShorts.stream()
                .filter(iah -> metricSpace.isDuplicate(iah.hash))
                .count();

        double percentage = (double) duplicates / count;
        if (percentage <= bucketDuplicateThreshold)
            saver.saveBatch(meme, memeShorts);
        else
            LOGGER.debug("The meme {}, duplicated = {}", meme, duplicates);
    }

    private static byte[] loadImage(String url) throws IOException {
        LOGGER.info("Load image url = {}", url);
        InputStream stream = new URL(url).openConnection().getInputStream();
        return IOUtils.toByteArray(stream);
    }

    private static BufferedImage convertBytesToImage(byte[] image) throws Exception {
        return ImageIO.read(new ByteArrayInputStream(image));
    }
}
