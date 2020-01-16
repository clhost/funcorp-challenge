package com.clhost.memes.app.dao;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BucketEntity {
    private String url;
    private String text;
    private String bucketId;
}
