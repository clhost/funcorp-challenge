package com.clhost.memes.tree.dao.data;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.sql.Timestamp;
import java.util.List;

@Data
@Builder
@ToString
public class Bucket {
    private String bucketId;
    private String text;
    private String lang;
    private String source;
    private Timestamp pubDate;
    private List<com.clhost.memes.tree.dao.data.Data> images;
}
