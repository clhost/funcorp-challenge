package com.clhost.memes.app.dao;

import lombok.Builder;
import lombok.Data;

import java.sql.Timestamp;

@Data
@Builder
public class ContentEntity {
    private String bucketId;
    private String lang;
    private String text;
    private String source;
    private Timestamp bucketPubDate;
    private String contentId;
    private String url;
    private Timestamp contentPubDate;
}
