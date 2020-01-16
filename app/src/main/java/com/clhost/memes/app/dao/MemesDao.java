package com.clhost.memes.app.dao;

import com.clhost.memes.app.controller.ContentData;
import com.clhost.memes.app.controller.ContentItem;
import com.clhost.memes.app.controller.PreviewItem;
import lombok.Builder;
import lombok.Data;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MemesDao {
    private static final Logger LOGGER = LogManager.getLogger(MemesDao.class);

    private final NamedParameterJdbcTemplate template;

    public MemesDao(NamedParameterJdbcTemplate template) {
        this.template = template;
    }

    public List<PreviewItem> memesPage(long count, long offset, String lang) {
        String sql =
                "select m.bucket_id, b.text, m.url\n " +
                "from \n" +
                "(select bucket_id, lang, text from memes_bucket where lang = :lang " +
                "        order by pub_date limit :limit offset :offset) b\n " +
                "        join memes_data m on m.bucket_id = b.bucket_id";
        SqlParameterSource source = new MapSqlParameterSource()
                .addValue("lang", lang)
                .addValue("limit", count)
                .addValue("offset", offset);
        List<BucketEntity> bucketEntities = template.query(sql, source, (rs, rowNum) -> BucketEntity.builder()
                .bucketId(rs.getString("bucket_id"))
                .url(rs.getString("url"))
                .text(rs.getString("text"))
                .build());
        return mapToPreviewItems(bucketEntities);
    }

    public ContentItem contentItemInfo(String bucketId) {
        String sql =
                "select mb.bucket_id, mb.lang, mb.text, mb.source, mb.pub_date bucket_pub_date, \n" +
                "       md.content_id, md.url, md.pub_date content_pub_date\n " +
                "from memes_bucket mb\n " +
                "join memes_data md on mb.bucket_id = md.bucket_id\n " +
                "where mb.bucket_id = :bucket_id";
        SqlParameterSource source = new MapSqlParameterSource()
                .addValue("bucket_id", bucketId);
        List<ContentEntity> contents = template.query(sql, source, (rs, rowNum) -> ContentEntity.builder()
                .bucketId(rs.getString("bucket_id"))
                .lang(rs.getString("lang"))
                .text(rs.getString("text"))
                .source(rs.getString("source"))
                .bucketPubDate(rs.getTimestamp("bucket_pub_date"))
                .contentId(rs.getString("content_id"))
                .url(rs.getString("url"))
                .contentPubDate(rs.getTimestamp("content_pub_date"))
                .build());
        return mapToContentItem(contents);
    }

    private ContentItem mapToContentItem(List<ContentEntity> contents) {
        if (contents == null) return ContentItem.builder().build();
        return contents.stream()
                .collect(
                        Collectors.groupingBy(e -> ContentItemKey.builder()
                                        .bucketId(e.getBucketId())
                                        .lang(e.getLang())
                                        .text(e.getText())
                                        .source(e.getSource())
                                        .bucketPubDate(e.getBucketPubDate())
                                        .build(),
                                Collectors.mapping(e -> ContentData.builder()
                                        .contentId(e.getContentId())
                                        .url(e.getUrl())
                                        .contentPubDate(e.getContentPubDate())
                                        .build(), Collectors.toList())))
                .entrySet().stream()
                .map(entry -> ContentItem.builder()
                        .bucketId(entry.getKey().getBucketId())
                        .source(entry.getKey().getSource())
                        .lang(entry.getKey().getLang())
                        .text(entry.getKey().getText())
                        .bucketPubDate(entry.getKey().getBucketPubDate())
                        .items(entry.getValue())
                        .build())
                .findFirst().orElse(ContentItem.builder().build());
    }

    private List<PreviewItem> mapToPreviewItems(List<BucketEntity> bucketUrls) {
        if (bucketUrls == null) return Collections.emptyList();
        return bucketUrls.stream()
                .collect(Collectors.groupingBy(
                        e -> PreviewItemKey.builder().bucketId(e.getBucketId()).text(e.getText()).build(),
                        Collectors.mapping(BucketEntity::getUrl, Collectors.toList())))
                .entrySet().stream()
                .map(entry -> PreviewItem.builder()
                        .id(entry.getKey().getBucketId())
                        .text(entry.getKey().getText())
                        .urls(entry.getValue()).build())
                .collect(Collectors.toList());
    }

    @Data
    @Builder
    private static class PreviewItemKey {
        private String bucketId;
        private String text;
    }

    @Data
    @Builder
    private static class ContentItemKey {
        private String bucketId;
        private String lang;
        private String source;
        private String text;
        private Timestamp bucketPubDate;
    }
}
