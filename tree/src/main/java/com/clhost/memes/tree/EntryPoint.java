package com.clhost.memes.tree;

import com.clhost.memes.tree.data.CompleteMeme;
import com.clhost.memes.tree.data.MetaMeme;
import com.clhost.memes.tree.vp.VPTreeNode;
import com.clhost.memes.tree.vp.VPTreeService;
import com.github.kilianB.hash.Hash;
import com.github.kilianB.hashAlgorithms.HashingAlgorithm;
import com.github.kilianB.hashAlgorithms.PerceptiveHash;
import io.minio.MinioClient;
import lombok.AllArgsConstructor;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class EntryPoint {
    private static final Logger LOGGER = LogManager.getLogger(EntryPoint.class);

    private final VPTreeService treeService;
    private final MemesDao dao;
    private final MinioClient minioClient;
    private final HashingAlgorithm algorithm;
    private final HashProvider hashProvider;

    @Value("${}")
    public int bitResolution;

    @Value("${}")
    public double bucketMemesThreshold;

    @Value("${}")
    public String minioBucketName;

    @Value("${}")
    public String minioEndpoint;

    @Autowired
    public EntryPoint(VPTreeService treeService, MemesDao dao, MinioClient minioClient, HashProvider hashProvider) {
        this.treeService = treeService;
        this.dao = dao;
        this.minioClient = minioClient;
        this.hashProvider = hashProvider;
        this.algorithm = new PerceptiveHash(bitResolution);
    }

    // single threaded
    public void doIt(MetaMeme meme) {
        try {
            List<ImageAndHash> imageAndHashes = collectImagesAndHashes(meme.getUrls());
            makeDecision(meme, imageAndHashes);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            // как-то обработать
        }
    }

    @Transactional(rollbackFor = Throwable.class)
    public void save(MetaMeme meme, byte[] image, Hash hash) throws Exception {
        String objectName = hashProvider.hash(meme.getUrls());
        Timestamp now = Timestamp.from(OffsetDateTime.now().toInstant());
        saveToMinio(image, objectName);
        CompleteMeme completeMeme = CompleteMeme.builder()
                .bucketId(meme.getBucketId())
                .source(meme.getSource())
                .lang(meme.getLang())
                .date(now)
                .hash(hash.getHashValue().toString())
                .url(makeUrl(objectName))
                .build();
        dao.save(completeMeme);
        treeService.put(new VPTreeNode(hash, now));
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveBatch(MetaMeme meme, List<ImageAndHash> iahs) throws Exception {
        List<VPTreeNode> nodes = new ArrayList<>();
        for (ImageAndHash iah : iahs) {
            String objectName = hashProvider.hash(meme.getUrls());
            Timestamp now = Timestamp.from(OffsetDateTime.now().toInstant());
            saveToMinio(iah.image, objectName);
            CompleteMeme completeMeme = CompleteMeme.builder()
                    .bucketId(meme.getBucketId())
                    .source(meme.getSource())
                    .lang(meme.getLang())
                    .date(now)
                    .hash(iah.hash.getHashValue().toString())
                    .url(makeUrl(objectName))
                    .build();
            dao.save(completeMeme);
            nodes.add(new VPTreeNode(iah.hash, now));
        }
        treeService.putAll(nodes);
    }

    private void saveToMinio(byte[] image, String objectName) throws Exception {
        minioClient.putObject(
                minioBucketName, objectName,
                new ByteArrayInputStream(image), (long) image.length, null, null, null);
    }

    private List<ImageAndHash> collectImagesAndHashes(List<String> urls) throws Exception {
        List<ImageAndHash> imageAndHashes = new ArrayList<>();
        for (String url : urls) {
            byte[] imageBytes = loadImage(url);
            Hash hash = algorithm.hash(convertBytesToImage(imageBytes));
            imageAndHashes.add(new ImageAndHash(hash, imageBytes));
        }
        return imageAndHashes;
    }

    private void makeDecision(MetaMeme meme, List<ImageAndHash> imageAndHashes) throws Exception {
        if (imageAndHashes.isEmpty()) return;
        if (imageAndHashes.size() == 1)
            makeDecisionSingle(meme, imageAndHashes.get(0));
        else
            makeDecisionMultiple(meme, imageAndHashes);
    }

    private void makeDecisionSingle(MetaMeme meme, ImageAndHash imageAndHash) throws Exception {
        boolean duplicate = treeService.isDuplicate(new VPTreeNode(imageAndHash.hash, null)); // think: костыыыыыыыль
        if (duplicate) return;
        save(meme, imageAndHash.image, imageAndHash.hash);
    }

    private void makeDecisionMultiple(MetaMeme meme, List<ImageAndHash> imageAndHashes) throws Exception {
        long count = imageAndHashes.size();
        long duplicates = imageAndHashes.stream()
                .filter(iah -> treeService.isDuplicate(new VPTreeNode(iah.hash, null))) // think: костыыыыыыыль
                .count();
        double percentage = (double) duplicates / count;
        if (percentage <= bucketMemesThreshold) saveBatch(meme, imageAndHashes);
    }

    private byte[] loadImage(String url) throws IOException {
        InputStream stream = new URL(url).openConnection().getInputStream();
        return IOUtils.toByteArray(stream);
    }

    private BufferedImage convertBytesToImage(byte[] image) throws Exception {
        return Imaging.getBufferedImage(image);
    }

    private String makeUrl(String objectName) {
        return String.format("%s/%s/%s", minioEndpoint, minioBucketName, objectName);
    }

    @AllArgsConstructor
    private static class ImageAndHash {
        private Hash hash;
        private byte[] image;
    }
}
