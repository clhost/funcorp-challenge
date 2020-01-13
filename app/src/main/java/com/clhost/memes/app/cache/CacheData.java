package com.clhost.memes.app.cache;

import lombok.Data;

@Data
public class CacheData {
    private String id; // id of image
    private String url; // image url on aws s3
    private String perceptiveHash;
    private int width;
    private int height;
}
