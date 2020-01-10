package com.clhost.memes.redis;

import lombok.Data;

@Data
public class RedisData {
    private long date; // date in epoch millis
    private String id; // id of image
    private String url; // image url on aws s3
    private String perceptiveHash;
    private int width;
    private int height;
}
