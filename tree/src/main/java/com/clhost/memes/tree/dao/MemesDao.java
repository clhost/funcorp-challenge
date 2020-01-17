package com.clhost.memes.tree.dao;

import com.clhost.memes.tree.dao.data.Bucket;
import com.clhost.memes.tree.dao.data.Data;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.sql.Types;
import java.util.List;

public class MemesDao {

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

    public void saveBucket(Bucket meme) {
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

    public void saveData(String bucketId, List<Data> dataList) {
        SqlParameterSource[] sources = new SqlParameterSource[dataList.size()];
        String sql =
                "insert into memes_data(content_id, bucket_id, url, hash, pub_date) " +
                "values(:content_id, :bucket_id, :url, :hash, :pub_date)";
        for (int i = 0; i < dataList.size(); i++) {
            SqlParameterSource source = new MapSqlParameterSource()
                    .addValue("content_id", dataList.get(i).getContentId(), Types.VARCHAR)
                    .addValue("bucket_id", bucketId, Types.VARCHAR)
                    .addValue("url", dataList.get(i).getUrl(), Types.VARCHAR)
                    .addValue("hash", dataList.get(i).getHash(), Types.VARCHAR)
                    .addValue("pub_date", dataList.get(i).getPubDate(), Types.TIMESTAMP);
            sources[i] = source;
        }
        template.batchUpdate(sql, sources);
    }
}
