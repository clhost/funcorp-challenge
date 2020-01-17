package com.clhost.memes.tree.service;

import com.clhost.memes.tree.api.MetaMeme;
import com.clhost.memes.tree.dao.MemesDao;
import com.clhost.memes.tree.dao.data.Bucket;
import com.clhost.memes.tree.dao.data.Data;
import com.clhost.memes.tree.s3.MinioAsyncUploader;
import com.clhost.memes.tree.vptree.MetricSpace;
import com.clhost.memes.tree.vptree.VPTreeService;
import com.github.kilianB.hash.Hash;
import com.github.kilianB.hashAlgorithms.HashingAlgorithm;
import com.github.kilianB.hashAlgorithms.PerceptiveHash;
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
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class MemeHandler {
    private static final Logger LOGGER = LogManager.getLogger(MemeHandler.class);

    @Value("${service.tree.bucket_duplicate_threshold}")
    private double bucketDuplicateThreshold;

    @Value("${service.minio.bucket_name}")
    private String minioBucketName;

    @Value("${service.minio.endpoint}")
    private String minioEndpoint;

    @Value("${service.minio.port}")
    private int minioPort;

    private final MetricSpace metricSpace;
    private final MemesDao dao;
    private final HashingAlgorithm algorithm;
    private final ReentrantLock lock;
    private final HashProvider provider;
    private final MinioAsyncUploader asyncUploader;

    @Autowired
    public MemeHandler(VPTreeService metricSpace, MemesDao dao,
                       @Value("${service.tree.bit_resolution}") int bitResolution,
                       HashProvider provider, MinioAsyncUploader asyncUploader) {
        this.metricSpace = metricSpace;
        this.dao = dao;
        this.provider = provider;
        this.asyncUploader = asyncUploader;
        this.lock = new ReentrantLock();
        this.algorithm = new PerceptiveHash(bitResolution);
    }

    public void handleMeme(MetaMeme meme) {
        try {
            List<MemeShort> memeShorts = collectMemeShorts(meme.getUrls());
            makeDecision(meme, memeShorts);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Transactional(rollbackFor = Throwable.class)
    public void save(MetaMeme meme, byte[] image, Hash hash, String url) {
        Timestamp now = Timestamp.from(OffsetDateTime.now().atZoneSameInstant(ZoneId.systemDefault()).toInstant());
        String bucketId = provider.bucketId(meme.getUrls());
        String contentId = provider.contentId(url);
        Bucket bucket = Bucket.builder()
                .bucketId(bucketId)
                .source(meme.getSource())
                .text(meme.getText())
                .lang(meme.getLang())
                .pubDate(now)
                .images(Collections.singletonList(
                        new Data(contentId, hash.getHashValue().toString(), makeUrl(contentId), now)))
                .build();
        asyncUploader.uploadAsync(new MinioAsyncUploader.MinioObject(image, contentId));
        dao.save(bucket);
        metricSpace.put(hash);
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveBatch(MetaMeme meme, List<MemeShort> iahs) {
        List<Data> list = new ArrayList<>();
        List<Hash> nodes = new ArrayList<>();

        String bucketId = provider.bucketId(meme.getUrls());
        Timestamp now = Timestamp.from(OffsetDateTime.now().atZoneSameInstant(ZoneId.systemDefault()).toInstant());

        for (MemeShort iah : iahs) {
            String contentId = provider.contentId(iah.url);
            Data data = new Data(contentId, iah.hash.getHashValue().toString(), makeUrl(contentId), now);

            asyncUploader.uploadAsync(new MinioAsyncUploader.MinioObject(iah.image, contentId));

            list.add(data);
            nodes.add(iah.hash);
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
        metricSpace.putAll(nodes);
    }

    private List<MemeShort> collectMemeShorts(List<String> urls) throws Exception {
        List<MemeShort> memeShorts = new ArrayList<>();
        for (String url : urls) {
            byte[] imageBytes = loadImage(url);
            Hash hash = algorithm.hash(convertBytesToImage(imageBytes));
            memeShorts.add(new MemeShort(hash, imageBytes, url));
        }
        return memeShorts;
    }

    private void makeDecision(MetaMeme meme, List<MemeShort> memeShorts) {
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

    private void makeDecisionSingle(MetaMeme meme, MemeShort memeShort) {
        boolean duplicate = metricSpace.isDuplicate(memeShort.hash);
        LOGGER.debug("The meme {}, duplicated = {}", meme, duplicate);
        if (duplicate) return;
        save(meme, memeShort.image, memeShort.hash, memeShort.url);
    }

    private void makeDecisionMultiple(MetaMeme meme, List<MemeShort> memeShorts) {
        long count = memeShorts.size();
        long duplicates = memeShorts.stream()
                .filter(iah -> metricSpace.isDuplicate(iah.hash))
                .count();

        double percentage = (double) duplicates / count;
        if (percentage <= bucketDuplicateThreshold)
            saveBatch(meme, memeShorts);
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

    private String makeUrl(String objectName) {
        return String.format("%s:%d/%s/%s", minioEndpoint, minioPort, minioBucketName, objectName);
    }

    @AllArgsConstructor
    private static class MemeShort {
        private Hash hash;
        private byte[] image;
        private String url;
    }
}
