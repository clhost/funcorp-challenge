package com.clhost.memes.app.api.model;

import lombok.Builder;
import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

@Data
@Builder
public class ContentItem {
    private String bucketId;
    private String lang;
    private String source;
    private String text;
    private Timestamp bucketPubDate;
    private List<ContentData> items;
}
