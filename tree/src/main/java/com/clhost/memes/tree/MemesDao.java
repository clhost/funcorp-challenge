package com.clhost.memes.tree;

import com.clhost.memes.tree.data.CompleteMeme;
import com.clhost.memes.tree.data.VPTreeDaoNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.util.List;

public class MemesDao {
    private static final Logger LOGGER = LogManager.getLogger(MemesDao.class);

    private final NamedParameterJdbcTemplate template;

    public MemesDao(NamedParameterJdbcTemplate template) {
        this.template = template;
    }

    // этот метод в App компонент
    public List<CompleteMeme> lastMemes(long count) {
        LOGGER.debug("Get last {} memes", count);
        String sql = String.format("select * from memes_data order by date desc limit %d", count);
        return template.getJdbcOperations().query(sql, (rs, rowNum) -> CompleteMeme.builder()
                .hash(rs.getString("hash"))
                .bucketId(rs.getString("bucket_id"))
                .source(rs.getString("source"))
                .lang(rs.getString("lang"))
                .date(rs.getTimestamp("date"))
                .url(rs.getString("url"))
                .build());
    }

    public List<VPTreeDaoNode> lastNodes(long count) {
        LOGGER.debug("Get last {} memes", count);
        String sql = String.format("select hash, date from memes_data order by date desc limit %d", count);
        return template.getJdbcOperations().queryForList(sql, VPTreeDaoNode.class);
    }

    public void save(CompleteMeme meme) {
        LOGGER.debug("Save meme: {}", meme.toString());
        String sql =
                "insert into memes_meta(hash, url, source, lang, date, bucket_id) " +
                "values(:hash, :url, :source, :lang, :date, :bucket_id)";
        SqlParameterSource source = new MapSqlParameterSource()
                .addValue("hash", meme.getHash())
                .addValue("url", meme.getUrl())
                .addValue("source", meme.getSource())
                .addValue("lang", meme.getLang())
                .addValue("date", meme.getDate())
                .addValue("bucket_id", meme.getBucketId());
        template.update(sql, source);
    }
}
