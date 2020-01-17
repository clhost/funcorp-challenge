package com.clhost.memes.tree.service;

import com.clhost.memes.tree.api.MetaMeme;
import com.clhost.memes.tree.dao.MemesDao;
import com.clhost.memes.tree.dao.data.Bucket;
import com.clhost.memes.tree.dao.data.Data;
import com.clhost.memes.tree.s3.MinioAsyncUploader;
import com.clhost.memes.tree.vptree.MetricSpace;
import com.github.kilianB.hash.Hash;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class MemeSaver {
    private static final Logger LOGGER = LogManager.getLogger(MemeSaver.class);

    @Value("${service.minio.bucket_name}")
    private String minioBucketName;

    @Value("${service.minio.endpoint}")
    private String minioEndpoint;

    @Value("${service.minio.port}")
    private int minioPort;

    private final MemesDao dao;
    private final HashProvider provider;
    private final MetricSpace metricSpace;
    private final MinioAsyncUploader asyncUploader;

    @Autowired
    public MemeSaver(MemesDao dao, HashProvider provider, MetricSpace metricSpace, MinioAsyncUploader asyncUploader) {
        this.dao = dao;
        this.provider = provider;
        this.metricSpace = metricSpace;
        this.asyncUploader = asyncUploader;
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
        save(bucket);
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

        save(bucket);
        metricSpace.putAll(nodes);
    }

    private void save(Bucket bucket) {
        LOGGER.debug("Save bucket: {}", bucket.toString());
        dao.saveBucket(bucket);
        dao.saveData(bucket.getBucketId(), bucket.getImages());
    }

    private String makeUrl(String objectName) {
        return String.format("%s:%d/%s/%s", minioEndpoint, minioPort, minioBucketName, objectName);
    }

    @AllArgsConstructor
    public static class MemeShort {
        public Hash hash;
        public byte[] image;
        public String url;
    }
}
