package com.clhost.memes.tree.controller;

import com.clhost.memes.tree.dao.MemesDao;
import com.clhost.memes.tree.data.Bucket;
import com.clhost.memes.tree.data.Data;
import com.clhost.memes.tree.data.MetaMeme;
import com.clhost.memes.tree.vp.VPTreeNode;
import com.clhost.memes.tree.vp.VPTreeService;
import com.github.kilianB.hash.Hash;
import com.github.kilianB.hashAlgorithms.HashingAlgorithm;
import com.github.kilianB.hashAlgorithms.PerceptiveHash;
import io.minio.MinioClient;
import lombok.AllArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class EntryPoint {
    private static final Logger LOGGER = LogManager.getLogger(EntryPoint.class);

    private final VPTreeService treeService;
    private final MemesDao dao;
    private final MinioClient minioClient;
    private final HashingAlgorithm algorithm;
    private final ReentrantLock lock;
    private final HashProvider provider;

    @Value("${service.tree.bucket_duplicate_threshold}")
    private double bucketDuplicateThreshold;

    @Value("${service.minio.bucket_name}")
    private String minioBucketName;

    @Value("${service.minio.endpoint}")
    private String minioEndpoint;

    @Autowired
    public EntryPoint(VPTreeService treeService, MemesDao dao,
                      MinioClient minioClient, @Value("${service.tree.bit_resolution}") int bitResolution, HashProvider provider) {
        this.treeService = treeService;
        this.dao = dao;
        this.minioClient = minioClient;
        this.provider = provider;
        this.lock = new ReentrantLock();
        this.algorithm = new PerceptiveHash(bitResolution);
    }

    // single threaded
    public void doIt(MetaMeme meme) {
        try {
            List<MemeShort> memeShorts = collectMemeShorts(meme.getUrls());
            makeDecision(meme, memeShorts);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            // как-то обработать
        }
    }

    @Transactional(rollbackFor = Throwable.class)
    public void save(MetaMeme meme, byte[] image, Hash hash, String url) throws Exception {
        System.out.println("SINGLE: " + meme + " " + url);

        Timestamp now = Timestamp.from(OffsetDateTime.now().toInstant());
        String bucketId = provider.bucketId(meme.getUrls());
        String contentId = provider.contentId(url);
        Bucket bucket = Bucket.builder()
                .bucketId(bucketId)
                .source(meme.getSource())
                .text(meme.getText())
                .lang(meme.getLang())
                .pubDate(now)
                .images(Collections.singletonList(Data.builder()
                        .contentId(contentId)
                        .hash(hash.getHashValue().toString())
                        .url(makeUrl(contentId))
                        .pubDate(now)
                        .build()))
                .build();
        saveToMinio(image, contentId);
        dao.save(bucket);
        treeService.put(new VPTreeNode(hash, now));
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveBatch(MetaMeme meme, List<MemeShort> iahs) throws Exception {
        System.out.println("BATCH: " + meme + " " + iahs);

        List<Data> list = new ArrayList<>();
        List<VPTreeNode> nodes = new ArrayList<>();

        String bucketId = provider.bucketId(meme.getUrls());
        Timestamp now = Timestamp.from(OffsetDateTime.now().toInstant());

        for (MemeShort iah : iahs) {
            String contentId = provider.contentId(iah.url);
            Data data = Data.builder()
                    .contentId(contentId)
                    .hash(iah.hash.getHashValue().toString())
                    .url(makeUrl(contentId))
                    .pubDate(now)
                    .build();
            saveToMinio(iah.image, contentId);
            list.add(data);
            nodes.add(new VPTreeNode(iah.hash, now));
        }

        Bucket bucket = Bucket.builder()
                .bucketId(bucketId)
                .source(meme.getSource())
                .text(meme.getText())
                .lang(meme.getLang())
                .pubDate(now)
                .images(list)
                .build();

        dao.save(bucket);
        treeService.putAll(nodes);
    }

    private void saveToMinio(byte[] image, String objectName) throws Exception {
        System.out.println("Save " + objectName + " to Minio, with " + Thread.currentThread().getName());
        minioClient.putObject(
                minioBucketName, objectName,
                new ByteArrayInputStream(image), (long) image.length, null, null, null);
    }

    private List<MemeShort> collectMemeShorts(List<String> urls) throws Exception {
        List<MemeShort> memeShorts = new ArrayList<>();
        for (String url : urls) {
            long a = System.currentTimeMillis();
            byte[] imageBytes = loadImage(url);
            System.out.println("Image loading: " + (System.currentTimeMillis() - a));
            Hash hash = algorithm.hash(convertBytesToImage(imageBytes));
            memeShorts.add(new MemeShort(hash, imageBytes, url));
        }
        return memeShorts;
    }

    private void makeDecision(MetaMeme meme, List<MemeShort> memeShorts) throws Exception {
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

    private void makeDecisionSingle(MetaMeme meme, MemeShort memeShort) throws Exception {
        long a = System.currentTimeMillis();
        boolean duplicate = treeService.isDuplicate(new VPTreeNode(memeShort.hash, null)); // think: костыыыыыыыль
        System.out.println("Is duplicate meme (" + (a - System.currentTimeMillis()) + "): " + duplicate);
        if (duplicate) return;
        save(meme, memeShort.image, memeShort.hash, memeShort.url);
    }

    private void makeDecisionMultiple(MetaMeme meme, List<MemeShort> memeShorts) throws Exception {
        long count = memeShorts.size();
        long duplicates = memeShorts.stream()
                .filter(iah -> treeService.isDuplicate(new VPTreeNode(iah.hash, null))) // think: костыыыыыыыль
                .count();
        double percentage = (double) duplicates / count;
        if (percentage <= bucketDuplicateThreshold) saveBatch(meme, memeShorts);
    }

    private static byte[] loadImage(String url) throws IOException {
        InputStream stream = new URL(url).openConnection().getInputStream();
        return IOUtils.toByteArray(stream);
    }

    private static BufferedImage convertBytesToImage(byte[] image) throws Exception {
        return ImageIO.read(new ByteArrayInputStream(image));
    }

    private String makeUrl(String objectName) {
        return String.format("%s/%s/%s", minioEndpoint, minioBucketName, objectName);
    }

    @AllArgsConstructor
    private static class MemeShort {
        private Hash hash;
        private byte[] image;
        private String url;
    }
}
