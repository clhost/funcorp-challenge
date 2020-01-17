package com.clhost.memes.app.dao;

import lombok.Builder;
import lombok.Data;

import java.sql.Timestamp;

@Data
@Builder
public class BucketEntity {
    private String url;
    private String text;
    private String bucketId;
    private Timestamp pubDate;
}
