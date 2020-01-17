package com.clhost.memes.tree.dao;

import com.clhost.memes.tree.dao.data.Bucket;
import com.clhost.memes.tree.dao.data.Data;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Types;
import java.util.List;

public class MemesDao {
    private static final Logger LOGGER = LogManager.getLogger(MemesDao.class);

    private final NamedParameterJdbcTemplate template;

    public MemesDao(NamedParameterJdbcTemplate template) {
        this.template = template;
    }

    public List<String> lastNodes(long count) {
        String sql = "select hash from memes_data order by pub_date desc limit :limit";
        SqlParameterSource source = new MapSqlParameterSource()
                .addValue("limit", count);
        return template.query(sql, source, (rs, rowNum) -> rs.getString("hash"));
    }

    @Transactional
    public void save(Bucket bucket) {
        LOGGER.debug("Save bucket: {}", bucket.toString());
        System.out.println("Save bucket: " + bucket.toString());
        saveBucket(bucket);
        saveData(bucket.getBucketId(), bucket.getImages());
    }

    private void saveBucket(Bucket meme) {
        String sql =
                "insert into memes_bucket(bucket_id, lang, text, source, pub_date) " +
                "values(:bucket_id, :lang, :text, :source, :pub_date)";
        SqlParameterSource source = new MapSqlParameterSource()
                .addValue("bucket_id", meme.getBucketId(), Types.VARCHAR)
                .addValue("lang", meme.getLang(), Types.VARCHAR)
                .addValue("text", meme.getText(), Types.VARCHAR)
                .addValue("source", meme.getSource(), Types.VARCHAR)
                .addValue("pub_date", meme.getPubDate(), Types.TIMESTAMP);
        template.update(sql, source);
    }

    private void saveData(String bucketId, List<Data> dataList) {
        String sql =
                "insert into memes_data(content_id, bucket_id, url, hash, pub_date) " +
                "values(:content_id, :bucket_id, :url, :hash, :pub_date)";
        for (Data data : dataList) {
            SqlParameterSource source = new MapSqlParameterSource()
                    .addValue("content_id", data.getContentId(), Types.VARCHAR)
                    .addValue("bucket_id", bucketId, Types.VARCHAR)
                    .addValue("url", data.getUrl(), Types.VARCHAR)
                    .addValue("hash", data.getHash(), Types.VARCHAR)
                    .addValue("pub_date", data.getPubDate(), Types.TIMESTAMP);
            template.update(sql, source);
        }
    }
}
