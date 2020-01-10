package com.clhost.memes.redis;

import lombok.Data;

import java.util.List;

@Data
public class RedisBucket {
    private String id;
    private String source;
    private String lang;
    private List<RedisData> data;
}
