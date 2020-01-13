package com.clhost.memes.app.cache;

import lombok.Data;

import java.util.List;

@Data
public class CacheBucket {
    private String id;
    private String source;
    private String lang;
    private long date;
    private List<CacheData> data;
}
